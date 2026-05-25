package com.auction.client.controller;

import com.auction.client.service.SettingService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;
import com.auction.shared.network.Response;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SettingController {

    @FXML private TextField txtFullName;
    @FXML private TextField txtEmail;
    @FXML private TextField txtPhone; // Giữ lại theo FXML của nhóm

    @FXML private PasswordField txtCurrentPass;
    @FXML private PasswordField txtNewPass;
    @FXML private PasswordField txtConfirmPass;

    @FXML private Label lblDisplayUsername;
    @FXML private Label lblDisplayEmail;
    @FXML private Label lblDisplayRole;
    @FXML private Label lblDisplayBalance;

    // Kích hoạt Service trung gian lo việc điều phối Socket mạng
    private final SettingService settingService = new SettingService();

    @FXML
    public void initialize() {
        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc != null) {
            txtFullName.setText(currentAcc.getUsername());
            txtEmail.setText(currentAcc.getEmail() != null ? currentAcc.getEmail() : "");

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
        String name = txtFullName.getText().trim();
        String email = txtEmail.getText().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng không để trống Họ tên và Email!");
            return;
        }

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            // Đẩy tác vụ lưu dữ liệu mạng chạy ngầm ra lớp Service
            settingService.updateProfileAsync(current.getId(), name, email, response -> {
                if (response != null && response.isSuccess()) {
                    // Khi DB Server báo thành công -> Mới cập nhật dữ liệu bộ nhớ tạm Client (Session)
                    current.setUsername(name);
                    current.setEmail(email);

                    if (lblDisplayUsername != null) lblDisplayUsername.setText(name);
                    if (lblDisplayEmail != null) lblDisplayEmail.setText(email);

                    showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân hệ thống!");
                } else {
                    String errorMsg = (response != null) ? response.getMessage() : "Mất kết nối server.";
                    showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu dữ liệu: " + errorMsg);
                }
            });
        }
    }

    /**
     * 🔑 XỬ LÝ ĐỔI MẬT KHẨU AN TOÀN BẢO MẬT HỆ THỐNG
     */
    @FXML
    private void handleChangePassword() {
        String currentPass = txtCurrentPass.getText();
        String newPass = txtNewPass.getText();
        String confirmPass = txtConfirmPass.getText();

        Account currentAcc = CurrentAccount.getAccount();
        if (currentAcc == null) return;

        // 1. Kiểm tra các ô nhập liệu tại chỗ
        if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi form", "Vui lòng nhập đầy đủ các trường mật khẩu!");
            return;
        }

        // 2. Kiểm tra tính trùng khớp mật khẩu mới tại Client trước
        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi xác nhận", "Mật khẩu mới không khớp! Vui lòng nhập lại.");
            return;
        }

        // 3. CHUẨN BẢO MẬT: Bắn cả mật khẩu cũ lên Server để Server lo việc đối chiếu bằng Bcrypt/SHA trong DB
        settingService.changePasswordAsync(currentAcc.getId(), currentPass, newPass, response -> {
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
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}