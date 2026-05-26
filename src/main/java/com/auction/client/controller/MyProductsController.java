package com.auction.client.controller;

import com.auction.client.service.MyProductsService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Item;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MyProductsController {

    private static MyProductsController instance;

    @FXML private Label lblTotalAuctions;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblTotalValue;
    @FXML private VBox emptyStateView;
    @FXML private FlowPane productsGrid;

    private final MyProductsService productsService = new MyProductsService();

    // 🚀 TỐI ƯU DỰ ÁN LỚN: Định vị sẵn FXML mẫu ngay khi khởi chạy để tối ưu hóa I/O tốc độ đọc file
    private static java.net.URL productCardFxmlLocation;

    public static MyProductsController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;

        if (productCardFxmlLocation == null) {
            productCardFxmlLocation = getClass().getResource("/view/ProductCard.fxml");
            if (productCardFxmlLocation == null) productCardFxmlLocation = getClass().getResource("/com/auction/client/view/ProductCard.fxml");
            if (productCardFxmlLocation == null) productCardFxmlLocation = getClass().getResource("ProductCard.fxml");
            if (productCardFxmlLocation == null) productCardFxmlLocation = getClass().getResource("/ProductCard.fxml");
        }

        loadMyProductsData();
    }

    public void loadMyProductsData() {
        if (CurrentAccount.getAccount() == null) return;

        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // 1. Chạy ngầm lấy dữ liệu từ Server/DB thông qua Socket mạng
        productsService.loadOwnerProductsAsync(ownerId, myItems -> {

            // Tạo danh sách tạm để chứa các Card giao diện được dựng thô ngay trên luồng ngầm này
            List<Node> renderedCards = new ArrayList<>();
            double totalValueCalc = 0;
            int activeCountCalc = 0;
            int totalAuctionsCalc = (myItems != null) ? myItems.size() : 0;

            if (myItems != null && !myItems.isEmpty() && productCardFxmlLocation != null) {
                for (Item item : myItems) {
                    if (item == null) continue;

                    totalValueCalc += item.getStartingPrice();
                    if ("IN_AUCTION".equalsIgnoreCase(item.getStatus())) {
                        activeCountCalc++;
                    }

                    try {
                        // 🧠 KIẾN TRÚC DỰ ÁN LỚN: Phân tích và nạp cấu trúc FXML ngay tại luồng nền
                        FXMLLoader loader = new FXMLLoader(productCardFxmlLocation);
                        Node card = loader.load(); // Tác vụ đọc đĩa nặng nề chạy ngầm tại đây!

                        ProductCardController cardController = loader.getController();
                        if (cardController != null) {
                            String priceStr = String.format("%,.0f đ", item.getStartingPrice());
                            String statusStr = "IN_AUCTION".equalsIgnoreCase(item.getStatus()) ? "Đang diễn ra" : "Đã kết thúc";
                            String description = item.getDescription() != null ? item.getDescription() : "Không có mô tả.";
                            String sellerName = CurrentAccount.getAccount() != null ? CurrentAccount.getAccount().getUsername() : "Tôi";
                            String startTimeStr = "--/--/---- --:--";
                            String endTimeStr = "--/--/---- --:--";

                            cardController.setData(item.getName(), priceStr, statusStr, item.getImagePath(),
                                    description, sellerName, startTimeStr, endTimeStr);

                            cardController.setSellerMode(
                                    () -> handleEditProduct(item),
                                    () -> handleDeleteProduct(item)
                            );
                        }
                        renderedCards.add(card);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi nạp card sản phẩm trên luồng ngầm: " + e.getMessage());
                    }
                }
            }

            // Bản sao dữ liệu cấu hình để đẩy vào luồng UI
            final double finalTotalValue = totalValueCalc;
            final int finalActiveCount = activeCountCalc;

            // 2. Đẩy mớ giao diện thô đã dựng xong xuôi về luồng UI vẽ lên màn hình trong tích tắc
            Platform.runLater(() -> {
                if (productsGrid == null) return;

                if (myItems == null || myItems.isEmpty()) {
                    lblTotalAuctions.setText("0");
                    lblActiveAuctions.setText("0");
                    lblTotalValue.setText("0 đ");

                    emptyStateView.setVisible(true);
                    emptyStateView.setManaged(true);
                    productsGrid.setVisible(false);
                    productsGrid.setManaged(false);
                    return;
                }

                emptyStateView.setVisible(false);
                emptyStateView.setManaged(false);
                productsGrid.setVisible(true);
                productsGrid.setManaged(true);

                // Thêm hàng loạt Node đã dựng sẵn vào cây đồ họa (Vô cùng nhanh)
                productsGrid.getChildren().clear();
                productsGrid.getChildren().addAll(renderedCards);

                lblTotalAuctions.setText(String.valueOf(totalAuctionsCalc));
                lblActiveAuctions.setText(String.valueOf(finalActiveCount));
                lblTotalValue.setText(String.format("%,.0f đ", finalTotalValue));
            });
        });
    }

    private void handleEditProduct(Item item) {
        System.out.println("👉 Yêu cầu chỉnh sửa sản phẩm: " + item.getName());

        // Việc nạp form popup chỉnh sửa FXML cũng được đẩy vào luồng ngầm để tránh đơ nút bấm
        Thread openFormWorker = new Thread(() -> {
            try {
                java.net.URL fxmlLoc = getClass().getResource("/view/CreateProduct.fxml");
                if (fxmlLoc == null) fxmlLoc = getClass().getResource("/com/auction/client/view/CreateProduct.fxml");

                FXMLLoader loader = new FXMLLoader(fxmlLoc);
                javafx.scene.Parent root = loader.load();

                Platform.runLater(() -> {
                    try {
                        javafx.stage.Stage popupStage = new javafx.stage.Stage();
                        popupStage.setTitle("Chỉnh sửa sản phẩm: " + item.getName());
                        popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                        popupStage.initOwner(productsGrid.getScene().getWindow());
                        popupStage.setScene(new javafx.scene.Scene(root));

                        popupStage.showAndWait();
                        loadMyProductsData(); // Refresh lại dữ liệu sau khi tắt popup công việc
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("❌ Không nạp được form sửa ở luồng nền: " + e.getMessage());
            }
        });
        openFormWorker.setDaemon(true);
        openFormWorker.start();
    }

    private void handleDeleteProduct(Item item) {
        System.out.println("👉 Yêu cầu xóa sản phẩm: " + item.getName());

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa sản phẩm này không?");
        confirmAlert.setContentText("Sản phẩm: " + item.getName() + "\nHành động này không thể hoàn tác!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            productsService.deleteProductAsync(item.getId(), isDeleted -> {
                Platform.runLater(() -> {
                    if (isDeleted) {
                        System.out.println("✅ Đã xóa sản phẩm thành công khỏi DB!");
                        loadMyProductsData();
                    } else {
                        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                        errorAlert.setTitle("Lỗi xóa sản phẩm");
                        errorAlert.setHeaderText(null);
                        errorAlert.setContentText("Xóa thất bại! Sản phẩm này có thể đã được đưa vào phiên đấu giá.");
                        errorAlert.showAndWait();
                    }
                });
            });
        }
    }

    @FXML
    private void handleGoToCreateProduct() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showCreateProductView();
        }
    }
}