package com.bangla.karneval.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse {
    private boolean success;
    private String  message;
    private Object  data;

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static ApiResponse ok(String message)    { return new ApiResponse(true,  message); }
    public static ApiResponse error(String message) { return new ApiResponse(false, message); }
}
