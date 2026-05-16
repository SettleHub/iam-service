package org.settlehub.iam.core.security.identity.responses;

import lombok.Getter;
import lombok.Setter;

import org.settlehub.iam.core.security.models.Role;
import org.settlehub.iam.core.users.models.User;

/**
 * Response message for user credentials validating.
 */
@Getter
@Setter
public class WhoAmIResponse {
    private String username;
    private String email;
    private String fullName;
    private String roles;
    private String lastSignDate;

    /**
     * Single not auto generated constructor for more readable user content.
     * @param user is a {@link User} object which was received using the token provided by the user.
     */
    public WhoAmIResponse(User user) {
        username = user.getUsername().isEmpty() ? "''" : user.getUsername();

        email = user.getEmail().isEmpty() ? "''" : user.getEmail();

        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
        String middleName = user.getMiddleName() != null ? user.getMiddleName() : "";
        String lastName = user.getLastName() != null ? user.getLastName() : "";
        fullName = (firstName.isEmpty() && middleName.isEmpty() && lastName.isEmpty()) ? "''" : lastName + " " + firstName + " " + middleName;

        StringBuilder rolesBuilder = new StringBuilder();
        rolesBuilder.append("[ ");
        for (Role role : user.getRoles()) {
            if (!rolesBuilder.toString().endsWith(" ")) {
                rolesBuilder.append(", ");
            }
            rolesBuilder.append(role.getName());
        }
        rolesBuilder.append(" ]");
        roles = rolesBuilder.toString();

        lastSignDate = (user.getLastSignDate() == null || user.getLastSignDate().toString().isEmpty()) ? "''" : user.getLastSignDate().toString();
    }
}
