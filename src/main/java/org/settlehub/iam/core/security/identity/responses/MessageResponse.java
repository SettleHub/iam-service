package org.settlehub.iam.core.security.identity.responses;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Simple String response to client request.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MessageResponse {
    /**
     * One single string which will be returned in response to client request.
     */
    private String message;
}
