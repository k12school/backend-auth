package com.k12.user.application;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.ParentRepository;
import com.k12.user.domain.ports.out.StudentRepository;
import com.k12.user.domain.ports.out.TeacherRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.rest.dto.ChangeRoleRequest;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.UpdateUserRequest;
import com.k12.user.infrastructure.rest.dto.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;

    public Result<UserResponse, UserError> createUser(CreateUserRequest request) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }

    public Result<UserResponse, UserError> getUserById(UserId id) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }

    public Result<List<UserResponse>, UserError> listUsers(UserRole role, TenantId tenantId, UserStatus status) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }

    public Result<UserResponse, UserError> updateUserFields(UserId id, UpdateUserRequest request) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }

    public Result<UserResponse, UserError> changeUserRole(UserId id, ChangeRoleRequest request) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }

    public Result<Void, UserError> softDeleteUser(UserId id) {
        // TODO: Implement
        return Result.failure(UserError.PersistenceError.STORAGE_ERROR);
    }
}
