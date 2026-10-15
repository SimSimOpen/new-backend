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

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
