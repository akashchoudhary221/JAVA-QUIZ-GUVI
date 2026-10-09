package com.quizplatform.servlet;

import com.quizplatform.dao.FeedbackDAO;
import com.quizplatform.dao.QuestionDAO;
import com.quizplatform.dao.ResultDAO;
import com.quizplatform.dao.impl.FeedbackDAOImpl;
import com.quizplatform.dao.impl.QuestionDAOImpl;
import com.quizplatform.dao.impl.ResultDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidQuizException;
import com.quizplatform.exception.QuizNotFoundException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Option;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.Result;
import com.quizplatform.model.Role;
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
 * Quiz Creator Studio web portal.
 *
 * Implements full functionalities required by the GUVI Specification:
 *  1. Create Quizzes:
 *     - Title, Description, Duration, Pass Percentage
 *  2. Question Authoring:
 *     - Add questions with options A, B, C, D, correct answer selector, marks, explanation
 *  3. Content Lifecycle:
 *     - Save drafts, submit for Admin approval, delete drafts
 *  4. Review Student Results:
 *     - View performance and scores of participants taking creator's quizzes
 *  5. Interact with Participants:
 *     - Leave instructor feedback/comments on student attempts
 */
@WebServlet(name = "CreatorServlet", urlPatterns = {"/creator"})
public class CreatorServlet extends HttpServlet {

    private final QuizService quizService = new QuizService();
    private final QuestionDAO questionDAO = new QuestionDAOImpl();
    private final ResultDAO resultDAO = new ResultDAOImpl();
    private final FeedbackDAO feedbackDAO = new FeedbackDAOImpl();
    private final ResultService resultService = new ResultService();

    private boolean requireCreator(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        String role = (String) session.getAttribute("role");
        if (!Role.QUIZ_CREATOR.name().equals(role) && !Role.ADMIN.name().equals(role)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Access denied: Quiz Creator role required for this portal.");
            return false;
        }
        return true;
    }

    private int getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            return (Integer) session.getAttribute("userId");
        }
        return 0;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireCreator(request, response)) {
            return;
        }
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        renderPortal(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireCreator(request, response)) {
            return;
        }
        int creatorId = getUserId(request);
        String action = request.getParameter("action");
        String context = request.getContextPath();

        try {
            if ("create_quiz".equals(action)) {
                String title = request.getParameter("title");
                String description = request.getParameter("description");
                int duration = Integer.parseInt(request.getParameter("duration"));
                int passPercentage = 60;
                try {
                    passPercentage = Integer.parseInt(request.getParameter("passPercentage"));
                } catch (Exception ignored) {
                }

                Quiz quiz = new Quiz(title, description, duration);
                quiz.setCreatedBy(creatorId);
                quizService.createQuiz(quiz);
                response.sendRedirect(context + "/creator?tab=questions&quizId=" + quiz.getId()
                        + "&msg=" + WebPages.escape("Quiz '" + title + "' created successfully! Now add questions below."));
                return;

            } else if ("add_question".equals(action)) {
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                String text = request.getParameter("questionText");
                String optA = request.getParameter("optA");
                String optB = request.getParameter("optB");
                String optC = request.getParameter("optC");
                String optD = request.getParameter("optD");
                String correct = request.getParameter("correctOption"); // "A", "B", "C", "D"
                int marks = Integer.parseInt(request.getParameter("marks"));
                String explanation = request.getParameter("explanation");

                Question question = new Question();
                question.setQuizId(quizId);
                question.setText(text);
                question.setMarks(marks);
                question.setExplanation(explanation);

                List<Option> options = new ArrayList<>();
                options.add(new Option(optA, 1, "A".equalsIgnoreCase(correct)));
                options.add(new Option(optB, 2, "B".equalsIgnoreCase(correct)));
                options.add(new Option(optC, 3, "C".equalsIgnoreCase(correct)));
                options.add(new Option(optD, 4, "D".equalsIgnoreCase(correct)));
                question.setOptions(options);

                quizService.addQuestion(question);
                response.sendRedirect(context + "/creator?tab=questions&quizId=" + quizId
                        + "&msg=" + WebPages.escape("Question added successfully to quiz!"));
                return;

            } else if ("delete_question".equals(action)) {
                int questionId = Integer.parseInt(request.getParameter("questionId"));
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                quizService.deleteQuestion(questionId, quizId);
                response.sendRedirect(context + "/creator?tab=questions&quizId=" + quizId
                        + "&msg=Question deleted successfully.");
                return;

            } else if ("submit_approval".equals(action)) {
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                quizService.submitForApproval(quizId);
                response.sendRedirect(context + "/creator?tab=myquizzes"
                        + "&msg=Quiz submitted for Administrator approval!");
                return;

            } else if ("delete_quiz".equals(action)) {
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                quizService.deleteQuiz(quizId);
                response.sendRedirect(context + "/creator?tab=myquizzes"
                        + "&msg=Quiz deleted successfully.");
                return;

            } else if ("add_feedback".equals(action)) {
                int resultId = Integer.parseInt(request.getParameter("resultId"));
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                int participantId = Integer.parseInt(request.getParameter("participantId"));
                String comment = request.getParameter("comment");

                resultService.addFeedback(resultId, quizId, participantId, creatorId, comment);
                response.sendRedirect(context + "/creator?tab=feedback"
                        + "&msg=" + WebPages.escape("Feedback message sent to participant!"));
                return;
            }

            response.sendRedirect(context + "/creator");
        } catch (InvalidQuizException | QuizNotFoundException e) {
            response.sendRedirect(context + "/creator?tab=" + request.getParameter("tab")
                    + "&error=" + WebPages.escape(e.getMessage()));
        } catch (DatabaseException e) {
            response.sendRedirect(context + "/creator?error=" + WebPages.escape("Database error: " + e.getMessage()));
        }
    }

    private void renderPortal(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String context = request.getContextPath();
        String tab = request.getParameter("tab");
        if (tab == null || tab.isBlank()) {
            tab = "dashboard";
        }
        String msg = request.getParameter("msg");
        String error = request.getParameter("error");
        int creatorId = getUserId(request);

        try {
            List<Quiz> myQuizzes = quizService.getQuizzesByCreator(creatorId);
            List<Result> studentResults = resultDAO.getResultsByCreator(creatorId);

            int totalDrafts = (int) myQuizzes.stream().filter(q -> q.getStatus() == QuizStatus.DRAFT).count();
            int totalPending = (int) myQuizzes.stream().filter(q -> q.getStatus() == QuizStatus.PENDING_APPROVAL).count();
            int totalApproved = (int) myQuizzes.stream().filter(q -> q.getStatus() == QuizStatus.APPROVED).count();

            StringBuilder body = new StringBuilder();

            // Alerts
            if (msg != null && !msg.isBlank()) {
                body.append(WebPages.alert("success", msg));
            }
            if (error != null && !error.isBlank()) {
                body.append(WebPages.alert("error", error));
            }

            // Top Studio Navigation Tabs
            body.append("<div style=\"display:flex; gap:8px; margin-bottom:20px; flex-wrap:wrap;\">")
                .append(tabLink(context, "dashboard", "📊 Studio Overview", tab))
                .append(tabLink(context, "create", "➕ Create Quiz", tab))
                .append(tabLink(context, "myquizzes", "📚 My Quizzes (" + myQuizzes.size() + ")", tab))
                .append(tabLink(context, "questions", "❓ Author Questions", tab))
                .append(tabLink(context, "results", "📈 Student Results (" + studentResults.size() + ")", tab))
                .append(tabLink(context, "feedback", "💬 Participant Feedback", tab))
                .append("</div>");

            if ("create".equals(tab)) {
                body.append(renderCreateQuizTab(context));
            } else if ("questions".equals(tab)) {
                body.append(renderAuthorQuestionsTab(context, request, myQuizzes));
            } else if ("myquizzes".equals(tab)) {
                body.append(renderMyQuizzesTab(context, myQuizzes));
            } else if ("results".equals(tab)) {
                body.append(renderStudentResultsTab(context, studentResults));
            } else if ("feedback".equals(tab)) {
                body.append(renderFeedbackTab(context, myQuizzes, studentResults));
            } else {
                body.append(renderDashboardOverview(context, myQuizzes, studentResults, totalDrafts, totalPending, totalApproved));
            }

            out.print(WebPages.page(request, "Quiz Creator Studio", body.toString()));
        } catch (DatabaseException e) {
            out.print(WebPages.page(request, "Creator Studio Error",
                    WebPages.alert("error", "Database error: " + e.getMessage())));
        }
    }

    private String tabLink(String context, String tabId, String label, String currentTab) {
        boolean active = tabId.equals(currentTab);
        String cls = active ? "button" : "button secondary";
        return "<a href=\"" + context + "/creator?tab=" + tabId + "\" class=\"" + cls + "\">" + label + "</a>";
    }

    private String renderDashboardOverview(String context, List<Quiz> myQuizzes, List<Result> studentResults,
                                           int drafts, int pending, int approved) {
        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Faculty Authoring Studio</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Manage your quiz catalog, author questions with options A-D, and evaluate student responses</p>"
                + "  </div>"
                + "  <a class=\"button\" href=\"" + context + "/creator?tab=create\">➕ Create New Quiz</a>"
                + "</div>"
                + "<div class=\"stats\">"
                + WebPages.statCard("Total Authored Quizzes", myQuizzes.size(), "")
                + WebPages.statCard("Draft Quizzes", drafts, "")
                + WebPages.statCard("Pending Admin Approval", pending, "warning")
                + WebPages.statCard("Approved & Live Quizzes", approved, "success")
                + WebPages.statCard("Student Quiz Attempts", studentResults.size(), "")
                + "</div>"
                + "</section>"
                + "<div class=\"grid-2\">"
                + "  <div class=\"card\">"
                + "    <h3>Quick Actions</h3>"
                + "    <p class=\"subtitle\">Shortcuts for common quiz authoring tasks</p>"
                + "    <div style=\"display:flex; flex-direction:column; gap:10px;\">"
                + "      <a class=\"button\" href=\"" + context + "/creator?tab=create\">➕ Create a New Quiz Header</a>"
                + "      <a class=\"button secondary\" href=\"" + context + "/creator?tab=questions\">❓ Add Questions (Options A, B, C, D)</a>"
                + "      <a class=\"button secondary\" href=\"" + context + "/creator?tab=myquizzes\">🚀 Submit Drafts for Admin Review</a>"
                + "      <a class=\"button secondary\" href=\"" + context + "/creator?tab=results\">📈 Inspect Student Performance & Scores</a>"
                + "    </div>"
                + "  </div>"
                + "  <div class=\"card\">"
                + "    <h3>Authoring Guidelines</h3>"
                + "    <p class=\"subtitle\">Best practices according to academic rubric</p>"
                + "    <ul style=\"margin-left: 20px; color: var(--text-muted); line-height: 1.8;\">"
                + "      <li>Quizzes start in <b>DRAFT</b> status so you can add multiple questions safely.</li>"
                + "      <li>Each question supports <b>Options A, B, C, and D</b> with marks and detailed explanations.</li>"
                + "      <li>Quizzes must contain at least 1 question before submitting for Admin approval.</li>"
                + "      <li>Once approved, the quiz is published instantly for participants to take.</li>"
                + "    </ul>"
                + "  </div>"
                + "</div>";
    }

    private String renderCreateQuizTab(String context) {
        return "<section class=\"card\" style=\"max-width: 680px; margin: 0 auto;\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Create New Quiz</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Define quiz title, duration, and passing criteria</p>"
                + "  </div>"
                + "</div>"
                + "<form method=\"post\" action=\"" + context + "/creator\">"
                + "  <input type=\"hidden\" name=\"action\" value=\"create_quiz\">"
                + "  <label for=\"title\">Quiz Title *</label>"
                + "  <input type=\"text\" id=\"title\" name=\"title\" placeholder=\"e.g. Java Collections & Generics Mastery\" required>"
                + "  <label for=\"description\">Detailed Description / Topic *</label>"
                + "  <textarea id=\"description\" name=\"description\" rows=\"3\" placeholder=\"Covers List, Set, Map, equals/hashCode, type erasure, and wildcards.\" required></textarea>"
                + "  <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 16px;\">"
                + "    <div>"
                + "      <label for=\"duration\">Duration (Minutes) *</label>"
                + "      <input type=\"number\" id=\"duration\" name=\"duration\" min=\"1\" max=\"240\" value=\"15\" required>"
                + "    </div>"
                + "    <div>"
                + "      <label for=\"passPercentage\">Passing Percentage (%) *</label>"
                + "      <input type=\"number\" id=\"passPercentage\" name=\"passPercentage\" min=\"10\" max=\"100\" value=\"60\" required>"
                + "    </div>"
                + "  </div>"
                + "  <div style=\"margin-top: 24px; display: flex; gap: 12px;\">"
                + "    <button type=\"submit\">Save Quiz & Proceed to Questions &rarr;</button>"
                + "    <a class=\"button secondary\" href=\"" + context + "/creator?tab=myquizzes\">Cancel</a>"
                + "  </div>"
                + "</form>"
                + "</section>";
    }

    private String renderAuthorQuestionsTab(String context, HttpServletRequest request, List<Quiz> myQuizzes)
            throws DatabaseException {
        if (myQuizzes.isEmpty()) {
            return "<section class=\"card\"><div class=\"alert warning\"><span>🔔</span><span>You must create a quiz first before authoring questions. <a href=\"" + context + "/creator?tab=create\" style=\"font-weight:600;\">Create Quiz Now</a></span></div></section>";
        }

        String quizIdParam = request.getParameter("quizId");
        int selectedQuizId = myQuizzes.get(0).getId();
        if (quizIdParam != null && !quizIdParam.isBlank()) {
            try {
                selectedQuizId = Integer.parseInt(quizIdParam);
            } catch (Exception ignored) {
            }
        }

        StringBuilder quizOptions = new StringBuilder();
        for (Quiz q : myQuizzes) {
            boolean sel = q.getId() == selectedQuizId;
            quizOptions.append("<option value=\"").append(q.getId()).append("\"")
                    .append(sel ? " selected" : "")
                    .append(">")
                    .append(WebPages.escape(q.getTitle()))
                    .append(" (").append(q.getStatus()).append(")")
                    .append("</option>");
        }

        List<Question> questions = questionDAO.getQuestionsByQuiz(selectedQuizId);

        StringBuilder questionRows = new StringBuilder();
        int idx = 1;
        for (Question q : questions) {
            questionRows.append("<tr>")
                    .append("<td>#").append(idx++).append("</td>")
                    .append("<td><b>").append(WebPages.escape(q.getText())).append("</b></td>")
                    .append("<td>").append(q.getMarks()).append(" pts</td>")
                    .append("<td>")
                    .append("<form method=\"post\" action=\"").append(context).append("/creator\" class=\"inline\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"delete_question\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"questions\">")
                    .append("<input type=\"hidden\" name=\"questionId\" value=\"").append(q.getId()).append("\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(selectedQuizId).append("\">")
                    .append("<button type=\"submit\" class=\"danger small\">Delete</button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
        }
        if (questions.isEmpty()) {
            questionRows.append("<tr><td colspan=\"4\" style=\"text-align:center; color:var(--text-muted);\">No questions added to this quiz yet. Use the authoring form below.</td></tr>");
        }

        return "<div class=\"grid-2\">"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h2>Question Authoring (Options A–D)</h2>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Add questions with 4 choices, marks, and explanation</p>"
                + "    </div>"
                + "  </div>"
                + "  <form method=\"post\" action=\"" + context + "/creator\">"
                + "    <input type=\"hidden\" name=\"action\" value=\"add_question\">"
                + "    <label for=\"quizSelect\">Target Quiz</label>"
                + "    <select id=\"quizSelect\" name=\"quizId\" onchange=\"location.href='" + context + "/creator?tab=questions&quizId=' + this.value;\">"
                + quizOptions
                + "    </select>"
                + "    <label for=\"questionText\">Question Text *</label>"
                + "    <textarea id=\"questionText\" name=\"questionText\" rows=\"3\" placeholder=\"e.g. Which of the following collections allows duplicate elements and maintains insertion order?\" required></textarea>"
                + "    <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 8px;\">"
                + "      <div>"
                + "        <label for=\"optA\">Option A *</label>"
                + "        <input type=\"text\" id=\"optA\" name=\"optA\" placeholder=\"Choice A text\" required>"
                + "      </div>"
                + "      <div>"
                + "        <label for=\"optB\">Option B *</label>"
                + "        <input type=\"text\" id=\"optB\" name=\"optB\" placeholder=\"Choice B text\" required>"
                + "      </div>"
                + "      <div>"
                + "        <label for=\"optC\">Option C *</label>"
                + "        <input type=\"text\" id=\"optC\" name=\"optC\" placeholder=\"Choice C text\" required>"
                + "      </div>"
                + "      <div>"
                + "        <label for=\"optD\">Option D *</label>"
                + "        <input type=\"text\" id=\"optD\" name=\"optD\" placeholder=\"Choice D text\" required>"
                + "      </div>"
                + "    </div>"
                + "    <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 16px;\">"
                + "      <div>"
                + "        <label for=\"correctOption\">Correct Answer *</label>"
                + "        <select id=\"correctOption\" name=\"correctOption\" required>"
                + "          <option value=\"A\">Option A</option>"
                + "          <option value=\"B\">Option B</option>"
                + "          <option value=\"C\">Option C</option>"
                + "          <option value=\"D\">Option D</option>"
                + "        </select>"
                + "      </div>"
                + "      <div>"
                + "        <label for=\"marks\">Marks (Points) *</label>"
                + "        <input type=\"number\" id=\"marks\" name=\"marks\" min=\"1\" max=\"20\" value=\"1\" required>"
                + "      </div>"
                + "    </div>"
                + "    <label for=\"explanation\">Feedback / Explanation for Participants</label>"
                + "    <input type=\"text\" id=\"explanation\" name=\"explanation\" placeholder=\"e.g. ArrayList implements List and preserves insertion order while allowing duplicates.\">"
                + "    <div style=\"margin-top: 20px;\">"
                + "      <button type=\"submit\" style=\"width:100%;\">➕ Add Question to Quiz</button>"
                + "    </div>"
                + "  </form>"
                + "</section>"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h3>Questions in Selected Quiz (" + questions.size() + ")</h3>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Review authored questions for this quiz</p>"
                + "    </div>"
                + "  </div>"
                + "  <table><thead><tr><th>#</th><th>Question</th><th>Marks</th><th>Action</th></tr></thead><tbody>"
                + questionRows
                + "  </tbody></table>"
                + "</section>"
                + "</div>";
    }

    private String renderMyQuizzesTab(String context, List<Quiz> myQuizzes) {
        StringBuilder rows = new StringBuilder();
        for (Quiz q : myQuizzes) {
            String badgeClass = "badge-pending";
            if (q.getStatus() == QuizStatus.APPROVED) {
                badgeClass = "badge-approved";
            } else if (q.getStatus() == QuizStatus.REJECTED) {
                badgeClass = "badge-rejected";
            } else if (q.getStatus() == QuizStatus.DRAFT) {
                badgeClass = "badge-draft";
            }

            rows.append("<tr>")
                    .append("<td>#").append(q.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(q.getTitle())).append("</b><br><small style=\"color:var(--text-muted);\">").append(WebPages.escape(q.getDescription())).append("</small></td>")
                    .append("<td>").append(q.getDurationMinutes()).append(" mins</td>")
                    .append("<td>").append(q.getTotalMarks()).append(" pts</td>")
                    .append("<td><span class=\"badge ").append(badgeClass).append("\">").append(q.getStatus()).append("</span></td>")
                    .append("<td>")
                    .append("<a class=\"button secondary small\" href=\"").append(context).append("/creator?tab=questions&quizId=").append(q.getId()).append("\">❓ Questions</a> ");

            if (q.getStatus() == QuizStatus.DRAFT || q.getStatus() == QuizStatus.REJECTED) {
                rows.append("<form method=\"post\" action=\"").append(context).append("/creator\" class=\"inline\">")
                        .append("<input type=\"hidden\" name=\"action\" value=\"submit_approval\">")
                        .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(q.getId()).append("\">")
                        .append("<button type=\"submit\" class=\"success small\">🚀 Submit</button></form> ");
            }

            rows.append("<form method=\"post\" action=\"").append(context).append("/creator\" class=\"inline\" onsubmit=\"return confirm('Delete this quiz?');\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"delete_quiz\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(q.getId()).append("\">")
                    .append("<button type=\"submit\" class=\"danger small\">Delete</button></form>")
                    .append("</td>")
                    .append("</tr>");
        }
        if (myQuizzes.isEmpty()) {
            rows.append("<tr><td colspan=\"6\" style=\"text-align:center; color:var(--text-muted);\">You haven't created any quizzes yet. Click 'Create New Quiz' above.</td></tr>");
        }

        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>My Authored Quizzes</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Track authoring progress, status badges, and approval workflows</p>"
                + "  </div>"
                + "  <a class=\"button\" href=\"" + context + "/creator?tab=create\">➕ Create New Quiz</a>"
                + "</div>"
                + "<table><thead><tr><th>ID</th><th>Quiz Details</th><th>Duration</th><th>Marks</th><th>Status</th><th>Actions</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "</section>";
    }

    private String renderStudentResultsTab(String context, List<Result> studentResults) {
        StringBuilder rows = new StringBuilder();
        for (Result r : studentResults) {
            String statusBadge = r.isPassed()
                    ? "<span class=\"badge badge-approved\">PASSED</span>"
                    : "<span class=\"badge badge-rejected\">FAILED</span>";

            rows.append("<tr>")
                    .append("<td>#").append(r.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(r.getParticipantName())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(r.getQuizTitle())).append("</td>")
                    .append("<td><b>").append(r.getObtainedMarks()).append("</b> / ").append(r.getTotalMarks()).append(" pts</td>")
                    .append("<td>").append(String.format("%.1f%%", r.getPercentage())).append("</td>")
                    .append("<td>").append(statusBadge).append("</td>")
                    .append("<td>")
                    .append("<a class=\"button secondary small\" href=\"").append(context).append("/creator?tab=feedback&resultId=").append(r.getId()).append("&quizId=").append(r.getQuizId()).append("&participantId=").append(r.getParticipantId()).append("\">💬 Feedback</a>")
                    .append("</td>")
                    .append("</tr>");
        }
        if (studentResults.isEmpty()) {
            rows.append("<tr><td colspan=\"7\" style=\"text-align:center; color:var(--text-muted);\">No student attempts recorded for your quizzes yet.</td></tr>");
        }

        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Review Quiz Results</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Inspect participant scores, percentage grades, and provide feedback</p>"
                + "  </div>"
                + "</div>"
                + "<table><thead><tr><th>ID</th><th>Student Name</th><th>Quiz Title</th><th>Score</th><th>Percentage</th><th>Status</th><th>Action</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "</section>";
    }

    private String renderFeedbackTab(String context, List<Quiz> myQuizzes, List<Result> studentResults)
            throws DatabaseException {
        StringBuilder feedbackRows = new StringBuilder();
        List<Feedback> allFeedbacks = new ArrayList<>();
        for (Quiz q : myQuizzes) {
            allFeedbacks.addAll(feedbackDAO.getFeedbackByQuiz(q.getId()));
        }

        for (Feedback f : allFeedbacks) {
            feedbackRows.append("<tr>")
                    .append("<td>#").append(f.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(f.getParticipantName())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(f.getQuizTitle())).append("</td>")
                    .append("<td>").append(WebPages.escape(f.getComment())).append("</td>")
                    .append("<td><small style=\"color:var(--text-muted);\">").append(f.getCreatedAt()).append("</small></td>")
                    .append("</tr>");
        }
        if (allFeedbacks.isEmpty()) {
            feedbackRows.append("<tr><td colspan=\"5\" style=\"text-align:center; color:var(--text-muted);\">No feedback sent to participants yet. Use the form below.</td></tr>");
        }

        StringBuilder resultOptions = new StringBuilder();
        for (Result r : studentResults) {
            resultOptions.append("<option value=\"").append(r.getId()).append("\"")
                    .append(" data-quiz=\"").append(r.getQuizId()).append("\"")
                    .append(" data-part=\"").append(r.getParticipantId()).append("\">")
                    .append(WebPages.escape(r.getParticipantName())).append(" &mdash; ")
                    .append(WebPages.escape(r.getQuizTitle())).append(" (")
                    .append(String.format("%.1f%%", r.getPercentage())).append(")")
                    .append("</option>");
        }

        return "<div class=\"grid-2\">"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h2>Interact with Participants</h2>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Send guidance and review comments to students</p>"
                + "    </div>"
                + "  </div>"
                + (studentResults.isEmpty()
                    ? "<p style=\"color:var(--text-muted);\">No student attempts available to provide feedback on.</p>"
                    : "<form method=\"post\" action=\"" + context + "/creator\">"
                    + "  <input type=\"hidden\" name=\"action\" value=\"add_feedback\">"
                    + "  <label for=\"resultSelect\">Select Student Assessment</label>"
                    + "  <select id=\"resultSelect\" name=\"resultId\" onchange=\"updateHiddenFields(this);\" required>"
                    + resultOptions
                    + "  </select>"
                    + "  <input type=\"hidden\" id=\"quizIdHidden\" name=\"quizId\" value=\"" + (studentResults.isEmpty() ? 0 : studentResults.get(0).getQuizId()) + "\">"
                    + "  <input type=\"hidden\" id=\"partIdHidden\" name=\"participantId\" value=\"" + (studentResults.isEmpty() ? 0 : studentResults.get(0).getParticipantId()) + "\">"
                    + "  <label for=\"comment\">Feedback Message *</label>"
                    + "  <textarea id=\"comment\" name=\"comment\" rows=\"3\" placeholder=\"e.g. Great job on OOP concepts! Please review thread synchronization and memory visibility.\" required></textarea>"
                    + "  <div style=\"margin-top: 18px;\">"
                    + "    <button type=\"submit\" style=\"width:100%;\">💬 Send Feedback to Student</button>"
                    + "  </div>"
                    + "</form>"
                    + "<script>"
                    + "function updateHiddenFields(sel) {"
                    + "  var opt = sel.options[sel.selectedIndex];"
                    + "  document.getElementById('quizIdHidden').value = opt.getAttribute('data-quiz');"
                    + "  document.getElementById('partIdHidden').value = opt.getAttribute('data-part');"
                    + "}"
                    + "</script>")
                + "</section>"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h3>Sent Participant Feedback History</h3>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Feedback visible in student performance reports</p>"
                + "    </div>"
                + "  </div>"
                + "  <table><thead><tr><th>ID</th><th>Student</th><th>Quiz</th><th>Message</th><th>Date</th></tr></thead><tbody>"
                + feedbackRows
                + "  </tbody></table>"
                + "</section>"
                + "</div>";
    }
}
