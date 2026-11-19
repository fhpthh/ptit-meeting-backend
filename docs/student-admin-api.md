# Student administration API

All routes under `/api/v1/admin/students` require `ROLE_ADMIN`. Anonymous requests receive
401 and authenticated callers without that role receive 403. JSON responses use
`BaseResponse`. List and enrollment responses wrap `PageResponseDTO` with `content`, `page`,
`size`, `totalElements`, `totalPages`, and `last`. Exports return attachments.

| Method and path relative to the base | Request | Success content |
| --- | --- | --- |
| GET | List query below | Page of student accounts |
| GET `/{id}` | Numeric user ID | Student account |
| POST | Profile fields and `temporaryPassword` | Created account, HTTP 201 |
| PUT `/{id}` | Profile fields | Updated account |
| PUT `/{id}/academic-profile` | `administrativeClassId` and `studyStatus` | Assigned academic profile |
| PATCH `/{id}/status` | `{"status":"LOCKED"}` | Updated account |
| PATCH `/bulk-status` | `{"ids":[1,2],"status":"LOCKED"}` | `matched` and `changed` counts |
| POST `/{id}/reset-password` | `{"temporaryPassword":"…"}` | Success wrapper |
| DELETE `/{id}` | Numeric user ID | Success wrapper after archive |
| GET `/summary` | — | Global `total`, `active`, `locked`, `inactive` counts |
| GET `/filters` | — | Account statuses, departments, and academic structure options |
| GET `/export` | List filters and sort, `format=XLSX` (default) or `CSV` | `students.xlsx` or `students.csv` |
| GET `/{id}/enrollments` | Optional `semesterId`, `page`, `size` | Page of class enrollments |

Account responses contain `id`, `code`, `fullName`, `email`, `department`, `status`,
`mustChangePassword`, timestamps, and an optional `academicProfile`. The academic profile resolves
the study status, campus, admission cohort, major, and administrative class. Legacy accounts can
have a null academic profile until an administrator assigns one.
The profile body requires `email`, `code`, and `fullName`. Omitting `department` or sending null or
blank clears it on PUT. Emails are saved lowercase and codes use existing auth normalization.
Creation assigns `ROLE_STUDENT`, sets account status to `ACTIVE`, and stores only the BCrypt hash of
the supplied temporary password. Password reset replaces the local password hash and sets
`mustChangePassword`. The existing local login flow signals the password change to the client.
The temporary password must have at least eight characters and fit in 72 UTF-8 bytes. Reset leaves
Outlook credentials and provider links intact. Auth and timetable imports may still sync profile fields
through their existing flows. Admin writes check email and code against all account types. The
database enforces email uniqueness. Code uniqueness checks serialize admin identity writes, while
legacy provisioning and import are outside that lock and the code column has no unique constraint.

List query parameters are `keyword` (case-insensitive literal substring of code, email, or name),
`status` (`ACTIVE`, `LOCKED`, `INACTIVE`), `department`, `campusId`, `cohortId`, `majorId`,
`administrativeClassId`, and `studyStatus` (`ENROLLED`, `SUSPENDED`, `GRADUATED`, `WITHDRAWN`).
Academic filters only match students with an assigned profile. Pages are zero-based with default
size 20 and a maximum size of 100. `sortBy` defaults
to `createdAt` and accepts `id`, `code`, `fullName`, `email`, `department`, `status`, `createdAt`
or `updatedAt`. `direction` is `ASC` or `DESC` and defaults to `DESC`. Account ID breaks ties.
Export uses those filters and sort for the entire matching set with a limit of 10,000 rows. It omits
pagination and credential or password-change data. CSV includes a UTF-8 BOM and prefixes cells
that could be interpreted as formulas.

Missing IDs and IDs belonging to other account types return 404. Bulk status updates accept
1–100 IDs, deduplicate them, and reject the entire request if any ID is missing or belongs to
another account type. DELETE is a reversible soft archive implemented by setting `INACTIVE`.
It retains the user row, identity, roles, classes, and all other references. Archived accounts
remain visible in admin queries and can be reactivated using the status endpoint.
Label this action **Archive** in the UI. Its effect is the same as setting the account to `INACTIVE`.

`LOCKED` and `INACTIVE` are checked by existing password login, Outlook login, and refresh flows.
The stateless JWT filter evaluates token claims and the blacklist without reading account status.
Already-issued access tokens can remain valid until expiry or separate blacklisting. Admin status
changes, archive, and password reset do not revoke existing tokens. The unchanged first-time
password-change flow does not check account status before issuing a session.

Enrollment responses include the enrollment ID, section ID, class code, semester ID/code/name,
subject code/name, group number, practice group, enrollment status, and enrollment timestamp.
Both `ENROLLED` and `DROPPED` records are included, latest enrollment first. Standalone meeting
memberships are excluded. Timetable owns this data and exposes it through a public read service.

`department` remains the legacy auth-user field and is not used as an academic catalog. Academic
structure is stored independently in `academic_campuses`, `admission_cohorts`, `academic_majors`,
`administrative_classes`, and `student_academic_profiles`. Nothing is inferred from timetable class
enrollments.

## Academic structure

All routes below require `ROLE_ADMIN` and use `/api/v1/admin/student-structure`:

| Method and path | Purpose |
| --- | --- |
| GET `/options` | Cascading options; optional `campusId`, `cohortId`, and `majorId` |
| GET `/tree` | Campus → cohort → major → administrative-class tree with actual student counts; optional `keyword` |
| POST `/campuses` | Create a campus from `code` and `name` |
| POST `/cohorts` | Create an admission cohort from `code` and `name` |
| POST `/majors` | Create a major from `code` and `name` |
| POST `/administrative-classes` | Create a class from `code`, `name`, `campusId`, `cohortId`, and `majorId` |

The options endpoint narrows cohorts by campus, majors by campus and cohort, and administrative
classes by all supplied parent IDs. Tree counts come from `student_academic_profiles`; the UI values
are calculated data rather than fixed display numbers.
