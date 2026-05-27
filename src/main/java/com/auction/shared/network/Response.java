package com.auction.shared.network;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;
    private String type;
    private String requestId;

    // Constructor 1: 3 tham số chuẩn của nhóm ông
    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // 🎯 CONSTRUCTOR NẠP CHỒNG (MỚI): Nhận 2 tham số để bên Server gọi không bị lỗi
    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.data = null;
    }

    // Getter/Setter cho thuộc tính mới
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}