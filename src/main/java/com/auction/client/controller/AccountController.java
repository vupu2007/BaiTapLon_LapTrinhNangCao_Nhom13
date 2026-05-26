package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;

public class AccountController {

    /**
     * 🌟 CHUẨN HÓA ĐĂNG NHẬP: Trả về đối tượng Response đầy đủ.
     * Giúp UI lấy được thông báo lỗi chi tiết từ Server hoặc lỗi mất kết nối mạng.
     */
    public Response loginUser(String username, String password) {
        System.out.println("[AccountController] Nhận request login cho user: " + username);
        String[] data = {username, password};
        Request request = new Request(MessageType.LOGIN, data);

        try {
            Response response = ClientSocket.getInstance().sendRequest(request);
            if (response != null) {
                return response;
            }
            // Trường hợp Server bị ngắt ngầm không trả về dữ liệu
            return new Response(false, "Không nhận được phản hồi từ máy chủ!", null);

        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối server (login): " + e.getMessage());
            // Tự tạo Response báo lỗi mạng để UI hiển thị trực quan
            return new Response(false, "Mất kết nối: Không thể kết nối tới máy chủ!", null);
        }
    }

    /**
     * 🌟 CHUẨN HÓA ĐĂNG KÝ: Trả về đối tượng Response đầy đủ.
     * Giúp UI phân biệt được lỗi trùng Username, trùng Email hay lỗi hệ thống.
     */
    public Response registerUser(String username, String password, String email) {
        System.out.println("[AccountController] Nhận request register cho user: " + username);
        String[] data = {username, password, email};
        Request request = new Request(MessageType.REGISTER, data);

        try {
            Response response = ClientSocket.getInstance().sendRequest(request);
            if (response != null) {
                return response;
            }
            return new Response(false, "Không nhận được phản hồi từ máy chủ!", null);

        } catch (Exception e) {
            System.err.println("❌ Lỗi kết nối server (register): " + e.getMessage());
            return new Response(false, "Mất kết nối: Không thể kết nối tới máy chủ!", null);
        }
    }
}