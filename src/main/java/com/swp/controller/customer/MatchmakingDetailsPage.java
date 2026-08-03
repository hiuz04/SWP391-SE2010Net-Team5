package com.swp.controller.customer;

import com.swp.dao.MatchmakingPostDAO;
import com.swp.dao.MatchmakingPostResponseDAO;
import com.swp.model.User;
import com.swp.model.dto.MatchmakingPostDTO;
import com.swp.model.dto.MatchmakingPostResponseDTO;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.List;

@WebServlet("/matchmaking-details")
public class MatchmakingDetailsPage extends HttpServlet {
    private final MatchmakingPostDAO postDAO = new MatchmakingPostDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

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
            
            HttpSession session = req.getSession(false);
            if (session != null && session.getAttribute("user") != null) {
                User sessionUser = (User) session.getAttribute("user");
                if (postDTO.getPost().getAuthorId() == sessionUser.getUserId()) {
                    MatchmakingPostResponseDAO responseDAO = new MatchmakingPostResponseDAO();
                    List<MatchmakingPostResponseDTO> responses = responseDAO.getResponsesByPostId(postId);
                    req.setAttribute("responses", responses);
                }
            }

            req.getRequestDispatcher("/WEB-INF/customer/matchmaking-details.jsp").forward(req, resp);
        } catch (NumberFormatException e) {
            resp.sendRedirect(req.getContextPath() + "/matchmaking");
        }
    }
}
