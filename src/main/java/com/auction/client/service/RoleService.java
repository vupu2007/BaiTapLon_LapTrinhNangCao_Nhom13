package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import java.util.function.Consumer;

public class RoleService {

    public void switchRoleAsync(int accountId, String role, Consumer<Response> callback) {
        Thread worker = new Thread(() -> {
            Response resp = null;
            try {
                resp = ClientSocket.getInstance()
                        .sendRequest(new Request(MessageType.SWITCH_ROLE,
                                new Object[]{accountId, role}));
            } catch (Exception e) {
                System.err.println("❌ [RoleService] switchRole: " + e.getMessage());
            }
            final Response finalResp = resp;
            Platform.runLater(() -> callback.accept(finalResp));
        }, "RoleSwitchWorker");
        worker.setDaemon(true);
        worker.start();
    }
}