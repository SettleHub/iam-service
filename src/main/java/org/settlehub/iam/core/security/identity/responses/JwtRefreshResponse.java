package org.settlehub.iam.core.security.identity.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;
import org.settlehub.iam.core.security.jwt.AuthorizationTypes;

/**
 * Refresh Token Response.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JwtRefreshResponse {
    /**
     * String which contain jwt key for client Bearer authorization.
     */
    private String jwt;

    /**
     * Type of Authorization mechanism
     * Specified to comply with RFC 6750 for OAuth 2.0
     */
    private String type = AuthorizationTypes.BEARER;

    /**
     * User id from database.
     */
    private long id;

    /**
     * Username with which the authorisation was carried out.
     */
    private String username;

    /**
     * User email address (is also username).
     */
    private String email;

    /**
     * User roles in on service.
     */
    private List<String> roles;


    public JwtRefreshResponse(String jwt, long id, String username, String email, List<String> roles) {
        this.jwt = jwt;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
    
}
