package com.auction.shared.network;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type;
    private Object payload;
    private String requestId; // 🚀 MẢNH GHÉP MỚI: Định danh điều hướng đa luồng song song

    public Request(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getter/Setter cho thuộc tính mới
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    // Giữ nguyên toàn bộ cấu trúc cũ để không lỗi dự án
    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
}