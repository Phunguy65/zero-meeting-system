package com.example.zeromeeting.core.network;

import com.google.gson.annotations.SerializedName;

public class JsendResponse<T> {

    @SerializedName("status")
    private String status; // Thường là "success", "fail", hoặc "error"

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private T data; // Dữ liệu thật sẽ được bọc ở đây

    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public T getData() { return data; }

    public boolean isSuccess() {
        return "success".equalsIgnoreCase(status);
    }
}
