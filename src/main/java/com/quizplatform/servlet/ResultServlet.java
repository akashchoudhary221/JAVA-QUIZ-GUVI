package com.quizplatform.servlet;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Feedback;
import com.quizplatform.model.Result;
import com.quizplatform.service.ResultService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Performance history of the logged-in participant.
 *   GET /result          -> table of all previous attempts
 *   GET /result?id=12    -> full report of one result (ScoreService.generateReport)
 */
@WebServlet(name = "ResultServlet", urlPatterns = {"/result"})
public class ResultServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final ResultService resultService = new ResultService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        int userId = (Integer) session.getAttribute("userId");
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String idParameter = request.getParameter("id");
        if (idParameter != null) {
            try {
                renderDetail(request, out, request.getContextPath(), Integer.parseInt(idParameter), userId);
                return;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid result id");
                return;
            }
        }

        // History Table
        StringBuilder rows = new StringBuilder();
        try {
            List<Result> results = resultService.getHistory(userId);
            if (results.isEmpty()) {
                rows.append("<tr><td colspan=\"6\" style=\"text-align:center; color:var(--text-muted);\">You have not attempted any quizzes yet.</td></tr>");
            }
            for (Result result : results) {
                boolean passed = result.getPercentage() >= 60.0;
                String badge = passed ? "<span class=\"badge badge-approved\">" + result.getPercentage() + "%</span>"
                        : "<span class=\"badge badge-rejected\">" + result.getPercentage() + "%</span>";

                rows.append("<tr>")
                        .append("<td>#").append(result.getId()).append("</td>")
                        .append("<td><b>").append(WebPages.escape(result.getQuizTitle())).append("</b></td>")
                        .append("<td>").append(result.getSubmittedAt() == null ? "-" : DATE_FORMAT.format(result.getSubmittedAt())).append("</td>")
                        .append("<td>").append(result.getObtainedMarks()).append(" / ").append(result.getTotalMarks()).append("</td>")
                        .append("<td>").append(badge).append("</td>")
                        .append("<td><a class=\"button secondary\" href=\"").append(request.getContextPath())
                        .append("/result?id=").append(result.getId()).append("\">🔍 View Report</a></td>")
                        .append("</tr>");
            }
        } catch (DatabaseException e) {
            rows.append("<tr><td colspan=\"6\">").append(WebPages.escape(e.getMessage())).append("</td></tr>");
        }

        String body = "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>My Assessment History</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Review previous evaluation scores, percentage analytics, and instructor feedback</p>"
                + "  </div>"
                + "  <a class=\"button\" href=\"" + request.getContextPath() + "/quiz\">Take a Quiz Now</a>"
                + "</div>"
                + "<table><thead><tr><th>ID</th><th>Quiz Title</th><th>Submission Date</th>"
                + "<th>Marks Obtained</th><th>Score %</th><th>Action</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "</section>";

        out.print(WebPages.page(request, "My Results", body));
    }

    private void renderDetail(HttpServletRequest request, PrintWriter out, String context, int resultId, int userId)
            throws IOException {
        try {
            Result result = resultService.getAllResults().stream()
                    .filter(r -> r.getId() == resultId && r.getParticipantId() == userId)
                    .findFirst().orElse(null);
            if (result == null) {
                out.print(WebPages.page(request, "Not Found",
                        WebPages.alert("error", "The requested assessment result could not be found.")));
                return;
            }

            StringBuilder feedbackHtml = new StringBuilder();
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                feedbackHtml.append("<div style=\"margin: 18px 0; padding: 14px 18px; background: #f8fafc; border: 1px solid var(--border); border-radius: var(--radius-sm);\">")
                        .append("<h4 style=\"margin-bottom: 8px;\">Faculty Comments & Personalized Guidance</h4>");
                for (Feedback feedback : feedbackList) {
                    feedbackHtml.append("<p style=\"margin: 4px 0;\">&bull; \"<b>").append(WebPages.escape(feedback.getComment()))
                            .append("</b>\" &mdash; <span style=\"color:var(--text-muted);\">by ")
                            .append(WebPages.escape(feedback.getCommentedByName())).append("</span></p>");
                }
                feedbackHtml.append("</div>");
            }

            String body = "<section class=\"card\">"
                    + "<div class=\"card-header\">"
                    + "  <div>"
                    + "    <h2>Performance Report: " + WebPages.escape(result.getQuizTitle()) + "</h2>"
                    + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Participant: <b>" + WebPages.escape(result.getParticipantName()) + "</b></p>"
                    + "  </div>"
                    + "  <a class=\"button secondary\" href=\"" + context + "/result\">&larr; Back to History</a>"
                    + "</div>"
                    + "<pre>" + WebPages.escape(resultService.generateReport(result)) + "</pre>"
                    + feedbackHtml
                    + "<div style=\"margin-top: 20px;\">"
                    + "  <a class=\"button\" href=\"" + context + "/quiz\">Available Quizzes</a>"
                    + "</div>"
                    + "</section>";

            out.print(WebPages.page(request, "Assessment Report #" + resultId, body));
        } catch (DatabaseException e) {
            out.print(WebPages.page(request, "Error", WebPages.alert("error", e.getMessage())));
        }
    }
}
