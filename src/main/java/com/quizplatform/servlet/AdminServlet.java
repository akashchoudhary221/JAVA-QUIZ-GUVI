package com.quizplatform.servlet;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.impl.AttemptDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.Role;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.UserService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Admin web console.
 *   GET  /admin                  -> statistics + pending quizzes (ADMIN only)
 *   POST /admin (action=approve) -> approve a quiz
 *   POST /admin (action=reject)  -> reject a quiz
 *   GET  /admin?action=logout    -> end the session
 *
 * Demonstrates ROLE BASED ACCESS on the web layer.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin"})
public class AdminServlet extends HttpServlet {

    private final UserService userService = new UserService();
    private final QuizService quizService = new QuizService();
    private final AttemptDAO attemptDAO = new AttemptDAOImpl();

    /** Returns true when the session belongs to an ADMIN (else sends 403). */
    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        if (!Role.ADMIN.name().equals(session.getAttribute("role"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Administrator role required for this page.");
            return false;
        }
        return true;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        String action = request.getParameter("action");
        if ("logout".equals(action)) {
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        renderDashboard(request, response);
    }

    /** Quiz approval actions arrive as POST requests. */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        String action = request.getParameter("action");
        String idParameter = request.getParameter("quizId");
        try {
            int quizId = Integer.parseInt(idParameter);
            if ("approve".equals(action)) {
                quizService.approveQuiz(quizId);
            } else if ("reject".equals(action)) {
                quizService.rejectQuiz(quizId);
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action");
                return;
            }
            response.sendRedirect(request.getContextPath() + "/admin?msg=Quiz updated");
        } catch (NumberFormatException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid quiz id");
        } catch (DatabaseException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /** Renders statistics + the pending-quiz approval table. */
    private void renderDashboard(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        String context = request.getContextPath();
        String message = request.getParameter("msg");

        try {
            int totalUsers = userService.getAllUsers().size();
            int totalQuizzes = quizService.countAllQuizzes();
            int pending = quizService.countQuizzesByStatus(QuizStatus.PENDING_APPROVAL.name());
            int approved = quizService.countQuizzesByStatus(QuizStatus.APPROVED.name());
            int attempts = attemptDAO.countAllAttempts();

            StringBuilder pendingRows = new StringBuilder();
            List<Quiz> quizzes = quizService.getQuizzesByStatus(QuizStatus.PENDING_APPROVAL.name());
            if (quizzes.isEmpty()) {
                pendingRows.append("<tr><td colspan=\"6\">No quizzes waiting for approval.</td></tr>");
            }
            for (Quiz quiz : quizzes) {
                pendingRows.append("<tr>")
                        .append("<td>").append(quiz.getId()).append("</td>")
                        .append("<td>").append(WebPages.escape(quiz.getTitle())).append("</td>")
                        .append("<td>").append(WebPages.escape(quiz.getCreatorName())).append("</td>")
                        .append("<td>").append(quiz.getDurationMinutes()).append(" min</td>")
                        .append("<td>")
                        .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                        .append("<input type=\"hidden\" name=\"action\" value=\"approve\">")
                        .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quiz.getId()).append("\">")
                        .append("<button type=\"submit\">Approve</button></form> ")
                        .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                        .append("<input type=\"hidden\" name=\"action\" value=\"reject\">")
                        .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quiz.getId()).append("\">")
                        .append("<button type=\"submit\" class=\"danger\">Reject</button></form>")
                        .append("</td>")
                        .append("</tr>");
            }

            String body = "<section class=\"card\">"
                    + "<h2>Admin Dashboard</h2>"
                    + (message != null ? WebPages.alert("success", message) : "")
                    + "<div class=\"stats\">"
                    + stat("Total Users", totalUsers)
                    + stat("Total Quizzes", totalQuizzes)
                    + stat("Pending Quizzes", pending)
                    + stat("Approved Quizzes", approved)
                    + stat("Total Attempts", attempts)
                    + "</div></section>"
                    + "<section class=\"card\">"
                    + "<h2>Quiz Approval</h2>"
                    + "<table><thead><tr><th>ID</th><th>Title</th><th>Creator</th>"
                    + "<th>Duration</th><th>Actions</th></tr></thead><tbody>"
                    + pendingRows
                    + "</tbody></table>"
                    + "</section>"
                    + "<section class=\"card\"><p class=\"muted\">Logged in as "
                    + WebPages.escape(String.valueOf(request.getSession().getAttribute("userName")))
                    + " | <a href=\"" + context + "/admin?action=logout\">Logout</a></p></section>";

            out.print(WebPages.page(context, "Admin", body));
        } catch (DatabaseException e) {
            out.print(WebPages.page(context, "Admin error",
                    WebPages.alert("error", "Database error: " + e.getMessage())));
        }
    }

    private String stat(String caption, int value) {
        return "<div class=\"stat\"><span>" + value + "</span><p>" + caption + "</p></div>";
    }
}