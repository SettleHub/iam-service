package org.settlehub.iam.core.events.dto;

/**
 * Event DTO that will be sent to Kafka when a new user registers.
 */
public record UserEmailVerified(
    String email
) {}

