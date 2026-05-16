package org.settlehub.iam.core.security.identity;

import org.settlehub.iam.core.security.enums.ERole;
import org.settlehub.iam.core.security.identity.requests.SignInRequest;
import org.settlehub.iam.core.security.identity.requests.SignUpRequest;
import org.settlehub.iam.core.security.identity.requests.UpdatePasswordRequest;
import org.settlehub.iam.core.security.identity.responses.JwtRefreshResponse;
import org.settlehub.iam.core.security.identity.responses.JwtResponse;
import org.settlehub.iam.core.security.jwt.JwtService;
import org.settlehub.iam.core.security.models.Role;
import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.repositories.RoleRepository;
import org.settlehub.iam.core.users.services.UsersService;
import org.javatuples.Pair;
import org.settlehub.iam.core.result.OperationOutcome;
import org.settlehub.iam.tools.CodeGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.kafka.core.KafkaTemplate;
import org.settlehub.iam.core.events.dto.UserRegisteredEvent;

@Service
public class IdentityService {
    private static final Logger logger = LoggerFactory.getLogger(IdentityService.class);

    /**
     * Interface {@link AuthenticationManager} provide user authorization by using username and password credentials.
     */
    @Autowired
    AuthenticationManager authenticationManager;

    /**
     * Interface {@link PasswordEncoder} using for hashing user special authorization phrases (simply passwords).
     */
    @Autowired
    PasswordEncoder encoder;

    /**
     * Interface {@link RoleRepository} is a layer for accessing roles table in database.
     */
    @Autowired
    RoleRepository roleRepository;

    /**
     * {@link JwtService} provide a JWT Token generating, validating and others methods.
     */
    @Autowired
    JwtService jwtService;

    /**
     * {@link UsersService} provide users data managing methods.
     */
    @Autowired
    UsersService usersService;

    /**
     * {@link KafkaTemplate} Template for publishing asynchronous events to Kafka topics.
     */
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public JwtResponse authenticate(SignInRequest signInRequest) {
        Authentication authentication = authenticationManager
            .authenticate(new UsernamePasswordAuthenticationToken(signInRequest.getUsername(), signInRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.generateJwtToken(authentication);
        String refreshToken = jwtService.generateRefreshToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
            .map(item -> item.getAuthority())
            .collect(Collectors.toList());

        return new JwtResponse(jwt, refreshToken, userDetails.getId(), userDetails.getUsername(), userDetails.getEmail(), roles);
    }

    public JwtRefreshResponse refreshToken(String requestRefreshToken) {
        if (!jwtService.validateJwtToken(requestRefreshToken)) {
            throw new IllegalArgumentException("Invalid or expired Refresh Token.");
        }

        String tokenType = jwtService.getTokenTypeFromJwtToken(requestRefreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new IllegalArgumentException("Provided token is not a refresh token.");
        }

        String username = jwtService.getUserNameFromJwtToken(requestRefreshToken);

        User user = usersService.getUserByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found for this token."));

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        String newAccessToken = jwtService.generateTokenFromUsername(
                user.getUsername(), 
                roles, 
                "access", // Вказуємо тип нового токена
                jwtService.getEXPIRATION_TIME()
        );

        return new JwtRefreshResponse(newAccessToken, user.getId(), user.getUsername(), user.getEmail(), roles);
    }







    public OperationOutcome register(SignUpRequest signUpRequest) {
        Optional<User> optionalUser = usersService.getUserByUsername(signUpRequest.getUsername());
        if (optionalUser.isPresent()) {
            return OperationOutcome.ALREADY_EXISTS;
        }
        User user = new User(signUpRequest.getUsername(), signUpRequest.getEmail(), encoder.encode(signUpRequest.getPassword()));

        Set<String> strRoles = signUpRequest.getRole();
        logger.info("New user requested roles: {}", strRoles);
        Set<Role> roles = new HashSet<>();

        if (strRoles.isEmpty()) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
            roles.add(userRole);
        } else {
            strRoles.forEach(roleName -> {
                try {
                    Role role = roleRepository.findByName(ERole.valueOf(roleName))
                        .orElseThrow(() -> {
                            logger.error("Requested role {} is not found.", roleName);
                            return new RuntimeException("Error: Role not found.");
                        });
                    roles.add(role);
                } catch (IllegalArgumentException e) {
                    logger.error("Requested role {} is invalid.", roleName);
                }
            });
        }

        user.setRoles(roles);
        OperationOutcome outcome = usersService.add(user);

        if (outcome == OperationOutcome.SUCCESSFUL) {
            UserRegisteredEvent event = new UserRegisteredEvent(
                user.getUsername(),
                user.getEmail(),
                strRoles.isEmpty() ? Set.of("ROLE_USER") : strRoles
            );

            kafkaTemplate.send("user-registrations", user.getUsername(), event);
            logger.info("Event sent to Kafka topic 'user-registrations' for user: {}", user.getUsername());
        }

        return outcome;
    }

    public OperationOutcome verify(User user, String verificationCode) {
        if (user.getVerificationCode().equals(verificationCode)) {
            logger.info("New user requested role: {}", ERole.ROLE_USER.toString());
            Set<Role> roles = new HashSet<>();
            roles.add(
                roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> {
                        logger.error("Requested role {} is not found.", ERole.ROLE_USER.toString());
                        return new RuntimeException("Error: Role not found.");
                    })
            );
            logger.info("New user assigned role: {}", ERole.ROLE_USER.toString());
            user.setRoles(roles);
            user.setIsVerified(true);
            return usersService.updateById(user.getId(), user);
        } else {
            return OperationOutcome.UPDATE_FAILED;
        }
    }

    public OperationOutcome verify(Optional<User> optionalUser, String verificationCode) {
        if (optionalUser.isEmpty()) return OperationOutcome.NOT_FOUND;
        User user = optionalUser.get();
        return verify(user, verificationCode);
    }

    public OperationOutcome verify(String verificationCode) {
        Optional<User> optionalUser = usersService.getUserByVerificationCode(verificationCode);
        if (optionalUser.isEmpty()) return OperationOutcome.NOT_FOUND;
        User user = optionalUser.get();
        return verify(user, verificationCode);
    }

    public Pair<OperationOutcome, String> forgotPassword(String username) {
        Optional<User> optionalUser = usersService.getUserByUsername(username);
        if (optionalUser.isEmpty()) {
            return Pair.with(OperationOutcome.NOT_FOUND, "");
        }
        User user = optionalUser.get();

        String code = CodeGenerator.generateResetPasswordCode();
        user.setResetPasswordCode(code);
        return Pair.with(usersService.updateById(user.getId(), user), code);
    }

    public OperationOutcome verifyForgotPasswordCode(String username, String code) {
        Optional<User> optionalUser = usersService.getUserByUsername(username);
        if (optionalUser.isEmpty()) {
            return OperationOutcome.NOT_FOUND;
        }
        User user = optionalUser.get();

        String existsCode = user.getResetPasswordCode();
        if (!existsCode.isEmpty() && existsCode.equals(code)) {
            return OperationOutcome.SUCCESSFUL;
        }
        return OperationOutcome.INVALID_DATA;
    }

    public OperationOutcome updatePasswordByResetPasswordCode(UpdatePasswordRequest request) {
        if (request.getUsername().isEmpty()
            || request.getPassword().isEmpty()
            || request.getCode().isEmpty())
        {
            return OperationOutcome.INVALID_DATA;
        }

        Optional<User> optionalUser = usersService.getUserByUsername(request.getUsername());
        if (optionalUser.isEmpty()) {
            return OperationOutcome.NOT_FOUND;
        }
        User user = optionalUser.get();
        if (!user.getResetPasswordCode().equals(request.getCode())) {
            return OperationOutcome.INVALID_DATA;
        }
        user.setPassword(encoder.encode(request.getPassword()));
        return usersService.updateById(user.getId(), user);
    }
}
