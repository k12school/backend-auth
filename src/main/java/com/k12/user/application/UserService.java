package com.k12.user.application;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.ports.out.AdminRepository;
import com.k12.user.domain.ports.out.ParentRepository;
import com.k12.user.domain.ports.out.StudentRepository;
import com.k12.user.domain.ports.out.TeacherRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.persistence.TransactionalContext;
import com.k12.user.infrastructure.rest.dto.ChangeRoleRequest;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import com.k12.user.infrastructure.rest.dto.UpdateUserRequest;
import com.k12.user.infrastructure.rest.dto.UserResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;

@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

    /**
     * Default tenant ID used for user creation.
     * TODO: Extract from JWT token in production when implementing multi-tenant authentication
     */
    private static final String DEFAULT_TENANT_ID = "00000000-0000-0000-0000-000000000001";

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final AdminRepository adminRepository;
    private final TransactionalContext transactionalContext;

    @Transactional
    public Result<UserResponse, UserError> createUser(CreateUserRequest request) {
        // Get shared transaction context
        DSLContext ctx = transactionalContext.getContext();

        // Check email uniqueness
        var existingUser = userRepository.findByEmailAddress(request.email());
        if (existingUser.isPresent()) {
            return Result.failure(UserError.ConflictError.EMAIL_ALREADY_EXISTS);
        }

        // Hash password
        String passwordHash = com.k12.user.infrastructure.security.PasswordHasher.hash(request.password())
                .value();

        // Create base User via UserFactory
        var tenantId = new TenantId(DEFAULT_TENANT_ID);
        var userId = UserId.generate();

        var userResult = com.k12.user.domain.models.UserFactory.create(
                com.k12.user.domain.models.EmailAddress.of(request.email()),
                new com.k12.user.domain.models.PasswordHash(passwordHash),
                java.util.Set.of(com.k12.user.domain.models.UserRole.valueOf(
                        request.role().value())),
                com.k12.user.domain.models.UserName.of(request.name()),
                tenantId);

        if (userResult.isFailure()) {
            return Result.failure(userResult.getError());
        }

        var userCreatedEvent = userResult.get();
        var user = com.k12.user.domain.models.UserReconstructor.applyEvent(null, userCreatedEvent);

        // Save user FIRST (required for foreign key constraints)
        userRepository.save(user, ctx);

        // Create specialization based on role (user must exist first)
        switch (request.role().value()) {
            case "TEACHER" -> createTeacher(userId, request.teacherData(), ctx);
            case "PARENT" -> createParent(userId, request.parentData(), ctx);
            case "STUDENT" -> createStudent(userId, request.studentData(), ctx);
            case "ADMIN" -> createAdmin(userId, ctx);
        }

        // Build response
        return Result.success(buildUserResponse(user, request));
    }

    private void createTeacher(UserId userId, CreateUserRequest.TeacherData data, DSLContext ctx) {
        var teacher = com.k12.user.domain.models.specialization.teacher.TeacherFactory.create(
                userId, data.employeeId(), data.department(), java.time.LocalDate.parse(data.hireDate()));
        teacherRepository.save(teacher, ctx);
    }

    private void createParent(UserId userId, CreateUserRequest.ParentData data, DSLContext ctx) {
        var parent = com.k12.user.domain.models.specialization.parent.ParentFactory.create(
                userId, data.phoneNumber(), data.address(), data.emergencyContact());
        parentRepository.save(parent, ctx);
    }

    private void createStudent(UserId userId, CreateUserRequest.StudentData data, DSLContext ctx) {
        var guardianId = data.guardianId() != null
                ? new com.k12.user.domain.models.specialization.parent.ParentId(
                        new com.k12.common.domain.model.UserId(java.util.UUID.fromString(data.guardianId())))
                : null;

        var student = com.k12.user.domain.models.specialization.student.StudentFactory.create(
                userId,
                data.studentNumber(),
                data.gradeLevel(),
                java.time.LocalDate.parse(data.dateOfBirth()),
                guardianId);
        studentRepository.save(student, ctx);
    }

    private void createAdmin(UserId userId, DSLContext ctx) {
        var admin = com.k12.user.domain.models.specialization.admin.AdminFactory.create(
                new com.k12.user.domain.models.specialization.admin.AdminId(userId),
                java.util.Set.of(
                        com.k12.user.domain.models.specialization.admin.valueobjects.Permission.USER_MANAGEMENT));
        adminRepository.save(admin, ctx);
    }

    private UserResponse buildUserResponse(User user, CreateUserRequest request) {
        return new UserResponse(
                user.userId().value().toString(),
                user.emailAddress().value(),
                user.name().value(),
                user.userRole().iterator().next().name(),
                user.tenantId().value().toString(),
                user.status().name(),
                java.time.Instant.now(), // createdAt - will be set by repository
                request.teacherData() != null
                        ? new UserResponse.TeacherData(
                                request.teacherData().employeeId(),
                                request.teacherData().department(),
                                request.teacherData().hireDate())
                        : null,
                request.parentData() != null
                        ? new UserResponse.ParentData(
                                request.parentData().phoneNumber(),
                                request.parentData().address(),
                                request.parentData().emergencyContact())
                        : null,
                request.studentData() != null
                        ? new UserResponse.StudentData(
                                request.studentData().studentNumber(),
                                request.studentData().gradeLevel(),
                                request.studentData().dateOfBirth(),
                                request.studentData().guardianId())
                        : null);
    }

    public Result<UserResponse, UserError> getUserById(UserId id) {
        var userResult = userRepository.findById(id);
        if (userResult.isEmpty()) {
            return Result.failure(UserError.NotFoundError.USER_NOT_FOUND);
        }

        var user = userResult.get();
        var role = user.userRole().iterator().next();

        // Load specialization data
        var response = new UserResponse(
                user.userId().value().toString(),
                user.emailAddress().value(),
                user.name().value(),
                role.name(),
                user.tenantId().value(),
                user.status().name(),
                java.time.Instant.now(),
                null,
                null,
                null);

        switch (role.name()) {
            case "TEACHER" -> {
                var teacherResult = teacherRepository.findByUserId(id);
                if (teacherResult.isPresent()) {
                    var teacher = teacherResult.get();
                    response = new UserResponse(
                            response.userId(),
                            response.email(),
                            response.name(),
                            response.role(),
                            response.tenantId(),
                            response.status(),
                            response.createdAt(),
                            new UserResponse.TeacherData(
                                    teacher.employeeId(),
                                    teacher.department(),
                                    teacher.hireDate().toString()),
                            null,
                            null);
                }
            }
            case "PARENT" -> {
                var parentResult = parentRepository.findByUserId(id);
                if (parentResult.isPresent()) {
                    var parent = parentResult.get();
                    response = new UserResponse(
                            response.userId(),
                            response.email(),
                            response.name(),
                            response.role(),
                            response.tenantId(),
                            response.status(),
                            response.createdAt(),
                            null,
                            new UserResponse.ParentData(
                                    parent.phoneNumber(), parent.address(), parent.emergencyContact()),
                            null);
                }
            }
            case "STUDENT" -> {
                var studentResult = studentRepository.findByUserId(id);
                if (studentResult.isPresent()) {
                    var student = studentResult.get();
                    response = new UserResponse(
                            response.userId(),
                            response.email(),
                            response.name(),
                            response.role(),
                            response.tenantId(),
                            response.status(),
                            response.createdAt(),
                            null,
                            null,
                            new UserResponse.StudentData(
                                    student.studentNumber(),
                                    student.gradeLevel(),
                                    student.dateOfBirth().toString(),
                                    student.guardianId() != null
                                            ? student.guardianId()
                                                    .value()
                                                    .value()
                                                    .toString()
                                            : null));
                }
            }
        }

        return Result.success(response);
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
        var userResult = userRepository.findById(id);
        if (userResult.isEmpty()) {
            return Result.failure(UserError.NotFoundError.USER_NOT_FOUND);
        }

        var user = userResult.get();

        // Soft delete by updating status to DELETED
        var deletedUser = user.withStatus(com.k12.user.domain.models.UserStatus.DELETED);
        userRepository.save(deletedUser);

        return Result.success(null);
    }
}
