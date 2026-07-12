package com.swp.controller.customer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.swp.model.dto.FeedbackDTO;
import com.swp.service.FeedbackService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@WebServlet("/feedback")
public class FeedbackAPIController extends HttpServlet {

    private final FeedbackService feedbackService = new FeedbackService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String complexIdStr = req.getParameter("complexId");

        if (complexIdStr == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing complexId");
            return;
        }

        try {
            long complexId = Long.parseLong(complexIdStr);
            List<FeedbackDTO> list = feedbackService.getAllFeedbackOfThisComplexes(complexId);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(
                            LocalDateTime.class,
                            (JsonSerializer<LocalDateTime>)
                                    (src, type, context) -> new JsonPrimitive(src.toString())
                    )
                    .create();

            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            gson.toJson(list, resp.getWriter());

        } catch (NumberFormatException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid complexId");
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
