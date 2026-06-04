package com.swp.controller.auth;

import com.swp.service.GoogleOAuthService;
import com.swp.util.GoogleConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.UUID;

@WebServlet("/auth/google")
public class GoogleAuthServlet extends HttpServlet {

    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!GoogleConfig.isConfigured()) {
            response.sendRedirect(request.getContextPath() + "/login?googleError=not_configured");
            return;
        }

        HttpSession session = request.getSession(true);
        String state = UUID.randomUUID().toString();
        String redirectUri = GoogleConfig.resolveRedirectUri(request);

        session.setAttribute("googleOAuthState", state);
        session.setAttribute("googleOAuthRedirectUri", redirectUri);

        response.sendRedirect(googleOAuthService.buildAuthorizationUrl(state, redirectUri));
    }
}
