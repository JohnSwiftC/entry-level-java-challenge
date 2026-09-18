# Decisions

Fill this in as part of your submission. Brief and specific beats long and
vague — a few sentences per question is plenty. Replace the prompts with your
answers.

## 1. Which endpoints did you add, and why does this system need them?

I added an endpoint to terminate an employee by UUID with an optional date (defaults to the time of request).
This action was not already available with the base 3 endpoints, but appears to be a crucial component of
the Employee model. Without it, employees could not be terminated.

I also added a DELETE route for removing employee records by UUID. This allows for accidental, broken, etc Employee
records to be removed completely via UUID.

## 2. Did you add any dependency? What does it buy, and what does it cost?

I did not add any new dependencies.

## 3. How did you handle errors and logging, and why those mechanisms?

Errors: One @RestControllerAdvice maps exceptions to statuses, so the
controller has no try/catch and the service can throw plain
IllegalArgumentException (400) and IllegalStateException (409) instead of
web exceptions.

Logging: SLF4J with Lombok's @Slf4j. State changes are info, because
with no persistence layer the log is the only audit trail. A 404 or a
validation failure is debug since the system is working correctly, a 409 is
warn because a caller has diverged from our data, and unhandled exceptions
are error with the stack trace. No names, emails or salaries are logged.

## 4. Where the brief left a decision to you, what did you decide?

For the endpoint to create and Employee, I chose to exclude UUID from the request model.
It makes the most sense for the UUID to be generated server-side by the EmployeeService.

I also decided to make the fullName field in Employee be derived from firstName and lastName.
With a default implementation, its possible to have a different firstName + lastName than a fullName,
just due to there being several sources of truth. I removed the fullName field, and made the related functions
compose firstName and lastName.

For logging, I followed some standard patterns I've picked up from working on previous backends. RestControllerAdvice
is especially useful. I also deliberatly chose to make my EmployeeService return Optional when looking for an employee
by UUID, so the RestController could plainly return a status code when no employee was located.

## 5. What did you deliberately choose _not_ to do?

I chose to not use any new external dependencies. New dependencies add complexity,
and I heavily dislike unneccesary complexity. I ensured then to rely on what was already included
in the project to accomplish my goals.

As for my endpoints, I decided to only write what would realisty be the common cases besides the provided
endpoints: terminating, and deleting. While there are a huge amount of features I might want available
in the system, such as updating, sorting employees by X, etc, they are not the common case for this system,
so I did not include them.
