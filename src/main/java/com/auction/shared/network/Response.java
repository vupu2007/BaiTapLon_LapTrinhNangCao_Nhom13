package com.auction.shared.network;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;

    // BẠN HÃY SỬA LẠI CONSTRUCTOR THEO ĐÚNG THỨ TỰ NÀY: (boolean, String, Object)
    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Các Getters và Setters giữ nguyên
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}