# Users Management System Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement complete user management system with tenant associations and role-based specializations (Teacher, Parent, Student)

**Architecture:** Tenant-scoped multi-specialization pattern where SUPER_ADMIN creates tenants/tenant admins, and tenant ADMINs create users (TEACHER, PARENT, STUDENT) for their tenant. Users are automatically assigned to the tenant from JWT.

**Tech Stack:** Quarkus, JAX-RS, jOOQ, PostgreSQL, JWT security, Event Sourcing

---

## Task 1: Update User Domain Model with Tenant Association

**Files:**
- Modify: `src/main/java/com/k12/user/domain/models/User.java`
- Modify: `src/main/java/com/k12/user/domain/models/UserFactory.java`
- Test: `src/test/java/com/k12/user/domain/models/UserTest.java`

**Step 1: Update User record to include tenantId**

```java
// In User.java
package com.k12.user.domain.models;

import com.k12.common.domain.model.TenantId;
import com.k12.user.domain.models.specialization.admin.valueobjects.Permission;
import java.util.Set;

public record User(
        UserId userId,
        @With EmailAddress emailAddress,
        @With PasswordHash passwordHash,
        @With Set<UserRole> userRole,
        @With UserStatus status,
        @With UserName name,
        @With TenantId tenantId) {  // NEW: Tenant association

    // Existing methods remain unchanged
}
```

**Step 2: Update UserFactory to support tenantId**

```java
// In UserFactory.java, add new create method

public static Result<UserEvents, UserError> create(
        EmailAddress emailAddress,
        PasswordHash passwordHash,
        Set<UserRole> roles,
        UserName name,
        TenantId tenantId) {  // NEW parameter

    if (roles == null || roles.isEmpty()) {
        return Result.failure(ROLES_CANNOT_BE_EMPTY);
    }

    UserId userId = UserId.generate();
    java.time.Instant now = java.time.Instant.now();
    long version = 1L;

    return Result.success(new UserEvents.UserCreated(
            userId, emailAddress, passwordHash, roles, UserStatus.ACTIVE, name, tenantId, now, version));
}
```

**Step 3: Update UserCreated event to include tenantId**

```java
// In UserEvents.java, update UserCreated record
record UserCreated(
        UserId userId,
        EmailAddress email,
        PasswordHash passwordHash,
        Set<UserRole> roles,
        UserStatus status,
        UserName name,
        TenantId tenantId,  // NEW
        Instant createdAt,
        long version) implements UserEvents {}
```

**Step 4: Write test for user with tenant**

```java
// In UserTest.java
@Test
public void testUserCreationWithTenant() {
    TenantId tenantId = TenantId.of(UUID.randomUUID());
    User user = new User(
        UserId.generate(),
        EmailAddress.of("test@example.com"),
        PasswordHash.of("hashed"),
        Set.of(UserRole.USER),
        UserStatus.ACTIVE,
        UserName.of("Test User"),
        tenantId
    );

    assertEquals(tenantId, user.tenantId());
}
```

**Step 5: Run tests**

```bash
./gradlew test --tests UserTest
```

Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/models/User.java \
        src/main/java/com/k12/user/domain/models/UserFactory.java \
        src/main/java/com/k12/user/domain/models/events/UserEvents.java \
        src/test/java/com/k12/user/domain/models/UserTest.java
git commit -m "feat: add tenant association to User domain model"
```

---

## Task 2: Create Teacher Specialization Domain Model

**Files:**
- Create: `src/main/java/com/k12/user/domain/models/specialization/teacher/Teacher.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/teacher/TeacherId.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/teacher/TeacherFactory.java`
- Test: `src/test/java/com/k12/user/domain/models/specialization/teacher/TeacherTest.java`

**Step 1: Create TeacherId value object**

```java
package com.k12.user.domain.models.specialization.teacher;

import com.k12.common.domain.model.UserId;

public record TeacherId(UserId value) {
    public TeacherId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public UUID id() {
        return value.value();
    }
}
```

**Step 2: Create Teacher aggregate**

```java
package com.k12.user.domain.models.specialization.teacher;

import java.time.Instant;
import java.time.LocalDate;

public record Teacher(
        TeacherId teacherId,
        String employeeId,
        String department,
        LocalDate hireDate,
        Instant createdAt) {

    public Teacher {
        if (teacherId == null) {
            throw new IllegalArgumentException("TeacherId cannot be null");
        }
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID cannot be null or blank");
        }
        if (hireDate == null) {
            hireDate = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

**Step 3: Create TeacherFactory**

```java
package com.k12.user.domain.models.specialization.teacher;

import com.k12.common.domain.model.UserId;
import java.time.LocalDate;
import java.time.Instant;

public final class TeacherFactory {

    public static Teacher create(
            UserId userId,
            String employeeId,
            String department,
            LocalDate hireDate) {

        return new Teacher(
            TeacherId.of(userId),
            employeeId,
            department,
            hireDate,
            Instant.now()
        );
    }
}
```

**Step 4: Write test**

```java
package com.k12.user.domain.models.specialization.teacher;

import com.k12.common.domain.model.UserId;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class TeacherTest {

    @Test
    void testTeacherCreation() {
        UserId userId = UserId.generate();
        Teacher teacher = TeacherFactory.create(
            userId,
            "EMP001",
            "Mathematics",
            LocalDate.of(2024, 1, 15)
        );

        assertEquals(userId, teacher.teacherId().value());
        assertEquals("EMP001", teacher.employeeId());
        assertEquals("Mathematics", teacher.department());
    }

    @Test
    void testTeacherValidation_EmptyEmployeeId() {
        assertThrows(IllegalArgumentException.class, () ->
            TeacherFactory.create(
                UserId.generate(),
                "",  // invalid
                "Mathematics",
                LocalDate.now()
            )
        );
    }
}
```

**Step 5: Run tests**

```bash
./gradlew test --tests TeacherTest
```

Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/models/specialization/teacher/
git commit -m "feat: add Teacher specialization domain model"
```

---

## Task 3: Create Parent Specialization Domain Model

**Files:**
- Create: `src/main/java/com/k12/user/domain/models/specialization/parent/Parent.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/parent/ParentId.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/parent/ParentFactory.java`
- Test: `src/test/java/com/k12/user/domain/models/specialization/parent/ParentTest.java`

**Step 1: Create ParentId value object**

```java
package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;

public record ParentId(UserId value) {
    public ParentId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public UUID id() {
        return value.value();
    }
}
```

**Step 2: Create Parent aggregate**

```java
package com.k12.user.domain.models.specialization.parent;

import java.time.Instant;

public record Parent(
        ParentId parentId,
        String phoneNumber,
        String address,
        String emergencyContact,
        Instant createdAt) {

    public Parent {
        if (parentId == null) {
            throw new IllegalArgumentException("ParentId cannot be null");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

**Step 3: Create ParentFactory**

```java
package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import java.time.Instant;

public final class ParentFactory {

    public static Parent create(
            UserId userId,
            String phoneNumber,
            String address,
            String emergencyContact) {

        return new Parent(
            ParentId.of(userId),
            phoneNumber,
            address,
            emergencyContact,
            Instant.now()
        );
    }
}
```

**Step 4: Write test**

```java
package com.k12.user.domain.models.specialization.parent;

import com.k12.common.domain.model.UserId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParentTest {

    @Test
    void testParentCreation() {
        UserId userId = UserId.generate();
        Parent parent = ParentFactory.create(
            userId,
            "+1-555-0123",
            "123 Main St",
            "Jane Doe - +1-555-0987"
        );

        assertEquals(userId, parent.parentId().value());
        assertEquals("+1-555-0123", parent.phoneNumber());
    }
}
```

**Step 5: Run tests**

```bash
./gradlew test --tests ParentTest
```

Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/models/specialization/parent/
git commit -m "feat: add Parent specialization domain model"
```

---

## Task 4: Create Student Specialization Domain Model

**Files:**
- Create: `src/main/java/com/k12/user/domain/models/specialization/student/Student.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/student/StudentId.java`
- Create: `src/main/java/com/k12/user/domain/models/specialization/student/StudentFactory.java`
- Test: `src/test/java/com/k12/user/domain/models/specialization/student/StudentTest.java`

**Step 1: Create StudentId value object**

```java
package com.k12.user.domain.models.specialization.student;

import com.k12.common.domain.model.UserId;

public record StudentId(UserId value) {
    public StudentId {
        if (value == null) {
            throw new IllegalArgumentException("UserId cannot be null");
        }
    }

    public UUID id() {
        return value.value();
    }
}
```

**Step 2: Create Student aggregate**

```java
package com.k12.user.domain.models.specialization.student;

import com.k12.user.domain.models.specialization.parent.ParentId;
import java.time.Instant;
import java.time.LocalDate;

public record Student(
        StudentId studentId,
        String studentId,
        String gradeLevel,
        LocalDate dateOfBirth,
        ParentId guardianId,
        Instant createdAt) {

    public Student {
        if (studentId == null) {
            throw new IllegalArgumentException("StudentId cannot be null");
        }
        if (studentId == null || studentId.isBlank()) {
            throw new IllegalArgumentException("Student ID cannot be null or blank");
        }
        if (gradeLevel == null || gradeLevel.isBlank()) {
            throw new IllegalArgumentException("Grade level cannot be null or blank");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
```

**Step 3: Create StudentFactory**

```java
package com.k12.user.domain.models.specialization.student;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.ParentId;
import java.time.Instant;
import java.time.LocalDate;

public final class StudentFactory {

    public static Student create(
            UserId userId,
            String studentId,
            String gradeLevel,
            LocalDate dateOfBirth,
            ParentId guardianId) {

        return new Student(
            StudentId.of(userId),
            studentId,
            gradeLevel,
            dateOfBirth,
            guardianId,
            Instant.now()
        );
    }
}
```

**Step 4: Write tests**

```java
package com.k12.user.domain.models.specialization.student;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.ParentId;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StudentTest {

    @Test
    void testStudentCreation() {
        UserId userId = UserId.generate();
        ParentId guardianId = ParentId.of(UserId.generate());

        Student student = StudentFactory.create(
            userId,
            "STU001",
            "Grade 10",
            LocalDate.of(2010, 5, 15),
            guardianId
        );

        assertEquals(userId, student.studentId().value());
        assertEquals("STU001", student.studentId());
    }

    @Test
    void testStudentValidation_FutureDateOfBirth() {
        assertThrows(IllegalArgumentException.class, () ->
            StudentFactory.create(
                UserId.generate(),
                "STU002",
                "Grade 10",
                LocalDate.now().plusDays(1),  // future
                ParentId.of(UserId.generate())
            )
        );
    }
}
```

**Step 5: Run tests**

```bash
./gradlew test --tests StudentTest
```

Expected: PASS

**Step 6: Commit**

```bash
git add src/main/java/com/k12/user/domain/models/specialization/student/
git commit -m "feat: add Student specialization domain model"
```

---

## Task 5: Database Migration - Add Tenant to Users

**Files:**
- Create: `src/main/resources/db/migration/V10__Add_Tenant_To_Users.sql`

**Step 1: Write migration**

```sql
-- ============================================================
-- Add Tenant Association to Users
-- ============================================================

-- Add tenant_id column to users table
ALTER TABLE users ADD COLUMN tenant_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001';

-- Add foreign key constraint
ALTER TABLE users ADD CONSTRAINT fk_users_tenant
  FOREIGN KEY (tenant_id) REFERENCES tenants(id);

-- Add index for tenant-scoped queries
CREATE INDEX idx_users_tenant ON users(tenant_id);

-- Add tenant_id to user_events for event sourcing
ALTER TABLE user_events ADD COLUMN tenant_id UUID;

-- Update existing users to belong to default tenant
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000001'
WHERE tenant_id IS NULL;
```

**Step 2: Verify migration**

```bash
make db-up
```

Wait for database to start, then check:

```bash
docker exec k12-postgres psql -U k12_user -d k12_db -c "\d users"
```

Expected: tenant_id column present with foreign key

**Step 3: Commit**

```bash
git add src/main/resources/db/migration/V10__Add_Tenant_To_Users.sql
git commit -m "db: add tenant association to users table"
```

---

## Task 6: Database Migration - Create Specialization Tables

**Files:**
- Create: `src/main/resources/db/migration/V11__Create_Specialization_Tables.sql`

**Step 1: Write migration**

```sql
-- ============================================================
-- Create Specialization Tables
-- ============================================================

-- Teachers table
CREATE TABLE teachers (
  user_id UUID PRIMARY KEY,
  employee_id VARCHAR(100) UNIQUE NOT NULL,
  department VARCHAR(200),
  hire_date DATE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_teachers_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);

-- Parents table
CREATE TABLE parents (
  user_id UUID PRIMARY KEY,
  phone_number VARCHAR(50),
  address TEXT,
  emergency_contact VARCHAR(200),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_parents_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE
);

-- Students table
CREATE TABLE students (
  user_id UUID PRIMARY KEY,
  student_id VARCHAR(100) UNIQUE NOT NULL,
  grade_level VARCHAR(50) NOT NULL,
  date_of_birth DATE NOT NULL,
  guardian_id UUID,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_students_user FOREIGN KEY (user_id)
    REFERENCES users(id) ON DELETE CASCADE,
  CONSTRAINT fk_students_guardian FOREIGN KEY (guardian_id)
    REFERENCES parents(user_id)
);

-- Create indexes
CREATE INDEX idx_teachers_employee ON teachers(employee_id);
CREATE INDEX idx_students_guardian ON students(guardian_id);
```

**Step 2: Regenerate jOOQ classes**

```bash
make generate-jooq
```

Expected: BUILD SUCCESSFUL

**Step 3: Verify tables**

```bash
docker exec k12-postgres psql -U k12_user -d k12_db -c "\d teachers"
docker exec k12-postgres psql -U k12_user -d k12_db -c "\d parents"
docker exec k12-postgres psql -U k12_user -d k12_db -c "\d students"
```

Expected: All three tables present

**Step 4: Commit**

```bash
git add src/main/resources/db/migration/V11__Create_Specialization_Tables.sql
git commit -m "db: create teacher, parent, student specialization tables"
```

---

## Task 7: Create Repository Ports (Interfaces)

**Files:**
- Create: `src/main/java/com/k12/user/domain/ports/out/TeacherRepository.java`
- Create: `src/main/java/com/k12/user/domain/ports/out/ParentRepository.java`
- Create: `src/main/java/com/k12/user/domain/ports/out/StudentRepository.java`

**Step 1: Create TeacherRepository interface**

```java
package com.k12.user.domain.ports.out;

import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherId;
import java.util.Optional;

public interface TeacherRepository {
    Teacher save(Teacher teacher);
    Optional<Teacher> findById(TeacherId teacherId);
    Optional<Teacher> findByUserId(com.k12.common.domain.model.UserId userId);
    boolean existsByEmployeeId(String employeeId);
    void deleteById(TeacherId teacherId);
}
```

**Step 2: Create ParentRepository interface**

```java
package com.k12.user.domain.ports.out;

import com.k12.user.domain.models.specialization.parent.Parent;
import com.k12.user.domain.models.specialization.parent.ParentId;
import java.util.Optional;

public interface ParentRepository {
    Parent save(Parent parent);
    Optional<Parent> findById(ParentId parentId);
    Optional<Parent> findByUserId(com.k12.common.domain.model.UserId userId);
    void deleteById(ParentId parentId);
}
```

**Step 3: Create StudentRepository interface**

```java
package com.k12.user.domain.ports.out;

import com.k12.user.domain.models.specialization.student.Student;
import com.k12.user.domain.models.specialization.student.StudentId;
import java.util.Optional;

public interface StudentRepository {
    Student save(Student student);
    Optional<Student> findById(StudentId studentId);
    Optional<Student> findByUserId(com.k12.common.domain.model.UserId userId);
    boolean existsByStudentId(String studentId);
    void deleteById(StudentId studentId);
}
```

**Step 4: Update UserRepository interface**

```java
// In UserRepository.java, add methods
Optional<User> findByEmailInTenant(String email, TenantId tenantId);
boolean existsByEmailInTenant(String email, TenantId tenantId);
```

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/domain/ports/out/
git commit -m "feat: add specialization repository interfaces"
```

---

## Task 8: Implement TeacherRepository

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/persistence/TeacherRepositoryImpl.java`
- Test: `src/test/java/com/k12/user/infrastructure/persistence/TeacherRepositoryImplTest.java`

**Step 1: Write implementation**

```java
package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Teachers.TEACHERS;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherId;
import com.k12.user.domain.ports.out.TeacherRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class TeacherRepositoryImpl implements TeacherRepository {

    private final AgroalDataSource dataSource;

    @Override
    public Teacher save(Teacher teacher) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.insertInto(TEACHERS,
                TEACHERS.USER_ID,
                TEACHERS.EMPLOYEE_ID,
                TEACHERS.DEPARTMENT,
                TEACHERS.HIRE_DATE,
                TEACHERS.CREATED_AT,
                TEACHERS.UPDATED_AT)
                .values(
                    teacher.teacherId().value(),
                    teacher.employeeId(),
                    teacher.department(),
                    teacher.hireDate(),
                    OffsetDateTime.ofInstant(teacher.createdAt(), ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC)
                )
                .onConflict(TEACHERS.USER_ID)
                .doUpdate()
                .set(TEACHERS.EMPLOYEE_ID, teacher.employeeId())
                .set(TEACHERS.DEPARTMENT, teacher.department())
                .set(TEACHERS.HIRE_DATE, teacher.hireDate())
                .set(TEACHERS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return teacher;
    }

    @Override
    public Optional<Teacher> findById(TeacherId teacherId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        var record = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(teacherId.value()))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        return Optional.of(mapToTeacher(record));
    }

    @Override
    public Optional<Teacher> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        var record = ctx.selectFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(userId))
                .fetchOne();

        if (record == null) {
            return Optional.empty();
        }

        return Optional.of(mapToTeacher(record));
    }

    @Override
    public boolean existsByEmployeeId(String employeeId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchExists(
            ctx.selectFrom(TEACHERS)
                .where(TEACHERS.EMPLOYEE_ID.eq(employeeId))
        );
    }

    @Override
    public void deleteById(TeacherId teacherId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(TEACHERS)
                .where(TEACHERS.USER_ID.eq(teacherId.value()))
                .execute();
    }

    private Teacher mapToTeacher(org.jooq.Record record) {
        return new Teacher(
            TeacherId.of(record.get(TEACHERS.USER_ID, UserId.class)),
            record.get(TEACHERS.EMPLOYEE_ID),
            record.get(TEACHERS.DEPARTMENT),
            record.get(TEACHERS.HIRE_DATE, LocalDate.class),
            record.get(TEACHERS.CREATED_AT, OffsetDateTime.class).toInstant()
        );
    }
}
```

**Step 2: Write test**

```java
package com.k12.user.infrastructure.persistence;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherFactory;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class TeacherRepositoryImplTest {

    @Inject
    TeacherRepository teacherRepository;

    @Test
    void testSaveAndFindTeacher() {
        UserId userId = UserId.generate();
        Teacher teacher = TeacherFactory.create(
            userId,
            "EMP123",
            "Science",
            LocalDate.of(2024, 1, 15)
        );

        Teacher saved = teacherRepository.save(teacher);

        assertNotNull(saved);

        var found = teacherRepository.findByUserId(userId);
        assertTrue(found.isPresent());
        assertEquals("EMP123", found.get().employeeId());
    }
}
```

**Step 3: Run tests**

```bash
./gradlew test --tests TeacherRepositoryImplTest
```

Expected: PASS

**Step 4: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/persistence/TeacherRepositoryImpl.java \
        src/test/java/com/k12/user/infrastructure/persistence/TeacherRepositoryImplTest.java
git commit -m "feat: implement TeacherRepository"
```

---

## Task 9: Implement ParentRepository and StudentRepository

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/persistence/ParentRepositoryImpl.java`
- Create: `src/main/java/com/k12/user/infrastructure/persistence/StudentRepositoryImpl.java`
- Tests: Corresponding test files

**Step 1: Implement ParentRepositoryImpl**

```java
package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Parents.PARENTS;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.Parent;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.ports.out.ParentRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class ParentRepositoryImpl implements ParentRepository {

    private final AgroalDataSource dataSource;

    @Override
    public Parent save(Parent parent) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.insertInto(PARENTS,
                PARENTS.USER_ID,
                PARENTS.PHONE_NUMBER,
                PARENTS.ADDRESS,
                PARENTS.EMERGENCY_CONTACT,
                PARENTS.CREATED_AT,
                PARENTS.UPDATED_AT)
                .values(
                    parent.parentId().value(),
                    parent.phoneNumber(),
                    parent.address(),
                    parent.emergencyContact(),
                    OffsetDateTime.ofInstant(parent.createdAt(), ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC)
                )
                .onConflict(PARENTS.USER_ID)
                .doUpdate()
                .set(PARENTS.PHONE_NUMBER, parent.phoneNumber())
                .set(PARENTS.ADDRESS, parent.address())
                .set(PARENTS.EMERGENCY_CONTACT, parent.emergencyContact())
                .set(PARENTS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return parent;
    }

    @Override
    public Optional<Parent> findById(ParentId parentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(PARENTS)
                .where(PARENTS.USER_ID.eq(parentId.value()))
                .fetchOne();

        return record == null ? Optional.empty() : Optional.of(mapToParent(record));
    }

    @Override
    public Optional<Parent> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(PARENTS)
                .where(PARENTS.USER_ID.eq(userId))
                .fetchOne();

        return record == null ? Optional.empty() : Optional.of(mapToParent(record));
    }

    @Override
    public void deleteById(ParentId parentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(PARENTS)
                .where(PARENTS.USER_ID.eq(parentId.value()))
                .execute();
    }

    private Parent mapToParent(org.jooq.Record record) {
        return new Parent(
            ParentId.of(record.get(PARENTS.USER_ID, UserId.class)),
            record.get(PARENTS.PHONE_NUMBER),
            record.get(PARENTS.ADDRESS),
            record.get(PARENTS.EMERGENCY_CONTACT),
            record.get(PARENTS.CREATED_AT, OffsetDateTime.class).toInstant()
        );
    }
}
```

**Step 2: Implement StudentRepositoryImpl**

```java
package com.k12.user.infrastructure.persistence;

import static com.k12.backend.infrastructure.jooq.public_.tables.Students.STUDENTS;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.student.Student;
import com.k12.user.domain.models.specialization.student.StudentId;
import com.k12.user.domain.ports.out.StudentRepository;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

@ApplicationScoped
@RequiredArgsConstructor
public class StudentRepositoryImpl implements StudentRepository {

    private final AgroalDataSource dataSource;

    @Override
    public Student save(Student student) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

        ctx.insertInto(STUDENTS,
                STUDENTS.USER_ID,
                STUDENTS.STUDENT_ID,
                STUDENTS.GRADE_LEVEL,
                STUDENTS.DATE_OF_BIRTH,
                STUDENTS.GUARDIAN_ID,
                STUDENTS.CREATED_AT,
                STUDENTS.UPDATED_AT)
                .values(
                    student.studentId().value(),
                    student.studentId(),
                    student.gradeLevel(),
                    student.dateOfBirth(),
                    student.guardianId() != null ? student.guardianId().value() : null,
                    OffsetDateTime.ofInstant(student.createdAt(), ZoneOffset.UTC),
                    OffsetDateTime.now(ZoneOffset.UTC)
                )
                .onConflict(STUDENTS.USER_ID)
                .doUpdate()
                .set(STUDENTS.STUDENT_ID, student.studentId())
                .set(STUDENTS.GRADE_LEVEL, student.gradeLevel())
                .set(STUDENTS.DATE_OF_BIRTH, student.dateOfBirth())
                .set(STUDENTS.GUARDIAN_ID, student.guardianId() != null ? student.guardianId().value() : null)
                .set(STUDENTS.UPDATED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .execute();

        return student;
    }

    @Override
    public Optional<Student> findById(StudentId studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(studentId.value()))
                .fetchOne();

        return record == null ? Optional.empty() : Optional.of(mapToStudent(record));
    }

    @Override
    public Optional<Student> findByUserId(UserId userId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        var record = ctx.selectFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(userId))
                .fetchOne();

        return record == null ? Optional.empty() : Optional.of(mapToStudent(record));
    }

    @Override
    public boolean existsByStudentId(String studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        return ctx.fetchExists(
            ctx.selectFrom(STUDENTS)
                .where(STUDENTS.STUDENT_ID.eq(studentId))
        );
    }

    @Override
    public void deleteById(StudentId studentId) {
        DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);
        ctx.deleteFrom(STUDENTS)
                .where(STUDENTS.USER_ID.eq(studentId.value()))
                .execute();
    }

    private Student mapToStudent(org.jooq.Record record) {
        return new Student(
            StudentId.of(record.get(STUDENTS.USER_ID, UserId.class)),
            record.get(STUDENTS.STUDENT_ID),
            record.get(STUDENTS.GRADE_LEVEL),
            record.get(STUDENTS.DATE_OF_BIRTH, LocalDate.class),
            record.get(STUDENTS.GUARDIAN_ID) != null
                ? com.k12.user.domain.models.specialization.parent.ParentId.of(
                    record.get(STUDENTS.GUARDIAN_ID, UserId.class))
                : null
        );
    }
}
```

**Step 3: Update UserRepositoryImpl to add tenant-scoped methods**

```java
// In UserRepositoryImpl.java, add:

@Override
public Optional<User> findByEmailInTenant(String email, TenantId tenantId) {
    DSLContext ctx = DSL.using(dataSource, SQLDialect.POSTGRES);

    UUID userId = ctx.select(USERS.ID)
            .from(USERS)
            .where(USERS.EMAIL.eq(email))
            .and(USERS.TENANT_ID.eq(tenantId.value()))
            .fetchOne(USERS.ID);

    if (userId == null) {
        return Optional.empty();
    }

    return findById(new UserId(userId));
}

@Override
public boolean existsByEmailInTenant(String email, TenantId tenantId) {
    return findByEmailInTenant(email, tenantId).isPresent();
}
```

**Step 4: Run tests**

```bash
./gradlew test --tests ParentRepositoryImplTest --tests StudentRepositoryImplTest
```

Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/persistence/
git commit -m "feat: implement ParentRepository and StudentRepository"
```

---

## Task 10: Create DTOs

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/CreateUserRequest.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/CreateTeacherRequest.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/CreateParentRequest.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/CreateStudentRequest.java`
- Create: `src/main/java/com/k12/user/infrastructure/rest/dto/UserResponse.java`

**Step 1: Create CreateUserRequest base**

```java
package com.k12.user.infrastructure.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public abstract class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Name is required")
    private String name;

    public String email() { return email; }
    public String password() { return password; }
    public String name() { return name; }
}
```

**Step 2: Create specialization DTOs**

```java
// CreateTeacherRequest.java
public class CreateTeacherRequest extends CreateUserRequest {

    @NotBlank(message = "Employee ID is required")
    private String employeeId;

    private String department;

    private String hireDate;

    public String employeeId() { return employeeId; }
    public String department() { return department; }
    public String hireDate() { return hireDate; }
}

// CreateParentRequest.java
public class CreateParentRequest extends CreateUserRequest {

    private String phoneNumber;
    private String address;
    private String emergencyContact;

    public String phoneNumber() { return phoneNumber; }
    public String address() { return address; }
    public String emergencyContact() { return emergencyContact; }
}

// CreateStudentRequest.java
public class CreateStudentRequest extends CreateUserRequest {

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotBlank(message = "Grade level is required")
    private String gradeLevel;

    @NotNull(message = "Date of birth is required")
    private String dateOfBirth;

    private String guardianId;

    public String studentId() { return studentId; }
    public String gradeLevel() { return gradeLevel; }
    public String dateOfBirth() { return dateOfBirth; }
    public String guardianId() { return guardianId; }
}
```

**Step 3: Create UserResponse**

```java
package com.k12.user.infrastructure.rest.dto;

import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

public record UserResponse(
    UserId userId,
    TenantId tenantId,
    String email,
    String name,
    Set<String> roles,
    String status,
    String type,
    Map<String, Object> typeSpecificFields,
    Instant createdAt
) {
    public static UserResponse from(
            com.k12.user.domain.models.User user,
            String type,
            Map<String, Object> typeSpecificFields) {
        return new UserResponse(
            user.userId(),
            user.tenantId(),
            user.emailAddress().value(),
            user.name().value(),
            user.userRole().stream().map(Enum::name).collect(java.util.Set.toSet(java.util.HashSet::new)),
            user.status().name(),
            type,
            typeSpecificFields,
            java.time.Instant.now()
        );
    }
}
```

**Step 4: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/rest/dto/
git commit -m "feat: add user management DTOs"
```

---

## Task 11: Implement UserService

**Files:**
- Create: `src/main/java/com/k12/user/application/service/UserService.java`
- Create: `src/main/java/com/k12/user/domain/models/error/UserError.java` (extend with new errors)

**Step 1: Extend UserError enum**

```java
// In UserError.java, add new errors:
public sealed interface UserError extends Error {
    // ... existing errors ...

    enum AuthorizationError implements UserError {
        INSUFFICIENT_PERMISSIONS,
        NO_TENANT_IN_CONTEXT
    }

    enum ValidationError implements UserError {
        GUARDIAN_NOT_FOUND,
        INVALID_DATE_OF_BIRTH
    }

    enum DuplicateError implements UserError {
        DUPLICATE_EMPLOYEE_ID,
        DUPLICATE_STUDENT_ID
    }
}
```

**Step 2: Implement UserService**

```java
package com.k12.user.application.service;

import com.k12.common.domain.model.Result;
import com.k12.common.domain.model.TenantId;
import com.k12.common.domain.model.UserId;
import com.k12.common.infrastructure.security.SecurityContext;
import com.k12.user.domain.models.EmailAddress;
import com.k12.user.domain.models.PasswordHash;
import com.k12.user.domain.models.UserName;
import com.k12.user.domain.models.User;
import com.k12.user.domain.models.UserRole;
import com.k12.user.domain.models.UserStatus;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.models.specialization.student.StudentFactory;
import com.k12.user.domain.models.specialization.student.Student;
import com.k12.user.domain.models.specialization.student.StudentId;
import com.k12.user.domain.models.specialization.teacher.TeacherFactory;
import com.k12.user.domain.models.specialization.teacher.Teacher;
import com.k12.user.domain.models.specialization.teacher.TeacherId;
import com.k12.user.domain.models.specialization.parent.ParentFactory;
import com.k12.user.domain.models.specialization.parent.Parent;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.ports.out.ParentRepository;
import com.k12.user.domain.ports.out.StudentRepository;
import com.k12.user.domain.ports.out.TeacherRepository;
import com.k12.user.domain.ports.out.UserRepository;
import com.k12.user.infrastructure.rest.dto.CreateParentRequest;
import com.k12.user.infrastructure.rest.dto.CreateStudentRequest;
import com.k12.user.infrastructure.rest.dto.CreateTeacherRequest;
import com.k12.user.infrastructure.rest.dto.CreateUserRequest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;

@Slf4j
@ApplicationScoped
public class UserService {

    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;
    private final ParentRepository parentRepository;
    private final StudentRepository studentRepository;
    private final Tracer tracer;

    public UserService(
            UserRepository userRepository,
            TeacherRepository teacherRepository,
            ParentRepository parentRepository,
            StudentRepository studentRepository,
            Tracer tracer) {
        this.userRepository = userRepository;
        this.teacherRepository = teacherRepository;
        this.parentElementRepository = parentRepository;
        this.studentRepository = studentRepository;
        this.tracer = tracer;
    }

    @Transactional
    public Result<UserWithSpecialization, UserError> createUser(CreateUserRequest request) {
        Span span = tracer.spanBuilder("UserService.createUser")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            // 1. Extract tenantId from JWT
            TenantId tenantId = SecurityContext.getCurrentTenantId()
                .orElseThrow(() -> new IllegalStateException("No tenant in JWT"));

            // 2. Validate caller has ADMIN role
            if (!SecurityContext.hasRole("ADMIN")) {
                log.error("User creation attempted without ADMIN role");
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Unauthorized");
                return Result.failure(UserError.AuthorizationError.INSUFFICIENT_PERMISSIONS);
            }

            // 3. Validate email uniqueness within tenant
            if (userRepository.existsByEmailInTenant(request.email(), tenantId)) {
                log.error("Email already exists in tenant: {}", request.email());
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "Email exists");
                return Result.failure(UserError.EmailError.EMAIL_ALREADY_EXISTS);
            }

            // 4. Create base user
            UserId userId = UserId.generate();
            User user = new User(
                userId,
                EmailAddress.of(request.email()),
                PasswordHash.of(request.password()),  // NOTE: Should hash password in production
                Set.of(UserRole.USER),
                UserStatus.ACTIVE,
                UserName.of(request.name()),
                tenantId
            );

            // 5. Create specialization
            Object specialization = switch (request.getClass().getSimpleName()) {
                case "CreateTeacherRequest" -> {
                    CreateTeacherRequest teacherRequest = (CreateTeacherRequest) request;

                    if (teacherRepository.existsByEmployeeId(teacherRequest.employeeId())) {
                        return Result.failure(UserError.DuplicateError.DUPLICATE_EMPLOYEE_ID);
                    }

                    Teacher teacher = TeacherFactory.create(
                        userId,
                        teacherRequest.employeeId(),
                        teacherRequest.department(),
                        teacherRequest.hireDate() != null
                            ? LocalDate.parse(teacherRequest.hireDate())
                            : LocalDate.now()
                    );

                    teacherRepository.save(teacher);
                    yield teacher;
                }

                case "CreateParentRequest" -> {
                    CreateParentRequest parentRequest = (CreateParentRequest) request;
                    Parent parent = ParentFactory.create(
                        userId,
                        parentRequest.phoneNumber(),
                        parentRequest.address(),
                        parentRequest.emergencyContact()
                    );

                    parentRepository.save(parent);
                    yield parent;
                }

                case "CreateStudentRequest" -> {
                    CreateStudentRequest studentRequest = (CreateStudentRequest) request;

                    if (studentRepository.existsByStudentId(studentRequest.studentId())) {
                        return Result.failure(UserError.DuplicateError.DUPLICATE_STUDENT_ID);
                    }

                    LocalDate dob = LocalDate.parse(studentRequest.dateOfBirth());
                    if (dob.isAfter(LocalDate.now())) {
                        return Result.failure(UserError.ValidationError.INVALID_DATE_OF_BIRTH);
                    }

                    ParentId guardianId = null;
                    if (studentRequest.guardianId() != null) {
                        guardianId = ParentId.of(UserId.of(UUID.fromString(studentRequest.guardianId())));
                        if (!parentRepository.findByUserId(guardianId.value()).isPresent()) {
                            return Result.failure(UserError.ValidationError.GUARDIAN_NOT_FOUND);
                        }
                    }

                    Student student = StudentFactory.create(
                        userId,
                        studentRequest.studentId(),
                        studentRequest.gradeLevel(),
                        dob,
                        guardianId
                    );

                    studentRepository.save(student);
                    yield student;
                }

                default -> {
                    throw new IllegalArgumentException("Unknown request type");
                }
            };

            // 6. Save user
            userRepository.save(user);

            log.info("User created: {} for tenant: {}", userId, tenantId);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);

            return Result.success(new UserWithSpecialization(user, specialization));
        } catch (Exception e) {
            log.error("Failed to create user", e);
            span.recordException(e);
            span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }
}

// Helper record for response
public record UserWithSpecialization(User user, Object specialization) {}
```

**Step 3: Commit**

```bash
git add src/main/java/com/k12/user/application/service/UserService.java \
        src/main/java/com/k12/user/domain/models/error/UserError.java
git commit -m "feat: implement UserService with tenant scoping"
```

---

## Task 12: Create UserResource REST Endpoint

**Files:**
- Create: `src/main/java/com/k12/user/infrastructure/rest/resource/UserResource.java`

**Step 1: Implement UserResource**

```java
package com.k12.user.infrastructure.rest.resource;

import com.k12.common.infrastructure.security.SecurityContext;
import com.k12.user.application.service.UserService;
import com.k12.user.domain.models.error.UserError;
import com.k12.user.infrastructure.rest.dto.*;
import com.k12.user.infrastructure.rest.mapper.ErrorResponseMapper;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/api/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class UserResource {

    private final UserService userService;
    private final Tracer tracer;

    @POST
    @Operation(
        summary = "Create a new user",
        description = "Creates a new user (TEACHER, PARENT, or STUDENT) in the current tenant"
    )
    @APIResponse(responseCode = "201", description = "User created successfully")
    @APIResponse(responseCode = "400", description = "Invalid request data")
    @APIResponse(responseCode = "403", description = "Forbidden - ADMIN role required")
    @RolesAllowed("ADMIN")
    public Response createUser(@Valid CreateUserRequest request) {
        Span span = tracer.spanBuilder("UserResource.createUser")
                .setSpanKind(SpanKind.SERVER)
                .startSpan();

        try (var scope = span.makeCurrent()) {
            var result = userService.createUser(request);

            return result.fold(
                success -> {
                    Response response = Response.status(Response.Status.CREATED.getStatusCode())
                            .entity(buildResponse(success))
                            .build();
                    span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
                    return response;
                },
                error -> {
                    Response response = ErrorResponseMapper.toResponse(error);
                    span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR);
                    return response;
                }
            );
        } finally {
            span.end();
        }
    }

    private Object buildResponse(UserWithSpecialization userWithSpec) {
        // Build response based on specialization type
        return switch (userWithSpec.specialization()) {
            case com.k12.user.domain.models.specialization.teacher.Teacher t ->
                UserResponse.from(userWithSpec.user(), "TEACHER", Map.of(
                    "employeeId", t.employeeId(),
                    "department", t.department(),
                    "hireDate", t.hireDate()
                ));

            case com.k12.user.domain.models.specialization.parent.Parent p ->
                UserResponse.from(userWithSpec.user(), "PARENT", Map.of(
                    "phoneNumber", p.phoneNumber(),
                    "address", p.address(),
                    "emergencyContact", p.emergencyContact()
                ));

            case com.k12.user.domain.models.specialization.student.Student s ->
                UserResponse.from(userWithSpec.user(), "STUDENT", Map.of(
                    "studentId", s.studentId(),
                    "gradeLevel", s.gradeLevel(),
                    "dateOfBirth", s.dateOfBirth(),
                    "guardianId", s.guardianId() != null ? s.guardianId().id() : null
                ));

            default -> throw new IllegalArgumentException("Unknown specialization type");
        };
    }
}
```

**Step 2: Update ErrorResponseMapper to handle UserError**

```java
// In ErrorResponseMapper.java, add imports and cases
import com.k12.user.domain.models.error.UserError;
import com.k12.user.domain.models.error.UserError.*;

private Response toResponse(UserError error) {
    return switch (error) {
        case AuthorizationError e -> Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("AUTHORIZATION_ERROR", e.toString()))
                .build();

        case ValidationError e -> Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("VALIDATION_ERROR", e.toString()))
                .build();

        case DuplicateError e -> Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse("DUPLICATE_ERROR", e.toString()))
                .build();

        // ... existing cases ...
    };
}
```

**Step 3: Run application to verify no compilation errors**

```bash
./gradlew assemble
```

Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/java/com/k12/user/infrastructure/rest/resource/UserResource.java \
        src/main/java/com/k12/user/infrastructure/rest/mapper/ErrorResponseMapper.java
git commit -m "feat: add UserResource REST endpoint"
```

---

## Task 13: Update JWT to Include TenantId

**Files:**
- Check: JWT token generation (likely in authentication service)
- Test: Verify tenantId appears in JWT

**Step 1: Find JWT generation code**

```bash
grep -r "JsonWebToken" src/main/java --include="*.java"
```

Expected: Find JWT generation in authentication service

**Step 2: Update JWT generation to include tenantId**

In your authentication service, when generating JWT:

```java
// When generating JWT for login
Map<String, Object> claims = new HashMap<>();
claims.put("sub", user.userId().value());
claims.put("email", user.emailAddress().value());
claims.put("roles", user.roles());
claims.put("tenantId", user.tenantId().value());  // ADD THIS
```

**Step 3: Test login and verify JWT**

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@k12.com","password":"admin123"}' | jq -r '.token')

echo $TOKEN | jq -r '.'
```

Expected: JWT contains `"tenantId": "..."`

**Step 4: Commit**

```bash
git add <files-modified>
git commit -m "feat: include tenantId in JWT token"
```

---

## Task 14: Integration Tests

**Files:**
- Create: `src/test/java/com/k12/user/infrastructure/rest/resource/UserResourceIntegrationTest.java`

**Step 1: Write integration test**

```java
package com.k12.user.infrastructure.rest.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserResourceIntegrationTest {

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:8080";
    }

    @Test
    void testCreateTeacher_AsAdmin() {
        // First login as admin to get token
        String token = given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
            .when()
            .post("/api/auth/login")
            .jsonPath()
            .getString("token");

        // Create teacher
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "type": "TEACHER",
                    "email": "teacher@example.com",
                    "password": "Password123",
                    "name": "John Teacher",
                    "employeeId": "EMP001",
                    "department": "Mathematics",
                    "hireDate": "2024-01-15"
                }
                """)
            .when()
            .post("/api/users")
            .then()
            .statusCode(201)
            .body("type", equalTo("TEACHER"))
            .body("email", equalTo("teacher@example.com"))
            .body("tenantId", notNullValue());
    }

    @Test
    void testCreateStudent_WithGuardian() {
        String token = loginAsAdmin();

        // First create a parent
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + token)
            .body("""
                {
                    "type": "PARENT",
                    "email": "parent@example.com",
                    "password": "Password123",
                    "name": "Jane Parent",
                    "phoneNumber": "+1-555-0123",
                    "address": "123 Main St",
                    "emergencyContact": "John Doe - +1-555-0987"
                }
                """)
            .when()
            .post("/api/users")
            .then()
            .statusCode(201);

        // Then create student with that parent as guardian
        // (Implementation would require capturing parent userId)
    }

    private String loginAsAdmin() {
        return given()
            .contentType(ContentType.JSON)
            .body("{\"email\":\"admin@k12.com\",\"password\":\"admin123\"}")
            .when()
            .post("/api/auth/login")
            .jsonPath()
            .getString("token");
    }
}
```

**Step 2: Run integration tests**

```bash
./gradlew test --tests UserResourceIntegrationTest
```

Expected: PASS (may require test data setup)

**Step 3: Commit**

```bash
git add src/test/java/com/k12/user/infrastructure/rest/resource/UserResourceIntegrationTest.java
git commit -m "test: add user creation integration tests"
```

---

## Task 15: Manual Testing with Redoc

**Step 1: Rebuild and start application**

```bash
make docker-build
make docker-up
```

**Step 2: Access Redoc UI**

Open: http://localhost:8082

**Step 3: Configure JWT in header**

1. Click "Set Token" button in header
2. Paste JWT token from login

**Step 4: Test creating a teacher**

```
POST /api/users
Authorization: Bearer <token>
Content-Type: application/json

{
  "type": "TEACHER",
  "email": "teacher.test@example.com",
  "password": "SecurePass123",
  "name": "Test Teacher",
  "employeeId": "EMP999",
  "department": "Science",
  "hireDate": "2024-02-01"
}
```

Expected: 201 Created with full user object

**Step 5: Test authorization failure**

1. Log in as non-admin user (if available)
2. Try to create user

Expected: 403 Forbidden

**Step 6: Test duplicate prevention**

1. Create teacher with employeeId "EMP999"
2. Try to create another teacher with same employeeId

Expected: 409 Conflict

---

## Task 16: Update Documentation

**Files:**
- Update: `README.md` (if exists) or create user guide
- Update: `api-docs-custom.html` (add Users section to OpenAPI)

**Step 1: Document API in OpenAPI**

Add to api-docs-custom.html if needed, or verify OpenAPI annotations are complete.

**Step 2: Create user guide**

```bash
# Create or update docs with user management API usage examples
```

**Step 3: Commit**

```bash
git add docs/
git commit -m "docs: add user management API documentation"
```

---

## Success Criteria Verification

Run these checks to verify implementation is complete:

**Step 1: Run all tests**

```bash
./gradlew test
```

Expected: ALL TESTS PASS

**Step 2: Verify OpenAPI spec**

```bash
curl -s http://localhost:8080/q/openapi | jq '.paths["/api/users"]'
```

Expected: POST endpoint documented

**Step 3: Verify tenant isolation**

Create two tenants, two admins, verify users are scoped correctly.

**Step 4: Manual test through Redoc**

Test all three specialization types through Redoc UI.

---

## Notes for Implementation

1. **Password Hashing**: The example shows plaintext password - MUST integrate with PasswordHasher in production
2. **Error Messages**: UserError enum needs to be extended with proper error messages
3. **Date Parsing**: Use ISO format (YYYY-MM-DD) for dates in API
4. **UUID Handling**: Ensure UUIDs are properly validated in requests
5. **Transaction Rollback**: Verify @Transactional works correctly if specialization save fails
6. **Index Performance**: The idx_users_tenant index is critical for tenant-scoped queries

---

**Total Estimated Time:** 4-6 hours for full implementation
**Number of Commits:** ~16 commits (one per task)
**Lines of Code:** ~2000 LOC (domain, infrastructure, tests)
