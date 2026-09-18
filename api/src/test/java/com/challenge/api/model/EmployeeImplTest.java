package com.challenge.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Covers EmployeeImpl logic, especially working with fullName
 */
class EmployeeImplTest {

    @Test
    void fullNameIsDerivedFromFirstAndLastName() {
        EmployeeImpl employee =
                EmployeeImpl.builder().firstName("Ada").lastName("Lovelace").build();

        assertThat(employee.getFullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void fullNameFallsBackToWhicheverNameIsPresent() {
        assertThat(EmployeeImpl.builder().firstName("Grace").build().getFullName())
                .isEqualTo("Grace");
        assertThat(EmployeeImpl.builder().lastName("Hopper").build().getFullName())
                .isEqualTo("Hopper");
        assertThat(new EmployeeImpl().getFullName()).isNull();
    }

    @Test
    void settingFullNameSplitsItIntoFirstAndLastName() {
        EmployeeImpl employee = new EmployeeImpl();

        employee.setFullName("Alan Turing");

        assertThat(employee.getFirstName()).isEqualTo("Alan");
        assertThat(employee.getLastName()).isEqualTo("Turing");
    }

    @Test
    void settingFullNameKeepsEverythingAfterTheFirstTokenAsTheSurname() {
        EmployeeImpl employee = new EmployeeImpl();

        employee.setFullName("Ana de Armas");

        assertThat(employee.getFirstName()).isEqualTo("Ana");
        assertThat(employee.getLastName()).isEqualTo("de Armas");
    }

    @Test
    void settingASingleTokenFullNameLeavesNoSurname() {
        EmployeeImpl employee = new EmployeeImpl();

        employee.setFullName("  Cher  ");

        assertThat(employee.getFirstName()).isEqualTo("Cher");
        assertThat(employee.getLastName()).isNull();
    }

    @Test
    void settingABlankOrNullFullNameClearsBothNameFields() {
        EmployeeImpl employee =
                EmployeeImpl.builder().firstName("Ada").lastName("Lovelace").build();

        employee.setFullName("   ");

        assertThat(employee.getFirstName()).isNull();
        assertThat(employee.getLastName()).isNull();
        assertThat(employee.getFullName()).isNull();
    }
}
