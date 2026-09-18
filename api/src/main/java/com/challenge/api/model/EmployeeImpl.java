package com.challenge.api.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Concrete Employee backed by plain fields. This is the shape that gets serialized to JSON, so the field names here are
 * the attribute names the API contract promises.
 *
 * getFullName and setFullName are overridden. It was possible with a default impl to have a different
 * "firstName lastName" than fullName. I chose to instead concatenate the firstName and lastName fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeImpl implements Employee {

    private UUID uuid;

    private String firstName;

    private String lastName;

    private Integer salary;

    private Integer age;

    private String jobTitle;

    private String email;

    private Instant contractHireDate;

    /** Null until the Employee is terminated. */
    private Instant contractTerminationDate;

    /**
     * @return the two name fields joined by a space, whichever of them is present, or null if neither is.
     */
    @Override
    public String getFullName() {
        String first = blankToNull(firstName);
        String last = blankToNull(lastName);
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    /**
     * Splits a full name on its first whitespace.
     *
     * @param name full name to split; null or blank clears both name fields
     */
    @Override
    public void setFullName(String name) {
        String trimmed = blankToNull(name);
        if (trimmed == null) {
            this.firstName = null;
            this.lastName = null;
            return;
        }
        String[] parts = trimmed.split("\s+", 2);
        this.firstName = parts[0];
        this.lastName = parts.length > 1 ? parts[1] : null;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
