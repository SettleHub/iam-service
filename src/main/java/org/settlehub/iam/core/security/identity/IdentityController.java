package org.settlehub.iam.core.security.identity;

import jakarta.validation.Valid;
import org.settlehub.iam.core.security.identity.requests.SignInRequest;
import org.settlehub.iam.core.security.identity.requests.SignUpRequest;
import org.settlehub.iam.core.security.identity.requests.TokenRefreshRequest;
import org.settlehub.iam.core.security.identity.requests.UpdatePasswordRequest;
import org.settlehub.iam.core.security.identity.responses.JwtRefreshResponse;
import org.settlehub.iam.core.security.identity.responses.JwtResponse;
import org.settlehub.iam.core.security.identity.responses.MessageResponse;
import org.settlehub.iam.core.security.identity.responses.WhoAmIResponse;
import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.services.UsersService;
import org.javatuples.Pair;
import org.settlehub.iam.core.events.dto.UserForgotPassword;
import org.settlehub.iam.core.result.OperationOutcome;
import org.settlehub.iam.tools.interfaces.IResourceLoaderService;
import org.settlehub.iam.tools.resources.ResourceLoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.settlehub.iam.core.events.dto.UserEmailVerified;

/**
 * Provide the endpoints to user authorization and authentication
 * by using JWT tokens, user and role repositories, password encoder and authentication manager.
 */
@RestController
@RequestMapping("/identity")
public class IdentityController {
    private static final Logger logger = LoggerFactory.getLogger(IdentityController.class);

    @Value("${template-page.verification}")
    private String VERIFICATION_PAGE_TEMPLATE;

    /**
     * {@link IdentityService} provide identity data managing methods.
     */
    @Autowired
    IdentityService identityService;

    /**
     * {@link UsersService} provide users data managing methods.
     */
    @Autowired
    UsersService usersService;

    /**
     * {@link ResourceLoaderService} provide loading resource files methods.
     */
    @Autowired
    IResourceLoaderService resourceLoaderService;

    /**
     * {@link KafkaTemplate} Template for publishing asynchronous events to Kafka topics.
     */
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Endpoint for user authorization in service for accessing additional rights.
     * @param signInRequest is a {@link SignInRequest} object.
     * @return Response is a {@link JwtResponse} object which contains JWT Token for Bearer authentication and some user credentials.
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody SignInRequest signInRequest) {
        try {
            JwtResponse response = identityService.authenticate(signInRequest);
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException e) {
            logger.warn("Attempt to login when user does not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("User doesn't verified.");
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }
    }

    /**
     * Endpoint for user registration. User credentials will be saving if all conditions are met.
     * @param signUpRequest is a {@link SignUpRequest} object.
     * @return Response is a simple {@link MessageResponse} object.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        OperationOutcome status = identityService.register(signUpRequest);
        if (status.equals(OperationOutcome.SUCCESSFUL)) {
            return ResponseEntity.status(HttpStatus.OK).body("User registered successfully.");
        } else if (status.equals(OperationOutcome.ALREADY_EXISTS)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
    }

    /**
     * Endpoint for verification user information and privileges by Bearer authentication.
     * @param authentication is a Bearer authentication with JWT token.
     * @param isJson what the response will look like
     * @return {@link WhoAmIResponse} object or simple {@link String} which contains short user information.
    */
    @GetMapping("/whoami")
    public ResponseEntity<?> whoAmI(Authentication authentication,
        @RequestParam(value = "json", required = false, defaultValue = "false") boolean isJson) {
        return defineUserInformation(authentication, isJson);
    }

    /**
     * In method described logic of getting user object from {@link AuthenticationManager}.
     */
    private ResponseEntity<?> defineUserInformation(Authentication authentication, boolean isJson) {
        if (!authentication.isAuthenticated()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Authentication doesn't valid."));
        }

        Optional<User> optional = usersService.getUserFromAuthentication(authentication);
        if (optional.isEmpty()) {
            logger.error("Could not find user by authentication details. UserDetails: {};",  ((UserDetailsImpl) authentication.getPrincipal()).toString());
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new MessageResponse("Could not find user by authentication details."));
        }
        User user = optional.get();
        WhoAmIResponse response = new WhoAmIResponse(user);

        if (isJson) {
            return ResponseEntity.ok(response);
        } else {
            String simpleResponse = 
                "Username:   \t" + response.getUsername() + "\n" +
                "Email:      \t" + response.getEmail() + "\n" +
                "Full Name:  \t" + response.getFullName() + "\n" +
                "Roles:      \t" + response.getRoles() + "\n" +
                "Last signed:\t" + response.getLastSignDate() + "\n";

            return ResponseEntity.ok(simpleResponse);
        }
    }

    @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> verifyUser(@RequestParam("code") String code) throws IOException {
        Optional<User> user = usersService.getUserByVerificationCode(code);
        OperationOutcome status = identityService.verify(user, code);

        if (status == OperationOutcome.SUCCESSFUL) {

            InputStream stream = resourceLoaderService.getInputStreamFromResourceFile(VERIFICATION_PAGE_TEMPLATE);
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            String message = "<p>Congratulations</p>\n"
                            +"<p>Your email address has been successfully verified.</p>";
            String finalHtml = template.replace("AREA_FOR_PLACING_TEXT", message);

            String username = user.get().getUsername();
            UserEmailVerified event = new UserEmailVerified(username);
            kafkaTemplate.send("user-email-verified", username, event);
            logger.info("Event sent to Kafka topic 'user-email-verified' for user: {}", username);

            return ResponseEntity.status(HttpStatus.OK)
                    .contentType(MediaType.TEXT_HTML)
                    .body(finalHtml);

        } else if (status == OperationOutcome.INVALID_DATA) {

            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body("Verification failed: invalid user data.");
        
        } else if (status == OperationOutcome.NOT_FOUND) {
        
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Verification failed: user not found.");
        
        } else if (status == OperationOutcome.UPDATE_FAILED) {
        
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Verification failed: incorrect code.");
        
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
    }

    @PostMapping(value = "/forgotPassword")
    public ResponseEntity<?> forgotPassword(@RequestParam("email") String username) throws IOException {
        logger.info("Reset password request: (username: {};)", username);
        
        Pair<OperationOutcome, String> status = identityService.forgotPassword(username);

        if (status.getValue0() == OperationOutcome.SUCCESSFUL) {

            UserForgotPassword event = new UserForgotPassword (
                username,
                status.getValue1()
            );

            kafkaTemplate.send("user-forgot-password", username, event);
            logger.info("Event sent to Kafka topic 'user-forgot-password' for user: {}", username);
            return ResponseEntity.status(HttpStatus.OK).body("Successfully generated the code.");

        } else if (status.getValue0() == OperationOutcome.INVALID_DATA) {
            
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body("Reset password failed: invalid user data.");
        
        } else if (status.getValue0() == OperationOutcome.NOT_FOUND) {
            
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reset password failed: user not found.");
        
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
    }

    @PostMapping(value = "/forgotPassword/verify")
    public ResponseEntity<?> verifyForgotPasswordCode(@RequestParam("email") String username,
                                                      @RequestParam("code") String code) {
        logger.info("Forgot password code verification request: (username: {}; code: {};)", username, code);
        OperationOutcome status = identityService.verifyForgotPasswordCode(username, code);
        if (status == OperationOutcome.SUCCESSFUL) {
            return ResponseEntity.status(HttpStatus.OK).body("Code verification was successful.");
        } else if (status == OperationOutcome.INVALID_DATA) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Code verification check failed: invalid code.");
        } else if (status == OperationOutcome.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Code verification check failed: user not found.");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
    }

    @PostMapping(value = "/forgotPassword/update")
    public ResponseEntity<?> updatePasswordByResetPasswordCode(@RequestBody UpdatePasswordRequest request) {
        logger.info("Reset forgot password request: (username: {}; code: {};)", request.getUsername(), request.getCode());
        OperationOutcome status = identityService.updatePasswordByResetPasswordCode(request);
        if (status == OperationOutcome.SUCCESSFUL) {
            return ResponseEntity.status(HttpStatus.OK).body("Successfully reset password.");
        } else if (status == OperationOutcome.INVALID_DATA) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Reset forgot password failed: request contains invalid data.");
        } else if (status == OperationOutcome.NOT_FOUND) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Reset forgot password failed: user not found.");
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
    }


    /**
     * Endpoint for refreshing the Access Token using a valid Refresh Token.
     * <p>
     * This allows clients to maintain a session without forcing the user to 
     * sign in again, while keeping the lifespan of the Access Token short for security.
     * </p>
     * @param request is a {@link TokenRefreshRequest} object containing the refresh token.
     * @return Response is a {@link JwtResponse} with a new Access Token (and potentially a new Refresh Token).
     */
    @PostMapping(value = { "/refresh", "/refresh/" })
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        logger.info("Attempting to refresh user token.");

        try {
            JwtRefreshResponse response = identityService.refreshToken(requestRefreshToken);
            
            logger.info("Token successfully refreshed.");
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new MessageResponse(e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Internal error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error during token refresh."));
        }
    }


    /**
     * Terminated the user session and clears the security context.
     * <p>
     * Note: Since the system uses stateless JWT authentication, the server cannot 
     * "forcefully" invalidate a token without a centralized storage. 
     * Complete logout must be handled on the client-side by discarding the token.
     * </p>
     * * @param authentication the current {@link Authentication} object.
     * @return a {@link ResponseEntity} confirming the logout action.
     */
    @GetMapping(value = { "/logout", "/logout/"})
    public ResponseEntity<?> logout(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            logger.info("User logged out successfully: {}", username);
            
            /*
             * TODO: Implement Server-Side Token Blacklisting (Technical Debt)
             *
             * Current implementation relies on client-side token disposal. 
             * For enhanced security, we should implement a 'Blacklist' service 
             * (using Redis or a similar distributed cache). 
             * When a user logs out, their current JWT should be added to the 
             * blacklist with a TTL (Time-To-Live) equal to the remaining 
             * token expiration time. 
             * * A custom Security Filter should then check every request's token 
             * against this blacklist.
             */
        }

        // Clear the context for the current request thread
        SecurityContextHolder.clearContext();

        return ResponseEntity.ok(new MessageResponse("Log out successful. " +
                "Reminder: Ensure the JWT is deleted from the client storage."));
    }

}

