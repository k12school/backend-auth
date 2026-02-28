package com.k12.user.domain.ports.out;

import com.k12.common.domain.model.StudentId;
import com.k12.common.domain.model.UserId;
import com.k12.user.domain.models.specialization.parent.Parent;
import com.k12.user.domain.models.specialization.parent.ParentId;
import com.k12.user.domain.models.specialization.parent.ParentStatus;
import java.util.Optional;
import java.util.Set;
import org.jooq.DSLContext;

/**
 * Repository interface for Parent aggregates.
 */
public interface ParentRepository {

    /**
     * Saves a parent aggregate.
     *
     * @param parent The parent to save
     * @return The saved parent
     */
    Parent save(Parent parent);

    /**
     * Saves a parent aggregate using a specific DSLContext.
     * This overload supports multi-repository transactions.
     *
     * @param parent the parent to save
     * @param ctx the DSLContext to use (must be transactional)
     * @return the saved parent
     */
    Parent save(Parent parent, DSLContext ctx);

    /**
     * Finds a parent by ID.
     *
     * @param parentId The parent ID
     * @return Optional containing the parent if found
     */
    Optional<Parent> findById(ParentId parentId);

    /**
     * Finds a parent by user ID.
     *
     * @param userId The user ID
     * @return Optional containing the parent if found
     */
    Optional<Parent> findByUserId(UserId userId);

    /**
     * Checks if a parent exists by user ID.
     *
     * @param userId The user ID
     * @return true if a parent exists for this user
     */
    boolean existsByUserId(UserId userId);

    /**
     * Finds all parents.
     *
     * @return Set of all parents
     */
    Set<Parent> findAll();

    /**
     * Finds parents by status.
     *
     * @param status The parent status
     * @return Set of parents with the status
     */
    Set<Parent> findByStatus(ParentStatus status);

    /**
     * Finds parents linked to a specific student.
     *
     * @param studentId The student ID
     * @return Set of parents linked to the student
     */
    Set<Parent> findByLinkedStudent(StudentId studentId);

    /**
     * Deletes a parent by ID.
     *
     * @param parentId The parent ID
     */
    void deleteById(ParentId parentId);

    /**
     * Counts the total number of parents.
     *
     * @return The count of parents
     */
    long count();
}
