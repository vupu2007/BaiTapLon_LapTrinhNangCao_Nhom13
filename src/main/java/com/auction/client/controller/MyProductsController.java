package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Item;
import javafx.concurrent.Task;
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
    @FXML private VBox emptyStateView;      // VBox chứa thông báo "Bạn chưa có sản phẩm nào"
    @FXML private FlowPane productsGrid;    // Lưới FlowPane chứa các card sản phẩm

    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        instance = this;
        // Tự động tải dữ liệu khi giao diện được nạp
        loadMyProductsData();
    }

    public static MyProductsController getInstance() {
        return instance;
    }

    /**
     * 🚀 TỐI ƯU ĐA LUỒNG: Quét dữ liệu từ Database chạy ngầm, không gây đơ lag UI
     */
    public void loadMyProductsData() {
        if (CurrentAccount.getAccount() == null) return;

        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // Tạo một Task chạy ngầm lấy danh sách vật phẩm từ DB
        Task<List<Item>> loadTask = new Task<>() {
            @Override
            protected List<Item> call() throws Exception {
                return itemDAO.getItemsByOwner(ownerId);
            }
        };

        // XỬ LÝ KHI TRUY VẤN DATABASE THÀNH CÔNG
        loadTask.setOnSucceeded(event -> {
            List<Item> myItems = loadTask.getValue();

            // Trường hợp không có sản phẩm nào hoặc danh sách rỗng
            if (myItems == null || myItems.isEmpty()) {
                lblTotalAuctions.setText("0");
                lblActiveAuctions.setText("0");
                lblTotalValue.setText("0 đ");

                // Hiển thị giao diện trống rỗng, ẩn lưới sản phẩm đi
                emptyStateView.setVisible(true);
                emptyStateView.setManaged(true);
                productsGrid.setVisible(false);
                productsGrid.setManaged(false);
                return;
            }

            // Nếu có sản phẩm, bắt đầu ẩn vùng trống rỗng và hiện lưới FlowPane lên
            emptyStateView.setVisible(false);
            emptyStateView.setManaged(false);
            productsGrid.setVisible(true);
            productsGrid.setManaged(true);

            double totalValue = 0;
            int activeCount = 0;
            int totalAuctions = myItems.size();

            // Xóa sạch các thẻ sản phẩm cũ trên lưới trước khi nạp danh sách mới
            productsGrid.getChildren().clear();

            // Duyệt qua danh sách sản phẩm lấy từ DB về
            for (Item item : myItems) {
                totalValue += item.getStartingPrice();

                // Tính toán số phiên đang chạy dựa vào trạng thái vật phẩm
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

                        // 🔥 TRÍCH XUẤT THÊM DỮ LIỆU ĐỂ ĐỦ 8 THAM SỐ CHUYỂN SANG SANG TRANG CHI TIẾT
                        String description = item.getDescription() != null ? item.getDescription() : "Không có mô tả.";

                        // Lấy tên người bán của chính mình (Vì đây là màn hình "Sản phẩm của tôi")
                        String sellerName = CurrentAccount.getAccount() != null ? CurrentAccount.getAccount().getUsername() : "Tôi";

                        // Ép ngày giờ sang chuỗi chữ nếu DB của má có lưu, nếu không có sẵn trường này trong Item model thì tạm để chuỗi trống/mặc định
                        // (Thường thời gian này sẽ đồng bộ từ phiên đấu giá hoặc lấy mặc định thời gian tạo sản phẩm)
                        String startTimeStr = "--/--/---- --:--";
                        String endTimeStr = "--/--/---- --:--";

                        // 🔥 ĐÃ SỬA: Nạp đầy đủ 8 tham số mới cho hàm setData của ProductCard
                        cardController.setData(item.getName(), priceStr, statusStr, item.getImagePath(),
                                description, sellerName, startTimeStr, endTimeStr);

                        // Kích hoạt phân quyền nút bấm Sửa / Xóa cho riêng màn hình quản lý này
                        cardController.setSellerMode(
                                () -> handleEditProduct(item),   // Hành động khi nhấn nút Sửa
                                () -> handleDeleteProduct(item)  // Hành động khi nhấn nút Xóa
                        );
                    }

                    // Thêm card sản phẩm vào lưới FlowPane
                    productsGrid.getChildren().add(card);
                } catch (Exception e) {
                    System.err.println("❌ Lỗi nạp card sản phẩm: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Đổ các con số thống kê lên các ô màu sắc phía trên
            lblTotalAuctions.setText(String.valueOf(totalAuctions));
            lblActiveAuctions.setText(String.valueOf(activeCount));
            lblTotalValue.setText(String.format("%,.0f đ", totalValue));
        });

        // XỬ LÝ KHI GẶP LỖI TRUY VẤN
        loadTask.setOnFailed(e -> {
            Throwable exception = loadTask.getException();
            System.err.println("❌ Lỗi tải danh sách sản phẩm ngầm: " + exception.getMessage());
        });

        // Kích hoạt chạy ngầm lập tức
        Thread thread = new Thread(loadTask);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 📝 XỬ LÝ KHI BẤM NÚT SỬA SẢN PHẨM
     */
    private void handleEditProduct(Item item) {
        System.out.println("👉 Yêu cầu chỉnh sửa sản phẩm: " + item.getName());

        try {
            // 1. Nạp file FXML của cái Form Tạo/Sửa sản phẩm
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/CreateProduct.fxml"));
            javafx.scene.Parent root = loader.load();

            // 2. Khởi tạo một Stage mới làm Popup Modal chui lên giữa màn hình
            javafx.stage.Stage popupStage = new javafx.stage.Stage();
            popupStage.setTitle("Chỉnh sửa sản phẩm: " + item.getName());

            // Ngăn không cho click ra ngoài khi chưa tắt popup
            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            popupStage.initOwner(productsGrid.getScene().getWindow());

            popupStage.setScene(new javafx.scene.Scene(root));

            // 4. Khi người dùng đóng popup, tự động refresh lại lưới
            popupStage.showAndWait();
            loadMyProductsData();

        } catch (Exception e) {
            System.err.println("❌ Không mở được form sửa: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 🗑️ XỬ LÝ KHI BẤM NÚT XÓA SẢN PHẨM (Có xác nhận an toàn)
     */
    private void handleDeleteProduct(Item item) {
        System.out.println("👉 Yêu cầu xóa sản phẩm: " + item.getName());

        // Hiện hộp thoại xác nhận xóa cho chắc chắn, tránh bấm nhầm
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa sản phẩm này không?");
        confirmAlert.setContentText("Sản phẩm: " + item.getName() + "\nHành động này không thể hoàn tác!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Thực hiện xóa ngầm dưới DB thông qua ItemDAO
            try {
                boolean isDeleted = itemDAO.deleteItem(item.getId());

                if (isDeleted) {
                    System.out.println("✅ Đã xóa sản phẩm thành công khỏi DB!");
                    // Tự quét lại DB và vẽ lại giao diện mới tinh, không còn sản phẩm vừa xóa
                    loadMyProductsData();
                } else {
                    System.err.println("❌ Xóa thất bại, có thể sản phẩm đã có người vào đấu giá.");
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi xảy ra khi xóa sản phẩm: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Sự kiện khi người dùng bấm nút "Tạo sản phẩm ngay" lúc màn hình trống hoặc nút ở Header mới
     */
    @FXML
    private void handleGoToCreateProduct() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showCreateProductView();
        }
    }
}