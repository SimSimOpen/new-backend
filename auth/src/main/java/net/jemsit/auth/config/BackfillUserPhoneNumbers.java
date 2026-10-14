package net.jemsit.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.jemsit.auth.data.model.User;
import net.jemsit.auth.data.repository.UserRepository;
import net.jemsit.common.api.ProfileApi;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Copies the phone number from each user's profile onto the user row. users.phone_number was added
 * after accounts already existed with the number only on their profile; once every row is filled
 * the query below comes back empty and this does nothing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackfillUserPhoneNumbers implements ApplicationRunner {

    private final UserRepository userRepository;
    private final ProfileApi profileApi;

    @Override
    public void run(ApplicationArguments args) {
        List<User> updated = new ArrayList<>();
        for (User user : userRepository.findAllByPhoneNumberIsNullAndProfileIdIsNotNull()) {
            try {
                String phoneNumber = profileApi.getProfileById(user.getProfileId()).phoneNumber();
                if (phoneNumber != null && !phoneNumber.isBlank()) {
                    user.setPhoneNumber(phoneNumber.trim());
                    updated.add(user);
                }
            } catch (RuntimeException e) {
                log.warn("Skipping phone backfill for user {}: {}", user.getId(), e.getMessage());
            }
        }
        if (!updated.isEmpty()) {
            userRepository.saveAll(updated);
            log.info("Backfilled phone number for {} user(s) from their profiles", updated.size());
        }
    }
}
