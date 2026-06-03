package com.auction.client;

import com.auction.client.network.ClientSocket;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.*;

public class ClientConnection {
    // 🛑 Không khai báo Socket và Stream riêng nữa để tránh tranh chấp đường truyền với ClientSocket

    public static void connect() {
        // Chuyển hướng kích hoạt instance duy nhất của ClientSocket
        System.out.println("🔄 [ClientConnection] Đang đồng bộ luồng mạng qua ClientSocket lõi...");
        ClientSocket.getInstance();
    }

    public static void send(Request request) {
        try {
            // Mượn Stream đầu ra an toàn từ ClientSocket
            ObjectOutputStream socketOut = ClientSocket.getInstance().getOutputStream();
            if (socketOut != null) {
                synchronized (socketOut) {
                    socketOut.writeObject(request);
                    socketOut.flush();
                    socketOut.reset(); // Đảm bảo clear cache stream tránh tràn bộ nhớ
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 🧩 CẦU NỐI 1: Tiếp nhận dữ liệu từ ClientSocket nếu Server trả về định dạng cũ (Request)
     */
    public static void bridgeServerMessage(Request response) {
        handleServerMessage(response);
    }

    /**
     * 🧩 CẦU NỐI 2: Tiếp nhận dữ liệu từ ClientSocket nếu Server trả về định dạng mới (Response)
     * Giúp ép kiểu ngược lại để tương thích với logic cũ
     */
    public static void bridgeResponseCheck(Response res) {
        if (res != null && ("FORGOT_PASSWORD_SUCCESS".equals(res.getType()) || "OTP_SENT".equals(res.getMessage()))) {
            // Giả lập một Request mang MessageType cũ để kích hoạt hàm handleServerMessage bên dưới
            Request fakeReq = new Request(MessageType.FORGOT_PASSWORD_SUCCESS, null);
            handleServerMessage(fakeReq);
        }
    }

    /**
     * 🔴 LOGIC GỐC CỦA BẠN (GIỮ NGUYÊN 100%): Thực hiện nhiệm vụ in console, hiện Alert và nhảy màn hình
     */
    private static void handleServerMessage(Request response) {
        if (response.getType() == MessageType.FORGOT_PASSWORD_SUCCESS) {
            // 🟥 TEST 1: Xem trên bảng console Client có hiện dòng này không!
            System.out.println("👉 CRITICAL: Đã nhảy vào khối FORGOT_PASSWORD_SUCCESS!");

            Platform.runLater(() -> {
                // 🟥 TEST 2: Xem luồng JavaFX UI có thực sự kích hoạt không!
                System.out.println("👉 CRITICAL: Đang khởi tạo Alert...");

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thành công");
                alert.setHeaderText(null);
                alert.setContentText("Mã OTP đã được gửi thành công vào Email của bạn. Vui lòng kiểm tra hộp thư!");

                System.out.println("👉 CRITICAL: Chuẩn bị gọi lệnh showAndWait(). Màn hình PHẢI hiện pop-up ngay bây giờ!");

                // Lệnh đóng băng thần thánh của code cũ
                alert.showAndWait();

                System.out.println("👉 CRITICAL: Bạn đã bấm nút OK trên Alert! Chuẩn bị nhảy Scene...");

                MainApp.changeScene("/view/ResetPassword.fxml");
            });
        }
    }
}