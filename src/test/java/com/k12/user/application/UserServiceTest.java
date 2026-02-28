package com.k12.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.k12.user.domain.models.User;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.ParentRepository;
import com.k12.user.domain.ports.out.StudentRepository;
import com.k12.user.domain.ports.out.TeacherRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.UserResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private UserRepository userRepository;
    private TeacherRepository teacherRepository;
    private ParentRepository parentRepository;
    private StudentRepository studentRepository;
    private AdminRepository adminRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        teacherRepository = mock(TeacherRepository.class);
        parentRepository = mock(ParentRepository.class);
        studentRepository = mock(StudentRepository.class);
        adminRepository = mock(AdminRepository.class);
        userService = new UserService(
                userRepository, teacherRepository, parentRepository, studentRepository, adminRepository);
    }

    @Test
    void createUser_withTeacherRole_shouldSucceed() {
        // Arrange
        var request = new CreateUserRequest(
                "teacher@example.com",
                "password123",
                "John Teacher",
                new CreateUserRequest.UserRole("TEACHER"),
                new CreateUserRequest.TeacherData("EMP001", "Mathematics", "2020-08-15"),
                null,
                null);

        when(userRepository.findByEmailAddress("teacher@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = userService.createUser(request);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("teacher@example.com", response.email());
        assertEquals("John Teacher", response.name());
        assertEquals("TEACHER", response.role());
        assertEquals("EMP001", response.teacher().employeeId());
        assertEquals("Mathematics", response.teacher().department());
        assertEquals("2020-08-15", response.teacher().hireDate());

        verify(userRepository).findByEmailAddress("teacher@example.com");
        verify(userRepository).save(any(User.class));
        verify(teacherRepository).save(any());
    }

    @Test
    void createUser_withDuplicateEmail_shouldFail() {
        // Arrange
        var request = new CreateUserRequest(
                "existing@example.com",
                "password123",
                "John Doe",
                new CreateUserRequest.UserRole("TEACHER"),
                new CreateUserRequest.TeacherData("EMP001", "Mathematics", "2020-08-15"),
                null,
                null);

        var existingUser = mock(User.class);
        when(userRepository.findByEmailAddress("existing@example.com")).thenReturn(Optional.of(existingUser));

        // Act
        var result = userService.createUser(request);

        // Assert
        assertTrue(result.isFailure());
        assertEquals(UserError.ConflictError.EMAIL_ALREADY_EXISTS, result.getError());

        verify(userRepository).findByEmailAddress("existing@example.com");
        verify(userRepository, never()).save(any());
        verify(teacherRepository, never()).save(any());
    }
}
