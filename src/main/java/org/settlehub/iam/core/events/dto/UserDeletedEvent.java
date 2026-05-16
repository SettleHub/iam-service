package org.settlehub.iam.core.events.dto;

/**
 * Event DTO that will be sent to Kafka when deleted user.
 */
public record UserDeletedEvent (
    String user_id
) {}
