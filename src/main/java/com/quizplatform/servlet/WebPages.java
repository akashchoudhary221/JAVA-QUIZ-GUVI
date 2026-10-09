package com.quizplatform.servlet;

import com.quizplatform.model.Role;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * Server-side HTML template renderer for the Java Servlet module.
 *
 * Provides dynamic, role-segregated navigation so that Admin,
 * Quiz Creator, and Participant see ONLY their respective portals.
 */
public final class WebPages {

    private WebPages() {
    }

    /**
     * Session-aware page renderer that adapts navigation bar strictly
     * based on whether the logged in user is an Admin, Quiz Creator, or Participant.
     */
    public static String page(HttpServletRequest request, String title, String body) {
        String contextPath = request != null ? request.getContextPath() : "";
        HttpSession session = request != null ? request.getSession(false) : null;
        String role = session != null ? (String) session.getAttribute("role") : null;
        String userName = session != null ? (String) session.getAttribute("userName") : null;

        StringBuilder navHtml = new StringBuilder();
        String brandBadge = "Academic";
        String brandHref = contextPath + "/";

        if (Role.ADMIN.name().equals(role)) {
            brandBadge = "Admin Suite";
            brandHref = contextPath + "/admin";
            navHtml.append("<span class=\"brand-badge\" style=\"background:rgba(255,255,255,0.15); margin-right: 6px;\">🛡️ ").append(escape(userName)).append("</span>")
                   .append("<a href=\"").append(contextPath).append("/admin?action=logout\" class=\"logout-btn\">Sign Out</a>");
        } else if (Role.QUIZ_CREATOR.name().equals(role)) {
            brandBadge = "Creator Studio";
            brandHref = contextPath + "/creator";
            navHtml.append("<span class=\"brand-badge\" style=\"background:rgba(255,255,255,0.15); margin-right: 6px;\">✍️ ").append(escape(userName)).append("</span>")
                   .append("<a href=\"").append(contextPath).append("/creator?action=logout\" class=\"logout-btn\">Sign Out</a>");
        } else if (Role.PARTICIPANT.name().equals(role)) {
            brandBadge = "Student Portal";
            brandHref = contextPath + "/quiz";
            navHtml.append("<a href=\"").append(contextPath).append("/quiz\">📝 Available Quizzes</a>")
                   .append("<a href=\"").append(contextPath).append("/result\">🏆 My Results & History</a>")
                   .append("<span class=\"brand-badge\" style=\"background:rgba(255,255,255,0.15); margin-left: 8px;\">🎓 ").append(escape(userName)).append("</span>")
                   .append("<a href=\"").append(contextPath).append("/quiz?action=logout\" class=\"logout-btn\">Sign Out</a>");
        } else {
            navHtml.append("<a href=\"").append(contextPath).append("/\">Home</a>")
                   .append("<a href=\"").append(contextPath).append("/login\">Sign In</a>");
        }

        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>" + escape(title) + " | Java-Based Online Quiz Platform</title>\n"
                + "  <link rel=\"stylesheet\" href=\"" + contextPath + "/css/style.css\">\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header>\n"
                + "    <div class=\"header-container\">\n"
                + "      <a href=\"" + brandHref + "\" class=\"brand\">\n"
                + "        <span class=\"brand-icon\">⚡</span>\n"
                + "        <span class=\"brand-title\">QuizPlatform</span>\n"
                + "        <span class=\"brand-badge\">" + brandBadge + "</span>\n"
                + "      </a>\n"
                + "      <nav>\n"
                + navHtml
                + "      </nav>\n"
                + "    </div>\n"
                + "  </header>\n"
                + "  <main>\n"
                + body + "\n"
                + "  </main>\n"
                + "  <footer>\n"
                + "    <p>Java 17 &bull; Jakarta/Java Servlets &bull; Plain JDBC &bull; MySQL &bull; Apache Tomcat</p>\n"
                + "    <p style=\"margin-top: 4px; font-size: 12px;\">&copy; 2026 Java-Based Online Quiz Platform &mdash; Academic Viva Evaluation Project</p>\n"
                + "  </footer>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /** Overloaded helper for backwards-compatibility where HttpServletRequest is not directly available. */
    public static String page(String contextPath, String title, String body) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n"
                + "  <title>" + escape(title) + " | Java-Based Online Quiz Platform</title>\n"
                + "  <link rel=\"stylesheet\" href=\"" + contextPath + "/css/style.css\">\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header>\n"
                + "    <div class=\"header-container\">\n"
                + "      <a href=\"" + contextPath + "/\" class=\"brand\">\n"
                + "        <span class=\"brand-icon\">⚡</span>\n"
                + "        <span class=\"brand-title\">QuizPlatform</span>\n"
                + "        <span class=\"brand-badge\">Academic</span>\n"
                + "      </a>\n"
                + "      <nav>\n"
                + "        <a href=\"" + contextPath + "/\">Home</a>\n"
                + "        <a href=\"" + contextPath + "/login\">Sign In</a>\n"
                + "      </nav>\n"
                + "    </div>\n"
                + "  </header>\n"
                + "  <main>\n"
                + body + "\n"
                + "  </main>\n"
                + "  <footer>\n"
                + "    <p>Java 17 &bull; Jakarta/Java Servlets &bull; Plain JDBC &bull; MySQL &bull; Apache Tomcat</p>\n"
                + "    <p style=\"margin-top: 4px; font-size: 12px;\">&copy; 2026 Java-Based Online Quiz Platform &mdash; Academic Viva Evaluation Project</p>\n"
                + "  </footer>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /** Simple alert box used for error, warning, and success notifications. */
    public static String alert(String type, String message) {
        String icon = "error".equalsIgnoreCase(type) ? "⚠️" : ("warning".equalsIgnoreCase(type) ? "🔔" : "✔");
        return "<div class=\"alert " + escape(type) + "\">"
                + "<span>" + icon + "</span>"
                + "<span>" + escape(message) + "</span>"
                + "</div>";
    }

    /** Helper for generating KPI stat blocks. */
    public static String statCard(String label, Object value, String typeClass) {
        return "<div class=\"stat " + (typeClass != null ? typeClass : "") + "\">"
                + "<p class=\"stat-label\">" + escape(label) + "</p>"
                + "<span class=\"stat-num\">" + escape(String.valueOf(value)) + "</span>"
                + "</div>";
    }

    /** Escapes user input to prevent XSS vulnerabilities. */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
