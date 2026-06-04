package com.swp.controller.auth;

import com.swp.dao.RoleDAO;
import com.swp.dao.UserDAO;
import com.swp.model.User;
import com.swp.service.GoogleOAuthService;
import com.swp.service.GoogleOAuthService.GoogleUserInfo;
import com.swp.util.AuthUtil;
import com.swp.util.GoogleConfig;
import com.swp.util.RegisterValidator;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet("/auth/google/callback")
public class GoogleCallbackServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(GoogleCallbackServlet.class.getName());

    private final GoogleOAuthService googleOAuthService = new GoogleOAuthService();
    private final UserDAO userDAO = new UserDAO();
    private final RoleDAO roleDAO = new RoleDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String ctx = request.getContextPath();

        if (!GoogleConfig.isConfigured()) {
            response.sendRedirect(ctx + "/login?googleError=not_configured");
            return;
        }

        String error = request.getParameter("error");
        if (error != null && !error.isBlank()) {
            response.sendRedirect(ctx + "/login?googleError=cancelled");
            return;
        }

        HttpSession session = request.getSession(false);
        String expectedState = session != null ? (String) session.getAttribute("googleOAuthState") : null;
        String redirectUri = session != null ? (String) session.getAttribute("googleOAuthRedirectUri") : null;
        String state = request.getParameter("state");
        String code = request.getParameter("code");

        if (session == null || expectedState == null || !expectedState.equals(state)) {
            response.sendRedirect(ctx + "/login?googleError=invalid_state");
            return;
        }
        session.removeAttribute("googleOAuthState");

        if (redirectUri == null || redirectUri.isBlank()) {
            redirectUri = GoogleConfig.resolveRedirectUri(request);
        }
        session.removeAttribute("googleOAuthRedirectUri");

        if (code == null || code.isBlank()) {
            response.sendRedirect(ctx + "/login?googleError=no_code");
            return;
        }

        try {
            GoogleUserInfo googleUser = googleOAuthService.fetchUserInfo(code, redirectUri);
            User loggedIn = resolveUser(googleUser);
            session.setAttribute("user", loggedIn);
            session.setAttribute("navRole", AuthUtil.toNavRole(loggedIn.getRoleName()));
            response.sendRedirect(ctx + "/");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Google login failed", e);
            response.sendRedirect(ctx + "/login?googleError=failed");
        }
    }

    private User resolveUser(GoogleUserInfo googleUser) {
        Optional<User> byGoogleId = userDAO.findByGoogleId(googleUser.googleId());
        if (byGoogleId.isPresent()) {
            return byGoogleId.get();
        }

        Optional<User> byEmail = userDAO.findByEmail(googleUser.email());
        if (byEmail.isPresent()) {
            User existing = byEmail.get();
            if (existing.getGoogleId() != null && !existing.getGoogleId().equals(googleUser.googleId())) {
                throw new IllegalStateException("Email đã liên kết tài khoản Google khác.");
            }
            userDAO.linkGoogleAccount(existing.getUserId(), googleUser.googleId(), googleUser.avatarUrl());
            return userDAO.findByGoogleId(googleUser.googleId()).orElseThrow();
        }

        int roleId = roleDAO.findRoleIdByName(RegisterValidator.DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Vai trò CUSTOMER chưa có trong bảng roles."));

        User newUser = new User();
        newUser.setRoleId(roleId);
        newUser.setFullName(googleUser.fullName());
        newUser.setEmail(googleUser.email());
        newUser.setPhone(null);
        newUser.setPasswordHash(UUID.randomUUID().toString());
        newUser.setAvatarUrl(googleUser.avatarUrl());
        newUser.setGoogleId(googleUser.googleId());

        long userId = userDAO.insertGoogleUser(newUser);
        newUser.setUserId(userId);
        newUser.setRoleName(RegisterValidator.DEFAULT_ROLE);
        newUser.setStatus("ACTIVE");
        return newUser;
    }
}
