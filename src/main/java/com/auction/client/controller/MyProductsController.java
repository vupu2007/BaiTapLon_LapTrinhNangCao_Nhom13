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

    public static MyProductsController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        loadMyProductsData();
    }

    public void loadMyProductsData() {
        if (CurrentAccount.getAccount() == null) return;

        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        productsService.loadOwnerProductsAsync(ownerId, myItems -> {
            // 🌟 CRITICAL FIX: Đẩy toàn bộ khối lệnh cập nhật UI về luồng JavaFX Main Thread
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

                double totalValue = 0;
                int activeCount = 0;
                int totalAuctions = myItems.size();

                productsGrid.getChildren().clear();

                for (Item item : myItems) {
                    totalValue += item.getStartingPrice();

                    if ("IN_AUCTION".equalsIgnoreCase(item.getStatus())) {
                        activeCount++;
                    }

                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCard.fxml"));
                        Node card = loader.load();

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

                        productsGrid.getChildren().add(card);
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi nạp card sản phẩm: " + e.getMessage());
                    }
                }

                lblTotalAuctions.setText(String.valueOf(totalAuctions));
                lblActiveAuctions.setText(String.valueOf(activeCount));
                lblTotalValue.setText(String.format("%,.0f đ", totalValue));
            });
        });
    }

    private void handleEditProduct(Item item) {
        System.out.println("👉 Yêu cầu chỉnh sửa sản phẩm: " + item.getName());
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CreateProduct.fxml"));
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setTitle("Chỉnh sửa sản phẩm: " + item.getName());
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(productsGrid.getScene().getWindow());
            popupStage.setScene(new javafx.scene.Scene(root));

            popupStage.showAndWait();
            loadMyProductsData();

        } catch (Exception e) {
            System.err.println("❌ Không mở được form sửa: " + e.getMessage());
            e.printStackTrace();
        }
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
                // 🌟 CRITICAL FIX: Đẩy thông báo và lệnh Refresh UI về luồng chính
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