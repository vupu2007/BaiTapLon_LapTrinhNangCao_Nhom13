package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.shared.model.Account;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.util.function.Consumer;

public class AccountController {

    /**
     * 🚀 ĐĂNG NHẬP BẤT ĐỒNG BỘ: Chạy ngầm tránh đơ giao diện UI JavaFX
     */
    public void loginUserAsync(String username, String password, Consumer<Account> callback) {
        System.out.println("🔄 [AccountController] Đang xử lý đăng nhập chạy ngầm...");

        Thread worker = new Thread(() -> {
            String[] data = {username, password};
            // Khởi tạo đúng cấu trúc Constructor (MessageType, Object) của bạn
            Request request = new Request(MessageType.LOGIN, data);
            Account accountResult = null;

            try {
                Response response = ClientSocket.getInstance().sendRequest(request);
                // Gọi chính xác hàm isSuccess() và getData() từ file Response.java của bạn
                if (response != null && response.isSuccess()) {
                    accountResult = (Account) response.getData();
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi kết nối server (login): " + e.getMessage());
            }

            // Đẩy kết quả an toàn quay ngược lại luồng hiển thị JavaFX UI
            final Account finalAccount = accountResult;
            Platform.runLater(() -> callback.accept(finalAccount));
        }, "LoginWorkerThread");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 🚀 ĐĂNG KÝ BẤT ĐỒNG BỘ: Chạy ngầm tránh treo ứng dụng
     */
    public void registerUserAsync(String username, String password, String role, Consumer<Boolean> callback) {
        System.out.println("🔄 [AccountController] Đang xử lý đăng ký chạy ngầm...");

        Thread worker = new Thread(() -> {
            String[] data = {username, password, role};
            Request request = new Request(MessageType.REGISTER, data);
            boolean isSuccess = false;

            try {
                Response response = ClientSocket.getInstance().sendRequest(request);
                if (response != null) {
                    isSuccess = response.isSuccess();
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi kết nối server (register): " + e.getMessage());
            }

            // Trả kết quả true/false về cho giao diện xử lý hiện thông báo thành công/thất bại
            final boolean finalSuccess = isSuccess;
            Platform.runLater(() -> callback.accept(finalSuccess));
        }, "RegisterWorkerThread");

        worker.setDaemon(true);
        worker.start();
    }
}