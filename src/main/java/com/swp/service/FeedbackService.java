package com.swp.service;

import com.swp.dao.FeedbackDAO;
import com.swp.model.Feedback;
import com.swp.model.FeedbackImage;
import com.swp.model.dto.FeedbackDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedbackService {

    private static FeedbackDAO feedbackDAO = new FeedbackDAO();

    public List<FeedbackDTO> getAllFeedbackOfThisComplexes(long complexId) {
        List<FeedbackDTO> feedbacks = feedbackDAO.getAllFeedbackOfThisComplexes(complexId);
        List<FeedbackImage> images = feedbackDAO.getFeedbackImagesByComplexId(complexId);
        Map<Long, List<FeedbackImage>> imageMap = new HashMap<>();

        for (FeedbackImage image : images) {
            imageMap
                    .computeIfAbsent(image.getFeedbackId(), k -> new ArrayList<>())
                    .add(image);
        }

        for (FeedbackDTO feedback : feedbacks) {
            feedback.setImageList(
                    imageMap.getOrDefault(
                            feedback.getFeedbackId(),
                            new ArrayList<>()
                    )
            );
        }

        return feedbacks;
    }

}
