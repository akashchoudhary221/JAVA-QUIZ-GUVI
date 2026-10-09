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
                renderDetail(out, request.getContextPath(), Integer.parseInt(idParameter), userId);
                return;
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid result id");
                return;
            }
        }

        // ---- history table ----
        StringBuilder rows = new StringBuilder();
        try {
            List<Result> results = resultService.getHistory(userId);
            if (results.isEmpty()) {
                rows.append("<tr><td colspan=\"6\">You have not attempted any quiz yet.</td></tr>");
            }
            for (Result result : results) {
                rows.append("<tr>")
                        .append("<td>").append(result.getId()).append("</td>")
                        .append("<td>").append(WebPages.escape(result.getQuizTitle())).append("</td>")
                        .append("<td>").append(result.getSubmittedAt() == null ? "-"
                                : DATE_FORMAT.format(result.getSubmittedAt())).append("</td>")
                        .append("<td>").append(result.getObtainedMarks()).append("/")
                        .append(result.getTotalMarks()).append("</td>")
                        .append("<td>").append(result.getPercentage()).append("%</td>")
                        .append("<td><a class=\"button\" href=\"")
                        .append(request.getContextPath()).append("/result?id=")
                        .append(result.getId()).append("\">View</a></td>")
                        .append("</tr>");
            }
        } catch (DatabaseException e) {
            rows.append("<tr><td colspan=\"6\">")
                    .append(WebPages.escape(e.getMessage())).append("</td></tr>");
        }

        String body = "<section class=\"card\">"
                + "<h2>My Performance History</h2>"
                + "<table><thead><tr><th>ID</th><th>Quiz</th><th>Date</th><th>Score</th>"
                + "<th>Percentage</th><th>Report</th></tr></thead><tbody>"
                + rows
                + "</tbody></table></section>";
        out.print(WebPages.page(request.getContextPath(), "My Results", body));
    }

    /** Full performance report of one result + creator feedback. */
    private void renderDetail(PrintWriter out, String context, int resultId, int userId)
            throws IOException {
        try {
            Result result = resultService.getAllResults().stream()
                    .filter(r -> r.getId() == resultId && r.getParticipantId() == userId)
                    .findFirst().orElse(null);
            if (result == null) {
                out.print(WebPages.page(context, "Not found",
                        WebPages.alert("error", "Result not found.")));
                return;
            }
            StringBuilder feedbackHtml = new StringBuilder();
            List<Feedback> feedbackList = resultService.getFeedbackForResult(result.getId());
            if (!feedbackList.isEmpty()) {
                feedbackHtml.append("<h3>Feedback</h3><ul>");
                for (Feedback feedback : feedbackList) {
                    feedbackHtml.append("<li>").append(WebPages.escape(feedback.getComment()))
                            .append(" <i>(by ").append(WebPages.escape(feedback.getCommentedByName()))
                            .append(")</i></li>");
                }
                feedbackHtml.append("</ul>");
            }
            String body = "<section class=\"card\">"
                    + "<h2>Performance Report</h2>"
                    + "<pre>" + WebPages.escape(resultService.generateReport(result)) + "</pre>"
                    + feedbackHtml
                    + "<a class=\"button\" href=\"" + context + "/result\">Back to history</a>"
                    + "</section>";
            out.print(WebPages.page(context, "Report", body));
        } catch (DatabaseException e) {
            out.print(WebPages.page(context, "Error",
                    WebPages.alert("error", e.getMessage())));
        }
    }
}
