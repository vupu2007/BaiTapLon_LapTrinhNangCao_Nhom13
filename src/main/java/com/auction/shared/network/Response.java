package com.auction.shared.network;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;
    private boolean success;
    private String message;
    private Object data;
    private String type;
    private String requestId; // 🚀 MẢNH GHÉP MỚI: Đồng bộ khớp mã với Request tương ứng

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    // Getter/Setter cho thuộc tính mới
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    // Giữ nguyên toàn bộ code cũ của nhóm ông
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}