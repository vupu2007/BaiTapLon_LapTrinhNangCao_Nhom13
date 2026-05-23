package com.auction.client.controller;

// Thêm các import cho Network (Bạn hãy điều chỉnh package cho khớp nếu cần)
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;
import com.auction.client.network.ClientSocket;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordTextField;
    @FXML private ToggleButton togglePasswordBtn;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField confirmPasswordTextField;
    @FXML private ToggleButton toggleConfirmPasswordBtn;
    @FXML private TextField emailField;

    // ĐÃ XÓA: private final AccountService accountService = new AccountService();
    // Client không được khởi tạo trực tiếp Service của Server.

    @FXML
    public void initialize() {
        passwordTextField.textProperty().bindBidirectional(passwordField.textProperty());
        confirmPasswordTextField.textProperty().bindBidirectional(confirmPasswordField.textProperty());

        passwordTextField.setVisible(false);
        passwordTextField.setManaged(false);

        confirmPasswordTextField.setVisible(false);
        confirmPasswordTextField.setManaged(false);
    }

    @FXML
    void handleRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String email = emailField.getText().trim();

        // 1. Kiểm tra (Validate) dữ liệu trực tiếp tại Client để giảm tải cho mạng
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi", "Không được để trống thông tin!");
            return;
        }
        if (!password.equals(confirm)) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Mật khẩu xác nhận không khớp!");
            return;
        }

        // 2. Đóng gói dữ liệu thành Request
        String[] registerData = {username, password, email};
        Request request = new Request(MessageType.REGISTER, registerData);

        try {
            // 3. Gửi qua Socket (Giả sử ClientSocket của bạn dùng Singleton Pattern là getInstance())
            // Nếu bạn implement khác, hãy sửa dòng này cho phù hợp với class ClientSocket của bạn.
            Response response = ClientSocket.getInstance().sendRequest(request);

            // 4. Xử lý UI dựa trên Response trả về từ Server
            if (response.isSuccess()) {
                // Sử dụng chính câu thông báo từ Server gửi về cho sinh động
                showAlert(Alert.AlertType.INFORMATION, "Thành công", response.getMessage());
                goToLogin(event);
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", response.getMessage());
            }

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi mạng", "Không thể kết nối đến máy chủ.");
            e.printStackTrace();
        }
    }

    @FXML
    public void goToLogin(ActionEvent event) {
        switchScene(event, "/view/LoginView.fxml", "Đăng nhập");
    }

    @FXML
    private void togglePasswordVisibility() {
        boolean show = togglePasswordBtn.isSelected();

        passwordTextField.setVisible(show);
        passwordTextField.setManaged(show);

        passwordField.setVisible(!show);
        passwordField.setManaged(!show);

        togglePasswordBtn.setText(show ? "👁‍🗨" : "👁");
    }

    @FXML
    private void toggleConfirmPasswordVisibility() {
        boolean show = toggleConfirmPasswordBtn.isSelected();

        confirmPasswordTextField.setVisible(show);
        confirmPasswordTextField.setManaged(show);

        confirmPasswordField.setVisible(!show);
        confirmPasswordField.setManaged(!show);

        toggleConfirmPasswordBtn.setText(show ? "👁‍🗨" : "👁");
    }

    private void switchScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.getScene().setRoot(root);
            stage.setTitle(title);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}