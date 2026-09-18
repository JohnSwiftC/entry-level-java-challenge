package com.challenge.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import com.challenge.api.model.TerminateEmployeeRequest;
import com.challenge.api.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HTTP specific testing
 */
@WebMvcTest(EmployeeController.class)
class EmployeeControllerTest {

    private static final String BASE_PATH = "/api/v1/employee";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void getAllEmployeesReturnsTheEmployeeList() throws Exception {
        when(employeeService.getAllEmployees()).thenReturn(List.of(employee()));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Rosalind"))
                .andExpect(jsonPath("$[0].fullName").value("Rosalind Franklin"))
                .andExpect(jsonPath("$[0].contractHireDate").value("2020-05-04T00:00:00Z"));
    }

    @Test
    void getEmployeeByUuidReturnsTheEmployee() throws Exception {
        Employee employee = employee();
        when(employeeService.getEmployeeByUuid(employee.getUuid())).thenReturn(Optional.of(employee));

        mockMvc.perform(get(BASE_PATH + "/" + employee.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(employee.getUuid().toString()))
                .andExpect(jsonPath("$.contractTerminationDate").doesNotExist());
    }

    @Test
    void getEmployeeByUuidReturnsNotFoundWhenNoEmployeeHasThatUuid() throws Exception {
        UUID uuid = UUID.randomUUID();
        when(employeeService.getEmployeeByUuid(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE_PATH + "/" + uuid))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No employee with uuid " + uuid));
    }

    @Test
    void getEmployeeByUuidReturnsBadRequestWhenTheUuidIsMalformed() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void createEmployeeReturnsCreatedAndTheNewEmployee() throws Exception {
        Employee created = employee();
        when(employeeService.createEmployee(any())).thenReturn(created);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uuid").value(created.getUuid().toString()))
                .andExpect(jsonPath("$.jobTitle").value("Research Scientist"));
    }

    @Test
    void createEmployeeReturnsBadRequestWhenTheServiceRejectsTheRequest() throws Exception {
        when(employeeService.createEmployee(any()))
                .thenThrow(new IllegalArgumentException("Missing required attributes: lastName"));

        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("{\"firstName\":\"Rosalind\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Missing required attributes: lastName"));
    }

    @Test
    void createEmployeeReturnsBadRequestWhenTheBodyIsNotValidJson() throws Exception {
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("{oops}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEmployeeByUuidReturnsTheDeletedEmployee() throws Exception {
        Employee employee = employee();
        when(employeeService.deleteEmployeeByUuid(employee.getUuid())).thenReturn(Optional.of(employee));

        mockMvc.perform(delete(BASE_PATH + "/" + employee.getUuid()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uuid").value(employee.getUuid().toString()))
                .andExpect(jsonPath("$.fullName").value("Rosalind Franklin"));
    }

    @Test
    void deleteEmployeeByUuidReturnsNotFoundWhenNoEmployeeHasThatUuid() throws Exception {
        when(employeeService.deleteEmployeeByUuid(any())).thenReturn(Optional.empty());

        mockMvc.perform(delete(BASE_PATH + "/" + UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void deleteEmployeeByUuidReturnsBadRequestWhenTheUuidIsMalformed() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void terminateEmployeeReturnsTheTerminatedEmployee() throws Exception {
        Employee employee = employee();
        Instant effective = Instant.parse("2025-01-31T00:00:00Z");
        employee.setContractTerminationDate(effective);
        when(employeeService.terminateEmployee(eq(employee.getUuid()), eq(effective)))
                .thenReturn(Optional.of(employee));

        mockMvc.perform(post(BASE_PATH + "/" + employee.getUuid() + "/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TerminateEmployeeRequest(effective))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractTerminationDate").value("2025-01-31T00:00:00Z"));
    }

    @Test
    void terminateEmployeeDefaultsToNowWhenTheBodyIsAbsent() throws Exception {
        Employee employee = employee();
        when(employeeService.terminateEmployee(eq(employee.getUuid()), any())).thenReturn(Optional.of(employee));

        mockMvc.perform(post(BASE_PATH + "/" + employee.getUuid() + "/termination"))
                .andExpect(status().isOk());

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(employeeService).terminateEmployee(eq(employee.getUuid()), captor.capture());
        assertThat(captor.getValue()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void terminateEmployeeReturnsNotFoundWhenNoEmployeeHasThatUuid() throws Exception {
        when(employeeService.terminateEmployee(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post(BASE_PATH + "/" + UUID.randomUUID() + "/termination"))
                .andExpect(status().isNotFound());
    }

    @Test
    void terminateEmployeeReturnsConflictWhenAlreadyTerminated() throws Exception {
        when(employeeService.terminateEmployee(any(), any()))
                .thenThrow(new IllegalStateException("Employee was already terminated"));

        mockMvc.perform(post(BASE_PATH + "/" + UUID.randomUUID() + "/termination"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("Employee was already terminated"));
    }

    @Test
    void terminateEmployeeReturnsBadRequestWhenTheDatePrecedesTheHireDate() throws Exception {
        when(employeeService.terminateEmployee(any(), any()))
                .thenThrow(new IllegalArgumentException("precedes contractHireDate"));

        mockMvc.perform(post(BASE_PATH + "/" + UUID.randomUUID() + "/termination"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteIsNotMappedOnTheCollectionPath() throws Exception {
        mockMvc.perform(delete(BASE_PATH)).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(employeeService);
    }

    @Test
    void deleteEmployeeByUuidPassesThePathUuidToTheService() throws Exception {
        Employee employee = employee();
        when(employeeService.deleteEmployeeByUuid(employee.getUuid())).thenReturn(Optional.of(employee));

        mockMvc.perform(delete(BASE_PATH + "/" + employee.getUuid())).andExpect(status().isOk());

        verify(employeeService).deleteEmployeeByUuid(employee.getUuid());
    }

    @Test
    void terminateEmployeeReturnsBadRequestWhenTheUuidIsMalformed() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/not-a-uuid/termination")).andExpect(status().isBadRequest());
        verifyNoInteractions(employeeService);
    }

    @Test
    void terminateEmployeeReturnsBadRequestWhenTheBodyIsNotValidJson() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/" + UUID.randomUUID() + "/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{oops}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(employeeService);
    }

    @Test
    void terminateEmployeeTreatsAnExplicitNullDateAsAbsent() throws Exception {
        Employee employee = employee();
        when(employeeService.terminateEmployee(eq(employee.getUuid()), any())).thenReturn(Optional.of(employee));

        mockMvc.perform(post(BASE_PATH + "/" + employee.getUuid() + "/termination")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contractTerminationDate\":null}"))
                .andExpect(status().isOk());

        ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
        verify(employeeService).terminateEmployee(eq(employee.getUuid()), captor.capture());
        assertThat(captor.getValue()).isCloseTo(Instant.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void terminationIsNotReachableWithGet() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/" + UUID.randomUUID() + "/termination"))
                .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(employeeService);
    }

    @Test
    void anUnexpectedFailureBecomesA500ThatDoesNotLeakInternals() throws Exception {
        when(employeeService.getAllEmployees()).thenThrow(new RuntimeException("jdbc://secret-host password=hunter2"));

        mockMvc.perform(get(BASE_PATH))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("The request could not be completed."))
                .andExpect(
                        content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2"))));
    }

    @Test
    void errorResponsesUseTheProblemDetailShape() throws Exception {
        mockMvc.perform(delete(BASE_PATH))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.title").exists())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value(BASE_PATH));
    }

    private static Employee employee() {
        EmployeeImpl employee = EmployeeImpl.builder()
                .firstName("Rosalind")
                .lastName("Franklin")
                .salary(175000)
                .age(37)
                .jobTitle("Research Scientist")
                .email("rosalind.franklin@example.com")
                .contractHireDate(Instant.parse("2020-05-04T00:00:00Z"))
                .build();
        employee.setUuid(UUID.randomUUID());
        return employee;
    }

    private static CreateEmployeeRequest validRequest() {
        return new CreateEmployeeRequest(
                "Rosalind",
                "Franklin",
                175000,
                37,
                "Research Scientist",
                "rosalind.franklin@example.com",
                Instant.parse("2020-05-04T00:00:00Z"),
                null);
    }
}
