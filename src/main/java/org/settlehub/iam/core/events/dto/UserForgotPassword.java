package org.settlehub.iam.core.events.dto;

/**
 * Event DTO that will be sent to Kafka when a new user registers.
 */
public record UserForgotPassword(
    String email,
    String code
) {}

