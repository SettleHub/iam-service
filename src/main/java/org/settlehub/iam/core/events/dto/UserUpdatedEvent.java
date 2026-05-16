package org.settlehub.iam.core.events.dto;

/**
 * Event DTO that will be sent to Kafka when user update contact information.
 */
public record UserUpdatedEvent (
    Long user_id,
    String email,
    String phone,
    String birthDate
) {}

