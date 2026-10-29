package net.jemsit.auth.data.model;

import net.jemsit.common.data.enums.Roles;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class User extends BaseEntity implements UserDetails {
    @Column(name = "username", unique = true)
    private String username;
    @Column(name = "email", unique = true)
    private String email;
    @Column(name = "password")
    private String password;
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "authorities")
    @Enumerated(EnumType.STRING)
    private List<Roles> authorities;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Token> tokens = new ArrayList<>();

    @Column(name = "profile_id")
    private Long profileId;

    public void addToken(Token token) {
        tokens.add(token);
        token.setUser(this);
    }

    public void removeToken(Token token) {
        tokens.remove(token);
        token.setUser(null);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // The column is nullable, so a legacy row without roles must not take the caller down with an NPE
        if (this.authorities == null) {
            return List.of();
        }
        return this.authorities
                .stream().map(r -> new SimpleGrantedAuthority(r.name()))
                .toList();
    }
}
