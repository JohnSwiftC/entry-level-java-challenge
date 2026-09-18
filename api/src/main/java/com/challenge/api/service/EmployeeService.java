package com.challenge.api.service;

import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import com.challenge.api.model.EmployeeImpl;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Business logic and in-memory storage for Employees.
 * Contains my sample data.
 */
@Service
@Slf4j
public class EmployeeService {

    /**
     * Stands in for the persistence layer. ConcurrentHashMap because Spring serves
     * requests on multiple threads and
     * this single instance is shared across all of them. Iteration order is not
     * guaranteed, so nothing should depend
     * on the order {@link #getAllEmployees()} returns.
     */
    private final Map<UUID, Employee> employees = new ConcurrentHashMap<>();

    public EmployeeService() {
        seedMockEmployees();
        log.info("Initialized in-memory employee store with {} mock employees", employees.size());
    }

    /**
     * @return every known Employee
     */
    public List<Employee> getAllEmployees() {
        // Copy so a caller cannot mutate the backing store by mutating the list it was
        // handed.
        return List.copyOf(employees.values());
    }

    /**
     * @param uuid Employee UUID
     * @return the matching Employee, or empty if no Employee has that UUID.
     */
    public Optional<Employee> getEmployeeByUuid(UUID uuid) {
        Optional<Employee> found = Optional.ofNullable(employees.get(uuid));
        if (found.isEmpty()) {
            log.debug("No employee found for uuid={}", uuid);
        }
        return found;
    }

    /**
     * Records an Employee's termination by stamping their contractTerminationDate.
     *
     * The update runs inside {@code computeIfPresent} so the check and the write
     * are atomic, there is a specific test for this kind of issue.
     *
     * @param uuid            Employee UUID
     * @param terminationDate when the contract ends
     * @return the terminated Employee, or empty if no Employee had that UUID
     * @throws IllegalStateException    if the Employee is already terminated
     * @throws IllegalArgumentException if the termination date precedes the hire
     *                                  date
     */
    public Optional<Employee> terminateEmployee(UUID uuid, Instant terminationDate) {
        Employee terminated = employees.computeIfPresent(uuid, (key, employee) -> {
            if (employee.getContractTerminationDate() != null) {
                throw new IllegalStateException(
                        "Employee " + uuid + " was already terminated on " + employee.getContractTerminationDate());
            }
            if (employee.getContractHireDate() != null && terminationDate.isBefore(employee.getContractHireDate())) {
                throw new IllegalArgumentException("contractTerminationDate " + terminationDate
                        + " precedes contractHireDate " + employee.getContractHireDate());
            }
            employee.setContractTerminationDate(terminationDate);
            return employee;
        });
        if (terminated == null) {
            log.debug("No employee to terminate for uuid={}", uuid);
            return Optional.empty();
        }
        log.info("Terminated employee uuid={} effective={}", uuid, terminationDate);
        return Optional.of(terminated);
    }

    /**
     * Removes an Employee from the store.
     *
     * @param uuid Employee UUID
     * @return the Employee that was removed, or empty if no Employee had that UUID.
     */
    public Optional<Employee> deleteEmployeeByUuid(UUID uuid) {
        Optional<Employee> removed = Optional.ofNullable(employees.remove(uuid));
        removed.ifPresentOrElse(
                employee -> log.info("Deleted employee uuid={} jobTitle={}", uuid, employee.getJobTitle()),
                () -> log.debug("No employee to delete for uuid={}", uuid));
        return removed;
    }

    /**
     * Validates, creates, and stores a new Employee.
     *
     * @param request the attributes supplied by the caller
     * @return the newly created Employee
     * @throws IllegalArgumentException if any required attribute is missing
     */
    public Employee createEmployee(CreateEmployeeRequest request) {
        validate(request);
        Employee employee = EmployeeImpl.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .salary(request.salary())
                .age(request.age())
                .jobTitle(request.jobTitle())
                .email(request.email())
                .contractHireDate(request.contractHireDate())
                .contractTerminationDate(request.contractTerminationDate())
                .build();
        store(employee);
        log.info("Created employee uuid={} jobTitle={}", employee.getUuid(), employee.getJobTitle());
        return employee;
    }

    /**
     * Rejects a create request that is missing any attribute the contract calls
     * required. Every missing attribute is
     * reported at once, so a caller fixing a bad request does not have to discover
     * the problems one round trip at a
     * time. contractTerminationDate is the only optional attribute.
     */
    private void validate(CreateEmployeeRequest request) {
        List<String> missing = new ArrayList<>();
        if (isBlank(request.firstName())) {
            missing.add("firstName");
        }
        if (isBlank(request.lastName())) {
            missing.add("lastName");
        }
        if (request.salary() == null) {
            missing.add("salary");
        }
        if (request.age() == null) {
            missing.add("age");
        }
        if (isBlank(request.jobTitle())) {
            missing.add("jobTitle");
        }
        if (isBlank(request.email())) {
            missing.add("email");
        }
        if (request.contractHireDate() == null) {
            missing.add("contractHireDate");
        }
        if (!missing.isEmpty()) {
            log.debug("Rejected create request missing {}", missing);
            throw new IllegalArgumentException("Missing required attributes: " + String.join(", ", missing));
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void seedMockEmployees() {
        store(EmployeeImpl.builder()
                .firstName("Ada")
                .lastName("Lovelace")
                .salary(185000)
                .age(36)
                .jobTitle("Principal Engineer")
                .email("ada.lovelace@example.com")
                .contractHireDate(Instant.parse("2015-03-02T00:00:00Z"))
                .build());
        store(EmployeeImpl.builder()
                .firstName("Grace")
                .lastName("Hopper")
                .salary(210000)
                .age(45)
                .jobTitle("Director of Engineering")
                .email("grace.hopper@example.com")
                .contractHireDate(Instant.parse("2012-11-19T00:00:00Z"))
                .build());
        store(EmployeeImpl.builder()
                .firstName("Alan")
                .lastName("Turing")
                .salary(165000)
                .age(41)
                .jobTitle("Staff Engineer")
                .email("alan.turing@example.com")
                .contractHireDate(Instant.parse("2018-06-23T00:00:00Z"))
                .build());
        store(EmployeeImpl.builder()
                .firstName("Katherine")
                .lastName("Johnson")
                .salary(142000)
                .age(33)
                .jobTitle("Senior Data Analyst")
                .email("katherine.johnson@example.com")
                .contractHireDate(Instant.parse("2019-08-26T00:00:00Z"))
                .build());
        store(EmployeeImpl.builder()
                .firstName("Ana")
                .lastName("de Armas")
                .salary(98000)
                .age(29)
                .jobTitle("Software Engineer")
                .email("ana.dearmas@example.com")
                .contractHireDate(Instant.parse("2021-01-11T00:00:00Z"))
                .contractTerminationDate(Instant.parse("2024-04-30T00:00:00Z"))
                .build());
    }

    private void store(Employee employee) {
        employee.setUuid(UUID.randomUUID());
        employees.put(employee.getUuid(), employee);
    }
}
