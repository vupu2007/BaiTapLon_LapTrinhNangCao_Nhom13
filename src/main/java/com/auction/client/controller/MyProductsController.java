package com.auction.client.controller;

import com.auction.client.service.MyProductsService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Item;
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

    @FXML private Label lblTotalAuctions;   // Ô số "Tổng phiên"
    @FXML private Label lblActiveAuctions;  // Ô số "Đang diễn ra"
    @FXML private Label lblTotalValue;     // Ô số "Tổng giá trị"
    @FXML private VBox emptyStateView;      // VBox chứa thông báo rỗng
    @FXML private FlowPane productsGrid;    // Lưới chứa các card sản phẩm

    // Khởi tạo tầng nghiệp vụ chuyên biệt kết nối dữ liệu
    private final MyProductsService productsService = new MyProductsService();

    public static MyProductsController getInstance() {
        return instance;
    }

    @FXML
    public void initialize() {
        instance = this;
        loadMyProductsData();
    }

    /**
     * 🚀 ĐÃ CHUẨN HÓA: Chỉ làm nhiệm vụ điều phối giao diện hiển thị dữ liệu
     */
    public void loadMyProductsData() {
        if (CurrentAccount.getAccount() == null) return;

        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // Ủy quyền lấy dữ liệu mạng chạy ngầm cho lớp Service xử lý hoàn toàn
        productsService.loadOwnerProductsAsync(ownerId, myItems -> {
            if (productsGrid == null) return;

            // Kiểm tra nếu danh sách rỗng, đưa giao diện về trạng thái Empty State
            if (myItems.isEmpty()) {
                lblTotalAuctions.setText("0");
                lblActiveAuctions.setText("0");
                lblTotalValue.setText("0 đ");

                emptyStateView.setVisible(true);
                emptyStateView.setManaged(true);
                productsGrid.setVisible(false);
                productsGrid.setManaged(false);
                return;
            }

            // Ngược lại, kích hoạt lưới hiển thị danh sách sản phẩm
            emptyStateView.setVisible(false);
            emptyStateView.setManaged(false);
            productsGrid.setVisible(true);
            productsGrid.setManaged(true);

            double totalValue = 0;
            int activeCount = 0;
            int totalAuctions = myItems.size();

            productsGrid.getChildren().clear();

            // Tiến hành duyệt danh sách và nạp các Card đồ họa con lên màn hình
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

            // Gán các thông số thống kê sạch lên UI
            lblTotalAuctions.setText(String.valueOf(totalAuctions));
            lblActiveAuctions.setText(String.valueOf(activeCount));
            lblTotalValue.setText(String.format("%,.0f đ", totalValue));
        });
    }

    /**
     * XỬ LÝ CHỈNH SỬA SẢN PHẨM KHÔNG THAY ĐỔI
     */
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
            loadMyProductsData(); // Tải lại lưới mượt mà sau khi đóng form sửa

        } catch (Exception e) {
            System.err.println("❌ Không mở được form sửa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🗑️ ĐÃ CHUẨN HÓA: Hàm xóa gọi qua lớp Service trung gian độc lập
     */
    private void handleDeleteProduct(Item item) {
        System.out.println("👉 Yêu cầu xóa sản phẩm: " + item.getName());

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa sản phẩm này không?");
        confirmAlert.setContentText("Sản phẩm: " + item.getName() + "\nHành động này không thể hoàn tác!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {

            // Gọi Service thực thi lệnh xóa ngầm, nhận kết quả để đẩy giao diện đồ họa
            productsService.deleteProductAsync(item.getId(), isDeleted -> {
                if (isDeleted) {
                    System.out.println("✅ Đã xóa sản phẩm thành công khỏi DB!");
                    loadMyProductsData(); // Refresh lưới hiển thị
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Lỗi xóa sản phẩm");
                    errorAlert.setHeaderText(null);
                    errorAlert.setContentText("Xóa thất bại! Sản phẩm này có thể đã được đưa vào phiên đấu giá.");
                    errorAlert.showAndWait();
                }
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