package com.sorisonsoon.user.service;

import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private boolean success;
    private Object data;
    private String errorMessage;
    private int statusCode; // 상태 코드 추가

    public static Response success(Object data) {
        return new Response(true, data, null, HttpStatus.OK.value());
    }

    public static Response error(String errorMessage) {
        return new Response(false, null, errorMessage, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}

