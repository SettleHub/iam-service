package org.settlehub.iam.core.security.services;

import org.settlehub.iam.core.security.models.UserDetailsImpl;
import org.settlehub.iam.core.users.models.User;
import org.settlehub.iam.core.users.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    /**
     * Interface {@link UserRepository} is a layer for accessing users table in database.
     */
    @Autowired
    UserRepository userRepository;

    /**
     * Loads a user from the database by their username and converts it into a {@link UserDetails} object.
     * This method is used by Spring Security for authentication.
     * @param username the username of the user to be retrieved.
     * @return a {@link UserDetails} representation of the user.
     * @throws UsernameNotFoundException if no user is found with the given username.
     * @throws DisabledException if user with given username is not verified.
     */
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DisabledException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> {
                logger.error("User with username: '{}' is Not Found!", username);
                return new UsernameNotFoundException("User with username: '" + username + "' is Not Found!");
            });

        if (!user.getIsVerified()) {
            logger.error("User with username: '{}' is Not Verified!", username);
            throw new DisabledException("User with username: '" + username + "' is Not Verified!"); 
        }

        return UserDetailsImpl.build(user);
    }
}