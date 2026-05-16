package org.settlehub.iam.core.security.identity.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Simple forgot password request for updating password.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordRequest {
    /**
     * User username for authorization .
     */
    private String username = "";

    /**
     * Code which user get from sent to him email letter earlier.
     */
    private String code = "";

    /**
     * New user's password for validating.
     */
    private String password = "";
}
