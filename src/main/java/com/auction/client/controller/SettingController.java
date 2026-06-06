package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.SettingService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Account;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
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
        System.out.println("DEBUG class=" + currentAcc.getClass().getSimpleName());
        System.out.println("DEBUG role text=" + (currentAcc instanceof com.auction.shared.model.Seller ? "Người bán" : "Người mua"));
        if (currentAcc == null) return;

        if (txtFullName != null) txtFullName.setText(currentAcc.getUsername());
        if (txtEmail != null) txtEmail.setText(currentAcc.getEmail() != null ? currentAcc.getEmail() : "");
        if (lblDisplayUsername != null) lblDisplayUsername.setText(currentAcc.getUsername());
        if (lblDisplayEmail != null) lblDisplayEmail.setText(currentAcc.getEmail() != null ? currentAcc.getEmail() : "");
        if (lblDisplayRole != null) {
            String roleText = currentAcc instanceof com.auction.shared.model.Bidder ? "Người mua"
                    : currentAcc instanceof com.auction.shared.model.Seller ? "Người bán"
                    : "Quản trị viên";
            lblDisplayRole.setText(roleText);
        }
        if (lblDisplayBalance != null) lblDisplayBalance.setText("Đang tải...");

        new Thread(() -> {
            try {
                Response resp = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_ACCOUNT_BY_ID, Integer.parseInt(currentAcc.getId())));
                if (resp != null && resp.isSuccess() && resp.getData() instanceof Account fresh) {
                    double balance = fresh instanceof com.auction.shared.model.User u ? u.getBalance() : 0;
                    Platform.runLater(() -> {
                        if (lblDisplayBalance != null)
                            lblDisplayBalance.setText(String.format("%,.0f đ", balance));
                    });
                }
            } catch (Exception ignored) {}
        }).start();
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
                        // 1. Cập nhật dữ liệu Session Client toàn cục
                        current.setUsername(name);
                        current.setEmail(email);

                        // 2. Cập nhật nhãn tại màn hình cài đặt hiện tại
                        if (lblDisplayUsername != null) lblDisplayUsername.setText(name);
                        if (lblDisplayEmail != null) lblDisplayEmail.setText(email);

                        // 3. 🚀 ĐỒNG BỘ TOÀN CỤC KHÔNG DÙNG SINGLETON (Scene Graph Lookup)
                        // Định vị trực tiếp Label hiển thị thông tin tài khoản trên thanh Top-Bar/Sidebar của Layout cha
                        if (txtFullName.getScene() != null) {
                            Parent root = txtFullName.getScene().getRoot();

                            // Tìm kiếm thẻ Label hiển thị Username trên Main Layout (Hãy đảm bảo ID fx:id="lblHeaderUsername" khớp với FXML)
                            Node headerUserLabel = root.lookup("#lblHeaderUsername");
                            if (headerUserLabel == null) {
                                headerUserLabel = root.lookup("#lblUsername"); // Dự phòng ID thay thế khác
                            }

                            if (headerUserLabel instanceof Label lblHeader) {
                                lblHeader.setText("👤 " + name); // Đồng bộ chữ ngay lập tức mà không gây giật màn hình
                                System.out.println("🎯 [Sync] Đã cập nhật đồng bộ tên tài khoản lên thanh tiêu đề chính.");
                            }
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