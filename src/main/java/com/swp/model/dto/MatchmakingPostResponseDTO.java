package com.swp.model.dto;

import com.swp.model.MatchmakingPostResponse;
import java.io.Serializable;

public class MatchmakingPostResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private MatchmakingPostResponse response;
    private String responderName;
    private String responderPhone;

    public MatchmakingPostResponseDTO() {
    }

    public MatchmakingPostResponseDTO(MatchmakingPostResponse response, String responderName) {
        this.response = response;
        this.responderName = responderName;
    }

    public MatchmakingPostResponseDTO(MatchmakingPostResponse response, String responderName, String responderPhone) {
        this.response = response;
        this.responderName = responderName;
        this.responderPhone = responderPhone;
    }

    public String getResponderPhone() {
        return responderPhone;
    }

    public void setResponderPhone(String responderPhone) {
        this.responderPhone = responderPhone;
    }

    public MatchmakingPostResponse getResponse() {
        return response;
    }

    public void setResponse(MatchmakingPostResponse response) {
        this.response = response;
    }

    public String getResponderName() {
        return responderName;
    }

    public void setResponderName(String responderName) {
        this.responderName = responderName;
    }
}
