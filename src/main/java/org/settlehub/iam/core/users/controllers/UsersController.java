package org.settlehub.iam.core.users.controllers;

import java.util.*;
import java.util.stream.Collectors;
import org.settlehub.iam.core.security.identity.IdentityService;
import org.settlehub.iam.core.security.identity.requests.SignUpRequest;
import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.requests.UpdateContactsRequest;
import org.settlehub.iam.core.users.services.UsersService;
import org.settlehub.iam.core.events.dto.UserUpdatedEvent;
import org.settlehub.iam.core.result.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UsersController {
    private final Logger logger = LoggerFactory.getLogger(UsersController.class);

    /**
     * {@link IdentityService} provide identity data managing methods.
     */
    @Autowired
    IdentityService identityService;
    
    @Autowired
    private UsersService usersService;

    /**
     * {@link KafkaTemplate} Template for publishing asynchronous events to Kafka topics.
     */
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;


    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<Set<User>> getAllUsers() {
        return ResponseEntity.status(HttpStatus.OK).body(usersService.getAll());
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_MODERATOR', 'ROLE_ADMIN')")
    @GetMapping("/get")
    public ResponseEntity<?> getUserById(Authentication authentication,
                                         @RequestParam(value = "id", required = false) Long id,
                                         @RequestParam(value = "username", required = false) String username,
                                         @RequestParam(value = "email", required = false) String email) {
        Optional<User> optAuthUser = usersService.getUserFromAuthentication(authentication);
        if (optAuthUser.isEmpty()) {
            logger.warn("Access denied for {}", ((UserDetailsImpl) authentication.getPrincipal()).toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Compromised user.");
        }
        User authUser = optAuthUser.get();
        if (authUser.getId().equals(id) || usersService.hasModeratorOrAdminRole(authUser)) {
            Optional<User> optUser;
            if (id != null) {
                optUser = usersService.getUserById(id);
            } else if (username != null) {
                optUser = usersService.getUserByUsername(username);
            } else if (email != null) {
                optUser = usersService.getUserByEmail(email);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data.");
            }

            if (optUser.isEmpty()) {
                logger.info("User not found by:{id={}, username={}, email={}}", id, username, email);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found by:{id="+id+", username="+username+", email="+email+"}");
            } else {
                User user = optUser.get();
                return ResponseEntity.status(HttpStatus.OK).body(user);
            }
        } else {
            logger.warn("Access denied for {}", authUser);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
    }

    // @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    // @GetMapping("/search")
    // public ResponseEntity<?> getUserByQuery(@RequestParam("query") String query) {
    //     List<User> queriedUsers = service.getUserByQuery(query);
    //     if (queriedUsers.isEmpty()) {
    //         logger.info("Users by query={} not found.", query);
    //         return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Users by query=" + query + " not found.");
    //     } else {
    //         return ResponseEntity.status(HttpStatus.OK).body(queriedUsers);
    //     }
    // }

    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody User newUser) {
        Set<String> strRoles = newUser.getRoles().stream()
            .map(role -> role.getName().toString())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        OperationOutcome status = identityService.register(
            new SignUpRequest(newUser.getUsername(), newUser.getEmail(), newUser.getPassword(), strRoles)
        );

        if (status.equals(OperationOutcome.SUCCESSFUL)) {
            User registeredUser = usersService.getUserByUsername(newUser.getUsername()).get();
            refillPersonalData(registeredUser, newUser);
            OperationOutcome updateStatus = usersService.updateById(registeredUser.getId(), registeredUser);
            if (updateStatus.equals(OperationOutcome.SUCCESSFUL)) {
                return ResponseEntity.status(HttpStatus.OK).body("User added successfully.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS:" + status);
            }
        } else if (status.equals(OperationOutcome.ALREADY_EXISTS)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS:" + status);
        }
    }

    private void refillPersonalData(User registered, User templateUser) {
        registered.setFirstName(templateUser.getFirstName());
        registered.setMiddleName(templateUser.getMiddleName());
        registered.setLastName(templateUser.getLastName());
        registered.setBirthDate(templateUser.getBirthDate());
        registered.setPhotoUrl(templateUser.getPhotoUrl());;
        registered.setAvatarUrl(templateUser.getAvatarUrl());
        registered.setPhone(templateUser.getPhone());
    }

    private record UserOperationResult(User user, OperationOutcome status) { }

    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @PostMapping("/add/many")
    public ResponseEntity<?> addMany(@RequestBody Set<User> users) {
        for (User newUser : users) {
            if (usersService.isAlreadyExists(newUser)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exist. " + newUser.toString());
            }
        }

        List<UserOperationResult> results = new ArrayList<>();
        for (User newUser : users) {
            Set<String> strRoles = newUser.getRoles().stream()
                    .map(role -> role.getName().toString())
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            OperationOutcome status = identityService.register(
                    new SignUpRequest(newUser.getUsername(), newUser.getEmail(), newUser.getPassword(), strRoles));
            Optional<User> registeredUser = usersService.getUserByUsername(newUser.getUsername());
            if (status.equals(OperationOutcome.SUCCESSFUL) && registeredUser.isPresent()) {
                refillPersonalData(registeredUser.get(), newUser);
            }
            results.add(new UserOperationResult(newUser, status));
        }
        boolean allSuccessful = results.stream()
            .allMatch(result -> result.status.equals(OperationOutcome.SUCCESSFUL));
        HttpStatus responseStatus = allSuccessful ? HttpStatus.OK : HttpStatus.MULTI_STATUS;

        return ResponseEntity.status(responseStatus).body(results);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @PutMapping("/update")
    public ResponseEntity<?> updateById(@RequestParam("id") Long id, @RequestBody User updated) {
        OperationOutcome status = usersService.updateById(id, updated);
        if (status.equals(OperationOutcome.SUCCESSFUL)) {
            UserUpdatedEvent event = new UserUpdatedEvent (
                id,
                updated.getEmail(),
                updated.getPhone(),
                updated.getBirthDate()
            );

            kafkaTemplate.send("user-updated-contacts", id.toString(), event);
            logger.info("Event sent to Kafka topic 'user-updated-contacts' for user_id: {}", id);
            return ResponseEntity.status(HttpStatus.OK).body("User updated successfully.");
        } else if (status.equals(OperationOutcome.NOT_FOUND)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } else if (status.equals(OperationOutcome.INVALID_DATA)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS:" + status);
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_MODERATOR', 'ROLE_ADMIN')")
    @PutMapping("/update-contacts")
    public ResponseEntity<?> updateContactsById(Authentication authentication,
                                                @RequestParam("id") Long id,
                                                @RequestBody UpdateContactsRequest updatedContacts)
    {
        Optional<User> optAuthUser = usersService.getUserFromAuthentication(authentication);
        if (optAuthUser.isEmpty()) {
            logger.warn("Access denied for {}", ((UserDetailsImpl) authentication.getPrincipal()).toString());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Compromised user.");
        }
        User authUser = optAuthUser.get();
        if (authUser.getId().equals(id) || usersService.hasModeratorOrAdminRole(authUser)) {
            OperationOutcome status = usersService.updateContactsById(id, updatedContacts);
                if (status.equals(OperationOutcome.SUCCESSFUL)) {
                    UserUpdatedEvent event = new UserUpdatedEvent (
                    id,
                    updatedContacts.getEmail(),
                    updatedContacts.getPhone(),
                    updatedContacts.getBirthDate()
                );

                kafkaTemplate.send("user-updated-contacts", id.toString(), event);
                logger.info("Event sent to Kafka topic 'user-updated-contacts' for user_id: {}", id);

                return ResponseEntity.status(HttpStatus.OK).body("User contacts updated successfully.");
            } else if (status.equals(OperationOutcome.NOT_FOUND)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
            } else if (status.equals(OperationOutcome.INVALID_DATA)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid data.");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS:" + status);
            }
        } else {
            logger.warn("Access denied for {}", authUser);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied.");
        }
    }

    @PreAuthorize("hasAnyAuthority('ROLE_MODERATOR', 'ROLE_ADMIN')")
    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteById(@RequestParam("id") Long id) {
        OperationOutcome status = usersService.deleteById(id);
        if (status.equals(OperationOutcome.SUCCESSFUL)) {
            return ResponseEntity.status(HttpStatus.OK).body("User deleted successfully.");
        } else if (status.equals(OperationOutcome.NOT_FOUND)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("STATUS: " + status);
        }
    }
}
