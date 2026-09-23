package net.jemsit.profile.data.repository;

import net.jemsit.profile.data.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    boolean existsByProfileId(String code);

    Optional<UserProfile> findByUserId(Long userId);
}
