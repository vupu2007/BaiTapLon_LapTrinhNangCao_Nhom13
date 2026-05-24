package com.auction.client.controller;

import com.auction.shared.model.Account;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.client.network.ClientSocket;

public class AccountController {

    public Account loginUser(String username, String password) {
        System.out.println("Controller nhận request login");
        String[] data = {username, password};
        Request request = new Request(MessageType.LOGIN, data);
        try {
            Response response = ClientSocket.getInstance().sendRequest(request);
            if (response != null && response.isSuccess()) {
                return (Account) response.getData();
            }
        } catch (Exception e) {
            System.err.println("Lỗi kết nối server (login): " + e.getMessage());
        }
        return null;
    }

    public boolean registerUser(String username, String password, String role) {
        System.out.println("Controller nhận request register");
        String[] data = {username, password, role};
        Request request = new Request(MessageType.REGISTER, data);
        try {
            Response response = ClientSocket.getInstance().sendRequest(request);
            return response != null && response.isSuccess();
        } catch (Exception e) {
            System.err.println("Lỗi kết nối server (register): " + e.getMessage());
        }
        return false;
    }
}
