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
    @FXML private VBox buyerMenu, sellerMenu, roleBox;
    @FXML private Label lblRoleSidebar, lblClock, nameLabel;
    @FXML private MenuButton roleMenuButton;

    // ================= BUTTON MENU =================
    @FXML private Button btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings;

    private Timeline clockTimeline;

    @FXML
    public void initialize() {
        if (CurrentAccount.getAccount() != null) {
            nameLabel.setText("👤 " + CurrentAccount.getAccount().getUsername());
        }
        startRealtimeClock();

        // Mở trang chủ ngay khi giao diện tổng thể vừa khởi tạo xong
        openHome();
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
     * Giúp UI chính không bao giờ bị đơ (freeze) khi load các trang có giao diện nặng.
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
                return loader.load(); // Load file thô ở luồng ngầm (I/O Thread)
            }
        };

        // Khi tiến trình chạy ngầm thành công, đẩy giao diện mới lên UI Thread tại đây
        loadTask.setOnSucceeded(workerStateEvent -> {
            Parent page = loadTask.getValue();
            if (contentArea != null && page != null) {
                contentArea.getChildren().clear();

                // Tự động co giãn kích thước linh hoạt theo khung contentArea cha
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

        // Xử lý khi xảy ra lỗi load file hệ thống
        loadTask.setOnFailed(workerStateEvent ->
                System.err.println("❌ Lỗi nghiêm trọng khi tải giao diện: " + fxmlPath + " -> " + loadTask.getException().getMessage())
        );

        // Kích hoạt chạy luồng ngầm an toàn (Daemon Thread)
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Nhận trực tiếp một Node giao diện từ bên ngoài (Ví dụ từ trang MainController đẩy qua)
     */
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
     * Quản lý trạng thái Active của các nút Menu thông qua CSS class sạch sẽ
     */
    private void setActive(Button activeButton) {
        Button[] buttons = {btnHome, btnWallet, btnAuction, btnSelling, btnCreateAuction, btnHistory, btnSettings};
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

    /**
     * Điều hướng sang trang chi tiết bằng cách nạp bất đồng bộ và truyền thẳng Model dữ liệu vào Controller mới
     */
    public void openAuctionDetailWithObject(Object data) {
        if (data == null) return;

        String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

        setActive(btnAuction);

        // Đưa việc biên dịch FXML chi tiết nặng nề xuống luồng nền
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

    // ================= SỰ KIỆN MENU ĐIỀU HƯỚNG SẠCH SẼ (Bỏ hoàn toàn runLater thừa) =================

    @FXML public void openHome() { setActive(btnHome); loadPageAsync("/view/MainView.fxml"); }
    @FXML public void openSelling() { setActive(btnSelling); loadPageAsync("/view/MyProducts.fxml"); }
    @FXML private void openWallet() { setActive(btnWallet); loadPageAsync("/view/WalletView.fxml"); }
    @FXML private void openAuction() { setActive(btnAuction); loadPageAsync("/view/ActiveAuctions.fxml"); }
    @FXML private void openCreateAuction() { setActive(btnCreateAuction); loadPageAsync("/view/CreateAuction.fxml"); }
    @FXML private void openHistory() { setActive(btnHistory); loadPageAsync("/view/HistoryView.fxml"); }
    @FXML private void openSettings() { setActive(btnSettings); loadPageAsync("/view/Settings.fxml"); }

    public void showCreateProductView() { openCreateAuction(); }

    // ================= CẤU HÌNH PHÂN QUYỀN VAI TRÒ QUA CSS CLASS =================

    @FXML
    private void switchToBuyer() {
        updateRoleUI("role-buyer", "🛒 Người mua", true, false);
        openHome();
    }

    @FXML
    private void switchToSeller() {
        updateRoleUI("role-seller", "🏪 Người bán", false, true);
        openHome();
    }
    /**
     * Tối ưu hóa UI: Thay đổi theme bằng CSS Class thay vì viết code màu Hex cứng trong Java
     */
    private void updateRoleUI(String cssClass, String roleText, boolean showBuyer, boolean showSeller) {
        if (lblRoleSidebar != null) lblRoleSidebar.setText(roleText);
        if (roleMenuButton != null) roleMenuButton.setText("🔄 " + roleText.substring(2));

        if (roleBox != null) {
            roleBox.getStyleClass().removeAll("role-buyer", "role-seller");
            roleBox.getStyleClass().add(cssClass);
        }
        if (buyerMenu != null) { buyerMenu.setVisible(showBuyer); buyerMenu.setManaged(showBuyer); }
        if (sellerMenu != null) { sellerMenu.setVisible(showSeller); sellerMenu.setManaged(showSeller); }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        // Giải phóng luồng và dập tắt đồng hồ chạy ngầm hoàn toàn chống tràn RAM hệ thống
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