package com.auction.client.controller;

import com.auction.client.service.SettingService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SettingController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone;

    @FXML private PasswordField txtCurrentPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    @FXML private Label lblDisplayUsername;
    @FXML private Label lblDisplayEmail;
    @FXML private Label lblDisplayRole;
    @FXML private Label lblDisplayBalance;

    private final SettingService settingService = new SettingService();

    @FXML
    public void initialize() {
        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc != null) {
            // 🌟 PHÒNG VỆ: Bọc an toàn chống lỗi Null-Pointer khi nạp form
            if (txtFullName != null) txtFullName.setText(currentAcc.getUsername());
            if (txtEmail != null) txtEmail.setText(currentAcc.getEmail() != null ? currentAcc.getEmail() : "");
            if (txtPhone != null) txtPhone.setText(""); // Điểm mở rộng cho số điện thoại nếu cần

            if (lblDisplayUsername != null) lblDisplayUsername.setText(currentAcc.getUsername());
            if (lblDisplayEmail != null) lblDisplayEmail.setText(currentAcc.getEmail());
            if (lblDisplayRole != null) lblDisplayRole.setText(currentAcc.displayRole());
            if (lblDisplayBalance != null) lblDisplayBalance.setText("0 đ");
        }
    }

    /**
     * 📝 XỬ LÝ LƯU THÔNG TIN CÁ NHÂN CHUẨN ENTERPRISE
     */
    @FXML
    private void handleSaveInfo() {
        if (txtFullName == null || txtEmail == null) return;

        String name = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng không để trống Họ tên và Email!");
            return;
        }

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            settingService.updateProfileAsync(current.getId(), name, email, response -> {

                // 🌟 CRITICAL FIX: Đẩy toàn bộ tác vụ cập nhật UI và thông báo về luồng chính JavaFX
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        // 1. Cập nhật dữ liệu Session Client
                        current.setUsername(name);
                        current.setEmail(email);

                        // 2. Cập nhật nhãn tại màn hình cài đặt hiện tại
                        if (lblDisplayUsername != null) lblDisplayUsername.setText(name);
                        if (lblDisplayEmail != null) lblDisplayEmail.setText(email);

                        // 3. 🚀 ĐỒNG BỘ TOÀN CỤC: Gọi cập nhật trực tiếp lên thanh tiêu đề Main Layout
                        if (MainLayoutController.getInstance() != null) {
                            // Ép MainLayout cập nhật lại chữ hiển thị "👤 Tên người dùng" ngay lập tức
                            MainLayoutController.getInstance().initialize();
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân hệ thống!");
                    } else {
                        String errorMsg = (response != null) ? response.getMessage() : "Mất kết nối server.";
                        showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu dữ liệu: " + errorMsg);
                    }
                });
            });
        }
    }

    /**
     * 🔑 XỬ LÝ ĐỔI MẬT KHẨU AN TOÀN BẢO MẬT HỆ THỐNG
     */
    @FXML
    private void handleChangePassword() {
        if (txtCurrentPass == null || txtNewPass == null || txtConfirmPass == null) return;

        String currentPass = txtCurrentPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc == null) return;

        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi form", "Vui lòng nhập đầy đủ các trường mật khẩu!");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi xác nhận", "Mật khẩu mới không khớp! Vui lòng nhập lại.");
            return;
        }

        settingService.changePasswordAsync(currentAcc.getId(), currentPass, newPass, response -> {

            // 🌟 CRITICAL FIX: Đẩy thông báo và lệnh clear form về luồng chính để chống treo luồng mạng
            Platform.runLater(() -> {
                if (response != null && response.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu đã được thay đổi và lưu vào hệ thống!");

                    txtCurrentPass.clear();
                    txtNewPass.clear();
                    txtConfirmPass.clear();
                } else {
                    String errorMsg = (response != null) ? response.getMessage() : "Mật khẩu hiện tại không chính xác hoặc lỗi DB.";
                    showAlert(Alert.AlertType.ERROR, "Lỗi cập nhật", errorMsg);
                }
            });
        });
    }

    /**
     * Tiện ích bọc hiển thị thông báo an toàn đa luồng tuyệt đối
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}