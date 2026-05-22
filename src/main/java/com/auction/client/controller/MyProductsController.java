package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Item;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import java.util.List;

public class MyProductsController {

    private static MyProductsController instance;

    // ✅ ĐÃ ĐỒNG BỘ CHUẨN TÊN BIẾN THEO FXML CỦA MÁ
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
                    // Nạp từng card sản phẩm lên giao diện FlowPane
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/ProductCardView.fxml"));
                    Node card = loader.load();

                    ProductCardController cardController = loader.getController();
                    if (cardController != null) {
                        String priceStr = String.format("%,.0f đ", item.getStartingPrice());
                        String statusStr = "IN_AUCTION".equalsIgnoreCase(item.getStatus()) ? "Đang diễn ra" : "Đã kết thúc";

                        cardController.setData(item.getName(), priceStr, statusStr, item.getImagePath());
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
     * Sự kiện khi người dùng bấm nút "Tạo sản phẩm ngay" lúc màn hình trống
     */
    @FXML
    private void handleGoToCreateProduct() {
        if (MainLayoutController.getInstance() != null) {
            MainLayoutController.getInstance().showCreateProductView();
        }
    }
}