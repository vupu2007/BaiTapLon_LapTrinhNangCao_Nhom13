package com.auction.client.controller;

import com.auction.client.service.MainDashboardService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel, ongoingLabel, wonLabel, welcomeLabel;
    @FXML private Button btnFilterAll, btnFilterActive, btnFilterUpcoming;
    @FXML private FlowPane flowPane;

    private static MainController instance;
    private String currentFilter = "ALL";

    private final MainDashboardService dashboardService = new MainDashboardService();
    private static final String SERVER_IMAGE_BASE_URL = "http://localhost:8080/uploads/";

    // 🚀 TỐI ƯU DỰ ÁN LỚN: Định vị sẵn FXML mẫu ngay khi khởi chạy class, tránh quét bộ nhớ nhiều lần
    private static java.net.URL cachedFxmlLocation;

    public MainController() {}

    @FXML
    public void initialize() {
        instance = this;

        // Khởi tạo cache đường dẫn FXML một lần duy nhất
        if (cachedFxmlLocation == null) {
            cachedFxmlLocation = getClass().getResource("/view/ProductCard.fxml");
            if (cachedFxmlLocation == null) cachedFxmlLocation = getClass().getResource("/com/auction/client/view/ProductCard.fxml");
            if (cachedFxmlLocation == null) cachedFxmlLocation = getClass().getResource("ProductCard.fxml");
            if (cachedFxmlLocation == null) cachedFxmlLocation = getClass().getResource("/ProductCard.fxml");
        }

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            refreshDashboard(); // Gọi trực tiếp, việc chia luồng đã có dashboardService lo
        }
    }

    public static MainController getInstance() {
        return instance;
    }

    /**
     * Tải lại toàn bộ dữ liệu thống kê và danh sách sản phẩm từ cơ sở dữ liệu (Đã tối ưu hóa luồng ngầm hoàn toàn)
     */
    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();
        if (current == null) return;

        if (balanceLabel != null) {
            balanceLabel.setText(current instanceof User
                    ? String.format("%,.0f VND", ((User) current).getBalance()) : "N/A");
        }

        // Tải dữ liệu bất đồng bộ (Chạy trên luồng nền của Service)
        dashboardService.fetchDashboardDataAsync(current.getId(), currentFilter, (stats, items) -> {

            // 🧠 CHIẾN LƯỢC DỰ ÁN LỚN: Nạp FXML ngay trên LUỒNG NGẦM này trước khi đẩy về UI Thread
            List<VBox> renderedCards = new ArrayList<>();
            if (items != null && cachedFxmlLocation != null) {
                for (Item item : items) {
                    if (item == null) continue;
                    try {
                        // Đọc file FXML thô từ ổ cứng/bộ nhớ tại đây (Không gây block UI)
                        FXMLLoader loader = new FXMLLoader(cachedFxmlLocation);
                        VBox cardLayout = loader.load();

                        // Đổ dữ liệu vào Controller của Card
                        ProductCardController cardController = loader.getController();
                        String finalImageUrl = getFinalImageUrl(item.getImagePath());

                        String statusText = "UPCOMING".equals(currentFilter) || (item.getDescription() != null && item.getDescription().toLowerCase().contains("sắp diễn ra"))
                                ? "Sắp diễn ra" : "Đang diễn ra";

                        cardController.setData(item.getName(), String.format("%,.0f đ", item.getStartingPrice()), statusText, finalImageUrl, item.getDescription(), "Người bán ẩn", "", "");

                        // Cài đặt sự kiện click
                        cardLayout.setOnMouseClicked(e -> showAuctionDetail(item));
                        cardLayout.setStyle(cardLayout.getStyle() + "; -fx-cursor: hand;");
                        bindCardButtons(cardLayout, item);

                        renderedCards.add(cardLayout);
                    } catch (IOException e) {
                        System.err.println("❌ Lỗi nạp FXML ngầm cho item: " + item.getName());
                    }
                }
            }

            // Sau khi đã chuẩn bị xong xuôi toàn bộ mớ giao diện thô ở luồng ngầm, ta mới đẩy về UI Thread để hiển thị
            Platform.runLater(() -> {
                if (flowPane == null) return;

                if (stats != null) {
                    if (ongoingLabel != null) ongoingLabel.setText(String.valueOf(stats.getOrDefault("ongoing", 0)));
                    if (wonLabel != null) wonLabel.setText(String.valueOf(stats.getOrDefault("won", 0)));
                }

                switch (currentFilter) {
                    case "ALL" -> setButtonActive(btnFilterAll);
                    case "ACTIVE" -> setButtonActive(btnFilterActive);
                    case "UPCOMING" -> setButtonActive(btnFilterUpcoming);
                }

                // Xóa sạch các Node cũ và đẩy toàn bộ danh sách card mới lên (Chỉ mất vài mili-giây)
                flowPane.getChildren().clear();
                flowPane.getChildren().addAll(renderedCards);

                System.out.println("=== [UI] Đã hiển thị mượt mà " + flowPane.getChildren().size() + " thẻ đấu giá từ FXML mẫu.");
            });
        });
    }

    /**
     * 🌟 REAL-TIME MECHANISM: Tự động render bất đồng bộ và bắn card mới lên đầu
     */
    public void addAuctionToRealtimeUI(Auction newAuction) {
        if (newAuction == null || cachedFxmlLocation == null) return;

        // Tách việc load FXML của card real-time ra một luồng background riêng biệt
        Thread realtimeRenderWorker = new Thread(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(cachedFxmlLocation);
                VBox cardLayout = loader.load();

                ProductCardController cardController = loader.getController();
                String finalImageUrl = getFinalImageUrl(newAuction.getImagePath());

                cardController.setData(newAuction.getProductName(), String.format("%,.0f đ", newAuction.getStartPrice()), "Đang diễn ra", finalImageUrl, "Sản phẩm mới lên sàn đấu giá thời gian thực.", "Hệ thống", "", "");

                cardLayout.setOnMouseClicked(e -> showAuctionDetail(newAuction));
                cardLayout.setStyle(cardLayout.getStyle() + "; -fx-cursor: hand;");
                bindCardButtons(cardLayout, newAuction);

                // Sau khi dựng xong card, đẩy lệnh chèn vào đầu FlowPane lên UI Thread
                Platform.runLater(() -> {
                    if (flowPane != null && (currentFilter.equals("ALL") || currentFilter.equals("ACTIVE"))) {
                        flowPane.getChildren().add(0, cardLayout);
                    }
                });
            } catch (IOException e) {
                System.err.println("❌ Lỗi nạp FXML real-time: " + e.getMessage());
            }
        });
        realtimeRenderWorker.setDaemon(true);
        realtimeRenderWorker.start();
    }

    /**
     * Hàm tiện ích giúp xử lý logic chuỗi URL ảnh
     */
    private String getFinalImageUrl(String rawImagePath) {
        if (rawImagePath == null || rawImagePath.trim().isEmpty()) {
            return "default.png";
        } else if (rawImagePath.startsWith("http://") || rawImagePath.startsWith("https://") || rawImagePath.startsWith("base64:")) {
            return rawImagePath;
        } else {
            return SERVER_IMAGE_BASE_URL + rawImagePath;
        }
    }

    /**
     * Hàm tiện ích liên kết các nút bấm bên trong thẻ card
     */
    private void bindCardButtons(VBox cardLayout, Object originData) {
        try {
            Node actionBtn = cardLayout.lookup("#actionButton");
            if (actionBtn == null) actionBtn = cardLayout.lookup("#btnBid");
            if (actionBtn == null) actionBtn = cardLayout.lookup("#actionBtn");

            if (actionBtn instanceof Button button) {
                button.setOnAction(e -> {
                    e.consume();
                    showAuctionDetail(originData);
                });
            }
        } catch (Exception ignored) {}
    }

    private void setButtonActive(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterActive, btnFilterUpcoming};
        for (Button btn : filterButtons) {
            if (btn != null)
                btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
        }
        if (activeButton != null)
            activeButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
    }

    @FXML private void handleFilterAll() { currentFilter = "ALL"; refreshDashboard(); }
    @FXML private void handleFilterActive() { currentFilter = "ACTIVE"; refreshDashboard(); }
    @FXML private void handleFilterUpcoming() { currentFilter = "UPCOMING"; refreshDashboard(); }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();

        Thread logoutWorker = new Thread(() -> {
            try {
                java.net.URL loginLocation = getClass().getResource("/view/LoginView.fxml");
                if (loginLocation == null) loginLocation = getClass().getResource("/com/auction/client/view/LoginView.fxml");
                if (loginLocation == null) loginLocation = getClass().getResource("LoginView.fxml");

                Parent root = FXMLLoader.load(loginLocation);
                Platform.runLater(() -> {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(root);
                });
            } catch (IOException e) {
                System.err.println("❌ Không thể chuyển cảnh đăng xuất: " + e.getMessage());
            }
        });
        logoutWorker.setDaemon(true);
        logoutWorker.start();
    }

    public void showAuctionDetail(Object productData) {
        // Tách việc load màn hình chi tiết FXML nặng nề ra luồng ngầm
        Thread detailLoaderWorker = new Thread(() -> {
            try {
                String fxmlPath = "/view/AuctionDetail.fxml";
                java.net.URL fxmlLocation = getClass().getResource(fxmlPath);
                if (fxmlLocation == null) return;

                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Parent detailView = loader.load(); // Load ngầm cấu trúc giao diện chi tiết

                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    if (productData instanceof Item) {
                        detailController.loadProductDetail((Item) productData);
                    } else if (productData instanceof Auction) {
                        detailController.loadProductDetail((Auction) productData);
                    }
                }

                // Chuyển view thô về luồng UI để hiển thị lên màn hình chính
                Platform.runLater(() -> {
                    if (MainLayoutController.getInstance() != null) {
                        MainLayoutController.getInstance().setContent(detailView);
                        System.out.println("🎯 [UI Switch] Đã nạp thành công trang chi tiết sản phẩm.");
                    }
                });
            } catch (IOException e) {
                System.err.println("❌ Lỗi nghiêm trọng khi biên dịch cấu trúc FXML chi tiết: " + e.getMessage());
            }
        });
        detailLoaderWorker.setDaemon(true);
        detailLoaderWorker.start();
    }
}