# ReliaQuest's Entry-Level Java Challenge

Please keep the following in mind while working on this challenge:
* Your implementation is graded for **correctness**. The required endpoints must work.
* You must add at least one endpoint of your own — see "Endpoints of your own".
* Tests are required. Your submission must build with `./gradlew build`, tests passing.
* `DECISIONS.md` is required. It is where you explain your reasoning, and we read it closely.
* We also look at how you would operate this: error handling, and what you log.
* Use clean coding etiquette
  * E.g. avoid liberal use of new-lines, odd variable and method names, random indentation, etc...

## Problem Statement

Your employer has recently purchased a license to top-tier SaaS platform, Employees-R-US, to off-load all employee management responsibilities.
Unfortunately, your company's product has an existing employee management solution that is tightly coupled to other services and therefore
cannot be replaced whole-cloth. Product and Development leads in your department have decided it would be best to interface
the existing employee management solution with the commercial offering from Employees-R-US for the time being until all employees can be
migrated to the new SaaS platform.

Your ask is to expose employee information as a protected, secure REST API for consumption by Employees-R-US web hooks.
The initial REST API will consist of the 3 endpoints listed in the following section.

You are then asked to **extend it** — see "Endpoints of your own" below.

Good luck!

## Required endpoints (API module)

_See `com.challenge.api.controller.EmployeeController` for details._

These three are required, at exactly these paths and verbs. Your API is exercised
against them automatically, so the paths are not yours to choose.

| Verb | Path | Description |
|---|---|---|
| `GET` | `/api/v1/employee` | All employees, unfiltered |
| `GET` | `/api/v1/employee/{uuid}` | A single employee by UUID |
| `POST` | `/api/v1/employee` | Create an employee, returning the created employee |

`com.challenge.api.model.Employee` is a binding contract — the JSON your API
returns for an employee is expected to carry the attribute names on that
interface.

The create request body accepts these attributes:

    firstName, lastName, salary, age, jobTitle, email, contractHireDate

`contractTerminationDate` is optional. Requests that are missing required
attributes must be rejected — deciding what "required" means here is part of
the exercise.

A request for an employee that does not exist must not report success, and
neither must one where the identifier is not a valid UUID. Which status code
each of those deserves is your call.

You need not be concerned with an actual persistence layer. Generate mock
Employee models as necessary; in-memory storage is expected.

## Endpoints of your own

**Required.** Beyond the three above, add at least one endpoint that you judge
this system would benefit from, and explain in `DECISIONS.md` why it needs it.

We are looking for your read on what this API is missing, so we are not going to
tell you what to add. Two constraints:

* **Endpoints only.** No database, container, message broker, or authentication
  provider. Adding infrastructure is out of scope and will not earn credit.
* Anything you add should fit the patterns already in the project.

## `DECISIONS.md`

A `DECISIONS.md` template is at the root of this project. Fill it in. It is a
required part of your submission and a substantial part of how we evaluate it,
so treat it as more than a formality — brief and specific beats long and vague.

## Tests

Tests are required, and they must pass as part of `./gradlew build`.

Write tests that cover the cases your API can actually encounter. We are
interested in which cases you thought to cover, so the list is yours to work
out. See `api/src/test/java/com/challenge/api/README.md` for framework
conventions.

## Code Formatting

This project utilizes Gradle plugin [Diffplug Spotless](https://github.com/diffplug/spotless/tree/main/plugin-gradle) to enforce format
and style guidelines with every build.

To format code according to style guidelines, you can run **spotlessApply** task.
`./gradlew spotlessApply`

The spotless plugin will also execute check-and-validation tasks as part of the gradle **build** task.
`./gradlew build`

## Submitting

Commit your work as you go. Your submission must build with `./gradlew build`
from a clean clone.
