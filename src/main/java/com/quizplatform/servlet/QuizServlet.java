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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                rows.append("<tr><td colspan=\"5\" style=\"text-align: center; color: var(--text-muted);\">No approved quizzes available currently.</td></tr>");
            }
            for (Quiz quiz : quizzes) {
                rows.append("<tr>")
                        .append("<td>#").append(quiz.getId()).append("</td>")
                        .append("<td><b>").append(WebPages.escape(quiz.getTitle())).append("</b><br>")
                        .append("<small style=\"color: var(--text-muted);\">").append(WebPages.escape(quiz.getDescription())).append("</small></td>")
                        .append("<td>").append(quiz.getDurationMinutes()).append(" mins</td>")
                        .append("<td>").append(quiz.getTotalMarks()).append(" pts</td>")
                        .append("<td><a class=\"button\" href=\"").append(context)
                        .append("/quiz?action=attempt&id=").append(quiz.getId())
                        .append("\">▶ Start Quiz</a></td>")
                        .append("</tr>");
            }
        } catch (DatabaseException e) {
            rows.append("<tr><td colspan=\"5\">")
                    .append(WebPages.escape(e.getMessage())).append("</td></tr>");
        }

        String body = "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Available Quizzes</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Welcome back, <b>" + WebPages.escape(String.valueOf(userName)) + "</b> &mdash; select an approved quiz below to begin.</p>"
                + "  </div>"
                + "  <a class=\"button secondary\" href=\"" + context + "/result\">View My Past Results</a>"
                + "</div>"
                + "<table><thead><tr><th>ID</th><th>Quiz Details</th><th>Duration</th>"
                + "<th>Total Marks</th><th>Action</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "<div style=\"margin-top: 20px; padding: 12px 16px; background: #f8fafc; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px;\">"
                + "  <p style=\"color: var(--text-muted);\">💡 <b>Notice:</b> You are taking this quiz in real-time. Each approved quiz features automated timing, questions with options A-D, and immediate evaluation.</p>"
                + "</div>"
                + "</section>";
        out.print(WebPages.page(request, "Available Quizzes", body));
    }

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
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Quiz not found or contains no questions");
                return;
            }

            HttpSession session = request.getSession(true);
            session.setAttribute("attemptStart_" + quizId, System.currentTimeMillis());

            int totalSeconds = quiz.getDurationMinutes() * 60;

            StringBuilder html = new StringBuilder();
            html.append("<div id=\"timerBanner\" style=\"position:sticky; top:75px; z-index:40; background:#172554; color:#fff; padding:12px 20px; border-radius:var(--radius-md); margin-bottom:20px; display:flex; justify-content:space-between; align-items:center; box-shadow:var(--shadow-md);\">")
                    .append("  <div style=\"display:flex; align-items:center; gap:8px;\">")
                    .append("    <span style=\"font-size:20px;\">⏱️</span>")
                    .append("    <span style=\"font-weight:600;\">Time Remaining:</span>")
                    .append("    <span id=\"timerDisplay\" style=\"font-size:20px; font-weight:700; color:#3b82f6; font-family:monospace;\">")
                    .append(String.format("%02d:00", quiz.getDurationMinutes())).append("</span>")
                    .append("  </div>")
                    .append("  <span style=\"font-size:13px; color:#94a3b8;\">⚡ Auto-submits when time expires</span>")
                    .append("</div>")
                    .append("<section class=\"card\">")
                    .append("<div class=\"card-header\">")
                    .append("  <div>")
                    .append("    <h2>").append(WebPages.escape(quiz.getTitle())).append("</h2>")
                    .append("    <p class=\"subtitle\" style=\"margin-bottom:0;\">Duration: <b>").append(quiz.getDurationMinutes())
                    .append(" minutes</b> &bull; Total Marks: <b>").append(quiz.getTotalMarks()).append(" pts</b> &bull; ")
                    .append(questions.size()).append(" Questions</p>")
                    .append("  </div>")
                    .append("</div>")
                    .append("<form id=\"quizForm\" method=\"post\" action=\"").append(context).append("/quiz\" onsubmit=\"return confirm('Are you sure you want to submit your quiz?');\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quizId).append("\">");

            int number = 1;
            for (Question question : questions) {
                html.append("<div class=\"question-block\">")
                        .append("<h3><span>Question ").append(number++).append(" of ").append(questions.size()).append("</span>")
                        .append("<span class=\"badge badge-approved\">").append(question.getMarks()).append(" Mark(s)</span></h3>")
                        .append("<p style=\"font-size: 15px; margin-bottom: 14px;\">").append(WebPages.escape(question.getText())).append("</p>")
                        .append("<div class=\"options-group\">");

                for (Option option : question.getOptions()) {
                    html.append("<label class=\"option-card\">")
                            .append("<input type=\"radio\" name=\"q_").append(question.getId())
                            .append("\" value=\"").append(option.getId()).append("\"> ")
                            .append("<span><b>(").append(option.getLetter()).append(")</b> &nbsp; ")
                            .append(WebPages.escape(option.getOptionText()))
                            .append("</span></label>");
                }
                html.append("</div></div>");
            }

            html.append("<div style=\"margin-top: 24px; display: flex; gap: 12px;\">")
                    .append("<button type=\"submit\">✔ Submit Quiz Evaluation</button>")
                    .append("<a class=\"button secondary\" href=\"").append(context).append("/quiz\">Cancel & Return</a>")
                    .append("</div>")
                    .append("</form></section>")
                    .append("<script>")
                    .append("var timeLeft = ").append(totalSeconds).append(";")
                    .append("var timerElem = document.getElementById('timerDisplay');")
                    .append("var formElem = document.getElementById('quizForm');")
                    .append("var timerInt = setInterval(function() {")
                    .append("  timeLeft--;")
                    .append("  if (timeLeft <= 0) {")
                    .append("    clearInterval(timerInt);")
                    .append("    alert('Time has expired! Submitting your answers automatically now.');")
                    .append("    formElem.submit();")
                    .append("    return;")
                    .append("  }")
                    .append("  var m = Math.floor(timeLeft / 60);")
                    .append("  var s = timeLeft % 60;")
                    .append("  timerElem.textContent = (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;")
                    .append("  if (timeLeft < 60) { timerElem.style.color = '#ef4444'; }")
                    .append("}, 1000);")
                    .append("</script>");

            out.print(WebPages.page(request, "Attempt Quiz: " + quiz.getTitle(), html.toString()));
        } catch (DatabaseException e) {
            out.print(WebPages.page(request, "Error", WebPages.alert("error", e.getMessage())));
        }
    }

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

            List<Answer> answers = new ArrayList<>();
            for (Question question : questions) {
                String value = request.getParameter("q_" + question.getId());
                long selected = -1;
                if (value != null && !value.isBlank()) {
                    try {
                        selected = Long.parseLong(value);
                    } catch (NumberFormatException parseError) {
                        selected = -1;
                    }
                }
                answers.add(new Answer(question.getId(), selected));
            }

            Object startAttribute = session.getAttribute("attemptStart_" + quizId);
            long elapsedSeconds = 0;
            if (startAttribute instanceof Long start) {
                elapsedSeconds = (System.currentTimeMillis() - start) / 1000;
            }
            long allowed = quiz.getDurationMinutes() * 60L;
            boolean timedOut = elapsedSeconds > allowed;
            if (timedOut) {
                elapsedSeconds = allowed;
            }
            session.removeAttribute("attemptStart_" + quizId);

            Result result = resultService.submitAttempt(quiz, questions, userId, userName,
                    answers, elapsedSeconds, timedOut);

            renderResult(request, out, context, result, questions, answers);
        } catch (TimeExpiredException | InvalidQuizException | DatabaseException e) {
            out.print(WebPages.page(request, "Submission failed",
                    WebPages.alert("error", e.getMessage())
                            + "<a class=\"button\" href=\"" + context + "/quiz\">Back to quizzes</a>"));
        }
    }

    private void renderResult(HttpServletRequest request, PrintWriter out, String context, Result result,
                              List<Question> questions, List<Answer> answers) {
        Map<Integer, Long> chosenByQuestion = new HashMap<>();
        for (Answer answer : answers) {
            chosenByQuestion.put(answer.getQuestionId(), answer.getSelectedOptionId());
        }

        StringBuilder rows = new StringBuilder();
        int number = 1;
        for (Question question : questions) {
            long chosen = chosenByQuestion.getOrDefault(question.getId(), -1L);
            String yourAnswer = "Not Answered";
            boolean correct = false;
            boolean attempted = false;
            for (Option option : question.getOptions()) {
                if (option.getId() == chosen) {
                    yourAnswer = "(" + option.getLetter() + ") " + option.getOptionText();
                    correct = option.isCorrect();
                    attempted = true;
                }
            }
            Option correctOption = question.getCorrectOption();
            String correctAnswer = correctOption == null ? "-"
                    : "(" + correctOption.getLetter() + ") " + correctOption.getOptionText();
            String outcomeBadge = !attempted ? "<span class=\"badge badge-pending\">SKIPPED</span>"
                    : (correct ? "<span class=\"badge badge-approved\">CORRECT</span>" : "<span class=\"badge badge-rejected\">WRONG</span>");

            rows.append("<tr><td>").append(number++).append("</td>")
                    .append("<td>").append(WebPages.escape(question.getText())).append("</td>")
                    .append("<td>").append(WebPages.escape(yourAnswer)).append("</td>")
                    .append("<td>").append(WebPages.escape(correctAnswer)).append("</td>")
                    .append("<td>").append(outcomeBadge).append("</td></tr>");
        }

        boolean passed = result.getPercentage() >= 60.0;
        String passBadge = passed ? "<span class=\"badge badge-approved\">PASSED</span>" : "<span class=\"badge badge-rejected\">NEEDS IMPROVEMENT</span>";

        String body = "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Assessment Result: " + WebPages.escape(result.getQuizTitle()) + "</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Submitted on " + result.getSubmittedAt() + " " + passBadge + "</p>"
                + "  </div>"
                + "</div>"
                + "<div class=\"stats\">"
                + "  <div class=\"stat success\"><p class=\"stat-label\">Score Obtained</p><span class=\"stat-num\">" + result.getObtainedMarks() + "/" + result.getTotalMarks() + "</span></div>"
                + "  <div class=\"stat " + (passed ? "success" : "warning") + "\"><p class=\"stat-label\">Percentage</p><span class=\"stat-num\">" + result.getPercentage() + "%</span></div>"
                + "  <div class=\"stat\"><p class=\"stat-label\">Correct / Total</p><span class=\"stat-num\">" + result.getCorrectAnswers() + "/" + result.getTotalQuestions() + "</span></div>"
                + "  <div class=\"stat\"><p class=\"stat-label\">Time Taken</p><span class=\"stat-num\" style=\"font-size:22px;\">" + result.getFormattedTimeTaken() + "</span></div>"
                + "</div>"
                + (result.getFeedback() != null && !result.getFeedback().isBlank()
                ? WebPages.alert("success", "Evaluation: " + result.getFeedback()) : "")
                + "<h3>Detailed Question Review</h3>"
                + "<table><thead><tr><th>#</th><th>Question</th><th>Your Answer</th>"
                + "<th>Correct Answer</th><th>Outcome</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "<div style=\"margin-top: 24px; display: flex; gap: 12px;\">"
                + "  <a class=\"button\" href=\"" + context + "/quiz\">Available Quizzes</a>"
                + "  <a class=\"button secondary\" href=\"" + context + "/result\">View All My Results</a>"
                + "</div>"
                + "</section>";
        out.print(WebPages.page(request, "Quiz Result", body));
    }
}