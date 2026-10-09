package com.quizplatform.servlet;

import com.quizplatform.exception.DatabaseException;
import com.quizplatform.exception.InvalidLoginException;
import com.quizplatform.model.Role;
import com.quizplatform.model.User;
import com.quizplatform.service.AuthenticationService;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Demonstrates SERVLET INTEGRATION:
 *  - @WebServlet mapping (annotation based, also declared in web.xml)
 *  - doGet()  -> renders the login form
 *  - doPost() -> authenticates through the SAME service/DAO layer as the GUI
 *  - HttpSession -> role based session after login
 */
@WebServlet(name = "LoginServlet", urlPatterns = {"/login"})
public class LoginServlet extends HttpServlet {

    private final AuthenticationService authService = new AuthenticationService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("userId") != null) {
            String role = (String) session.getAttribute("role");
            String target = "/quiz";
            if (Role.ADMIN.name().equals(role)) {
                target = "/admin";
            } else if (Role.QUIZ_CREATOR.name().equals(role)) {
                target = "/creator";
            }
            response.sendRedirect(request.getContextPath() + target);
            return;
        }
        renderForm(request, response,
                (String) request.getAttribute("error"),
                (String) request.getAttribute("message"));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        try {
            User user = authService.login(email, password);

            HttpSession session = request.getSession(true);
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("role", user.getRole().name());

            String target = "/quiz";
            if (user.getRole() == Role.ADMIN) {
                target = "/admin";
            } else if (user.getRole() == Role.QUIZ_CREATOR) {
                target = "/creator";
            }
            response.sendRedirect(request.getContextPath() + target);

        } catch (InvalidLoginException e) {
            request.setAttribute("error", e.getMessage());
            renderForm(request, response, e.getMessage(), null);
        } catch (DatabaseException e) {
            renderForm(request, response, "Database unavailable: " + e.getMessage(), null);
        }
    }

    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            String error, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String body = "<section class=\"card narrow\">"
                + "<h2>Sign In</h2>"
                + "<p class=\"subtitle\">Access your quiz evaluations and reports</p>"
                + (error != null ? WebPages.alert("error", error) : "")
                + (message != null ? WebPages.alert("success", message) : "")
                + "<form method=\"post\" action=\"" + request.getContextPath() + "/login\">"
                + "  <label for=\"email\">Email Address</label>"
                + "  <input type=\"email\" id=\"email\" name=\"email\" placeholder=\"e.g. student@quiz.com\" required autofocus>"
                + "  <label for=\"password\">Password</label>"
                + "  <input type=\"password\" id=\"password\" name=\"password\" placeholder=\"Enter your password\" required>"
                + "  <button type=\"submit\" style=\"width: 100%; margin-top: 22px;\">Sign In &rarr;</button>"
                + "</form>"
                + "<div style=\"margin-top: 24px; padding: 14px; background: #f8fafc; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px;\">"
                + "  <p style=\"font-weight: 600; margin-bottom: 4px;\">Preconfigured Demo Accounts:</p>"
                + "  <p style=\"color: var(--text-muted); margin: 2px 0;\">&bull; Admin: <code>admin@quiz.com</code> / admin123</p>"
                + "  <p style=\"color: var(--text-muted); margin: 2px 0;\">&bull; Creator: <code>creator@quiz.com</code> / creator123</p>"
                + "  <p style=\"color: var(--text-muted); margin: 2px 0;\">&bull; Student: <code>student@quiz.com</code> / student123</p>"
                + "</div>"
                + "</section>";
        out.print(WebPages.page(request, "Sign In", body));
    }
}
