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
            // already logged in -> go to the quiz list (or admin for admins)
            String role = (String) session.getAttribute("role");
            response.sendRedirect(request.getContextPath()
                    + (Role.ADMIN.name().equals(role) ? "/admin" : "/quiz"));
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
            // SAME architecture as the Swing GUI: Servlet -> Service -> DAO -> JDBC -> MySQL
            User user = authService.login(email, password);

            HttpSession session = request.getSession(true);     // create session
            session.setAttribute("userId", user.getId());
            session.setAttribute("userName", user.getName());
            session.setAttribute("userEmail", user.getEmail());
            session.setAttribute("role", user.getRole().name());

            response.sendRedirect(request.getContextPath()
                    + (user.getRole() == Role.ADMIN ? "/admin" : "/quiz"));

        } catch (InvalidLoginException e) {
            // custom exception from the service layer -> shown in the form
            request.setAttribute("error", e.getMessage());
            renderForm(request, response, e.getMessage(), null);
        } catch (DatabaseException e) {
            renderForm(request, response, "Database error: " + e.getMessage(), null);
        }
    }

    /** Renders the login form (and an optional error message). */
    private void renderForm(HttpServletRequest request, HttpServletResponse response,
                            String error, String message) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        String body = "<section class=\"card narrow\">"
                + "<h2>Login</h2>"
                + (error != null ? WebPages.alert("error", error) : "")
                + (message != null ? WebPages.alert("success", message) : "")
                + "<form method=\"post\" action=\"" + request.getContextPath() + "/login\">"
                + "  <label>Email</label>"
                + "  <input type=\"email\" name=\"email\" required autofocus>"
                + "  <label>Password</label>"
                + "  <input type=\"password\" name=\"password\" required>"
                + "  <button type=\"submit\">Login</button>"
                + "</form>"
                + "<p class=\"muted\">Demo accounts: admin@quiz.com / admin123, "
                + "creator@quiz.com / creator123, student@quiz.com / student123</p>"
                + "</section>";
        out.print(WebPages.page(request.getContextPath(), "Login", body));
    }
}
