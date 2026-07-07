package com.swp.model.dto;

import com.swp.model.MatchmakingPost;
import java.io.Serializable;
import java.time.LocalDateTime;

public class MatchmakingPostDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MatchmakingPost post;
    private String authorName;
    private String facilityName;
    private int responseCount;

    public MatchmakingPostDTO() {
    }

    public MatchmakingPostDTO(MatchmakingPost post, String authorName, String facilityName) {
        this.post = post;
        this.authorName = authorName;
        this.facilityName = facilityName;
    }

    public MatchmakingPostDTO(MatchmakingPost post, String authorName, String facilityName, int responseCount) {
        this.post = post;
        this.authorName = authorName;
        this.facilityName = facilityName;
        this.responseCount = responseCount;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    public MatchmakingPost getPost() {
        return post;
    }

    public void setPost(MatchmakingPost post) {
        this.post = post;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }
}
