
# Java-Based Online Quiz Platform

An academic **Java** project that implements a complete online quiz platform for Java
programming: participants take **timed quizzes**, receive **scores and feedback** and
review their **performance reports**, quiz creators build and submit quizzes, and
administrators manage users, quiz approvals and platform settings.

> **Java is the primary technology throughout** - Core Java, OOP, Swing GUI, Java
> Servlets, JDBC and MySQL. There is **no React, Next.js, Node.js, Express, Python or
> PHP** anywhere in this project.

---

## 🚀 Quick start — run on MySQL + Tomcat (production)

> **Automated scripts provided. Full step-by-step guide: [`docs/PRODUCTION_DEPLOYMENT.md`](docs/PRODUCTION_DEPLOYMENT.md)**

1. Install **MySQL** + **Tomcat 9** (one time — see the doc for links).
2. Load the DB:  `powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 -MySqlBin "C:\Program Files\MySQL\MySQL Server 8.0\bin" -User root`
3. Set your password in `src/main/resources/config.properties`.
4. Build + deploy:  `powershell -ExecutionPolicy Bypass -File scripts\deploy-tomcat.ps1 -TomcatHome "C:\Program Files\Apache Software Foundation\Tomcat 9.0"`
5. Open `http://localhost:8080/java-quiz-platform` and log in.

---

## 1. Project Objective
## 1. Project Objective

Create an online quiz platform where:

| Role | What they can do |
|------|------------------|
| **ADMIN** | Login, create/update/delete users, assign roles, view all users, approve/reject submitted quizzes, view quiz details, manage system settings, view platform statistics |
| **QUIZ CREATOR** | Login, create quizzes, add/edit/delete questions, set duration/title/options/correct answers/marks, submit quizzes for admin approval, view quiz status, review participant results, give feedback |
| **PARTICIPANT** | Register/login, view approved quizzes, start a **timed** quiz, navigate between questions, submit (or auto-submit at 00:00), view score/percentage/correct-incorrect answers/feedback, review previous attempts |

---

## 2. Features

* Three roles with **role-based access control** (Swing + Servlet sessions)
* **Timed quizzes** with a dedicated Java countdown thread and auto-submission at 00:00
* Full quiz lifecycle: `DRAFT -> PENDING_APPROVAL -> APPROVED / REJECTED`
* Score calculation (marks, percentage, correct/incorrect, automatic feedback)
* Question-wise performance review + detailed performance report
* Performance history in a `JTable`
* Creator/Admin feedback for participants
* SHA-256 password hashing (never store plain text passwords)
* Platform statistics dashboards (users, quizzes, pending, attempts)
* Web module (Java Servlets): login, quiz attempt, results, admin console

---

## 3. Technologies

| Layer | Technology |
|-------|-----------|
| Language | Java 17 (works with Java 21 too) |
| Desktop GUI | Swing (`JFrame`, `JPanel`, `CardLayout`, `JTable`, `JRadioButton`, `JProgressBar`, ...) |
| Web layer | Java Servlets (`HttpServlet`, `@WebServlet`, `HttpSession`) + plain HTML/CSS |
| Server | Apache Tomcat 9.x (Servlet 4.0 / `javax.servlet`) |
| Persistence | **Plain JDBC** (`Connection`, `PreparedStatement`, `ResultSet`, transactions) |
| Database | MySQL 8.x (`java_quiz_platform`) |
| Build | Maven (`pom.xml`, WAR packaging) |
| Tests | JUnit 5 + in-memory H2 (MySQL mode) for the DAO layer |

---

## 4. Architecture

```
        +---------------------+        +---------------------+
        |  Swing GUI (frames) |        |  Java Servlets (web)|
        +----------+----------+        +----------+----------+
                   |                              |
                   +-------------+----------------+
                                 |
                       +---------v---------+
                       |   Service layer   |  AuthenticationService, UserService,
                       | (business rules,  |  QuizService, ScoreService,
                       |  validations)     |  ResultService, ResultManager(sync)
                       +---------+---------+
                                 |
                       +---------v---------+
                       |  DAO interfaces   |  UserDAO, QuizDAO, QuestionDAO,
                       +---------+---------+  OptionDAO, AttemptDAO, ResultDAO,
                                 |            FeedbackDAO, SystemSettingsDAO
                       +---------v---------+
                       |   DAO impl (JDBC) |  PreparedStatement + ResultSet
                       +---------+---------+
                                 |
                       +---------v---------+
                       |      MySQL        |  java_quiz_platform
                       +-------------------+
```

* **DAO pattern** - SQL never appears inside a GUI class.
* **Service layer** - validation + custom exceptions.
* **Repository pattern** - generic `Repository<T>` used as an in-memory cache.
* **MVC-style separation** for the Swing screens (model / service / view packages).

---

## 5. Java Concepts Demonstrated (where to find them)

| Concept | Where |
|---------|-------|
| **Inheritance** | `User` (abstract) <- `Admin`, `QuizCreator`, `Participant`; also `DashboardFrame` <- 3 dashboards |
| **Polymorphism** | `user.login(...)` and `user.displayDashboard()` called through a `User` reference; `UserDAOImpl.mapRow()` builds the concrete class from the role column |
| **Interfaces** | `Authenticatable`, `UserOperations`, `QuizOperations`, `ResultOperations` + all `*DAO` interfaces |
| **Abstract classes** | `User` (abstract `displayDashboard()` / `getDashboardName()`) |
| **Exception handling** | 6 custom checked exceptions + `try/catch/finally/throw/throws`, no empty catch blocks |
| **Collections** | `List<Question>`, `List<Quiz>`, `List<Result>`, `Map<Integer,Question>`, `Set`-style dedupe checks |
| **Generics** | `Repository<T>` (`Repository<Quiz>`, `Repository<Question>`, `Repository<Result>`), generic DAOs |
| **Multithreading** | `QuizTimer implements Runnable`, `Thread.sleep(1000)`, `interrupt()`, daemon thread |
| **Swing threading** | `SwingUtilities.invokeLater()` for every timer-driven UI update |
| **Synchronization** | `ResultManager.saveResult()` synchronized (EDT + timer thread), synchronized `Repository` methods |
| **JDBC** | `DatabaseConnection`, `PreparedStatement`, `ResultSet`, generated keys, batch insert |
| **Transactions** | `AttemptDAOImpl.submitAttempt()` - attempt + answers + result commit/rollback |
| **DAO classes** | 8 interfaces + 8 implementations |
| **Swing GUI** | 15+ frames/panels with CardLayout navigation |
| **Servlets** | `LoginServlet`, `QuizServlet`, `ResultServlet`, `AdminServlet` (`doGet`/`doPost`/`HttpSession`) |

---

## 6. Rubric Mapping

### Java GUI rubric

| Marks | Requirement | Proof in code |
|-------|-------------|---------------|
| 10 - OOP | Polymorphism | `User user = ...; user.login(...); user.displayDashboard();` (`User`, `Admin`, `QuizCreator`, `Participant`) |
| 10 - OOP | Inheritance | `Admin extends User` etc. + `DashboardFrame` <- 3 dashboards |
| 10 - OOP | Exception handling | 6 custom exceptions + `try/catch/finally`, every JDBC failure wrapped in `DatabaseException` |
| 10 - OOP | Interfaces | `Authenticatable`, `UserOperations`, `QuizOperations`, `ResultOperations`, 8 DAO interfaces |
| 6 - Collections & Generics | Lists/Maps/Sets | `List<Question>`, `Map<Integer,Question>`, in `ScoreService`, `QuizService`, `ResultManager` |
| 6 - Collections & Generics | Generic Repository | `Repository<T>` used as `Repository<Quiz>`, `Repository<Question>`, `Repository<Result>` |
| 4 - Multithreading & Sync | Timer thread | `QuizTimer implements Runnable`; `Thread.sleep`, `interrupt`, daemon thread |
| 4 - Multithreading & Sync | Synchronization | `ResultManager.saveResult()` is `synchronized` (shared statistics updated by EDT + timer thread) |
| 7 - Database classes | DAO interfaces | Clean `dao` -> `dao.impl` -> JDBC -> MySQL separation; no SQL inside GUI classes |
| 7 - Database classes | Connection + Service | `DatabaseConnection` centralizes config; `service` package holds the business logic |
| 3 - JDBC | Connectivity | `Connection`, `PreparedStatement` everywhere, `ResultSet` row mappers |
| 3 - JDBC | CRUD + Transactions | Full CRUD on all tables; `AttemptDAOImpl.submitAttempt()` commits attempt+answers+result or rolls back |

### Java Web rubric

| Marks | Requirement | Proof in code |
|-------|-------------|---------------|
| 8 - Design | Understanding | Layered design (Servlet -> Service -> DAO -> JDBC -> MySQL) with a common model |
| 10 - Core Java | Concepts | Same service/DAO code is reused by Swing and Servlets (no duplication) |
| 8 - JDBC | DB integration | Servlets read/write through the DAO layer only (`QuizServlet`, `ResultServlet`, `AdminServlet`) |
| 7 - Servlets | Web integration | 4 servlets with `@WebServlet`, `doGet`/`doPost`, `HttpSession` role checks, `/login /quiz /result /admin` |

---

## 7. Database Setup (MySQL)

```sql
-- 1) create the schema
mysql -u root -p < database/schema.sql

-- or inside the mysql client:
SOURCE database/schema.sql;
SOURCE database/sample_data.sql;
```

Tables:

| Table | Purpose |
|-------|---------|
| `roles` | ADMIN / QUIZ_CREATOR / PARTICIPANT (referenced by `users.role`) |
| `users` | Accounts incl. SHA-256 password hashes |
| `quizzes` | Title, description, duration, marks, status, creator |
| `questions` | Question text, marks, explanation, quiz FK |
| `options` | A/B/C/D choices with `is_correct` flag |
| `quiz_attempts` | One row per participant attempt (start/submit/time/status) |
| `answers` | Selected option per question (NULL = not answered) |
| `results` | Calculated score (marks, percentage, feedback) - 1:1 with attempt |
| `feedback` | Human comments from creators/admins |
| `system_settings` | Quiz defaults, timer settings, platform configuration |

Relationships (with foreign keys + `ON DELETE CASCADE`):

```
users ──< quizzes ──< questions ──< options
  │         │
  │         └─< quiz_attempts ──< answers
  └───────────< quiz_attempts ──(1:1)─ results ──< feedback
```

---

## 8. MySQL Configuration

Copy `config.properties.example` to `src/main/resources/config.properties`
(there is already a ready-to-use copy in the project) and edit:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/java_quiz_platform?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.user=root
db.password=root
```

Credentials are read **only** by `DatabaseConnection` - they are not hardcoded anywhere.

---

## 9. Maven Build

```powershell
cd "JAVA QUIZ"
mvn compile        # compile everything
mvn test           # 18 unit/integration tests (no MySQL needed)
mvn package        # builds target\java-quiz-platform.war for Tomcat
```

---

## 10. How to Run the Swing Application

Requirements: Java 17+, MySQL running, database + sample data loaded (steps 7-8).

```powershell
mvn compile exec:java
```

The **LoginFrame** opens. Demo flow: admin login -> manage users ->
approve quiz -> creator login -> create quiz -> participant login -> timed quiz
-> auto-submit -> performance history.

---

## 11. How to Deploy the Servlet Module on Tomcat

1. Install **Apache Tomcat 9.x** (supports Servlet 4.0 / `javax.servlet`).
2. Build the WAR: `mvn package`.
3. Copy `target\java-quiz-platform.war` into Tomcat's `webapps` folder.
4. Start Tomcat (`bin\startup.bat`), then open:

```
http://localhost:8080/java-quiz-platform/
```

5. Pages: `/` (home), `/login`, `/quiz` (list/attempt), `/result` (history),
   `/admin` (admin only - plain participants get HTTP 403).
6. The web module uses the **same service/DAO/JDBC layer** as the Swing app
   (see `LoginServlet`, `QuizServlet`, `ResultServlet`, `AdminServlet`).

---

## 12. Default Login Credentials

| Role | Email | Password |
|------|-------|----------|
| Administrator | admin@quiz.com | admin123 |
| Quiz Creator | creator@quiz.com | creator123 |
| Participant | student@quiz.com | student123 |

Sample quizzes: 3 approved quizzes (OOP, Collections/Threads, JDBC/Servlets)
+ 1 pending quiz for the approval demo + 2 sample attempts of the student.

---

## 13. Folder Structure

```
JAVA QUIZ/
├── pom.xml                                Maven build (WAR, Java 17, MySQL, servlet, JUnit, H2)
├── README.md                              this file
├── config.properties.example              copy template for DB credentials
├── database/
│   ├── schema.sql                         full MySQL schema (10 tables, PKs, FKs, CHECKs)
│   └── sample_data.sql                    roles, 3 users, 4 quizzes, 33 questions, 132 options,
│                                          2 attempts, creator feedback for the student
└── src/
    ├── main/
    │   ├── java/com/quizplatform/
    │   │   ├── QuizApplication.java       Swing entry point (EDT launch)
    │   │   ├── model/                     User (+Admin/QuizCreator/Participant), Quiz,
    │   │   │                              Question, Option, QuizAttempt, Answer, Result,
    │   │   │                              Feedback, SystemSetting + enums
    │   │   ├── interfaces/                Authenticatable, UserOperations,
    │   │   │                              QuizOperations, ResultOperations
    │   │   ├── exception/                 InvalidLogin/Quiz/NotFound/User, Database, TimeExpired
    │   │   ├── database/                  DatabaseConnection (config + JDBC factory)
    │   │   ├── repository/                Repository<T> (generic in-memory cache)
    │   │   ├── util/                      PasswordUtil (SHA-256), ValidationUtil
    │   │   ├── dao/                       8 DAO interfaces
    │   │   ├── dao/impl/                  8 JDBC implementations (PreparedStatement+ResultSet)
    │   │   ├── service/                   Authentication/User/Quiz/Score/Result services,
    │   │   │                              ResultManager (synchronized), SystemSettingsService
    │   │   ├── timer/                     QuizTimer (dedicated countdown thread)
    │   │   ├── gui/                       Login/Register frames, 3 dashboards, 15 screens
    │   │   │                              (users, approval, settings, create-quiz, questions,
    │   │   │                              results, feedback, attempt, history, profile...)
    │   │   └── servlet/                   Login/Quiz/Result/Admin servlets + WebPages helper
    │   ├── resources/config.properties    DB credentials (classpath)
    │   └── webapp/                        index.html, css/style.css, WEB-INF/web.xml
    └── test/java/com/quizplatform/        5 test classes, 18 tests
```

---

## 14. Testing

`mvn test` runs everything **without MySQL** (real JDBC is tested against an
in-memory H2 database in MySQL mode using the actual `database/schema.sql`):

| Test class | Covers |
|------------|--------|
| `ScoreServiceTest` | correct/incorrect/unanswered marks, percentage, feedback, `generateReport` |
| `RepositoryTest` | generic `Repository<T>` for `Quiz` and `Result` |
| `ValidationAndExceptionTest` | SHA-256, email/duration/password validation, role conversion, `InvalidLoginException` |
| `QuizTimerTest` | countdown ticks, expiry callback, `stop()` via interrupt, MM:SS format |
| `DatabaseOperationsTest` | user CRUD + duplicate email, login success/failure, quiz validation + approval workflow, transactional submission + score, history, `TimeExpiredException`, DB-failure |

Manual test checklist for the viva:

1. Admin login success / wrong password error
2. Create user -> duplicate email rejected
3. Update user (blank password keeps old one)
4. Delete user with confirmation
5. Create quiz with 0-minute duration rejected
6. Add question with < 2 options or no correct answer rejected
7. Submit empty quiz rejected
8. Admin approves quiz -> participant can see it
9. Admin rejects quiz -> participant cannot see it
10. Participant takes timed quiz, navigates, submits
11. Unanswered warning on submit
12. Timer reaches 00:00 -> auto-submit
13. Score/percentage/correct-incorrect/feedback screen
14. Performance history + detail review
15. Creator gives feedback -> participant sees it
16. MySQL stopped -> friendly `DatabaseException` message (no stack trace in UI)

---

## 15. 10-Minute Demo Script (viva)

1. `mvn compile exec:java` -> LoginFrame
2. `admin@quiz.com` -> Dashboard shows users, quizzes, pending, attempts
3. Users -> create a participant, update a role, delete with confirmation
4. Quiz Approval -> View Details of quiz #4 -> **Approve** -> participant sees it
5. Logout -> `creator@quiz.com` -> Create Quiz + add a question (radio = correct answer)
6. My Quizzes -> Submit for Approval -> status becomes PENDING
7. Logout -> `student@quiz.com` -> Available Quizzes -> Start -> answer with radio buttons
8. Submit -> Result screen (score, `JProgressBar`, question review, feedback)
9. My Attempts -> View Full Detail; Profile -> change name/password
10. (Optional) Deploy WAR on Tomcat -> open `/admin` -> approve/reject from the browser

---

## 16. Common Errors and Fixes

| Error | Fix |
|-------|-----|
| `Cannot connect to database` | Start MySQL; check `src/main/resources/config.properties` (user/password); run `schema.sql` + `sample_data.sql` |
| `No suitable driver` | MySQL connector is a Maven dependency - run `mvn compile` once with internet |
| `Table 'java_quiz_platform.X' doesn't exist` | Run `database/schema.sql` (creates the DB) then `database/sample_data.sql` |
| `Duplicate entry '...' for key 'email'` | Expected behavior - the email is already registered (also covered by a test) |
| Timer not firing / window frozen | The app uses `invokeLater` on a background thread - one `QuizTimer` per attempt, stopped via `timer.stop()` on re-entry |
| `403 Administrator role required` (web) | Login as `admin@quiz.com` - participants cannot open `/admin` (by design) |
| `404` on Tomcat | WAR name must be `java-quiz-platform.war`; open `/java-quiz-platform/` with the context path |
| `mvn: command not found` | Install Maven 3.8+ and Java 17, set `JAVA_HOME` |

---

## 17. Viva Preparation Pointers

Point the examiner at the comments in the code - every concept is marked:

* `// Demonstrates inheritance / runtime polymorphism / method overriding` (model)
* `// Demonstrates interface implementation` (services)
* `// Demonstrates custom exception handling` (services, GUI, DAOs)
* `// Demonstrates Generics` (repository, service caches)
* `// Demonstrates multithreading` (timer)
* `// Demonstrates synchronization` (ResultManager)
* `// Demonstrates JDBC / transaction management` (dao.impl)
* `// Demonstrates Servlet integration` (servlet package)

Know in one sentence each: why `PreparedStatement` (SQL injection safety), why
`synchronized` on `saveResult` (the EDT and the timer thread share statistics),
why `invokeLater` (Swing is single-threaded), what happens on `commit` vs
`rollback` in `submitAttempt`, and how role strings become `Admin` /
`QuizCreator` / `Participant` objects (`UserDAOImpl.mapRow`).

