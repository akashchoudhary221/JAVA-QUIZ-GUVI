# Java-Based Online Quiz Platform

An enterprise-grade academic **Java** project implementing a comprehensive online quiz platform for Java programming education: participants take **timed quizzes** with an interactive question navigation palette, receive **real-time scores and analytics**, and review their **performance reports**; quiz creators author questions, manage options, and submit quizzes; and administrators govern users, quiz approvals, system settings, and platform reports.

> **100% Java Stack Throughout** &mdash; Core Java (Java 17/21), OOP Design, Swing GUI with FlatLaf Modern Look & Feel, Java Servlets (Jakarta/Javax), Direct JDBC, and MySQL. There is **no React, Next.js, Node.js, Express, Python, or PHP** anywhere in this project.

---

## 🚀 Quick Start — Desktop Swing Application

Requirements: Java 17+, MySQL Server 8.x running, Maven 3.8+.

1. **Initialize Database** (MySQL):
   ```sql
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/sample_data.sql
   ```
2. **Verify Database Credentials**:
   Inspect or edit `src/main/resources/config.properties`:
   ```properties
   db.driver=com.mysql.cj.jdbc.Driver
   db.url=jdbc:mysql://localhost:3306/java_quiz_platform?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
   db.user=root
   db.password=root
   ```
3. **Launch the Swing Application**:
   ```powershell
   mvn compile exec:java
   ```

---

## 🌐 Quick Start — Apache Tomcat Web Module

1. **Build Deployable WAR**:
   ```powershell
   mvn clean package
   ```
   This generates `target/java-quiz-platform.war` and executes all unit tests.
2. **Deploy to Tomcat**:
   Copy `target/java-quiz-platform.war` to your Apache Tomcat `webapps/` directory.
3. **Access Web Portal**:
   Open your browser at:
   ```
   http://localhost:8080/java-quiz-platform/
   ```

---

## 1. Project Objectives & Roles

| Role | Core Capabilities |
|------|-------------------|
| **ADMINISTRATOR** | Secure sign in, User CRUD management (create, read, update, delete, search filter), role assignment, quiz approval workflow (inspect questions, approve/reject), system settings configuration, platform analytics |
| **QUIZ CREATOR** | Secure sign in, quiz creation (title, description, duration), question authoring with A-D options and single correct answer, draft state management, submission for admin approval, participant evaluation reviews, personalized student feedback |
| **PARTICIPANT** | Self-registration & sign in, view approved quizzes, timed test taking with live countdown thread, interactive question navigator palette, auto-submit on timeout, instant scoring & percentage calculation, detailed question-by-question review, attempt history |

---

## 2. Architecture & Design System

### A. Architectural Layers
```
┌─────────────────────────────────┐       ┌─────────────────────────────────┐
│     Swing GUI (FlatLaf Look)    │       │     Java Servlets (Web Portal)  │
└────────────────┬────────────────┘       └────────────────┬────────────────┘
                 │                                         │
                 └────────────────────┬────────────────────┘
                                      │
                           ┌──────────▼──────────┐
                           │    Service Layer    │ AuthenticationService, UserService,
                           │   (Business Logic   │ QuizService, ScoreService,
                           │  & Validations)     │ ResultService, ResultManager (Sync)
                           └──────────┬──────────┘
                                      │
                           ┌──────────▼──────────┐
                           │    DAO Layer        │ UserDAO, QuizDAO, QuestionDAO,
                           │  (Interfaces)       │ OptionDAO, AttemptDAO, ResultDAO,
                           │                     │ FeedbackDAO, SystemSettingsDAO
                           └──────────┬──────────┘
                                      │
                           ┌──────────▼──────────┐
                           │  DAO Implementation │ PreparedStatement, ResultSet,
                           │     (Plain JDBC)    │ Batch Updates, Transactions
                           └──────────┬──────────┘
                                      │
                           ┌──────────▼──────────┐
                           │   MySQL Database    │ java_quiz_platform
                           └─────────────────────┘
```

### B. UI/UX Design System Specifications
Both the Swing desktop client and Servlet web application implement a unified, modern aesthetic:
- **Primary Color**: Deep Navy (`#172554`) & Dark Navy (`#0F172A`)
- **Accent Color**: Blue (`#3B82F6`) with hover states (`#2563EB`)
- **Main Background**: Soft Neutral (`#F5F7FB`)
- **Content Surfaces**: Pure White (`#FFFFFF`)
- **Primary Typography**: Dark Slate (`#172033`)
- **Secondary Typography**: Muted Slate (`#64748B`)
- **Success State**: Emerald Green (`#10B981`)
- **Warning State**: Amber (`#F59E0B`)
- **Danger / Error State**: Coral Red (`#EF4444`)
- **Borders & Dividers**: Light Neutral Gray (`#E2E8F0`)

---

## 3. Key UI/UX Screen Improvements

1. **FlatLaf Modern Look & Feel**:
   - Clean system typography antialiasing and rounded corners (`Button.arc: 8`, `Component.arc: 8`, `ProgressBar.arc: 8`).
   - Unified color tokens defined in `UIUtil.java`.

2. **Dashboard Shell Navigation (`DashboardFrame.java`)**:
   - Modern sidebar with brand wordmark (`⚡ QuizPlatform`).
   - **Active Navigation Highlighting**: Clicking any sidebar button highlights the active view in Accent Blue (`#3B82F6`) with bold typography while inactive items remain sleek dark navy.
   - Header with user profile chip, email, and colored role pill badge.
   - Distinct red logout action safely invalidating the session.

3. **Participant Quiz Experience (`QuizAttemptPanel.java`)**:
   - **Interactive Question Navigator Palette**: A grid of buttons (`1`, `2`, `3`... `N`) color-coded to show **Answered (Green)**, **Current (Blue)**, and **Unanswered (Gray)**. Clicking any button saves the current selection and jumps instantly.
   - **Card-Style Radio Options**: Options are enclosed in clean white cards with hover states and a blue border/tint on selection.
   - **Urgency Countdown Timer**: Header timer badge transitions from blue to amber (< 60s) and danger red (< 30s) before automatic submission.
   - **Submission Confirmation**: Summarizes total questions, answered count, and unanswered warning before final submission.

4. **Result Analytics Hero (`ResultPanel.java`)**:
   - Large hero percentage badge with `PASSED` (>= 60%) or `NEEDS IMPROVEMENT` status.
   - Progress bar and KPI metric boxes for Total Questions, Attempted, Correct, Incorrect, and Time Taken.
   - Question review table featuring color-coded pill badges (`CORRECT`, `WRONG`, `SKIPPED`).

5. **Admin Management Suite (`AdminDashboard.java`, `UserManagementPanel.java`, `QuizManagementPanel.java`)**:
   - KPI metric cards for Total Users, Total Quizzes, Pending Approval, Approved, and Total Attempts.
   - Real-time search filter in User Management.
   - Status badge rendering for quiz states (`APPROVED`, `PENDING_APPROVAL`, `REJECTED`, `DRAFT`).
   - Comprehensive quiz inspection dialog detailing every question, option, correct answer, and explanation.

6. **Web Servlet Module (`style.css`, `WebPages.java`, `index.html`)**:
   - Fully responsive layout with sticky header and card surfaces.
   - Styled forms, tables, and badge pills matching desktop styling.

---

## 4. Complete College Marking Rubric Mapping

### Part A: Java GUI Based Projects (33 Marks Total)

| Rubric Criterion | Marks | Proof in Codebase | Exact File & Method Location |
|------------------|:-----:|-------------------|------------------------------|
| **OOP: Polymorphism** | 2.5 | `user.displayDashboard()` called polymorphically on base `User` reference opening `AdminDashboard`, `QuizCreatorDashboard`, or `ParticipantDashboard`; `UserDAOImpl.mapRow()` constructs concrete instances based on role. | [LoginFrame.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/gui/LoginFrame.java#L138)<br>[UserDAOImpl.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/dao/impl/UserDAOImpl.java#L156) |
| **OOP: Inheritance** | 2.5 | `Admin`, `QuizCreator`, and `Participant` extend abstract class `User`; `AdminDashboard`, `QuizCreatorDashboard`, and `ParticipantDashboard` extend `DashboardFrame`. | [Admin.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/model/Admin.java#L8)<br>[Participant.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/model/Participant.java#L8)<br>[DashboardFrame.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/gui/DashboardFrame.java#L29) |
| **OOP: Exception Handling** | 2.5 | 6 custom checked exceptions (`DatabaseException`, `InvalidLoginException`, `InvalidQuizException`, `InvalidUserException`, `QuizNotFoundException`, `TimeExpiredException`) with full `try-catch-finally-throw-throws` propagation and meaningful user dialogs. | [exception package](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/exception)<br>[AuthenticationService.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/service/AuthenticationService.java#L35) |
| **OOP: Interfaces** | 2.5 | Core operation interfaces implemented by service and DAO layers (`Authenticatable`, `UserOperations`, `QuizOperations`, `ResultOperations`, 8 DAO interfaces). | [Authenticatable.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/interfaces/Authenticatable.java)<br>[UserOperations.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/interfaces/UserOperations.java)<br>[QuizOperations.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/interfaces/QuizOperations.java) |
| **Collections & Generics** | 6 | Use of `List<Question>`, `List<Quiz>`, `Map<Integer, Long>` (question answers), and the generic repository `Repository<T>` parameterized as `Repository<Quiz>`, `Repository<Question>`, and `Repository<Result>`. | [Repository.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/repository/Repository.java#L11)<br>[ResultManager.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/service/ResultManager.java#L28)<br>[QuizAttemptPanel.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/gui/QuizAttemptPanel.java#L53) |
| **Multithreading & Synchronization** | 4 | `QuizTimer implements Runnable` runs on a dedicated background thread counting seconds via `Thread.sleep(1000)` and triggers `onTimeExpired()`. Marshalling to EDT via `SwingUtilities.invokeLater()`. `ResultManager.saveResult()` is `synchronized` to prevent lost updates between manual submit (EDT) and auto-submit (Timer thread). | [QuizTimer.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/timer/QuizTimer.java#L14)<br>[ResultManager.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/service/ResultManager.java#L47)<br>[QuizAttemptPanel.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/gui/QuizAttemptPanel.java#L75) |
| **Classes for Database Operations** | 7 | Clear architectural segregation between GUI, Service, and DAO interfaces + implementations. No raw SQL inside GUI frames or servlets. | [dao package](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/dao)<br>[dao.impl package](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/dao/impl) |
| **Database Connectivity using JDBC** | 3 | `DatabaseConnection` reads credentials from classpath `config.properties`, registers MySQL driver, and returns standard `java.sql.Connection`. | [DatabaseConnection.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/database/DatabaseConnection.java#L28) |
| **JDBC Implementation** | 3 | Full CRUD using `PreparedStatement` (parameter binding against SQL injection), `ResultSet` mapping, generated keys retrieval, batch inserts (`executeBatch`), and multi-table transactional commits/rollbacks. | [AttemptDAOImpl.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/dao/impl/AttemptDAOImpl.java#L46)<br>[UserDAOImpl.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/dao/impl/UserDAOImpl.java#L35) |

---

### Part B: Java Web Based Projects (33 Marks Total)

| Rubric Criterion | Marks | Proof in Codebase | Exact File & Method Location |
|------------------|:-----:|-------------------|------------------------------|
| **Problem Understanding & Solution Design** | 8 | End-to-end layered architecture: Servlets handle HTTP requests and delegate business logic to `Service` and `DAO` layers, reusing the domain model. | [QuizServlet.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/servlet/QuizServlet.java#L33)<br>[AdminServlet.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/servlet/AdminServlet.java#L32) |
| **Core Java Concepts** | 10 | Encapsulation, abstraction, exception hierarchies, stream processing (`.filter()`, `.mapToDouble()`), and secure password hashing (`PasswordUtil`). | [PasswordUtil.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/util/PasswordUtil.java#L21)<br>[ValidationUtil.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/util/ValidationUtil.java#L16) |
| **Database Integration using JDBC** | 8 | Servlets perform database queries exclusively through the DAO layer, guaranteeing connection pooling, parameter sanitation, and clean connection closures. | [AdminServlet.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/servlet/AdminServlet.java#L105)<br>[ResultServlet.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/servlet/ResultServlet.java#L56) |
| **Servlets and Web Integration** | 7 | `@WebServlet` annotations, `doGet()` and `doPost()` lifecycle handling, session authorization (`HttpSession.getAttribute("role")`), HTTP status codes (`403 Forbidden`, `400 Bad Request`, `404 Not Found`), and web deployment configuration. | [LoginServlet.java](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/java/com/quizplatform/servlet/LoginServlet.java#L25)<br>[web.xml](file:///c:/Users/manis/OneDrive/Desktop/JAVA-QUIZ-GUVI/src/main/webapp/WEB-INF/web.xml) |

---

## 5. Preconfigured Evaluator Accounts

| Role | Email Address | Password | Permissions & Test Flow |
|------|---------------|----------|--------------------------|
| **ADMIN** | `admin@quiz.com` | `admin123` | Inspects dashboard counters, manages users, approves/rejects quiz #4, updates system settings, views all reports |
| **QUIZ CREATOR** | `creator@quiz.com` | `creator123` | Creates new quizzes, authors questions with options, submits for admin approval, inspects student results, submits feedback |
| **PARTICIPANT** | `student@quiz.com` | `student123` | Takes timed tests (quizzes 1, 2, 3), navigates via palette, views instant score report, checks performance history |

---

## 6. Comprehensive Java Viva Questions & Answers

### Q1: How does your project demonstrate Runtime Polymorphism?
> **Answer**: When a user signs in through `LoginFrame`, the authentication service returns an abstract `User` reference. However, the concrete object instantiated is either an `Admin`, `QuizCreator`, or `Participant` based on the database role. Calling `user.displayDashboard()` polymorphically executes the subclass's overridden implementation, automatically launching the correct dashboard window without rigid `if-else` type checking.

### Q2: Why is `ResultManager.saveResult()` synchronized?
> **Answer**: Quiz submissions can originate from two concurrent threads: (1) manual submission triggered by the user on the Swing Event Dispatch Thread (EDT), or (2) automatic timeout submission triggered by the background `QuizTimer` thread when the countdown hits `00:00`. Because both threads update shared statistics (`totalSubmissions`, `percentageSum`, `bestPercentage`), `synchronized` enforces mutual exclusion, preventing race conditions and lost updates.

### Q3: Why is `SwingUtilities.invokeLater()` necessary in `QuizTimer`?
> **Answer**: Java Swing is single-threaded and not thread-safe. All GUI modifications (updating labels, text, buttons) must occur strictly on the Event Dispatch Thread (EDT). Since `QuizTimer` counts down on a separate worker thread (`QuizTimer-Thread`), it marshals its tick and expiry callbacks onto the EDT queue using `SwingUtilities.invokeLater()`, preventing UI freezing or concurrent rendering artifacts.

### Q4: How do JDBC transactions guarantee data integrity during quiz submission?
> **Answer**: In `AttemptDAOImpl.submitAttempt()`, an attempt consists of three related tables: `quiz_attempts`, `answers`, and `results`. We invoke `conn.setAutoCommit(false)` to start a database transaction. If all three inserts succeed, `conn.commit()` is called. If an error occurs anywhere (e.g., database constraint failure), the catch block executes `conn.rollback()`, ensuring the database is never left with orphaned attempts or incomplete answers.

### Q5: Why do you use `PreparedStatement` instead of `Statement`?
> **Answer**: `PreparedStatement` precompiles SQL statements and binds inputs using positional parameters (`?`). This separates query code from user data, neutralizing SQL Injection vulnerabilities. Furthermore, for bulk operations like inserting student answers, `PreparedStatement.addBatch()` and `executeBatch()` optimize performance by minimizing network round-trips.

### Q6: How is password security implemented without external frameworks?
> **Answer**: In `PasswordUtil.java`, we utilize Java's standard cryptographic API (`java.security.MessageDigest`). Passwords are never stored or transmitted in plain text. When a user registers or updates their password, a SHA-256 digest is generated, converted to a 64-character hexadecimal string, and stored in the MySQL `password` column. Login validation hashes the entered password and compares the hashes.

### Q7: What role does the Generic Repository pattern play in your application?
> **Answer**: `Repository<T>` in `com.quizplatform.repository` is a generic in-memory repository parameterized with type `T`. It provides reusable CRUD and query operations (`add`, `findById`, `findAll`, `delete`) using an internal `Map<Integer, T>`. It is reused as `Repository<Quiz>`, `Repository<Question>`, and `Repository<Result>`, demonstrating Java Generics and eliminating duplicate collection handling code.

### Q8: How does your Servlet module achieve role-based access control?
> **Answer**: When a user authenticates via `LoginServlet`, their role string (`ADMIN`, `QUIZ_CREATOR`, `PARTICIPANT`) is stored in the `HttpSession`. In `AdminServlet`, the `requireAdmin()` helper method verifies `session.getAttribute("role")`. If a participant attempts to access `/admin`, the servlet sends `HttpServletResponse.SC_FORBIDDEN` (HTTP 403), protecting administrative endpoints on the web layer.

---

## 7. Build & Verification Checklist

- [x] **Maven Compile & Dependency Resolution**: FlatLaf, MySQL Connector/J, Servlet API, JUnit 5, and H2 test database compile cleanly.
- [x] **Automated Test Suite**: 18 unit and integration tests passing (`mvn test`).
- [x] **WAR Packaging**: `mvn clean package` generates valid `target/java-quiz-platform.war`.
- [x] **Swing Look & Feel**: Modern FlatLaf theme initialized with `#172554` Navy / `#3B82F6` Blue design tokens.
- [x] **Participant Interactive Experience**: Real-time question navigator palette with answered/unanswered color tracking.
- [x] **Thread Safety**: Multithreaded timer with clean interruption and synchronized in-memory statistics.
