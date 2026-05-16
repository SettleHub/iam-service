package org.settlehub.iam.core.security.identity.requests;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Simple authorization request template with using username and password.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignInRequest {
    /**
     * Value contain user email address.
     */
    private String username;

    /**
     * Simply user password for authorization.
     */
    private String password;
}
