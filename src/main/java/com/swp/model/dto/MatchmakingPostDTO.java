package com.swp.model.dto;

import com.swp.model.MatchmakingPost;
import java.io.Serializable;

public class MatchmakingPostDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MatchmakingPost post;
    private String authorName;
    private String complexName;
    private int responseCount;

    public MatchmakingPostDTO() {
    }

    public MatchmakingPostDTO(MatchmakingPost post, String authorName, String complexName) {
        this.post = post;
        this.authorName = authorName;
        this.complexName = complexName;
    }

    public MatchmakingPostDTO(MatchmakingPost post, String authorName, String complexName, int responseCount) {
        this.post = post;
        this.authorName = authorName;
        this.complexName = complexName;
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

    public String getComplexName() {
        return complexName;
    }

    public void setComplexName(String complexName) {
        this.complexName = complexName;
    }
}
