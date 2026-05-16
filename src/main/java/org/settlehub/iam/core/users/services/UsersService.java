package org.settlehub.iam.core.users.services;

import org.settlehub.iam.core.security.enums.ERole;
import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.repositories.UserRepository;
import org.settlehub.iam.core.users.requests.UpdateContactsRequest;
import org.settlehub.iam.core.result.OperationOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsersService {
    private static final Logger logger = LoggerFactory.getLogger(UsersService.class);

    /**
     * Interface {@link UserRepository} is a layer for accessing users table in database.
     */
    @Autowired
    UserRepository userRepository;

    public Optional<User> getUserFromAuthentication(Authentication authentication) {
        if (!authentication.isAuthenticated()) return Optional.empty();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return userRepository.findByUsername(userDetails.getUsername());
    }

    public Boolean isVerified(User user) {
        return user.getIsVerified();
    }

    public Boolean hasAnyRole(User user) {
        return user.getRoles().stream()
            .anyMatch(role ->
                role.getName().equals(ERole.ROLE_USER) ||
                role.getName().equals(ERole.ROLE_MODERATOR) ||
                role.getName().equals(ERole.ROLE_ADMIN));
    }

    public Boolean hasModeratorOrAdminRole(User user) {
        return user.getRoles().stream()
            .anyMatch(role ->
                role.getName().equals(ERole.ROLE_MODERATOR) ||
                role.getName().equals(ERole.ROLE_ADMIN));
    }

    public Set<User> getAll() {
        return this.userRepository.findAll()
            .stream()
            .map(user -> (User) user)
            .collect(Collectors.toSet());
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByVerificationCode(String code) {
        return userRepository.findByVerificationCode(code);
    }

    public Optional<String> getVerificationCodeByUsername(String username) {
        return userRepository.findAll().stream()
            .filter(user -> user.getUsername().equals(username))
            .map(User::getVerificationCode)
            .findFirst();
    }

    private Optional<User> findByFullName(String fullName) {
        return userRepository.findAll()
            .stream()
            .filter(user -> (user.getLastName() + " " + user.getFirstName() + " " + user.getMiddleName()).contains(fullName))
            .findFirst();
    }

    private Optional<User> findByFullName(String lastName, String firstName, String middleName) {
        return userRepository.findAll()
            .stream()
            .filter(user -> (user.getLastName() + " " + user.getFirstName() + " " + user.getMiddleName()).contains(lastName + firstName + middleName))
            .findFirst();
    }

    public OperationOutcome add(User newUser) {
        if (isAlreadyExists(newUser)) {
            return OperationOutcome.ALREADY_EXISTS;
        } else {
            userRepository.save(newUser);
            logger.info("User registered: {}", newUser);
            return OperationOutcome.SUCCESSFUL;
        }
    }

    public OperationOutcome update(User target, User updated) {
        if (userRepository.findById(target.getId()).isEmpty()) return OperationOutcome.NOT_FOUND;
        if (!(target.getId().equals(updated.getId()))) return OperationOutcome.INVALID_DATA;
        if (!(target.getUsername().equals(updated.getUsername()))) return OperationOutcome.INVALID_DATA;
        userRepository.save(updated);
        return OperationOutcome.SUCCESSFUL;
    }

    public OperationOutcome updateById(Long id, User updated) {
        Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) return OperationOutcome.NOT_FOUND;
        return update(optional.get(), updated);
    }

    public OperationOutcome updateContactsById(Long id, UpdateContactsRequest updatedContacts) {
        Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) return OperationOutcome.NOT_FOUND;
        User user = optional.get();
        user.setEmail(updatedContacts.getEmail());
        user.setPhone(updatedContacts.getPhone());
        user.setBirthDate(updatedContacts.getBirthDate());
        return updateById(id, user);
    }

    public OperationOutcome delete(User target) {
        Optional<User> optional = userRepository.findById(target.getId());
        if (optional.isEmpty()) return OperationOutcome.NOT_FOUND;
        userRepository.delete(target);
        logger.info("Deleted {}", target);
        return OperationOutcome.SUCCESSFUL;
    }

    public OperationOutcome deleteById(Long id) {
        Optional<User> optional = userRepository.findById(id);
        if (optional.isEmpty()) return OperationOutcome.NOT_FOUND;
        return delete(optional.get());
    }

    public boolean isAlreadyExists(User user) {
        if (user.getId() != null && getUserById(user.getId()).isPresent()) {
            return true;
        }

        if (user.getUsername() != null && getUserByUsername(user.getUsername()).isPresent()) {
            return true;
        }

        if (user.getEmail() != null && getUserByEmail(user.getEmail()).isPresent()) {
            return true;
        }

        if (user.getFirstName() != null && !user.getFirstName().isEmpty() &&
            user.getLastName() != null && !user.getLastName().isEmpty() &&
            user.getMiddleName() != null && !user.getMiddleName().isEmpty() &&
            findByFullName(user.getLastName(), user.getFirstName(), user.getMiddleName()).isPresent()
        ) {
            return true;
        }

        return false;
    }
}