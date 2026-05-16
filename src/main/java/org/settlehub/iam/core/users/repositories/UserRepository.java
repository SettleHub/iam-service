package org.settlehub.iam.core.users.repositories;

import jakarta.validation.constraints.NotBlank;

import org.settlehub.iam.core.security.enums.ERole;
import org.settlehub.iam.core.users.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);
    
    Boolean existsByUsername(String username);

    Boolean existsByEmail(String email);
    
    Boolean existsByRolesName(ERole roleName);

    String email(@NotBlank String email);
}
