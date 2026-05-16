package org.settlehub.iam.core.security.identity;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.security.models.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import org.settlehub.iam.core.security.enums.ERole;
import org.settlehub.iam.core.users.repositories.UserRepository;
import org.settlehub.iam.core.users.repositories.RoleRepository;
import org.settlehub.iam.core.users.services.UsersService;

@Configuration
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Bean
    public CommandLineRunner initDatabase(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UsersService usersService,
            PasswordEncoder passwordEncoder) {
        
        return args -> {
            if (!roleRepository.existsByName(ERole.ROLE_ADMIN)) {
                roleRepository.save(new Role(ERole.ROLE_ADMIN));
                roleRepository.save(new Role(ERole.ROLE_MODERATOR));
                roleRepository.save(new Role(ERole.ROLE_USER));
                roleRepository.save(new Role(ERole.ROLE_VISITOR));
                logger.info("Default Roles were successfully created.");
            }
            
            if (!userRepository.existsByRolesName(ERole.ROLE_ADMIN)) {
                User admin = new User("admin@settlehub", "admin@settlehub", passwordEncoder.encode("settlehub"));
                admin.setFirstName("Admin");
                admin.setIsVerified(true);
                
                Set<Role> roles = new HashSet<>();
                Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
                roles.add(adminRole);
                
                admin.setRoles(roles);
                usersService.add(admin);
                
                logger.info("Default Admin User was successfully created.");
            }
        };
    }
}