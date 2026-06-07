package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

public class AccountService {

    private static final Logger LOGGER = Logger.getLogger(AccountService.class.getName());
    private static final String SERVER_NO_RESPONSE = "Không nhận được phản hồi từ máy chủ!";
    private static final String SERVER_CONNECTION_ERROR = "Mất kết nối: Không thể kết nối tới máy chủ!";

    /**
     * Xử lý logic Đăng nhập
     */
    public Response loginUser(String username, String password) {
        Request request = new Request(MessageType.LOGIN, new String[]{username, password});
        return executeNetworkRequest(request, "login");
    }

    public Response registerUser(String username, String password, String email) {
        Request request = new Request(MessageType.REGISTER, new String[]{username, password, email});
        return executeNetworkRequest(request, "register");
    }

    public Response forgotPassword(String username, String email) {
        Request request = new Request(MessageType.FORGOT_PASSWORD, new String[]{username, email});
        return executeNetworkRequest(request, "forgotPassword");
    }

    private Response executeNetworkRequest(Request request, String context) {
        // Đây là nơi tập trung logic xử lý mạng
        try {
            Response response = ClientSocket.getInstance().sendRequest(request);
            return (response != null) ? response : new Response(false, SERVER_NO_RESPONSE, null);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format("❌ Lỗi mạng trong tiến trình (%s): %s", context, e.getMessage()), e);
            return new Response(false, SERVER_CONNECTION_ERROR, null);
        }
    }
}