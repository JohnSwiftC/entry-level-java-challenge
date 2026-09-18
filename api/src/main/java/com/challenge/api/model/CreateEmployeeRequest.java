package com.challenge.api.model;

import java.time.Instant;

/**
 * Attributes accepted when creating an Employee.
 *
 * Intentionally does not include UUID or a fullName field. UUID is determined by EmployeeService
 * when a new employee is created, and fullName is firstName concatenated with lastName.
 *
 * @param contractTerminationDate optional
 */
public record CreateEmployeeRequest(
        String firstName,
        String lastName,
        Integer salary,
        Integer age,
        String jobTitle,
        String email,
        Instant contractHireDate,
        Instant contractTerminationDate) {}
