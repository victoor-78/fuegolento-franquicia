package com.fuegolento.fuegolento_franquicia.repository;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.fuegolento.fuegolento_franquicia.model.User;

/**
 * Repository for User entity.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    // ========= DASHBOARD QUERIES =========
    interface DailyUserCountRow {
        java.sql.Date getDay();
        Long getCount();
    }

    @Query("""
        SELECT
            function('date', u.createdAt) as day,
            count(u) as count
        FROM UserTable u
        WHERE u.createdAt IS NOT NULL
          AND u.createdAt >= :from
        GROUP BY function('date', u.createdAt)
        ORDER BY function('date', u.createdAt)
    """)
    List<DailyUserCountRow> registrationsByDaySince(@Param("from") LocalDateTime from);
}
