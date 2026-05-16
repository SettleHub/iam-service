package org.settlehub.iam.core.users.controllers;

import java.util.*;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.services.UsersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Internal Machine-to-Machine (M2M) Communication.
 * <p>
 * Provides endpoints for other microservices to fetch user data, bypassing 
 * standard user JWT authentication. Access to these endpoints must be strictly 
 * restricted at the API Gateway or network level.
 * </p>
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUsersController {
    private final Logger logger = LoggerFactory.getLogger(InternalUsersController.class);

    @Autowired
    private UsersService usersService;

    /**
     * Retrieves a set of all registered users for system operations.
     * * @return a {@link ResponseEntity} containing a collection of users.
     */
    @GetMapping(value = { "/all", "/all/" })
    public ResponseEntity<Set<User>> getAllUsers(
        @RequestHeader(value = "X-Service-Name", defaultValue = "UNKNOWN_SERVICE") String serviceName) {

        logger.info("Internal request: Fetching ALL users. Requested by: [{}]", serviceName);
        return ResponseEntity.ok(usersService.getAll());
    }

    /**
     * Retrieves a specific user by their unique database identifier.
     * * @param id the unique ID of the user.
     * @return a {@link ResponseEntity} containing the user if found, or HTTP 404 otherwise.
     */
    @GetMapping(value = { "/{id}", "/{id}/" })
    public ResponseEntity<?> getUserById(
        @RequestHeader(value = "X-Service-Name", defaultValue = "UNKNOWN_SERVICE") String serviceName,
        @PathVariable("id") Long id) {

        logger.info("Internal request: Fetching user by ID [{}]. Requested by: [{}]", id, serviceName);

        Optional<User> user = usersService.getUserById(id);
        if (user.isEmpty()) {
            logger.warn("Internal request failed: User with ID [{}] not found. Requested by: [{}]", id, serviceName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user.get());
    }

    /**
     * Retrieves a specific user by their username.
     * * @param username the username to search for.
     * @return a {@link ResponseEntity} containing the user if found, or HTTP 404 otherwise.
     */
    @GetMapping(value = { "/username/{username}", "/username/{username}/" })
    public ResponseEntity<?> getUserByUsername(
        @RequestHeader(value = "X-Service-Name", defaultValue = "UNKNOWN_SERVICE") String serviceName,
        @PathVariable("username") String username) {

        logger.info("Internal request: Fetching user by username [{}]. Requested by: [{}]", username, serviceName);

        Optional<User> user = usersService.getUserByUsername(username);
        if (user.isEmpty()) {
            logger.warn("Internal request failed: User [{}] not found. Requested by: [{}]", username, serviceName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user.get());
    }
    
    /**
     * Retrieves a specific user by their email address.
     * * @param email the email address to search for.
     * @return a {@link ResponseEntity} containing the user if found, or HTTP 404 otherwise.
     */
    @GetMapping(value = { "/email/{email}", "/email/{email}/" })
    public ResponseEntity<?> getUserByEmail(
        @RequestHeader(value = "X-Service-Name", defaultValue = "UNKNOWN_SERVICE") String serviceName,
        @PathVariable("email") String email) {

        logger.info("Internal request: Fetching user by email [{}]. Requested by: [{}]", email, serviceName);

        Optional<User> user = usersService.getUserByEmail(email);
        if (user.isEmpty()) {
            logger.warn("Internal request failed: User with email [{}] not found. Requested by: [{}]", email, serviceName);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(user.get());
    }

}
