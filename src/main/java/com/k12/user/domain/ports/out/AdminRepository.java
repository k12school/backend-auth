package com.k12.user.domain.ports.out;

import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.admin.Admin;
import com.k12.user.domain.models.specialization.admin.AdminId;
import com.k12.user.domain.models.specialization.admin.AdminStatus;
import java.util.Optional;
import java.util.Set;

/**
 * Repository interface for Admin aggregates.
 */
public interface    AdminRepository {

    /**
     * Saves an admin aggregate.
     *
     * @param admin The admin to save
     * @return The saved admin
     */
    Admin save(Admin admin);

    /**
     * Finds an admin by ID.
     *
     * @param adminId The admin ID
     * @return Optional containing the admin if found
     */
    Optional<Admin> findById(AdminId adminId);

    /**
     * Finds an admin by user ID.
     *
     * @param userId The user ID
     * @return Optional containing the admin if found
     */
    Optional<Admin> findByUserId(UserId userId);

    /**
     * Checks if an admin exists by user ID.
     *
     * @param userId The user ID
     * @return true if an admin exists for this user
     */
    boolean existsByUserId(UserId userId);

    /**
     * Finds all admins.
     *
     * @return Set of all admins
     */
    Set<Admin> findAll();

    /**
     * Finds admins by status.
     *
     * @param status The admin status
     * @return Set of admins with the status
     */
    Set<Admin> findByStatus(AdminStatus status);

    /**
     * Deletes an admin by ID.
     *
     * @param adminId The admin ID
     */
    void deleteById(AdminId adminId);

    /**
     * Counts the total number of admins.
     *
     * @return The count of admins
     */
    long count();
}
