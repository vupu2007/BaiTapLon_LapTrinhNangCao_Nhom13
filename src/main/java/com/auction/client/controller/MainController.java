package com.auction.client.controller;

import com.auction.client.service.MainDashboardService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;
import com.auction.client.network.ClientSocket;

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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainController {

    @FXML private Label balanceLabel, ongoingLabel, wonLabel, welcomeLabel;
    @FXML private Button btnFilterAll, btnFilterActive, btnFilterUpcoming, btnFilterEnded;
    @FXML private FlowPane flowPane;

    private String currentFilter = "ALL";
    private final MainDashboardService dashboardService = new MainDashboardService();
    private static final String SERVER_IMAGE_BASE_URL = "http://localhost:8080/uploads/";

    private final ExecutorService executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r);
                t.setDaemon(true);
                return t;
            }
    );

    private static java.net.URL cachedFxmlLocation;

    public MainController() {}

    @FXML
    public void initialize() {
        initFxmlCache();

        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            refreshDashboard();
        }

        // Giữ nguyên — đây là Observer local, không phải network call
        ClientSocket.getInstance().setOnAuctionUpdate(() -> refreshDashboard());

        ScheduledExecutorService poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> Platform.runLater(this::refreshDashboard), 5, 5, TimeUnit.SECONDS);
    }

    private void initFxmlCache() {
        if (cachedFxmlLocation == null) {
            cachedFxmlLocation = getClass().getResource("/view/ProductCard.fxml");
            if (cachedFxmlLocation == null)
                cachedFxmlLocation = getClass().getResource("/com/auction/client/view/ProductCard.fxml");
        }
    }

    public void refreshDashboard() {
        dashboardService.resetFetching();
        final String filterSnapshot = currentFilter;
        Account current = CurrentAccount.getAccount();
        if (current == null) return;

        if (balanceLabel != null) {
            balanceLabel.setText(current instanceof User
                    ? String.format("%,.0f VND", ((User) current).getBalance()) : "N/A");
        }

        dashboardService.fetchDashboardDataAsync(current.getId(), filterSnapshot, (stats, items) -> {
            executorService.submit(() -> {
                List<VBox> renderedCards = new ArrayList<>();
                if (items != null && cachedFxmlLocation != null) {
                    for (Item item : items) {
                        if (item == null) continue;
                        try {
                            FXMLLoader loader = new FXMLLoader(cachedFxmlLocation);
                            VBox cardLayout = loader.load();
                            ProductCardController cardController = loader.getController();
                            if (cardController != null) {
                                String finalImageUrl = getFinalImageUrl(item.getImagePath());
                                String statusText = "OPEN".equals(item.getAuctionStatus()) ? "Sắp diễn ra"
                                        : "FINISHED".equals(item.getAuctionStatus()) ? "Đã kết thúc"
                                          : "Đang diễn ra";
                                String priceStr = String.format("%,.0f đ",
                                        item.getCurrentPrice() > 0 ? item.getCurrentPrice() : item.getStartingPrice());
                                String timeStr = "Sắp diễn ra".equals(statusText)
                                        ? item.getStartTimeStr() : item.getEndTimeStr();
                                String startTimeStr = item.getStartTimeStr();
                                cardController.setProductModelData(null, item.getName(), priceStr,
                                        statusText, finalImageUrl, item.getDescription(), startTimeStr, timeStr);
                            }

                            // ✅ Dùng service thay vì ClientSocket trực tiếp
                            cardLayout.setOnMouseClicked(e -> {
                                if (item.getAuctionId() > 0) {
                                    dashboardService.fetchAuctionByIdAsync(item.getAuctionId(), auction -> {
                                        if (auction != null) showAuctionDetail(auction);
                                        else showAuctionDetail(item);
                                    });
                                } else {
                                    showAuctionDetail(item);
                                }
                            });

                            bindCardButtons(cardLayout, item);
                            renderedCards.add(cardLayout);
                        } catch (IOException e) {
                            System.err.println("❌ Lỗi nạp FXML: " + item.getName());
                        }
                    }
                }

                Platform.runLater(() -> {
                    if (flowPane == null) return;
                    if (stats != null) {
                        if (ongoingLabel != null) ongoingLabel.setText(String.valueOf(stats.getOrDefault("ongoing", 0)));
                        if (wonLabel != null) wonLabel.setText(String.valueOf(stats.getOrDefault("won", 0)));
                    }
                    currentFilter = filterSnapshot;
                    updateFilterButtonStyles();
                    flowPane.getChildren().clear();
                    flowPane.getChildren().addAll(renderedCards);
                });
            });
        });
    }

    public void addAuctionToRealtimeUI(Auction newAuction) {
        if (newAuction == null || cachedFxmlLocation == null) return;

        executorService.submit(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(cachedFxmlLocation);
                VBox cardLayout = loader.load();
                ProductCardController cardController = loader.getController();
                if (cardController != null) {
                    String finalImageUrl = getFinalImageUrl(newAuction.getImagePath());
                    String priceStr = String.format("%,.0f đ", newAuction.getStartPrice());
                    cardController.setProductModelData(newAuction, newAuction.getProductName(),
                            priceStr, "Đang diễn ra", finalImageUrl,
                            newAuction.getDescription() != null ? newAuction.getDescription() : "",
                            newAuction.getStartTime() != null
                                    ? newAuction.getStartTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null,
                            newAuction.getEndTime() != null
                                    ? newAuction.getEndTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null);
                }

                cardLayout.setOnMouseClicked(e -> showAuctionDetail(newAuction));
                bindCardButtons(cardLayout, newAuction);

                Platform.runLater(() -> {
                    if (flowPane != null && ("ALL".equals(currentFilter) || "ACTIVE".equals(currentFilter))) {
                        flowPane.getChildren().add(0, cardLayout);
                    }
                });
            } catch (IOException e) {
                System.err.println("❌ Lỗi nạp FXML real-time: " + e.getMessage());
            }
        });
    }

    private String getFinalImageUrl(String rawImagePath) {
        if (rawImagePath == null || rawImagePath.trim().isEmpty()) return "default.png";
        if (rawImagePath.startsWith("http://") || rawImagePath.startsWith("https://")
                || rawImagePath.startsWith("base64:")) return rawImagePath;
        return SERVER_IMAGE_BASE_URL + rawImagePath;
    }

    // ✅ Dùng service thay vì ClientSocket trực tiếp
    private void bindCardButtons(VBox cardLayout, Object originData) {
        try {
            Node actionBtn = cardLayout.lookup("#actionButton");
            if (actionBtn instanceof Button button) {
                button.setOnAction(e -> {
                    e.consume();
                    if (originData instanceof Item item && item.getAuctionId() > 0) {
                        dashboardService.fetchAuctionByIdAsync(item.getAuctionId(), auction -> {
                            if (auction != null) showAuctionDetail(auction);
                            else showAuctionDetail(originData);
                        });
                    } else {
                        showAuctionDetail(originData);
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private void updateFilterButtonStyles() {
        String normal = "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;";
        String active = "-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;";

        if (btnFilterAll != null) btnFilterAll.setStyle(normal);
        if (btnFilterActive != null) btnFilterActive.setStyle(normal);
        if (btnFilterUpcoming != null) btnFilterUpcoming.setStyle(normal);
        if (btnFilterEnded != null) btnFilterEnded.setStyle(normal);

        Button activeBtn = switch (currentFilter) {
            case "ALL" -> btnFilterAll;
            case "ACTIVE" -> btnFilterActive;
            case "UPCOMING" -> btnFilterUpcoming;
            case "FINISHED" -> btnFilterEnded;
            default -> null;
        };
        if (activeBtn != null) activeBtn.setStyle(active);
    }

    @FXML private void handleFilterAll() { currentFilter = "ALL"; refreshDashboard(); }
    @FXML private void handleFilterActive() { currentFilter = "ACTIVE"; refreshDashboard(); }
    @FXML private void handleFilterUpcoming() { currentFilter = "UPCOMING"; refreshDashboard(); }
    @FXML private void handleFilterEnded() { currentFilter = "FINISHED"; refreshDashboard(); }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();
        shutdownExecutor();

        executorService.submit(() -> {
            try {
                URL loginLocation = getClass().getResource("/view/LoginView.fxml");
                if (loginLocation == null) return;
                Parent root = FXMLLoader.load(loginLocation);
                Platform.runLater(() -> {
                    Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                    stage.getScene().setRoot(root);
                });
            } catch (IOException e) {
                System.err.println("❌ Lỗi chuyển cảnh đăng xuất: " + e.getMessage());
            }
        });
    }

    public void showAuctionDetail(Object productData) {
        System.out.println("DEBUG showAuctionDetail type: " + productData.getClass().getSimpleName());
        if (flowPane == null || flowPane.getScene() == null) return;

        executorService.submit(() -> {
            try {
                String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                        ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent detailView = loader.load();

                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    if (productData instanceof Item item) detailController.loadProductDetail(item);
                    else if (productData instanceof Auction auction) detailController.loadProductDetail(auction);
                }

                Platform.runLater(() -> {
                    if (flowPane.getScene() != null) {
                        Parent root = flowPane.getScene().getRoot();
                        Node layoutCenter = root.lookup("#contentArea");
                        if (layoutCenter instanceof javafx.scene.layout.StackPane contentArea) {
                            contentArea.getChildren().setAll(detailView);
                        } else {
                            System.err.println("❌ Không tìm thấy #contentArea.");
                        }
                    }
                });
            } catch (IOException e) {
                System.err.println("❌ Lỗi load trang chi tiết: " + e.getMessage());
            }
        });
    }

    public void shutdownExecutor() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}