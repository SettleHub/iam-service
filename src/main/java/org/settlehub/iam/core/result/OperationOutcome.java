package org.settlehub.iam.core.result;

/**
 * Represents the outcome of a business operation.
 */
public enum OperationOutcome {

    /**
     * The operation was completed successfully.
     */
    SUCCESSFUL,

    /**
     * An entity with the same unique identifier already exists.
     */
    ALREADY_EXISTS,

    /**
     * The requested entity was not found.
     */
    NOT_FOUND,

    /**
     * Failed to create the entity due to a database or validation error.
     */
    CREATION_FAILED,

    /**
     * Failed to update the entity due to an invalid state or missing data.
     */
    UPDATE_FAILED,

    /**
     * Failed to delete the entity (it may not exist or deletion is restricted).
     */
    DELETE_FAILED,

    /**
     * The provided data is invalid or incomplete.
     */
    INVALID_DATA,

    /**
     * The requested status transition is not allowed for the current entity.
     */
    INVALID_STATUS_TRANSITION,

    /**
     * The operation is not supported for the current entity type or state.
     */
    UNSUPPORTED_OPERATION,

    /**
     * The operation is not allowed for the current user or entity state.
     */
    NOT_ALLOWED_OPERATION,

    /**
     * The operation resulted in multiple statuses.
     */
    MULTI_STATUS_OPERATION
}
