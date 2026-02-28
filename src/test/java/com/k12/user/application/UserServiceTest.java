package com.k12.user.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.ParentRepository;
import com.k12.user.domain.ports.out.StudentRepository;
import com.k12.user.domain.ports.out.TeacherRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.UserResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
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

    @Test
    void createUser_withParentRole_shouldSucceed() {
        // Arrange
        var request = new CreateUserRequest(
                "parent@example.com",
                "password123",
                "Jane Parent",
                new CreateUserRequest.UserRole("PARENT"),
                null,
                new CreateUserRequest.ParentData("+1234567890", "123 Main St", "Emergency Contact"),
                null);

        when(userRepository.findByEmailAddress("parent@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = userService.createUser(request);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("parent@example.com", response.email());
        assertEquals("Jane Parent", response.name());
        assertEquals("PARENT", response.role());
        assertEquals("+1234567890", response.parent().phoneNumber());
        assertEquals("123 Main St", response.parent().address());
        assertEquals("Emergency Contact", response.parent().emergencyContact());

        verify(userRepository).findByEmailAddress("parent@example.com");
        verify(userRepository).save(any(User.class));
        verify(parentRepository).save(any());
    }

    @Test
    void createUser_withStudentRole_shouldSucceed() {
        // Arrange
        var request = new CreateUserRequest(
                "student@example.com",
                "password123",
                "John Student",
                new CreateUserRequest.UserRole("STUDENT"),
                null,
                null,
                new CreateUserRequest.StudentData("STU001", "GRADE_10", "2010-05-15", null));

        when(userRepository.findByEmailAddress("student@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = userService.createUser(request);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("student@example.com", response.email());
        assertEquals("John Student", response.name());
        assertEquals("STUDENT", response.role());
        assertEquals("STU001", response.student().studentNumber());
        assertEquals("GRADE_10", response.student().gradeLevel());
        assertEquals("2010-05-15", response.student().dateOfBirth());
        assertNull(response.student().guardianId());

        verify(userRepository).findByEmailAddress("student@example.com");
        verify(userRepository).save(any(User.class));
        verify(studentRepository).save(any());
    }

    @Test
    void createUser_withAdminRole_shouldSucceed() {
        // Arrange
        var request = new CreateUserRequest(
                "admin@example.com",
                "password123",
                "System Admin",
                new CreateUserRequest.UserRole("ADMIN"),
                null,
                null,
                null);

        when(userRepository.findByEmailAddress("admin@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        var result = userService.createUser(request);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("admin@example.com", response.email());
        assertEquals("System Admin", response.name());
        assertEquals("ADMIN", response.role());
        // Admin specialization data is not returned in UserResponse

        verify(userRepository).findByEmailAddress("admin@example.com");
        verify(userRepository).save(any(User.class));
        verify(adminRepository).save(any());
    }

    @Test
    void getUserById_withTeacherRole_shouldReturnTeacherWithSpecialization() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("teacher@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Teacher"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.TEACHER));

        var teacherId = new com.k12.user.domain.models.specialization.teacher.TeacherId(userId);
        var teacher = new com.k12.user.domain.models.specialization.teacher.Teacher(
                teacherId, "EMP001", "Mathematics", LocalDate.of(2020, 8, 15), Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(teacherRepository.findByUserId(userId)).thenReturn(Optional.of(teacher));

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("teacher@example.com", response.email());
        assertEquals("John Teacher", response.name());
        assertEquals("TEACHER", response.role());
        assertEquals("EMP001", response.teacher().employeeId());
        assertEquals("Mathematics", response.teacher().department());
        assertEquals("2020-08-15", response.teacher().hireDate());

        verify(userRepository).findById(userId);
        verify(teacherRepository).findByUserId(userId);
    }

    @Test
    void getUserById_withParentRole_shouldReturnParentWithSpecialization() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("parent@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("Jane Parent"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.PARENT));

        var parentId = new com.k12.user.domain.models.specialization.parent.ParentId(userId);
        var parent = new com.k12.user.domain.models.specialization.parent.Parent(
                parentId, "+1234567890", "123 Main St", "Emergency Contact", Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(parentRepository.findByUserId(userId)).thenReturn(Optional.of(parent));

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("parent@example.com", response.email());
        assertEquals("Jane Parent", response.name());
        assertEquals("PARENT", response.role());
        assertEquals("+1234567890", response.parent().phoneNumber());
        assertEquals("123 Main St", response.parent().address());
        assertEquals("Emergency Contact", response.parent().emergencyContact());

        verify(userRepository).findById(userId);
        verify(parentRepository).findByUserId(userId);
    }

    @Test
    void getUserById_withStudentRole_shouldReturnStudentWithSpecialization() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();
        var guardianId = new UserId(java.util.UUID.randomUUID());

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("student@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Student"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.STUDENT));

        var studentId = new com.k12.user.domain.models.specialization.student.StudentId(userId);
        var parentId = new ParentId(guardianId);
        var student = new com.k12.user.domain.models.specialization.student.Student(
                studentId, "STU001", "GRADE_10", LocalDate.of(2010, 5, 15), parentId, Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("student@example.com", response.email());
        assertEquals("John Student", response.name());
        assertEquals("STUDENT", response.role());
        assertEquals("STU001", response.student().studentNumber());
        assertEquals("GRADE_10", response.student().gradeLevel());
        assertEquals("2010-05-15", response.student().dateOfBirth());
        assertEquals(guardianId.value().toString(), response.student().guardianId());

        verify(userRepository).findById(userId);
        verify(studentRepository).findByUserId(userId);
    }

    @Test
    void getUserById_withAdminRole_shouldReturnAdminWithoutSpecialization() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("admin@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("System Admin"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.ADMIN));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("admin@example.com", response.email());
        assertEquals("System Admin", response.name());
        assertEquals("ADMIN", response.role());
        assertNull(response.teacher());
        assertNull(response.parent());
        assertNull(response.student());

        verify(userRepository).findById(userId);
        verifyNoInteractions(teacherRepository);
        verifyNoInteractions(parentRepository);
        verifyNoInteractions(studentRepository);
    }

    @Test
    void getUserById_whenUserNotFound_shouldReturnNotFoundError() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isFailure());
        assertEquals(UserError.NotFoundError.USER_NOT_FOUND, result.getError());

        verify(userRepository).findById(userId);
        verifyNoInteractions(teacherRepository);
        verifyNoInteractions(parentRepository);
        verifyNoInteractions(studentRepository);
    }

    @Test
    void getUserById_whenStudentHasNoGuardian_shouldReturnStudentWithNullGuardian() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("student@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Student"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.STUDENT));

        var studentId = new com.k12.user.domain.models.specialization.student.StudentId(userId);
        var student = new com.k12.user.domain.models.specialization.student.Student(
                studentId, "STU002", "GRADE_11", LocalDate.of(2009, 3, 20), null, Instant.now());

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(studentRepository.findByUserId(userId)).thenReturn(Optional.of(student));

        // Act
        var result = userService.getUserById(userId);

        // Assert
        assertTrue(result.isSuccess());
        UserResponse response = result.get();
        assertEquals("student@example.com", response.email());
        assertEquals("STUDENT", response.role());
        assertEquals("STU002", response.student().studentNumber());
        assertNull(response.student().guardianId()); // Guardian ID should be null

        verify(userRepository).findById(userId);
        verify(studentRepository).findByUserId(userId);
    }

    @Test
    void softDeleteUser_whenUserExists_shouldChangeStatusToDeleted() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Doe"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.TEACHER));

        var deletedUser = mock(User.class);
        when(deletedUser.userId()).thenReturn(userId);
        when(deletedUser.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(deletedUser.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Doe"));
        when(deletedUser.tenantId()).thenReturn(tenantId);
        when(deletedUser.status()).thenReturn(com.k12.user.domain.models.UserStatus.DELETED);
        when(deletedUser.userRole()).thenReturn(Set.of(UserRole.TEACHER));

        when(user.withStatus(com.k12.user.domain.models.UserStatus.DELETED)).thenReturn(deletedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(deletedUser)).thenReturn(deletedUser);

        // Act
        var result = userService.softDeleteUser(userId);

        // Assert
        assertTrue(result.isSuccess());
        assertNull(result.get()); // Void result

        verify(userRepository).findById(userId);
        verify(user).withStatus(com.k12.user.domain.models.UserStatus.DELETED);
        verify(userRepository).save(deletedUser);
    }

    @Test
    void softDeleteUser_whenUserNotFound_shouldReturnNotFoundError() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act
        var result = userService.softDeleteUser(userId);

        // Assert
        assertTrue(result.isFailure());
        assertEquals(UserError.NotFoundError.USER_NOT_FOUND, result.getError());

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    @Test
    void softDeleteUser_whenUserIsSuspended_shouldChangeStatusToDeleted() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var suspendedUser = mock(User.class);
        when(suspendedUser.userId()).thenReturn(userId);
        when(suspendedUser.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(suspendedUser.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Doe"));
        when(suspendedUser.tenantId()).thenReturn(tenantId);
        when(suspendedUser.status()).thenReturn(com.k12.user.domain.models.UserStatus.SUSPENDED);
        when(suspendedUser.userRole()).thenReturn(Set.of(UserRole.TEACHER));

        var deletedUser = mock(User.class);
        when(deletedUser.userId()).thenReturn(userId);
        when(deletedUser.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(deletedUser.name()).thenReturn(com.k12.user.domain.models.UserName.of("John Doe"));
        when(deletedUser.tenantId()).thenReturn(tenantId);
        when(deletedUser.status()).thenReturn(com.k12.user.domain.models.UserStatus.DELETED);
        when(deletedUser.userRole()).thenReturn(Set.of(UserRole.TEACHER));

        when(suspendedUser.withStatus(com.k12.user.domain.models.UserStatus.DELETED))
                .thenReturn(deletedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(suspendedUser));
        when(userRepository.save(deletedUser)).thenReturn(deletedUser);

        // Act
        var result = userService.softDeleteUser(userId);

        // Assert
        assertTrue(result.isSuccess());
        assertNull(result.get()); // Void result

        verify(userRepository).findById(userId);
        verify(suspendedUser).withStatus(com.k12.user.domain.models.UserStatus.DELETED);
        verify(userRepository).save(deletedUser);
    }

    @Test
    void softDeleteUser_preservesUserDataInDatabase() {
        // Arrange
        var userId = new UserId(java.util.UUID.randomUUID());
        var tenantId = TenantId.generate();

        var user = mock(User.class);
        when(user.userId()).thenReturn(userId);
        when(user.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(user.name()).thenReturn(com.k12.user.domain.models.UserName.of("Jane Smith"));
        when(user.tenantId()).thenReturn(tenantId);
        when(user.status()).thenReturn(com.k12.user.domain.models.UserStatus.ACTIVE);
        when(user.userRole()).thenReturn(Set.of(UserRole.PARENT));

        var deletedUser = mock(User.class);
        when(deletedUser.userId()).thenReturn(userId);
        when(deletedUser.emailAddress()).thenReturn(com.k12.user.domain.models.EmailAddress.of("user@example.com"));
        when(deletedUser.name()).thenReturn(com.k12.user.domain.models.UserName.of("Jane Smith"));
        when(deletedUser.tenantId()).thenReturn(tenantId);
        when(deletedUser.status()).thenReturn(com.k12.user.domain.models.UserStatus.DELETED);
        when(deletedUser.userRole()).thenReturn(Set.of(UserRole.PARENT));

        when(user.withStatus(com.k12.user.domain.models.UserStatus.DELETED)).thenReturn(deletedUser);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(deletedUser)).thenReturn(deletedUser);

        // Act
        var result = userService.softDeleteUser(userId);

        // Assert
        assertTrue(result.isSuccess());

        // Verify that withStatus was called, which means the user object was modified
        // but not removed from the database (soft delete)
        verify(user).withStatus(com.k12.user.domain.models.UserStatus.DELETED);
        verify(userRepository).save(deletedUser);

        // Verify that the deleted user still has the same ID, email, and name
        assertEquals(userId, deletedUser.userId());
    }
}
