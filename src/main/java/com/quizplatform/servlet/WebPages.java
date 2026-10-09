package com.quizplatform.servlet;

/**
 * Tiny HTML helper for the servlet module.
 *
 * Keeps the presentation of servlet responses in ONE place (server-side
 * Java instead of a JavaScript framework - on purpose: this project
 * demonstrates Java web integration).
 */
public final class WebPages {

    private WebPages() {
    }

    /** Wraps a body into a complete HTML page using the shared stylesheet. */
    public static String page(String contextPath, String title, String body) {
        return "<!DOCTYPE html>\n"
                + "<html lang=\"en\">\n"
                + "<head>\n"
                + "  <meta charset=\"UTF-8\">\n"
                + "  <title>" + title + " | Java Quiz Platform</title>\n"
                + "  <link rel=\"stylesheet\" href=\"" + contextPath + "/css/style.css\">\n"
                + "</head>\n"
                + "<body>\n"
                + "  <header>\n"
                + "    <h1>Java-Based Online Quiz Platform</h1>\n"
                + "    <nav>\n"
                + "      <a href=\"" + contextPath + "/\">Home</a>\n"
                + "      <a href=\"" + contextPath + "/login\">Login</a>\n"
                + "      <a href=\"" + contextPath + "/quiz\">My Quizzes</a>\n"
                + "      <a href=\"" + contextPath + "/result\">My Results</a>\n"
                + "      <a href=\"" + contextPath + "/admin\">Admin</a>\n"
                + "    </nav>\n"
                + "  </header>\n"
                + "  <main>\n"
                + body
                + "  </main>\n"
                + "  <footer>Java Servlets + JDBC + MySQL | Java-Based Online Quiz Platform</footer>\n"
                + "</body>\n"
                + "</html>\n";
    }

    /** Simple alert box used for error / success messages. */
    public static String alert(String type, String message) {
        return "<div class=\"alert " + type + "\">" + escape(message) + "</div>";
    }

    /** Escapes user input before it is written into HTML (basic XSS protection). */
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
