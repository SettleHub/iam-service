package org.settlehub.iam.core.security.identity.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenRefreshRequest {
    
    @NotBlank
    private String refreshToken;

}
