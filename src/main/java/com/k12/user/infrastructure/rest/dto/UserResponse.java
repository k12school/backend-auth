package com.k12.user.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        String userId,
        String email,
        String name,
        String role,
        String tenantId,
        String status,
        Instant createdAt,
        TeacherData teacher,
        ParentData parent,
        StudentData student) {

    public record TeacherData(String employeeId, String department, String hireDate) {}

    public record ParentData(String phoneNumber, String address, String emergencyContact) {}

    public record StudentData(String studentNumber, String gradeLevel, String dateOfBirth, String guardianId) {}
}
