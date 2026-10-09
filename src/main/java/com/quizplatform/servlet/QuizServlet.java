package com.quizplatform.servlet;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.TimeExpiredException;
import com.quizplatform.model.Answer;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.Result;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.ResultService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Web version of the quiz flow:
 *   GET  /quiz                 -> list of approved quizzes
 *   GET  /quiz?action=attempt  -> shows the questions as an HTML form
 *   POST /quiz                 -> stores the attempt in ONE JDBC transaction
 *   GET  /quiz?action=logout   -> ends the session
 */
@WebServlet(name = "QuizServlet", urlPatterns = {"/quiz"})
public class QuizServlet extends HttpServlet {

    private final QuizService quizService = new QuizService();
    private final ResultService resultService = new ResultService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        String action = request.getParameter("action");
        if (action == null) {
            action = "list";
        }
        switch (action) {
            case "logout":
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/login");
                break;
            case "attempt":
                renderAttemptForm(request, response);
                break;
            default:
                renderQuizList(request, response);
        }
    }

    private void renderQuizList(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String context = request.getContextPath();
        String userName = (String) request.getSession().getAttribute("userName");

        StringBuilder rows = new StringBuilder();
        try {
            List<Quiz> quizzes = quizService.getApprovedQuizzes();
            if (quizzes.isEmpty()) {
                rows.append("<tr><td colspan=\"5\">No approved quizzes available yet.</td></tr>");
            }
            for (Quiz quiz : quizzes) {
                rows.append("<tr>")
                        .append("<td>").append(quiz.getId()).append("</td>")
                        .append("<td>").append(WebPages.escape(quiz.getTitle())).append("</td>")
                        .append("<td>").append(quiz.getDurationMinutes()).append(" min</td>")
                        .append("<td>").append(quiz.getTotalMarks()).append("</td>")
                        .append("<td><a class=\"button\" href=\"").append(context)
                        .append("/quiz?action=attempt&id=").append(quiz.getId())
                        .append("\">Start</a></td>")
                        .append("</tr>");
            }
        } catch (DatabaseException e) {
            rows.append("<tr><td colspan=\"5\">")
                    .append(WebPages.escape(e.getMessage())).append("</td></tr>");
        }

        String body = "<section class=\"card\">"
                + "<h2>Welcome " + WebPages.escape(String.valueOf(userName))
                + " - available quizzes</h2>"
                + "<table><thead><tr><th>ID</th><th>Title</th><th>Duration</th>"
                + "<th>Marks</th><th>Action</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "<p class=\"muted\">The countdown timer of this project is implemented in Java "
                + "(Swing desktop application). This web module focuses on servlet integration.</p>"
                + "</section>";
        out.print(WebPages.page(context, "Available Quizzes", body));
    }

    /** Renders the questions of one quiz as an HTML form (radio buttons). */
    private void renderAttemptForm(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String context = request.getContextPath();

        int quizId;
        try {
            quizId = Integer.parseInt(request.getParameter("id"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid quiz id");
            return;
        }

        try {
            Quiz quiz = quizService.getQuizById(quizId);
            List<Question> questions = quizService.getQuestionsForQuiz(quizId);
            if (quiz == null || questions.isEmpty()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
                return;
            }
            // remember the start time in the session (used for time taken)
            HttpSession session = request.getSession(true);
            session.setAttribute("attemptStart_" + quizId, System.currentTimeMillis());

            StringBuilder html = new StringBuilder();
            html.append("<section class=\"card\">")
                    .append("<h2>").append(WebPages.escape(quiz.getTitle())).append("</h2>")
                    .append("<p class=\"muted\">Duration: ").append(quiz.getDurationMinutes())
                    .append(" minutes | Marks: ").append(quiz.getTotalMarks()).append("</p>")
                    .append("<form method=\"post\" action=\"").append(context).append("/quiz\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quizId).append("\">");

            int number = 1;
            for (Question question : questions) {
                html.append("<fieldset>")
                        .append("<legend>Q").append(number++).append(". ")
                        .append(WebPages.escape(question.getText()))
                        .append(" [").append(question.getMarks()).append(" mark(s)]</legend>");
                for (Option option : question.getOptions()) {
                    html.append("<label class=\"option\">")
                            .append("<input type=\"radio\" name=\"q_").append(question.getId())
                            .append("\" value=\"").append(option.getId()).append("\"> ")
                            .append(option.getLetter()).append(") ")
                            .append(WebPages.escape(option.getOptionText()))
                            .append("</label>");
                }
                html.append("</fieldset>");
            }
            html.append("<button type=\"submit\">Submit Quiz</button>")
                    .append("</form></section>");

            out.print(WebPages.page(context, "Attempt Quiz", html.toString()));
        } catch (DatabaseException e) {
            out.print(WebPages.page(context, "Error",
                    WebPages.alert("error", e.getMessage())));
        }
    }

    /** Stores attempt + answers + result in ONE transaction (AttemptDAO.submitAttempt). */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        int userId = (Integer) session.getAttribute("userId");
        String userName = (String) session.getAttribute("userName");
        String context = request.getContextPath();

        int quizId;
        try {
            quizId = Integer.parseInt(request.getParameter("quizId"));
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid quiz id");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            Quiz quiz = quizService.getQuizById(quizId);
            if (quiz == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found");
                return;
            }
            List<Question> questions = quizService.getQuestionsForQuiz(quizId);

            // ---- collect the submitted answers from the request parameters ----
            List<Answer> answers = new ArrayList<>();
            for (Question question : questions) {
                String value = request.getParameter("q_" + question.getId());
                long selected = -1;
                if (value != null && !value.isBlank()) {
                    try {
                        selected = Long.parseLong(value);
                    } catch (NumberFormatException parseError) {
                        selected = -1;   // ignore invalid input instead of crashing
                    }
                }
                answers.add(new Answer(question.getId(), selected));
            }

            // ---- elapsed time from the session start timestamp ----
            Object startAttribute = session.getAttribute("attemptStart_" + quizId);
            long elapsedSeconds = 0;
            if (startAttribute instanceof Long start) {
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000;
            }
            long allowed = quiz.getDurationMinutes() * 60L;
            boolean timedOut = elapsedSeconds > allowed;
            if (timedOut) {
                elapsedSeconds = allowed;   // keep the answers that were given before expiry
            }
            session.removeAttribute("attemptStart_" + quizId);

            Result result = resultService.submitAttempt(quiz, questions, userId, userName,
                    answers, elapsedSeconds, timedOut);

            renderResult(out, context, result, questions, answers);
        } catch (TimeExpiredException | InvalidQuizException | DatabaseException e) {
            out.print(WebPages.page(context, "Submission failed",
                    WebPages.alert("error", e.getMessage())
                            + "<a class=\"button\" href=\"" + context + "/quiz\">Back to quizzes</a>"));
        }
    }

    /** Renders the score + question-wise review of a finished attempt. */
    private void renderResult(PrintWriter out, String context, Result result,
                              List<Question> questions, List<Answer> answers) {
        java.util.Map<Integer, Long> chosenByQuestion = new java.util.HashMap<>();
        for (Answer answer : answers) {
            chosenByQuestion.put(answer.getQuestionId(), answer.getSelectedOptionId());
        }

        StringBuilder rows = new StringBuilder();
        int number = 1;
        for (Question question : questions) {
            long chosen = chosenByQuestion.getOrDefault(question.getId(), -1L);
            String yourAnswer = "Not answered";
            boolean correct = false;
            boolean attempted = false;
            for (Option option : question.getOptions()) {
                if (option.getId() == chosen) {
                    yourAnswer = option.getLetter() + ") " + option.getOptionText();
                    correct = option.isCorrect();
                    attempted = true;
                }
            }
            Option correctOption = question.getCorrectOption();
            String correctAnswer = correctOption == null ? "-"
                    : correctOption.getLetter() + ") " + correctOption.getOptionText();
            String outcome = !attempted ? "SKIPPED" : (correct ? "CORRECT" : "WRONG");
            rows.append("<tr><td>").append(number++).append("</td>")
                    .append("<td>").append(WebPages.escape(question.getText())).append("</td>")
                    .append("<td>").append(WebPages.escape(yourAnswer)).append("</td>")
                    .append("<td>").append(WebPages.escape(correctAnswer)).append("</td>")
                    .append("<td class=\"").append(outcome.toLowerCase()).append("\">")
                    .append(outcome).append("</td></tr>");
        }

        String body = "<section class=\"card\">"
                + "<h2>Result: " + WebPages.escape(result.getQuizTitle()) + "</h2>"
                + WebPages.alert("success", "Score " + result.getObtainedMarks() + "/"
                + result.getTotalMarks() + "  (" + result.getPercentage() + "%)")
                + "<p><b>Correct:</b> " + result.getCorrectAnswers()
                + " &nbsp; <b>Incorrect:</b> " + result.getIncorrectAnswers()
                + " &nbsp; <b>Time taken:</b> " + result.getFormattedTimeTaken() + "</p>"
                + "<p><i>" + WebPages.escape(result.getFeedback()) + "</i></p>"
                + "<table><thead><tr><th>#</th><th>Question</th><th>Your answer</th>"
                + "<th>Correct answer</th><th>Result</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "<a class=\"button\" href=\"" + context + "/quiz\">Back to quizzes</a> "
                + "<a class=\"button\" href=\"" + context + "/result\">My performance history</a>"
                + "</section>";
        out.print(WebPages.page(context, "Quiz Result", body));
    }
}