package net.jemsit.auth.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tokens")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Token extends BaseEntity {
    // JWTs grow with the username and claims; see WidenTokenColumns for databases created at 255.
    @Column(name = "token", unique = true, length = 1024)
    private String token;
    @Column(name = "refresh_token", unique = true, length = 1024)
    private String refreshToken;

    /**
     * The refresh token this row was rotated from. Still accepted, so a refresh whose response never
     * reached the browser (tab closed, network dropped, two tabs refreshing at once) does not sign
     * the user out.
     */
    @Column(name = "previous_refresh_token", length = 1024)
    private String previousRefreshToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
