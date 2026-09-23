package net.jemsit.profile.data.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfile extends BaseEntity {
    private Long userId;
    private String profileId;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String description;
}
