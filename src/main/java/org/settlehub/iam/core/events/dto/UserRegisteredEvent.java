package org.settlehub.iam.core.events.dto;

import java.util.Set;

/**
 * Event DTO that will be sent to Kafka when a new user registers.
 */
public record UserRegisteredEvent(
    String username,
    String email,
    Set<String> roles
) {}

