package org.settlehub.iam.core.users.repositories;

import org.settlehub.iam.core.security.enums.ERole;
import org.settlehub.iam.core.security.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Layer between Application and Database which responsible for saving, updating and deleting data.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Method to search for a role in repository of roles.
     * @param name which enumerated in {@link ERole} enum class.
     * @return Optional of {@link Role} object.
     */
    Optional<Role> findByName(ERole name);

    Boolean existsByName(ERole name);
}