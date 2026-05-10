package com.auction.controller;

import com.auction.dao.AccountDAO;
import com.auction.model.Account;
import com.auction.util.CurrentAccount;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class SettingController {

    @FXML private VBox sidebar;
    @FXML private Button btnHome, btnProducts, btnCreate, btnHistory;

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

    private boolean isCollapsed = false;

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

    @FXML
    private void handleSaveInfo() {
        String name = txtFullName.getText();
        String email = txtEmail.getText();

        if (name.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng không để trống Họ tên và Email!");
            return;
        }

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            AccountDAO dao = new AccountDAO();
            // Truyền vào: ID, Tên mới, Email mới
            if (dao.updateProfile(current.getId(), name, email)) {
                // Chỉ khi DB thành công mới cập nhật Session
                current.setUsername(name);
                current.setEmail(email);
                // Cập nhật hiển thị khung bên phải
                if (lblDisplayUsername != null) lblDisplayUsername.setText(name);
                if (lblDisplayEmail != null) lblDisplayEmail.setText(email);

                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã cập nhật thông tin cá nhân vào Database!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể lưu dữ liệu. Hãy kiểm tra lại AccountDAO!");
            }
        }
    }
    @FXML
    private void handleChangePassword() {
        // 1. Lấy dữ liệu từ giao diện
        String current = txtCurrentPass.getText();
        String next = txtNewPass.getText();
        String confirm = txtConfirmPass.getText();

        // 2. Lấy tài khoản hiện tại từ Session
        Account currentAcc = CurrentAccount.getAccount();

        // 3. Kiểm tra các ô nhập liệu có trống không
        if (current.isEmpty() || next.isEmpty() || confirm.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đầy đủ các trường mật khẩu!");
            return;
        }

        // 4. Kiểm tra mật khẩu hiện tại nhập vào có khớp với mật khẩu cũ không
        if (!currentAcc.getPassword().equals(current)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu hiện tại không chính xác!");
            return;
        }

        // 5. Kiểm tra mật khẩu mới và xác nhận mật khẩu
        if (!next.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu mới không khớp!");
            return;
        }

        // 6. Thực hiện cập nhật vào Database qua DAO
        AccountDAO dao = new AccountDAO();
        if (dao.updatePassword(currentAcc.getId(), next)) {
            currentAcc.setPassword(next); // Cập nhật bộ nhớ tạm để đồng bộ
            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Mật khẩu đã được thay đổi và lưu vào hệ thống!");

            txtCurrentPass.clear();
            txtNewPass.clear();
            txtConfirmPass.clear();
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể cập nhật mật khẩu vào cơ sở dữ liệu!");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}