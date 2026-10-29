package net.jemsit.auth.data.repository;

import net.jemsit.auth.data.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameOrEmail(String username, String email);

    List<User> findAllByOrderByCreatedAtDesc();

    // authorities is a varchar[] column, which JPQL cannot test membership on
    @Query(value = """
            SELECT * FROM users u
            WHERE 'CLIENT' = ANY(u.authorities)
              AND (:search = '' OR u.username ILIKE CONCAT('%', :search, '%')
                   OR u.phone_number ILIKE CONCAT('%', :search, '%')
                   OR u.email ILIKE CONCAT('%', :search, '%'))
            ORDER BY u.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM users u
            WHERE 'CLIENT' = ANY(u.authorities)
              AND (:search = '' OR u.username ILIKE CONCAT('%', :search, '%')
                   OR u.phone_number ILIKE CONCAT('%', :search, '%')
                   OR u.email ILIKE CONCAT('%', :search, '%'))
            """,
            nativeQuery = true)
    Page<User> findClients(@Param("search") String search, Pageable pageable);

    List<User> findAllByPhoneNumberIsNullAndProfileIdIsNotNull();

    /**
     * The account an OTP sign-in belongs to: the one whose username is the number the code was sent to
     * (accounts created by the OTP flow itself are named that way) or any account carrying the same
     * digits in its phone column. Agent phones were typed into a free-text admin field while the
     * client always sends E.164, so formatting must not decide the match. One number can hold both a
     * client account opened by an earlier OTP sign-in and the agent account an admin created for the
     * same person, and the agent one wins - that is where the listings live.
     */
    @Query(value = """
            SELECT * FROM users u
            WHERE u.is_deleted = false
              AND (u.username = :username
                   OR (:digits <> '' AND regexp_replace(u.phone_number, '\\D', '', 'g') = :digits))
            ORDER BY CASE
                       WHEN 'AGENT' = ANY(u.authorities) THEN 0
                       WHEN 'ADMIN' = ANY(u.authorities) OR 'MANAGER' = ANY(u.authorities) THEN 1
                       ELSE 2
                     END,
                     u.id ASC
            LIMIT 1
            """, nativeQuery = true)
    Optional<User> findOtpAccount(@Param("username") String username, @Param("digits") String digits);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
