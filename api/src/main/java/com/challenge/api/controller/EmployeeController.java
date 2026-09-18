package com.challenge.api.controller;

import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.TerminateEmployeeRequest;
import com.challenge.api.service.EmployeeService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/employee")
public class EmployeeController {

    private final EmployeeService employeeService;

    EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /**
     * @implNote Need not be concerned with an actual persistence layer. Generate mock Employee models as necessary.
     * @return One or more Employees.
     */
    @GetMapping
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    /**
     * @implNote Need not be concerned with an actual persistence layer. Generate mock Employee model as necessary.
     * @param uuid Employee UUID
     * @return Requested Employee if exists
     */
    @GetMapping("{uuid}")
    public Employee getEmployeeByUuid(@PathVariable UUID uuid) {
        Optional<Employee> employee = employeeService.getEmployeeByUuid(uuid);

        if (employee.isEmpty()) {
            throw notFound(uuid);
        }

        return employee.get();
    }

    /**
     * @implNote Need not be concerned with an actual persistence layer.
     * @param requestBody hint!
     * @return Newly created Employee
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee createEmployee(@RequestBody CreateEmployeeRequest requestBody) {
        return employeeService.createEmployee(requestBody);
    }

    /**
     * Terminates an employee
     *
     * @param uuid Employee UUID
     * @param requestBody optional, an absent body or date terminates the Employee as of now
     * @return the terminated Employee
     */
    @PostMapping("{uuid}/termination")
    public Employee terminateEmployee(
            @PathVariable UUID uuid, @RequestBody(required = false) TerminateEmployeeRequest requestBody) {
        Instant terminationDate = requestBody == null || requestBody.contractTerminationDate() == null
                ? Instant.now()
                : requestBody.contractTerminationDate();
        return employeeService.terminateEmployee(uuid, terminationDate).orElseThrow(() -> notFound(uuid));
    }

    /**
     * Deletes an employee by UUID, returns the deleted Employee to the caller.
     * This endpoint it used to correct any errors or bad records, not for terminating
     * an Employee.
     *
     * @param uuid Employee UUID
     * @return the Employee that was deleted
     */
    @DeleteMapping("{uuid}")
    public Employee deleteEmployeeByUuid(@PathVariable UUID uuid) {
        return employeeService.deleteEmployeeByUuid(uuid).orElseThrow(() -> notFound(uuid));
    }

    private static ResponseStatusException notFound(UUID uuid) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No employee with uuid " + uuid);
    }
}
