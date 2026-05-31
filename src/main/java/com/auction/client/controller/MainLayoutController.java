package com.auction.client.controller;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import com.auction.client.util.CurrentAccount;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.application.Platform;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainLayoutController {

    @FXML private StackPane contentArea;
    @FXML private VBox buyerMenu, sellerMenu, adminMenu, roleBox; // Đã thêm adminMenu vào đây
    @FXML private Label lblRoleSidebar, lblClock, nameLabel;
    @FXML private MenuButton roleMenuButton;

    // ================= BUTTON MENU =================
    @FXML private Button btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings;
    @FXML private Button btnDashboard, btnUserMgmt; // Khai báo thêm các nút đặc quyền Admin

    private Timeline clockTimeline;

    @FXML
    public void initialize() {
        startRealtimeClock();

        // Kiểm tra thông tin tài khoản hiện tại từ bộ nhớ tạm (CurrentAccount)
        if (CurrentAccount.getAccount() != null) {
            String username = CurrentAccount.getAccount().getUsername();
            String role = CurrentAccount.getAccount().getRole(); // Giả định Model Account của bạn có hàm getRole()

            nameLabel.setText("👤 " + username);

            // PHÂN QUYỀN GIAO DIỆN NGAY KHI KHỞI TẠO
            if ("ADMIN".equalsIgnoreCase(role)) {
                setupAdminUI();
            } else {
                setupUserUI();
            }
        } else {
            // Trường hợp dự phòng nếu chưa có session tài khoản
            setupUserUI();
        }
    }

    /**
     * Cấu hình giao diện đặc quyền cho Admin
     */
    private void setupAdminUI() {
        if (lblRoleSidebar != null) lblRoleSidebar.setText("🔑 Admin");

        // Ẩn Menu đổi vai trò Người mua / Người bán (Vì Admin có vai trò cố định)
        if (roleMenuButton != null) {
            roleMenuButton.setVisible(false);
            roleMenuButton.setManaged(false);
        }

        // Ẩn các menu giao dịch của người dùng thường
        if (buyerMenu != null) { buyerMenu.setVisible(false); buyerMenu.setManaged(false); }
        if (sellerMenu != null) { sellerMenu.setVisible(false); sellerMenu.setManaged(false); }

        // BẬT MENU QUẢN TRỊ ADMIN
        if (adminMenu != null) {
            adminMenu.setVisible(true);
            adminMenu.setManaged(true);
        }

        if (roleBox != null) {
            roleBox.getStyleClass().removeAll("role-buyer", "role-seller");
            roleBox.getStyleClass().add("role-admin"); // Bạn có thể định nghĩa thêm màu nền cho Admin trong style.css
        }

        // Mặc định mở trang Tổng quan khi Admin vừa vào hệ thống
        openDashboard();
    }

    /**
     * Giao diện chuẩn dành cho User thường (Buyer / Seller)
     */
    private void setupUserUI() {
        if (adminMenu != null) {
            adminMenu.setVisible(false);
            adminMenu.setManaged(false);
        }
        if (roleMenuButton != null) {
            roleMenuButton.setVisible(true);
            roleMenuButton.setManaged(true);
        }
        // Mặc định ban đầu cho User thường là vai trò người mua
        switchToBuyer();
    }

    /**
     * Đồng hồ thời gian thực được quản lý vòng đời chặt chẽ, tự tắt khi logout để tránh ngốn RAM ngầm
     */
    private void startRealtimeClock() {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        clockTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    if (lblClock != null) {
                        lblClock.setText(LocalDateTime.now().format(formatter));
                    }
                })
        );
        clockTimeline.setCycleCount(Animation.INDEFINITE);
        clockTimeline.play();
    }

    /**
     * 🚀 CHUẨN XỬ LÝ DỰ ÁN LỚN: Tải FXML bất đồng bộ thông qua JavaFX Task
     */
    private void loadPageAsync(String fxmlPath) {
        Task<Parent> loadTask = new Task<> () {
            @Override
            protected Parent call() throws Exception {
                URL fxmlUrl = getClass().getResource(fxmlPath);
                if (fxmlUrl == null) {
                    throw new IllegalArgumentException("Không tìm thấy file FXML tại đường dẫn: " + fxmlPath);
                }
                FXMLLoader loader = new FXMLLoader(fxmlUrl);
                return loader.load();
            }
        };

        loadTask.setOnSucceeded(workerStateEvent -> {
            Parent page = loadTask.getValue();
            if (contentArea != null && page != null) {
                contentArea.getChildren().clear();

                if (page instanceof Region region) {
                    region.prefWidthProperty().bind(contentArea.widthProperty());
                    region.prefHeightProperty().bind(contentArea.heightProperty());
                    region.setMaxWidth(Double.MAX_VALUE);
                    region.setMaxHeight(Double.MAX_VALUE);
                }

                StackPane.setAlignment(page, Pos.TOP_CENTER);
                contentArea.getChildren().add(page);
            }
        });

        loadTask.setOnFailed(workerStateEvent ->
                System.err.println("❌ Lỗi nghiêm trọng khi tải giao diện: " + fxmlPath + " -> " + loadTask.getException().getMessage())
        );

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    public void setContent(Node content) {
        if (contentArea != null && content != null) {
            if (Platform.isFxApplicationThread()) {
                contentArea.getChildren().setAll(content);
            } else {
                Platform.runLater(() -> contentArea.getChildren().setAll(content));
            }
        }
    }

    /**
     * Quản lý trạng thái Active: Đã thêm btnDashboard và btnUserMgmt để quản lý hiệu ứng đổi màu nút bấm của Admin
     */
    private void setActive(Button activeButton) {
        Button[] buttons = {btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings, btnDashboard, btnUserMgmt};
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
                if (!btn.getStyleClass().contains("nav-button")) {
                    btn.getStyleClass().add("nav-button");
                }
            }
        }
        if (activeButton != null) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    // ================= CHUYỂN TRANG CHI TIẾT ĐẤU GIÁ =================

    public void openAuctionDetailWithObject(Object data) {
        if (data == null) return;

        String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

        setActive(btnAuction);

        Task<Parent> loadTask = new Task<>() {
            @Override
            protected Parent call() throws Exception {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent page = loader.load();

                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    if (data instanceof com.auction.shared.model.Auction a) {
                        detailController.loadProductDetail(a);
                    } else if (data instanceof com.auction.shared.model.Item i) {
                        detailController.loadProductDetail(i);
                    }
                }
                return page;
            }
        };

        loadTask.setOnSucceeded(e -> setContent(loadTask.getValue()));
        loadTask.setOnFailed(e -> System.err.println("❌ Không thể nạp trang chi tiết sản phẩm: " + loadTask.getException().getMessage()));

        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ================= SỰ KIỆN ĐIỀU HƯỚNG USER THƯỜNG =================

    @FXML public void openHome() { setActive(btnHome); loadPageAsync("/view/MainView.fxml"); }
    @FXML public void openSelling() { setActive(btnSelling); loadPageAsync("/view/MyProducts.fxml"); }
    @FXML private void openWallet() { setActive(btnWallet); loadPageAsync("/view/WalletView.fxml"); }
    @FXML private void openAuction() { setActive(btnAuction); loadPageAsync("/view/ActiveAuctions.fxml"); }
    @FXML private void openCreateAuction() { setActive(btnCreateAuction); loadPageAsync("/view/CreateAuction.fxml"); }
    @FXML private void openHistory() { setActive(btnHistory); loadPageAsync("/view/HistoryView.fxml"); }
    @FXML private void openSettings() { setActive(btnSettings); loadPageAsync("/view/Settings.fxml"); }

    public void showCreateProductView() { openCreateAuction(); }

    // ================= SỰ KIỆN ĐIỀU HƯỚNG DÀNH RIÊNG ADMIN =================

    @FXML
    public void openDashboard() {
        setActive(btnDashboard);
        loadPageAsync("/view/AdminDashboard.fxml"); // Hoặc đường dẫn file Dashboard admin của bạn
    }

    @FXML
    public void openUserMgmt() {
        setActive(btnUserMgmt);
        loadPageAsync("/view/UserManagement.fxml"); // Hoặc đường dẫn file Quản lý user của bạn
    }

    // ================= CẤU HÌNH PHÂN QUYỀN VAI TRÒ QUA CSS CLASS =================

    @FXML
    private void switchToBuyer() {
        updateRoleUI("role-buyer", "🛒 Người mua", true, false);
    }

    @FXML
    private void switchToSeller() {
        updateRoleUI("role-seller", "🏪 Người bán", false, true);
    }

    private void updateRoleUI(String cssClass, String roleText, boolean showBuyer, boolean showSeller) {
        if (lblRoleSidebar != null) lblRoleSidebar.setText(roleText);
        if (roleMenuButton != null) roleMenuButton.setText("🔄 " + roleText.substring(2));

        if (roleBox != null) {
            roleBox.getStyleClass().removeAll("role-buyer", "role-seller", "role-admin");
            roleBox.getStyleClass().add(cssClass);
        }
        if (buyerMenu != null) { buyerMenu.setVisible(showBuyer); buyerMenu.setManaged(showBuyer); }
        if (sellerMenu != null) { sellerMenu.setVisible(showSeller); sellerMenu.setManaged(showSeller); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (clockTimeline != null) {
            clockTimeline.stop();
        }

        try {
            CurrentAccount.setAccount(null);
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
            stage.setTitle("Đăng nhập");
        } catch (IOException e) {
            System.err.println("❌ Lỗi đăng xuất hệ thống: " + e.getMessage());
        }
    }
}