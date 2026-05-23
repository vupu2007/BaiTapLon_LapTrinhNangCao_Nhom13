package com.auction.shared.network;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID = 1L;
    private MessageType type; // Sử dụng Enum thay cho String
    private Object payload;

    public Request(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public MessageType getType() { return type; }
    public Object getPayload() { return payload; }
}