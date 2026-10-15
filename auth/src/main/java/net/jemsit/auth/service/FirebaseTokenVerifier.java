package net.jemsit.auth.service;

import lombok.extern.slf4j.Slf4j;
import net.jemsit.common.exceptions.UserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Checks a Firebase Authentication ID token the way the Firebase docs describe for third-party JWT
 * libraries: RS256 signed by one of Google's securetoken keys, issuer and audience naming our project,
 * and not expired. Only the public keys are needed, so there is no service-account file to manage.
 *
 * <p>Firebase has already sent and checked the SMS code by the time the client has a token, so the
 * verified {@code phone_number} claim is as good as our own OTP check.
 */
@Service
@Slf4j
public class FirebaseTokenVerifier {

    private static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

    private final String projectId;
    private final JwtDecoder decoder;

    public FirebaseTokenVerifier(@Value("${firebase.project-id:}") String projectId) {
        this.projectId = projectId == null ? "" : projectId.trim();
        this.decoder = this.projectId.isEmpty() ? null : buildDecoder(this.projectId);
        if (this.decoder == null) {
            log.warn("firebase.project-id is not set: phone sign-in through Firebase is disabled");
        }
    }

    /** Returns the E.164 phone number the token was issued for, e.g. "+998901234567". */
    public String verifyPhoneNumber(String idToken) {
        if (decoder == null) {
            throw new UserException("Phone sign-in is not configured on the server");
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            log.warn("Rejected Firebase ID token for project {}: {}", projectId, e.getMessage());
            throw new UserException("Phone verification failed, please try again");
        }

        Object firebase = jwt.getClaims().get("firebase");
        Object provider = firebase instanceof Map<?, ?> map ? map.get("sign_in_provider") : null;
        String phoneNumber = jwt.getClaimAsString("phone_number");
        if (!"phone".equals(provider) || phoneNumber == null || phoneNumber.isBlank()) {
            throw new UserException("Phone verification failed, please try again");
        }
        return phoneNumber.trim();
    }

    private static JwtDecoder buildDecoder(String projectId) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience() != null && jwt.getAudience().contains(projectId)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong audience", null));
        OAuth2TokenValidator<Jwt> subject = jwt -> jwt.getSubject() != null && !jwt.getSubject().isBlank()
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Missing subject", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("https://securetoken.google.com/" + projectId),
                audience,
                subject));
        return decoder;
    }
}
