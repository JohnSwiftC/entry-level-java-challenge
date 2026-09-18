package com.challenge.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.challenge.api.model.CreateEmployeeRequest;
import com.challenge.api.model.Employee;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EmployeeServiceTest {

    private EmployeeService service;

    @BeforeEach
    void setUp() {
        service = new EmployeeService();
    }

    @Test
    void storeIsSeededOnConstruction() {
        assertThat(service.getAllEmployees()).isNotEmpty();
    }

    @Test
    void getAllEmployeesReturnsAnImmutableSnapshot() {
        List<Employee> employees = service.getAllEmployees();

        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> employees.add(null));
    }

    @Test
    void getEmployeeByUuidReturnsTheMatchingEmployee() {
        Employee seeded = service.getAllEmployees().get(0);

        Optional<Employee> found = service.getEmployeeByUuid(seeded.getUuid());

        assertThat(found).containsSame(seeded);
    }

    @Test
    void getEmployeeByUuidReturnsEmptyWhenNoEmployeeHasThatUuid() {
        assertThat(service.getEmployeeByUuid(UUID.randomUUID())).isEmpty();
    }

    @Test
    void createEmployeeAssignsAUuidAndStoresTheEmployee() {
        Employee created = service.createEmployee(validRequest());

        assertThat(created.getUuid()).isNotNull();
        assertThat(created.getFullName()).isEqualTo("Rosalind Franklin");
        assertThat(service.getEmployeeByUuid(created.getUuid())).containsSame(created);
    }

    @Test
    void createEmployeeAcceptsAnAbsentContractTerminationDate() {
        Employee created = service.createEmployee(validRequest());

        assertThat(created.getContractTerminationDate()).isNull();
    }

    @Test
    void createEmployeeRejectsARequestMissingRequiredAttributes() {
        CreateEmployeeRequest request =
                new CreateEmployeeRequest("Rosalind", null, null, 37, "Research Scientist", null, null, null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.createEmployee(request))
                .withMessageContaining("lastName")
                .withMessageContaining("salary")
                .withMessageContaining("email")
                .withMessageContaining("contractHireDate");
    }

    @Test
    void createEmployeeTreatsBlankTextAsMissing() {
        CreateEmployeeRequest request = new CreateEmployeeRequest(
                "   ", "Franklin", 175000, 37, "Research Scientist", "r.franklin@example.com", Instant.now(), null);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.createEmployee(request))
                .withMessageContaining("firstName");
    }

    @Test
    void createEmployeeLeavesExistingEmployeesAlone() {
        int before = service.getAllEmployees().size();

        service.createEmployee(validRequest());

        assertThat(service.getAllEmployees()).hasSize(before + 1);
    }

    @Test
    void deleteEmployeeByUuidReturnsAndRemovesTheEmployee() {
        Employee seeded = service.getAllEmployees().get(0);
        int before = service.getAllEmployees().size();

        Optional<Employee> deleted = service.deleteEmployeeByUuid(seeded.getUuid());

        assertThat(deleted).containsSame(seeded);
        assertThat(service.getAllEmployees()).hasSize(before - 1);
        assertThat(service.getEmployeeByUuid(seeded.getUuid())).isEmpty();
    }

    @Test
    void deleteEmployeeByUuidReturnsEmptyWhenNoEmployeeHasThatUuid() {
        int before = service.getAllEmployees().size();

        assertThat(service.deleteEmployeeByUuid(UUID.randomUUID())).isEmpty();
        assertThat(service.getAllEmployees()).hasSize(before);
    }

    @Test
    void deletingTheSameEmployeeTwiceRemovesItOnlyOnce() {
        Employee seeded = service.getAllEmployees().get(0);

        assertThat(service.deleteEmployeeByUuid(seeded.getUuid())).isPresent();
        assertThat(service.deleteEmployeeByUuid(seeded.getUuid())).isEmpty();
    }

    @Test
    void terminateEmployeeStampsTheTerminationDate() {
        Employee seeded = activeEmployee();
        Instant effective = Instant.parse("2025-01-31T00:00:00Z");

        Optional<Employee> terminated = service.terminateEmployee(seeded.getUuid(), effective);

        assertThat(terminated).isPresent();
        assertThat(terminated.get().getContractTerminationDate()).isEqualTo(effective);
        assertThat(service.getEmployeeByUuid(seeded.getUuid()))
                .get()
                .extracting(Employee::getContractTerminationDate)
                .isEqualTo(effective);
    }

    @Test
    void terminateEmployeeReturnsEmptyWhenNoEmployeeHasThatUuid() {
        assertThat(service.terminateEmployee(UUID.randomUUID(), Instant.now())).isEmpty();
    }

    @Test
    void terminateEmployeeRejectsAnEmployeeThatIsAlreadyTerminated() {
        Employee seeded = activeEmployee();
        service.terminateEmployee(seeded.getUuid(), Instant.parse("2025-01-31T00:00:00Z"));

        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> service.terminateEmployee(seeded.getUuid(), Instant.now()))
                .withMessageContaining("already terminated");
    }

    @Test
    void terminateEmployeeRejectsATerminationDateBeforeTheHireDate() {
        Employee seeded = activeEmployee();

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.terminateEmployee(
                        seeded.getUuid(), seeded.getContractHireDate().minusSeconds(1)))
                .withMessageContaining("precedes contractHireDate");
    }

    @Test
    void terminateEmployeeLeavesTheEmployeeInTheStore() {
        Employee seeded = activeEmployee();
        int before = service.getAllEmployees().size();

        service.terminateEmployee(seeded.getUuid(), Instant.parse("2025-01-31T00:00:00Z"));

        assertThat(service.getAllEmployees()).hasSize(before);
    }

    @Test
    void deleteEmployeeByUuidLeavesEveryOtherEmployeeReachable() {
        List<Employee> before = service.getAllEmployees();
        Employee doomed = before.get(0);

        service.deleteEmployeeByUuid(doomed.getUuid());

        for (Employee survivor : before) {
            if (!survivor.getUuid().equals(doomed.getUuid())) {
                assertThat(service.getEmployeeByUuid(survivor.getUuid())).containsSame(survivor);
            }
        }
    }

    @Test
    void deleteEmployeeByUuidRemovesAnEmployeeCreatedAtRuntime() {
        Employee created = service.createEmployee(validRequest());

        assertThat(service.deleteEmployeeByUuid(created.getUuid())).containsSame(created);
        assertThat(service.getEmployeeByUuid(created.getUuid())).isEmpty();
    }

    @Test
    void deleteEmployeeByUuidRemovesAnAlreadyTerminatedEmployee() {
        Employee seeded = activeEmployee();
        service.terminateEmployee(seeded.getUuid(), Instant.parse("2025-01-31T00:00:00Z"));

        assertThat(service.deleteEmployeeByUuid(seeded.getUuid())).isPresent();
        assertThat(service.getEmployeeByUuid(seeded.getUuid())).isEmpty();
    }

    @Test
    void terminateEmployeeAllowsATerminationDateEqualToTheHireDate() {
        Employee seeded = activeEmployee();

        Optional<Employee> terminated = service.terminateEmployee(seeded.getUuid(), seeded.getContractHireDate());

        assertThat(terminated).isPresent();
        assertThat(terminated.get().getContractTerminationDate()).isEqualTo(seeded.getContractHireDate());
    }

    @Test
    void terminateEmployeeAllowsAFutureTerminationDate() {
        Employee seeded = activeEmployee();
        Instant lastDay = Instant.now().plus(30, ChronoUnit.DAYS);

        assertThat(service.terminateEmployee(seeded.getUuid(), lastDay))
                .get()
                .extracting(Employee::getContractTerminationDate)
                .isEqualTo(lastDay);
    }

    @Test
    void terminateEmployeeChangesNothingButTheTerminationDate() {
        Employee seeded = activeEmployee();
        String jobTitle = seeded.getJobTitle();
        Integer salary = seeded.getSalary();
        String email = seeded.getEmail();
        Instant hireDate = seeded.getContractHireDate();

        service.terminateEmployee(seeded.getUuid(), Instant.parse("2025-01-31T00:00:00Z"));

        Employee after = service.getEmployeeByUuid(seeded.getUuid()).orElseThrow();
        assertThat(after.getJobTitle()).isEqualTo(jobTitle);
        assertThat(after.getSalary()).isEqualTo(salary);
        assertThat(after.getEmail()).isEqualTo(email);
        assertThat(after.getContractHireDate()).isEqualTo(hireDate);
    }

    @Test
    void terminateEmployeeReturnsEmptyOnceTheEmployeeHasBeenDeleted() {
        Employee seeded = activeEmployee();
        service.deleteEmployeeByUuid(seeded.getUuid());

        assertThat(service.terminateEmployee(seeded.getUuid(), Instant.now())).isEmpty();
    }

    /**
     * The whole reason terminateEmployee does its check and its write inside
     * computeIfPresent. With a plain
     * get-check-put, several threads could each read a null termination date and
     * each believe they terminated the
     * Employee, and the 409 the API promises would be decorative.
     */
    @Test
    void concurrentTerminationsOfTheSameEmployeeSucceedExactlyOnce() throws Exception {
        Employee seeded = activeEmployee();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startLine = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();

        List<Future<?>> attempts = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            attempts.add(pool.submit(() -> {
                startLine.await();
                try {
                    service.terminateEmployee(seeded.getUuid(), Instant.parse("2025-01-31T00:00:00Z"));
                    succeeded.incrementAndGet();
                } catch (IllegalStateException alreadyTerminated) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }
        startLine.countDown();
        for (Future<?> attempt : attempts) {
            attempt.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(succeeded.get()).isEqualTo(1);
        assertThat(rejected.get()).isEqualTo(threads - 1);
    }

    /** A seeded Employee that has not been terminated yet. */
    private Employee activeEmployee() {
        return service.getAllEmployees().stream()
                .filter(employee -> employee.getContractTerminationDate() == null)
                .findFirst()
                .orElseThrow();
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
