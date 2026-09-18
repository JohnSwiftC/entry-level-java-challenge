package com.challenge.api.model;

import java.time.Instant;

/**
 * Body of a termination request.
 *
 * @param contractTerminationDate when the contract ends; optional, defaulting to now. A future date is allowed, because
 *     a last working day is usually known before it arrives.
 */
public record TerminateEmployeeRequest(Instant contractTerminationDate) {}
