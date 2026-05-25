package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.util.function.Consumer;

public class SettingService {

    /**
     * Gửi yêu cầu cập nhật thông tin cá nhân (Profile) lên Server ngầm
     */
    public void updateProfileAsync(String accountId, String fullName, String email, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                Request req = new Request(MessageType.UPDATE_PROFILE, new String[]{accountId, fullName, email});
                resp = ClientSocket.getInstance().sendRequest(req);
            } catch (Exception ex) {
                System.err.println("❌ [SettingService] Lỗi cập nhật profile: " + ex.getMessage());
            }
            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "ProfileUpdateWorker");

        worker.setDaemon(true);
        worker.start();
    }

    /**
     * Gửi yêu cầu đổi mật khẩu lên Server ngầm (Server lo check mật khẩu cũ)
     */
    public void changePasswordAsync(String accountId, String currentPass, String newPass, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                // Gửi cả ID, mật khẩu cũ thô (để server hash check) và mật khẩu mới
                Request req = new Request(MessageType.CHANGE_PASSWORD, new String[]{accountId, currentPass, newPass});
                resp = ClientSocket.getInstance().sendRequest(req);
            } catch (Exception ex) {
                System.err.println("❌ [SettingService] Lỗi đổi mật khẩu: " + ex.getMessage());
            }
            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "PasswordChangeWorker");

        worker.setDaemon(true);
        worker.start();
    }
}