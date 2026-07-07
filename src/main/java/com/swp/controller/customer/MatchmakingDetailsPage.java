package com.swp.controller.customer;

import com.swp.dao.MatchmakingPostDAO;
import com.swp.model.dto.MatchmakingPostDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/matchmaking-details")
public class MatchmakingDetailsPage extends HttpServlet {
    private final MatchmakingPostDAO postDAO = new MatchmakingPostDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        String idStr = req.getParameter("id");
        if (idStr == null || idStr.trim().isEmpty()) {
            resp.sendRedirect(req.getContextPath() + "/matchmaking");
            return;
        }

        try {
            long postId = Long.parseLong(idStr);
            MatchmakingPostDTO postDTO = postDAO.getPostById(postId);
            if (postDTO == null) {
                resp.sendRedirect(req.getContextPath() + "/matchmaking");
                return;
            }
            req.setAttribute("postDTO", postDTO);
            req.getRequestDispatcher("/WEB-INF/customer/matchmaking-details.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/matchmaking");
        }
    }
}
