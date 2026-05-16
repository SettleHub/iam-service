package org.settlehub.iam.core.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;

import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.security.core.GrantedAuthority;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for generating, validating and parsing JWT tokens.
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties("jwt-service")
public class JwtService {
    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    /**
     * JWT Secret key for signing tokens.
     */
    @Value("${jwt-service.secret-key}")
    public String SECRET_KEY;

    /**
     * Token expiration time in ms.
     */
    @Getter
    @Value("${jwt-service.expiration-time-ms}")
    private long EXPIRATION_TIME;

    /**
     * Refresh Token expiration time in ms.
     */
    @Getter
    @Value("${jwt-service.refresh-expiration-time-ms}")
    private long REFRESH_EXPIRATION_TIME;

    /**
     * 
     */
    private final static String ACCESS_TOKEN_TYPE = "access";

    /**
     * 
     */
    private final static String REFRESH_TOKEN_TYPE = "refresh";


    /**
     * Generates a JSON Web Token (JWT) for the specified user.
     * @param username  the username (subject) to be included in the token.
     * @param roles     the list of user roles to be added as claims.
     * @param expirationTime the validity duration of the token in milliseconds.
     * @return a signed JWT string.
     */
    public String generateTokenFromUsername(String username, List<String> roles, String type, long expirationTime) {
        return Jwts.builder()
            .setSubject(username)
            .claim("roles", roles)
            .claim("type", type)
            .setIssuedAt(new Date())
            .setExpiration(new Date((new Date()).getTime() + expirationTime))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * Generate a JWT Token with Secret Key signing.
     * Is a Short-Lived JWT Token.
     * @param authentication using user credentials.
     * @return generated JWT token.
     */
    public String generateJwtToken(Authentication authentication) {
        UserDetailsImpl userPrincipal = (UserDetailsImpl) authentication.getPrincipal();
        return generateTokenFromUsername(
            userPrincipal.getUsername(), 
            getRolesFromUserPrincipals(userPrincipal),
            ACCESS_TOKEN_TYPE,
            EXPIRATION_TIME
        );
    }

    /**
     * Generate a JWT Token with Secret Key signing.
     * Is a Long-Lived JWT Token.
     * @param authentication using user credentials.
     * @return generated JWT token.
     */
    public String generateRefreshToken(Authentication authentication) {
        return generateTokenFromUsername(
            ((UserDetailsImpl) authentication.getPrincipal()).getUsername(), 
            List.of(), 
            REFRESH_TOKEN_TYPE,
            REFRESH_EXPIRATION_TIME
        );
    }

    private List<String> getRolesFromUserPrincipals(UserDetailsImpl userPrincipal) {
        return userPrincipal.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(Collectors.toList());
    }

    /**
     * Generates a cryptographic key for HMAC-SHA signing using the JWT secret key.
     * @return a {@link Key} instance derived from the base64-decoded secret key.
     */
    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
    }

    /**
     * Parse a username from token body.
     * @param token a JWT token in its own person.
     * @return parsed username string.
     */
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().setSigningKey(key()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    /**
     * Parse a user roles from token claims.
     * @param token a JWT token in its own person.
     * @return Collection of user roles.
     */
    public Set<String> getRolesFromJwtToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(key()).build().parseClaimsJws(token).getBody();
        return claims.get("roles", Set.class);
    }

    /**
     * Parse a token type from token claims.
     * @param token a JWT token in its own person.
     * @return type of the token (e.g. "access" or "refresh").
     */
    public String getTokenTypeFromJwtToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(key()).build().parseClaimsJws(token).getBody();
        return claims.get("type", String.class);
    }

    /**
     * Validate a token using the signing key.
     * @param authToken a JWT token in its own person.
     * @return result of token validating.
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Claims claims = Jwts.parser().setSigningKey(key()).build().parseClaimsJws(authToken).getBody();
            logger.info("JWT valid, roles: {}", claims.get("roles"));
            return true;
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

}
