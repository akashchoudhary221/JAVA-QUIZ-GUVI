package com.quizplatform;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.impl.AttemptDAOImpl;
import com.quizplatform.database.DatabaseConnection;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.exception.TimeExpiredException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Option;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizAttempt;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.Result;
import com.quizplatform.model.Role;
import com.quizplatform.model.User;
import com.quizplatform.service.AuthenticationService;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultManager;
import com.quizplatform.service.ResultService;
import com.quizplatform.service.UserService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DATABASE / JDBC INTEGRATION TEST.
 *
 * Runs the REAL DAO implementations and the REAL database/schema.sql
 * against an in-memory H2 database in MySQL compatibility mode, so the
 * whole persistence layer can be verified without a MySQL server.
 * (The application itself always uses MySQL - see config.properties.)
 *
 * Covered rubric points:
 *  - DAO classes and CRUD operations
 *  - PreparedStatement / ResultSet mapping
 *  - transaction commit during quiz submission
 *  - custom exceptions (invalid user, invalid quiz, time expired, DB failure)
 *  - runtime polymorphism when mapping the role column to Admin/Creator/Participant
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DatabaseOperationsTest {

    private static final String H2_URL =
            "jdbc:h2:mem:quizdb;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    @BeforeAll
    static void setUpDatabase() throws Exception {
        // programmatic override of config.properties (used by tests)
        DatabaseConnection.configure(H2_URL, "sa", "");
        executeSchemaScript();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement statement = conn.createStatement()) {
            statement.execute("INSERT INTO roles (role_id, role_name) VALUES "
                    + "(1, 'ADMIN'), (2, 'QUIZ_CREATOR'), (3, 'PARTICIPANT')");
        }
    }

    /**
     * Executes database/schema.sql on the in-memory database.
     * MySQL specific parts (CREATE DATABASE / USE / ENGINE clause) are skipped.
     */
    private static void executeSchemaScript() throws Exception {
        String raw = Files.readString(Path.of("database", "schema.sql"));
        StringBuilder cleaned = new StringBuilder();
        for (String line : raw.split("\n")) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        String sql = cleaned.toString().replaceAll("(?i)ENGINE\\s*=[^;]*", "");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement statement = conn.createStatement()) {
            for (String part : sql.split(";")) {
                String stmt = part.trim();
                if (stmt.isEmpty()) {
                    continue;
                }
                String upper = stmt.toUpperCase();
                if (upper.startsWith("CREATE DATABASE") || upper.startsWith("USE ")) {
                    continue;
                }
                statement.execute(stmt);
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("UserDAO + UserService: full CRUD with validation and duplicate email check")
    void userCrud() throws Exception {
        UserService userService = new UserService();

        // ---- CREATE ----
        Participant participant = new Participant(0, "Test Student",
                "test.student@example.com", "secret123");
        User created = userService.createUser(participant);
        assertTrue(created.getId() > 0, "generated user id expected");

        // ---- READ ----
        User loaded = userService.getUserByEmail("test.student@example.com");
        assertNotNull(loaded);
        assertEquals("Test Student", loaded.getName());
        assertEquals(64, loaded.getPassword().length(), "password must be stored as SHA-256 hash");
        assertFalse(loaded.getPassword().contains("secret123"));
        // Demonstrates runtime polymorphism: role column -> concrete class
        assertTrue(loaded instanceof Participant, "role PARTICIPANT must be mapped to Participant");
        assertEquals(Role.PARTICIPANT, loaded.getRole());

        // ---- VALIDATION / CUSTOM EXCEPTIONS ----
        assertThrows(InvalidUserException.class, () -> userService.createUser(
                new Participant(0, "Duplicate", "test.student@example.com", "secret123")),
                "duplicate email must be rejected");
        assertThrows(InvalidUserException.class, () -> userService.createUser(
                new Participant(0, "Bad Email", "not-an-email", "secret123")),
                "invalid email must be rejected");
        assertThrows(InvalidUserException.class, () -> userService.createUser(
                new Participant(0, "", "ok@example.com", "secret123")),
                "empty name must be rejected");
        assertThrows(InvalidUserException.class, () -> userService.createUser(
                new Participant(0, "Weak", "weak@example.com", "123")),
                "weak password must be rejected");

        // ---- UPDATE ----
        loaded.setName("Renamed Student");
        userService.updateUser(loaded);
        assertEquals("Renamed Student", userService.getUserById(loaded.getId()).getName());

        List<User> allUsers = userService.getAllUsers();
        assertTrue(allUsers.stream().anyMatch(u -> u.getId() == loaded.getId()));

        // ---- DELETE ----
        userService.deleteUser(loaded.getId());
        assertNull(userService.getUserById(loaded.getId()));
    }

    @Test
    @Order(2)
    @DisplayName("AuthenticationService: successful login + InvalidLoginException cases")
    void authenticationFlow() throws Exception {
        UserService userService = new UserService();
        AuthenticationService auth = new AuthenticationService();

        userService.createUser(new Participant(0, "Auth User",
                "auth.user@example.com", "password123"));

        // successful login - returns a polymorphic User object
        User user = auth.login("auth.user@example.com", "password123");
        assertTrue(user instanceof Participant);
        assertTrue(user.isLoggedIn());

        // custom exception: wrong password
        InvalidLoginException wrongPassword = assertThrows(InvalidLoginException.class,
                () -> auth.login("auth.user@example.com", "wrongPassword"));
        assertTrue(wrongPassword.getMessage().contains("Incorrect password"));

        // custom exception: unknown email
        assertThrows(InvalidLoginException.class,
                () -> auth.login("ghost@example.com", "password123"));

        // custom exception: empty input
        assertThrows(InvalidLoginException.class, () -> auth.login("", "password123"));

        auth.logout(user);
        assertFalse(user.isLoggedIn());
    }

    @Test
    @Order(3)
    @DisplayName("QuizService: validation, question storage and approval workflow")
    void quizWorkflow() throws Exception {
        UserService userService = new UserService();
        QuizService quizService = new QuizService();

        QuizCreator creator = (QuizCreator) userService.createUser(
                new QuizCreator(0, "Flow Creator", "flow.creator@example.com", "creatorpass"));
        assertTrue(creator instanceof QuizCreator);

        // ---- invalid quiz input -> InvalidQuizException ----
        assertThrows(InvalidQuizException.class, () -> quizService.createQuiz(
                new Quiz("", "description", 10)), "empty title must be rejected");
        assertThrows(InvalidQuizException.class, () -> quizService.createQuiz(
                new Quiz("Valid Title", "description", 0)), "duration 0 must be rejected");
        assertThrows(InvalidQuizException.class, () -> quizService.createQuiz(
                new Quiz("Valid Title", "description", 9999)), "duration too large must be rejected");
        assertThrows(InvalidQuizException.class, () -> quizService.createQuiz(
                new Quiz("Valid Title", "", 10)), "empty description must be rejected");

        // ---- create ----
        Quiz quiz = new Quiz("H2 Integration Quiz", "Used by the database test", 10);
        quiz.setCreatedBy(creator.getId());
        quizService.createQuiz(quiz);
        assertTrue(quiz.getId() > 0);
        assertEquals(QuizStatus.DRAFT, quiz.getStatus());

        // ---- submit without questions -> custom exception ----
        assertThrows(InvalidQuizException.class, () -> quizService.submitForApproval(quiz.getId()));

        // ---- invalid questions ----
        Question noCorrect = new Question("No correct option?", 1, "");
        noCorrect.setQuizId(quiz.getId());
        noCorrect.addOption(new Option("A", 1, false));
        noCorrect.addOption(new Option("B", 2, false));
        assertThrows(InvalidQuizException.class, () -> quizService.addQuestion(noCorrect));

        Question onlyOneOption = new Question("Only one option?", 1, "");
        onlyOneOption.setQuizId(quiz.getId());
        onlyOneOption.addOption(new Option("Only", 1, true));
        assertThrows(InvalidQuizException.class, () -> quizService.addQuestion(onlyOneOption));

        // ---- valid question with options ----
        Question valid = new Question("What is 2 + 2 ?", 2, "Basic arithmetic");
        valid.setQuizId(quiz.getId());
        valid.addOption(new Option("3", 1, false));
        valid.addOption(new Option("4", 2, true));
        valid.addOption(new Option("5", 3, false));
        quizService.addQuestion(valid);
        assertTrue(valid.getId() > 0);

        List<Question> questions = quizService.getQuestionsForQuiz(quiz.getId());
        assertEquals(1, questions.size());
        assertEquals(3, questions.get(0).getOptions().size());
        assertTrue(questions.get(0).getOptions().get(1).isCorrect(), "correct flag must persist");
        assertEquals(2, quizService.getQuizById(quiz.getId()).getTotalMarks(),
                "total marks must be recomputed");

        // ---- approval workflow ----
        quizService.submitForApproval(quiz.getId());
        assertEquals(QuizStatus.PENDING_APPROVAL, quizService.getQuizById(quiz.getId()).getStatus());

        quizService.approveQuiz(quiz.getId());
        assertEquals(QuizStatus.APPROVED, quizService.getQuizById(quiz.getId()).getStatus());
        assertTrue(quizService.getApprovedQuizzes().stream().anyMatch(q -> q.getId() == quiz.getId()),
                "approved quiz must be visible to participants");

        quizService.rejectQuiz(quiz.getId());
        assertEquals(QuizStatus.REJECTED, quizService.getQuizById(quiz.getId()).getStatus());
        assertFalse(quizService.getApprovedQuizzes().stream().anyMatch(q -> q.getId() == quiz.getId()));

        // ---- custom exception: missing quiz ----
        assertThrows(QuizNotFoundException.class, () -> quizService.getQuizByIdOrThrow(999999));
    }

    @Test
    @Order(4)
    @DisplayName("ResultService: transactional submission, score calculation and history")
    void submissionTransactionAndScores() throws Exception {
        UserService userService = new UserService();
        QuizService quizService = new QuizService();
        ResultService resultService = new ResultService();
        AttemptDAO attemptDAO = new AttemptDAOImpl();

        QuizCreator creator = (QuizCreator) userService.getUserByEmail("flow.creator@example.com");
        Participant participant = (Participant) userService.createUser(
                new Participant(0, "Submission User", "submit.user@example.com", "password123"));

        Quiz quiz = new Quiz("Transaction Quiz", "commit / rollback demo", 10);
        quiz.setCreatedBy(creator.getId());
        quizService.createQuiz(quiz);

        // two questions, each worth 1 mark
        Question first = new Question("First question?", 1, "");
        first.setQuizId(quiz.getId());
        first.addOption(new Option("wrong", 1, false));
        first.addOption(new Option("right", 2, true));
        quizService.addQuestion(first);

        Question second = new Question("Second question?", 1, "");
        second.setQuizId(quiz.getId());
        second.addOption(new Option("right", 1, true));
        second.addOption(new Option("wrong", 2, false));
        quizService.addQuestion(second);

        List<Question> questions = quizService.getQuestionsForQuiz(quiz.getId());
        long correctOfFirst = questions.get(0).getCorrectOptionId();
        long correctOfSecond = questions.get(1).getCorrectOptionId();

        int submissionsBefore = ResultManager.getInstance().getTotalSubmissions();

        // ---- successful submission: both answers correct ----
        List<Answer> answers = List.of(
                new Answer(questions.get(0).getId(), correctOfFirst),
                new Answer(questions.get(1).getId(), correctOfSecond));
        Result result = resultService.submitAttempt(quiz, questions, participant.getId(),
                participant.getName(), answers, 60, false);

        assertTrue(result.getId() > 0, "result must be persisted inside the transaction");
        assertEquals(2, result.getCorrectAnswers());
        assertEquals(0, result.getIncorrectAnswers());
        assertEquals(100.0, result.getPercentage(), 0.001);
        assertEquals(2, result.getTotalMarks());
        assertEquals(2, result.getObtainedMarks());

        // synchronized ResultManager must see the new submission
        assertEquals(submissionsBefore + 1, ResultManager.getInstance().getTotalSubmissions());

        // ---- everything is stored (attempt + answers + result) ----
        assertTrue(resultService.getHistory(participant.getId()).stream()
                .anyMatch(r -> r.getId() == result.getId()));
        assertFalse(attemptDAO.getAttemptsByParticipant(participant.getId()).isEmpty());
        assertEquals(2, attemptDAO.getAnswersByAttempt(result.getAttemptId()).size());

        // ---- partial answers (one question skipped) ----
        List<Answer> partial = List.of(
                new Answer(questions.get(0).getId(), correctOfFirst),
                new Answer(questions.get(1).getId(), -1L));
        Result partialResult = resultService.submitAttempt(quiz, questions, participant.getId(),
                participant.getName(), partial, 30, false);
        assertEquals(1, partialResult.getAttemptedQuestions());
        assertEquals(1, partialResult.getCorrectAnswers());
        assertEquals(50.0, partialResult.getPercentage(), 0.001);

        // ---- custom exceptions ----
        assertThrows(InvalidQuizException.class, () -> resultService.submitAttempt(
                quiz, questions, participant.getId(), participant.getName(), List.of(), 10, false),
                "empty answer list must be rejected");
        assertThrows(TimeExpiredException.class, () -> resultService.submitAttempt(
                quiz, questions, participant.getId(), participant.getName(), answers, 601, false),
                "time beyond the duration must be rejected");
        assertThrows(InvalidQuizException.class, () -> resultService.submitAttempt(
                quiz, List.of(), participant.getId(), participant.getName(), answers, 10, false),
                "quiz without questions must be rejected");

        // average of the shared synchronized statistics stays consistent
        assertTrue(ResultManager.getInstance().getAveragePercentage() >= 0);
    }

    @Test
    @Order(5)
    @DisplayName("Database failures are wrapped in DatabaseException (never silent)")
    void databaseFailureIsReported() {
        try {
            // point the configuration to a MySQL server that does not exist
            DatabaseConnection.configure(
                    "jdbc:mysql://127.0.0.1:1/fail?connectTimeout=300&socketTimeout=300", "bad", "bad");
            assertThrows(DatabaseException.class, DatabaseConnection::getConnection,
                    "connection failure must raise DatabaseException");
        } finally {
            // restore the in-memory configuration for any later test
            DatabaseConnection.configure(H2_URL, "sa", "");
        }
    }
}
