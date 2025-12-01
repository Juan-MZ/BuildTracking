package com.construmedicis.buildtracking.util.response.handler;

import com.construmedicis.buildtracking.util.response.Response;
import lombok.Getter;

@Getter
public class ResponseHandler<T> {
    private final Response<T> response;

    public ResponseHandler(int status, String userMessage, String infoRuta, T data) {
        response = new Response<>();

        response.setStatus(status);
        response.setUserMessage(userMessage);
        response.setMoreInfo(infoRuta);
        response.setData(data);
    }
}