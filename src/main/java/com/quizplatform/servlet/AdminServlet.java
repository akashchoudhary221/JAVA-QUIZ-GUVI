package com.quizplatform.servlet;

import com.quizplatform.dao.AttemptDAO;
import com.quizplatform.dao.QuestionDAO;
import com.quizplatform.dao.ResultDAO;
import com.quizplatform.dao.impl.AttemptDAOImpl;
import com.quizplatform.dao.impl.QuestionDAOImpl;
import com.quizplatform.dao.impl.ResultDAOImpl;
import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidUserException;
import com.quizplatform.model.Admin;
import com.quizplatform.model.Participant;
import com.quizplatform.model.Question;
import com.quizplatform.model.Quiz;
import com.quizplatform.model.QuizCreator;
import com.quizplatform.model.QuizStatus;
import com.quizplatform.model.Result;
import com.quizplatform.model.Role;
import com.quizplatform.model.SystemSetting;
import com.quizplatform.model.User;
import com.quizplatform.service.QuizService;
import com.quizplatform.service.SystemSettingsService;
import com.quizplatform.service.UserService;

import javax.servlet.ServletException;
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
 * Administrator web management suite.
 *
 * Implements all functionalities required by the GUVI Specification:
 *  1. User Management (CRUD):
 *     - Input: User details (name, email, password, role)
 *     - Output: Confirmation message for user creation/update/deletion
 *     - Functionality: Manage user accounts and roles.
 *  2. Quiz Content Management:
 *     - Input: Quiz details
 *     - Output: Content approval status
 *     - Functionality: Approve or reject quiz content submitted by quiz creators.
 *  3. System Settings:
 *     - Input: Configuration settings (key, value)
 *     - Output: Confirmation message for successful settings update
 *     - Functionality: Manage system-wide settings.
 *  4. Platform Reports:
 *     - Platform analytics, metrics, and assessment evaluations.
 */
@WebServlet(name = "AdminServlet", urlPatterns = {"/admin"})
public class AdminServlet extends HttpServlet {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final UserService userService = new UserService();
    private final QuizService quizService = new QuizService();
    private final AttemptDAO attemptDAO = new AttemptDAOImpl();
    private final ResultDAO resultDAO = new ResultDAOImpl();
    private final QuestionDAO questionDAO = new QuestionDAOImpl();
    private final SystemSettingsService settingsService = new SystemSettingsService();

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

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!requireAdmin(request, response)) {
            return;
        }
        String action = request.getParameter("action");
        String context = request.getContextPath();

        try {
            if ("approve".equals(action)) {
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                quizService.approveQuiz(quizId);
                response.sendRedirect(context + "/admin?tab=approvals&msg="
                        + WebPages.escape("Quiz #" + quizId + " approved successfully and is now LIVE for students!"));
                return;

            } else if ("reject".equals(action)) {
                int quizId = Integer.parseInt(request.getParameter("quizId"));
                quizService.rejectQuiz(quizId);
                response.sendRedirect(context + "/admin?tab=approvals&msg="
                        + WebPages.escape("Quiz #" + quizId + " rejected. The creator has been notified to revise."));
                return;

            } else if ("add_user".equals(action)) {
                String name = request.getParameter("name");
                String email = request.getParameter("email");
                String password = request.getParameter("password");
                String roleStr = request.getParameter("role");
                Role role = Role.valueOf(roleStr);

                User user;
                if (role == Role.ADMIN) {
                    user = new Admin(0, name, email, password);
                } else if (role == Role.QUIZ_CREATOR) {
                    user = new QuizCreator(0, name, email, password);
                } else {
                    user = new Participant(0, name, email, password);
                }

                userService.createUser(user);
                response.sendRedirect(context + "/admin?tab=users&msg="
                        + WebPages.escape("User '" + name + "' (" + email + ") created successfully with role " + role + "!"));
                return;

            } else if ("change_role".equals(action)) {
                int targetUserId = Integer.parseInt(request.getParameter("userId"));
                String newRoleStr = request.getParameter("role");
                Role newRole = Role.valueOf(newRoleStr);

                User target = userService.getUserById(targetUserId);
                if (target != null) {
                    User updated;
                    if (newRole == Role.ADMIN) {
                        updated = new Admin(target.getId(), target.getName(), target.getEmail(), target.getPassword());
                    } else if (newRole == Role.QUIZ_CREATOR) {
                        updated = new QuizCreator(target.getId(), target.getName(), target.getEmail(), target.getPassword());
                    } else {
                        updated = new Participant(target.getId(), target.getName(), target.getEmail(), target.getPassword());
                    }
                    userService.updateUser(updated);
                    response.sendRedirect(context + "/admin?tab=users&msg="
                            + WebPages.escape("Role updated successfully for user #" + targetUserId + " to " + newRole + "!"));
                    return;
                }

            } else if ("delete_user".equals(action)) {
                int targetUserId = Integer.parseInt(request.getParameter("userId"));
                int currentAdminId = (Integer) request.getSession().getAttribute("userId");
                if (targetUserId == currentAdminId) {
                    response.sendRedirect(context + "/admin?tab=users&error="
                            + WebPages.escape("Cannot delete your own active administrator account!"));
                    return;
                }
                userService.deleteUser(targetUserId);
                response.sendRedirect(context + "/admin?tab=users&msg="
                        + WebPages.escape("User #" + targetUserId + " deleted successfully from system."));
                return;

            } else if ("update_setting".equals(action)) {
                String key = request.getParameter("settingKey");
                String val = request.getParameter("settingValue");
                settingsService.updateSetting(key, val);
                response.sendRedirect(context + "/admin?tab=settings&msg="
                        + WebPages.escape("System setting '" + key + "' updated successfully to '" + val + "'!"));
                return;
            }

            response.sendRedirect(context + "/admin");
        } catch (InvalidUserException e) {
            response.sendRedirect(context + "/admin?tab=" + request.getParameter("tab")
                    + "&error=" + WebPages.escape(e.getMessage()));
        } catch (DatabaseException e) {
            response.sendRedirect(context + "/admin?error=" + WebPages.escape("Database error: " + e.getMessage()));
        }
    }

    private void renderDashboard(HttpServletRequest request, HttpServletResponse response)
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

        try {
            List<User> allUsers = userService.getAllUsers();
            List<Quiz> allQuizzes = quizService.getAllQuizzes();
            List<Quiz> pendingQuizzes = quizService.getQuizzesByStatus(QuizStatus.PENDING_APPROVAL.name());
            List<Result> allResults = resultDAO.getAllResults();
            List<SystemSetting> allSettings = settingsService.getAllSettings();

            int totalUsers = allUsers.size();
            int totalQuizzes = allQuizzes.size();
            int pending = pendingQuizzes.size();
            int approved = quizService.countQuizzesByStatus(QuizStatus.APPROVED.name());
            int attempts = attemptDAO.countAllAttempts();

            StringBuilder body = new StringBuilder();

            // Alerts
            if (msg != null && !msg.isBlank()) {
                body.append(WebPages.alert("success", msg));
            }
            if (error != null && !error.isBlank()) {
                body.append(WebPages.alert("error", error));
            }

            // Top Tab Navigation Bar
            body.append("<div style=\"display:flex; gap:8px; margin-bottom:20px; flex-wrap:wrap;\">")
                .append(tabLink(context, "dashboard", "📊 Overview", tab))
                .append(tabLink(context, "users", "👥 User Management (" + totalUsers + ")", tab))
                .append(tabLink(context, "approvals", "📝 Quiz Approvals (" + pending + ")", tab))
                .append(tabLink(context, "settings", "⚙️ System Settings (" + allSettings.size() + ")", tab))
                .append(tabLink(context, "reports", "📈 Platform Reports", tab))
                .append("</div>");

            if ("users".equals(tab)) {
                body.append(renderUserManagementTab(context, allUsers));
            } else if ("approvals".equals(tab)) {
                body.append(renderApprovalsTab(context, pendingQuizzes, allQuizzes));
            } else if ("settings".equals(tab)) {
                body.append(renderSettingsTab(context, allSettings));
            } else if ("reports".equals(tab)) {
                body.append(renderReportsTab(context, allUsers, allQuizzes, allResults));
            } else {
                body.append(renderOverviewTab(context, totalUsers, totalQuizzes, pending, approved, attempts, pendingQuizzes));
            }

            out.print(WebPages.page(request, "Admin Console", body.toString()));
        } catch (DatabaseException e) {
            out.print(WebPages.page(request, "Admin Error",
                    WebPages.alert("error", "Database error: " + e.getMessage())));
        }
    }

    private String tabLink(String context, String tabId, String label, String currentTab) {
        boolean active = tabId.equals(currentTab);
        String cls = active ? "button" : "button secondary";
        return "<a href=\"" + context + "/admin?tab=" + tabId + "\" class=\"" + cls + "\">" + label + "</a>";
    }

    private String renderOverviewTab(String context, int users, int quizzes, int pending, int approved,
                                     int attempts, List<Quiz> pendingQuizzes) {
        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Administrator Console Overview</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Platform telemetry, statistics, and system governance</p>"
                + "  </div>"
                + "  <a class=\"button danger\" href=\"" + context + "/admin?action=logout\">🚪 Sign Out</a>"
                + "</div>"
                + "<div class=\"stats\">"
                + WebPages.statCard("Total Users", users, "")
                + WebPages.statCard("Total Quizzes", quizzes, "")
                + WebPages.statCard("Pending Approvals", pending, "warning")
                + WebPages.statCard("Live Approved Quizzes", approved, "success")
                + WebPages.statCard("Total Attempts", attempts, "")
                + "</div>"
                + "</section>"
                + "<section class=\"card\">"
                + "  <h3>Quizzes Awaiting Immediate Approval</h3>"
                + "  <p class=\"subtitle\">Submitted by faculty creators that require administrator verification</p>"
                + renderPendingTable(context, pendingQuizzes)
                + "</section>";
    }

    private String renderUserManagementTab(String context, List<User> allUsers) {
        StringBuilder rows = new StringBuilder();
        for (User u : allUsers) {
            String roleBadge = "badge-approved";
            if (u.getRole() == Role.ADMIN) {
                roleBadge = "badge-pending";
            } else if (u.getRole() == Role.QUIZ_CREATOR) {
                roleBadge = "badge-draft";
            }

            rows.append("<tr>")
                    .append("<td>#").append(u.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(u.getName())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(u.getEmail())).append("</td>")
                    .append("<td><span class=\"badge ").append(roleBadge).append("\">").append(u.getRole()).append("</span></td>")
                    .append("<td>")
                    // Role Change Form
                    .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"change_role\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"users\">")
                    .append("<input type=\"hidden\" name=\"userId\" value=\"").append(u.getId()).append("\">")
                    .append("<select name=\"role\" style=\"padding:4px 8px; font-size:12px; margin-right:4px;\">")
                    .append("<option value=\"ADMIN\"").append(u.getRole() == Role.ADMIN ? " selected" : "").append(">ADMIN</option>")
                    .append("<option value=\"QUIZ_CREATOR\"").append(u.getRole() == Role.QUIZ_CREATOR ? " selected" : "").append(">QUIZ_CREATOR</option>")
                    .append("<option value=\"PARTICIPANT\"").append(u.getRole() == Role.PARTICIPANT ? " selected" : "").append(">PARTICIPANT</option>")
                    .append("</select>")
                    .append("<button type=\"submit\" class=\"secondary small\">Update Role</button>")
                    .append("</form> ")
                    // Delete Form
                    .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\" onsubmit=\"return confirm('Delete user #").append(u.getId()).append(" (").append(WebPages.escape(u.getEmail())).append(")?');\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"delete_user\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"users\">")
                    .append("<input type=\"hidden\" name=\"userId\" value=\"").append(u.getId()).append("\">")
                    .append("<button type=\"submit\" class=\"danger small\">Delete</button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
        }

        return "<div class=\"grid-2\">"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h2>Create New User Account</h2>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Add an administrator, quiz creator, or participant</p>"
                + "    </div>"
                + "  </div>"
                + "  <form method=\"post\" action=\"" + context + "/admin\">"
                + "    <input type=\"hidden\" name=\"action\" value=\"add_user\">"
                + "    <input type=\"hidden\" name=\"tab\" value=\"users\">"
                + "    <label for=\"name\">Full Name *</label>"
                + "    <input type=\"text\" id=\"name\" name=\"name\" placeholder=\"e.g. Dr. Rajesh Sharma\" required>"
                + "    <label for=\"email\">Email Address *</label>"
                + "    <input type=\"email\" id=\"email\" name=\"email\" placeholder=\"e.g. rajesh@university.edu\" required>"
                + "    <label for=\"password\">Initial Password *</label>"
                + "    <input type=\"password\" id=\"password\" name=\"password\" placeholder=\"Minimum 6 characters\" required>"
                + "    <label for=\"role\">Assigned Role *</label>"
                + "    <select id=\"role\" name=\"role\" required>"
                + "      <option value=\"PARTICIPANT\">Student (PARTICIPANT)</option>"
                + "      <option value=\"QUIZ_CREATOR\">Faculty (QUIZ_CREATOR)</option>"
                + "      <option value=\"ADMIN\">System Administrator (ADMIN)</option>"
                + "    </select>"
                + "    <div style=\"margin-top: 22px;\">"
                + "      <button type=\"submit\" style=\"width:100%;\">➕ Create User Account</button>"
                + "    </div>"
                + "  </form>"
                + "</section>"
                + "<section class=\"card\">"
                + "  <div class=\"card-header\">"
                + "    <div>"
                + "      <h3>Registered Platform Users (" + allUsers.size() + ")</h3>"
                + "      <p class=\"subtitle\" style=\"margin-bottom:0;\">Manage existing user accounts and modify privileges</p>"
                + "    </div>"
                + "  </div>"
                + "  <table><thead><tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Actions</th></tr></thead><tbody>"
                + rows
                + "  </tbody></table>"
                + "</section>"
                + "</div>";
    }

    private String renderApprovalsTab(String context, List<Quiz> pendingQuizzes, List<Quiz> allQuizzes) {
        StringBuilder allRows = new StringBuilder();
        for (Quiz q : allQuizzes) {
            String badgeCls = "badge-pending";
            if (q.getStatus() == QuizStatus.APPROVED) badgeCls = "badge-approved";
            else if (q.getStatus() == QuizStatus.REJECTED) badgeCls = "badge-rejected";
            else if (q.getStatus() == QuizStatus.DRAFT) badgeCls = "badge-draft";

            allRows.append("<tr>")
                    .append("<td>#").append(q.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(q.getTitle())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(q.getCreatorName())).append("</td>")
                    .append("<td>").append(q.getDurationMinutes()).append(" mins</td>")
                    .append("<td>").append(q.getTotalMarks()).append(" pts</td>")
                    .append("<td><span class=\"badge ").append(badgeCls).append("\">").append(q.getStatus()).append("</span></td>")
                    .append("</tr>");
        }

        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Quiz Content Approval & Rejection Queue</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Review content submitted by quiz creators before publishing</p>"
                + "  </div>"
                + "</div>"
                + renderPendingTable(context, pendingQuizzes)
                + "</section>"
                + "<section class=\"card\">"
                + "  <h3>All Platform Quizzes (" + allQuizzes.size() + ")</h3>"
                + "  <p class=\"subtitle\">Full quiz registry across all statuses</p>"
                + "  <table><thead><tr><th>ID</th><th>Quiz Title</th><th>Creator</th><th>Duration</th><th>Marks</th><th>Status</th></tr></thead><tbody>"
                + allRows
                + "  </tbody></table>"
                + "</section>";
    }

    private String renderPendingTable(String context, List<Quiz> quizzes) {
        if (quizzes.isEmpty()) {
            return "<div class=\"alert success\" style=\"margin-top:14px;\"><span>✔</span><span>No quizzes currently awaiting approval. All submitted quizzes have been reviewed!</span></div>";
        }
        StringBuilder rows = new StringBuilder();
        for (Quiz quiz : quizzes) {
            rows.append("<tr>")
                    .append("<td>#").append(quiz.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(quiz.getTitle())).append("</b><br><small style=\"color:var(--text-muted);\">").append(WebPages.escape(quiz.getDescription())).append("</small></td>")
                    .append("<td>").append(WebPages.escape(quiz.getCreatorName())).append("</td>")
                    .append("<td>").append(quiz.getDurationMinutes()).append(" mins</td>")
                    .append("<td>").append(quiz.getTotalMarks()).append(" pts</td>")
                    .append("<td><span class=\"badge badge-pending\">PENDING</span></td>")
                    .append("<td>")
                    .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"approve\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"approvals\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quiz.getId()).append("\">")
                    .append("<button type=\"submit\" class=\"success small\">✔ Approve</button></form> ")
                    .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"reject\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"approvals\">")
                    .append("<input type=\"hidden\" name=\"quizId\" value=\"").append(quiz.getId()).append("\">")
                    .append("<button type=\"submit\" class=\"danger small\">✖ Reject</button></form>")
                    .append("</td>")
                    .append("</tr>");
        }
        return "<table><thead><tr><th>ID</th><th>Quiz Title</th><th>Creator</th><th>Duration</th><th>Marks</th><th>Status</th><th>Review Actions</th></tr></thead><tbody>"
                + rows + "</tbody></table>";
    }

    private String renderSettingsTab(String context, List<SystemSetting> settings) {
        StringBuilder rows = new StringBuilder();
        for (SystemSetting s : settings) {
            rows.append("<tr>")
                    .append("<td><code>").append(WebPages.escape(s.getKey())).append("</code></td>")
                    .append("<td><b>").append(WebPages.escape(s.getValue())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(s.getDescription())).append("</td>")
                    .append("<td>")
                    .append("<form method=\"post\" action=\"").append(context).append("/admin\" class=\"inline\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"update_setting\">")
                    .append("<input type=\"hidden\" name=\"tab\" value=\"settings\">")
                    .append("<input type=\"hidden\" name=\"settingKey\" value=\"").append(WebPages.escape(s.getKey())).append("\">")
                    .append("<input type=\"text\" name=\"settingValue\" value=\"").append(WebPages.escape(s.getValue())).append("\" style=\"padding:4px 8px; font-size:13px; width:120px; margin-right:4px;\">")
                    .append("<button type=\"submit\" class=\"secondary small\">Save</button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
        }

        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>System Configuration Settings</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Manage system-wide defaults, timer configurations, and pass thresholds</p>"
                + "  </div>"
                + "</div>"
                + "<table><thead><tr><th>Setting Key</th><th>Current Value</th><th>Description</th><th>Action</th></tr></thead><tbody>"
                + rows
                + "</tbody></table>"
                + "</section>";
    }

    private String renderReportsTab(String context, List<User> allUsers, List<Quiz> allQuizzes, List<Result> allResults) {
        int passedCount = (int) allResults.stream().filter(Result::isPassed).count();
        double passRate = allResults.isEmpty() ? 0.0 : (passedCount * 100.0 / allResults.size());

        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long creatorCount = allUsers.stream().filter(u -> u.getRole() == Role.QUIZ_CREATOR).count();
        long studentCount = allUsers.stream().filter(u -> u.getRole() == Role.PARTICIPANT).count();

        StringBuilder resultRows = new StringBuilder();
        int count = 0;
        for (Result r : allResults) {
            if (++count > 15) break; // show latest 15 for summary
            resultRows.append("<tr>")
                    .append("<td>#").append(r.getId()).append("</td>")
                    .append("<td><b>").append(WebPages.escape(r.getParticipantName())).append("</b></td>")
                    .append("<td>").append(WebPages.escape(r.getQuizTitle())).append("</td>")
                    .append("<td>").append(r.getObtainedMarks()).append(" / ").append(r.getTotalMarks()).append(" pts</td>")
                    .append("<td>").append(String.format("%.1f%%", r.getPercentage())).append("</td>")
                    .append("<td>").append(r.isPassed() ? "<span class=\"badge badge-approved\">PASSED</span>" : "<span class=\"badge badge-rejected\">FAILED</span>").append("</td>")
                    .append("<td><small style=\"color:var(--text-muted);\">").append(r.getSubmittedAt()).append("</small></td>")
                    .append("</tr>");
        }
        if (allResults.isEmpty()) {
            resultRows.append("<tr><td colspan=\"7\" style=\"text-align:center; color:var(--text-muted);\">No assessments taken yet.</td></tr>");
        }

        return "<section class=\"card\">"
                + "<div class=\"card-header\">"
                + "  <div>"
                + "    <h2>Platform Analytics & Reports</h2>"
                + "    <p class=\"subtitle\" style=\"margin-bottom:0;\">Comprehensive telemetry across users, quizzes, and academic performance</p>"
                + "  </div>"
                + "</div>"
                + "<div class=\"stats\">"
                + WebPages.statCard("Students Enrolled", studentCount, "")
                + WebPages.statCard("Faculty Creators", creatorCount, "")
                + WebPages.statCard("System Admins", adminCount, "")
                + WebPages.statCard("Total Assessments", allResults.size(), "")
                + WebPages.statCard("Platform Pass Rate", String.format("%.1f%%", passRate), passRate >= 60 ? "success" : "warning")
                + "</div>"
                + "</section>"
                + "<section class=\"card\">"
                + "  <h3>Recent Student Assessments</h3>"
                + "  <p class=\"subtitle\">Live feed of completed assessments</p>"
                + "  <table><thead><tr><th>ID</th><th>Student</th><th>Quiz</th><th>Score</th><th>Percentage</th><th>Outcome</th><th>Timestamp</th></tr></thead><tbody>"
                + resultRows
                + "  </tbody></table>"
                + "</section>";
    }
}