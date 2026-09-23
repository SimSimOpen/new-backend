package net.jemsit.auth.service;

import net.jemsit.auth.data.model.User;
import net.jemsit.common.data.enums.Roles;
import net.jemsit.common.exceptions.TokenInvalidException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    public <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts
                    .parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (MalformedJwtException malformedJwtException) {
            throw new TokenInvalidException("Token is invalid");
        } catch (ExpiredJwtException expiredJwtException) {
            throw new TokenInvalidException("Token is expired");
        } catch (JwtException jwtException) {
            throw new TokenInvalidException("Token is invalid");
        }
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public List<Roles> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        List<String> roleStrings = claims.get("roles", List.class);

        if (roleStrings == null || roleStrings.isEmpty()) {
            return Collections.emptyList();
        }

        return roleStrings.stream()
                .map(Roles::valueOf)
                .collect(Collectors.toList());
    }

    public Long extractUserId(String token) {
        Object value = extractAllClaims(token).get("userId");
        return value instanceof Number number ? number.longValue() : null;
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        Set<String> userRoles = new HashSet<>();
        user.getAuthorities().forEach(u -> userRoles.add(u.getAuthority()));
        claims.put("roles", userRoles);
        claims.put("userId", user.getId());
        return generateTokenWithExpireTime(claims, user);
    }

    public String generateTokenWithExpireTime(Map<String, Object> extraClaims, User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(extraClaims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 30)) // 30 min
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(Map<String, Object> extraClaims, User user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .claims(extraClaims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 5)) // 5 hours
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        Set<String> userRoles = new HashSet<>();
        user.getAuthorities().forEach(u -> userRoles.add(u.getAuthority()));
        claims.put("roles", userRoles);
        claims.put("userId", user.getId());
        return generateRefreshToken(claims, user);
    }

}
