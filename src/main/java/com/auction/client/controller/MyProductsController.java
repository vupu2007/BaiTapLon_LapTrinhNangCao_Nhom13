package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.MyProductsService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;
import com.auction.shared.network.MessageType;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MyProductsController {

    @FXML private Label lblTotalAuctions;
    @FXML private Label lblActiveAuctions;
    @FXML private Label lblTotalValue;
    @FXML private VBox emptyStateView;
    @FXML private FlowPane productsGrid;

    private final MyProductsService productsService = new MyProductsService();

    // 🚀 TỐI ƯU DỰ ÁN LỚN: Định vị sẵn FXML mẫu ngay khi khởi chạy để tối ưu hóa I/O tốc độ đọc file
    private static java.net.URL productCardFxmlLocation;

    @FXML
    public void initialize() {
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

        // 👉 ĐẶT MÁY NGHE LÉN SỐ 1
        System.out.println("🔄 DEBUG: Bắt đầu chạy loadMyProductsData() để refresh UI...");

        new Thread(() -> {
            try {
                Response resp = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_AUCTIONS_BY_SELLER, ownerId));

                List<Auction> auctions = (resp != null && resp.isSuccess())
                        ? (List<Auction>) resp.getData() : null;

                // 👉 ĐẶT MÁY NGHE LÉN SỐ 2
                if (auctions != null && !auctions.isEmpty()) {
                    System.out.println("🔄 DEBUG: Nhận được " + auctions.size() + " món từ Server.");
                    System.out.println("🔄 DEBUG: Check thử tên món đầu tiên: " + auctions.get(0).getProductName());
                }

                List<Node> running = new ArrayList<>(), open = new ArrayList<>(), finished = new ArrayList<>();
                double totalValueCalc = 0;
                int activeCountCalc = 0;
                java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                if (auctions != null && productCardFxmlLocation != null) {
                    for (Auction auction : auctions) {
                        if (auction == null) continue;
                        try {
                            FXMLLoader loader = new FXMLLoader(productCardFxmlLocation);
                            Node card = loader.load();
                            card.setUserData(auction.getItemId());
                            ProductCardController cc = loader.getController();

                            double price = auction.getCurrentPrice() > 0 ? auction.getCurrentPrice() : auction.getStartPrice();
                            String priceStr = String.format("%,.0f đ", price);
                            String statusStr = auction.getStatus() == Auction.AuctionStatus.RUNNING ? "Đang diễn ra"
                                    : auction.getStatus() == Auction.AuctionStatus.OPEN ? "Sắp diễn ra" : "Đã kết thúc";
                            String endTimeStr = auction.getEndTime() != null ? auction.getEndTime().format(fmt) : "--/--/---- --:--";
                            String startTimeStr = auction.getStartTime() != null ? auction.getStartTime().format(fmt) : "--/--/---- --:--";
                            String timeStr = "Sắp diễn ra".equals(statusStr) ? startTimeStr : endTimeStr;

                            totalValueCalc += price;
                            if (auction.getStatus() == Auction.AuctionStatus.RUNNING) activeCountCalc++;

                            if (cc != null) {
                                cc.setData(auction.getProductName(), priceStr, statusStr, auction.getImagePath(),
                                        auction.getDescription() != null ? auction.getDescription() : "",
                                        CurrentAccount.getAccount().getUsername(), startTimeStr, timeStr);
                                cc.setSellerMode(
                                        auction.getStatus() == Auction.AuctionStatus.OPEN
                                                ? () -> handleEditProduct(auction)
                                                : () -> new Alert(Alert.AlertType.WARNING, "Chỉ có thể sửa sản phẩm ở trạng thái Sắp diễn ra!").showAndWait(),
                                        () -> handleDeleteProduct(getItemFromAuction(auction))
                                );
                            }

                            if ("Đang diễn ra".equals(statusStr)) running.add(card);
                            else if ("Sắp diễn ra".equals(statusStr)) open.add(card);
                            else finished.add(card);
                        } catch (Exception e) {
                            System.err.println("❌ Lỗi card: " + e.getMessage());
                        }
                    }
                }

                List<Node> renderedCards = new ArrayList<>();
                renderedCards.addAll(running); renderedCards.addAll(open); renderedCards.addAll(finished);
                final double ftv = totalValueCalc; final int fac = activeCountCalc;
                final int total = auctions != null ? auctions.size() : 0;

                Platform.runLater(() -> {
                    if (productsGrid == null) return;
                    if (total == 0) {
                        lblTotalAuctions.setText("0"); lblActiveAuctions.setText("0"); lblTotalValue.setText("0 đ");
                        emptyStateView.setVisible(true); emptyStateView.setManaged(true);
                        productsGrid.setVisible(false); productsGrid.setManaged(false);
                        return;
                    }
                    emptyStateView.setVisible(false); emptyStateView.setManaged(false);
                    productsGrid.setVisible(true); productsGrid.setManaged(true);
                    productsGrid.getChildren().setAll(renderedCards);
                    lblTotalAuctions.setText(String.valueOf(total));
                    lblActiveAuctions.setText(String.valueOf(fac));
                    lblTotalValue.setText(String.format("%,.0f đ", ftv));
                });
            } catch (Exception e) {
                System.err.println("❌ Lỗi load: " + e.getMessage());
            }
        }, "MyProductsLoader").start();
    }

    private Item getItemFromAuction(Auction auction) {
        Electronics item = new Electronics();
        item.setItemId(auction.getItemId());
        item.setName(auction.getProductName() != null ? auction.getProductName() : "");
        item.setDescription(auction.getDescription() != null ? auction.getDescription() : "");
        item.setStartingPrice(auction.getStartPrice());
        item.setImagePath(auction.getImagePath());
        item.setOwnerId(auction.getSellerId());
        return item;
    }

    // Đổi tham số từ Item thành Auction để mang theo được ngày giờ
    private void handleEditProduct(Auction auction) {
        System.out.println("👉 Yêu cầu chỉnh sửa sản phẩm: " + auction.getProductName());

        Thread openFormWorker = new Thread(() -> {
            try {
                java.net.URL fxmlLoc = getClass().getResource("/view/CreateAuction.fxml");
                if (fxmlLoc == null) fxmlLoc = getClass().getResource("/com/auction/client/view/CreateProduct.fxml");

                FXMLLoader loader = new FXMLLoader(fxmlLoc);
                Parent root = loader.load();
                CreateProductController ctrl = loader.getController();

                // 👉 THAY ĐỔI: Chuyền thẳng Auction vào form!
                if (ctrl != null) ctrl.loadAuctionForEdit(auction);

                Platform.runLater(() -> {
                    try {
                        if (productsGrid.getScene() != null) {
                            javafx.stage.Stage popupStage = new javafx.stage.Stage();
                            popupStage.setTitle("Chỉnh sửa sản phẩm: " + auction.getProductName());
                            popupStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                            popupStage.initOwner(productsGrid.getScene().getWindow());
                            popupStage.setScene(new javafx.scene.Scene(root));

                            popupStage.showAndWait();
                            loadMyProductsData(); // Refresh lại dữ liệu sau khi tắt popup
                        }
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
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText("Bạn có chắc chắn muốn xóa sản phẩm này không?");
        confirmAlert.setContentText("Sản phẩm: " + item.getName() + "\nHành động này không thể hoàn tác!");

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            productsService.deleteProductAsync(item.getItemId(), isDeleted -> {
                Platform.runLater(() -> {
                    if (isDeleted) {
                        productsGrid.getChildren().removeIf(node -> item.getItemId().equals(node.getUserData()));
                        new Alert(Alert.AlertType.INFORMATION, "Đã xóa sản phẩm thành công!").showAndWait();
                    } else {
                        new Alert(Alert.AlertType.ERROR, "Không thể xóa sản phẩm này!").showAndWait();
                    }
                });
            });
        }
    }

    /**
     * 🚀 ĐÃ SỬA LỖI BIÊN DỊCH: Sử dụng Kỹ thuật Scene Graph Lookup
     * Tìm kiếm và chèn trang "Tạo sản phẩm" động lên vùng chứa tổng của Layout cha
     */
    @FXML
    private void handleGoToCreateProduct() {
        if (productsGrid.getScene() == null) return;

        // Bắn luồng ngầm biên dịch FXML tạo sản phẩm mới, không block nút bấm
        Thread navigationWorker = new Thread(() -> {
            try {
                java.net.URL createFxmlLoc = getClass().getResource("/view/CreateAuction.fxml");
                if (createFxmlLoc == null) createFxmlLoc = getClass().getResource("/view/CreateProduct.fxml");

                FXMLLoader loader = new FXMLLoader(createFxmlLoc);
                Parent createView = loader.load();

                Platform.runLater(() -> {
                    Parent root = productsGrid.getScene().getRoot();
                    Node layoutCenter = root.lookup("#contentArea");

                    if (layoutCenter instanceof StackPane contentArea) {
                        contentArea.getChildren().setAll(createView);
                        System.out.println("🎯 [Navigation] Đã chuyển đổi màn hình sang form Tạo sản phẩm an toàn.");
                    } else {
                        System.err.println("❌ Không tìm thấy vùng chứa trung tâm #contentArea.");
                    }
                });
            } catch (Exception e) {
                System.err.println("❌ Lỗi load trang tạo sản phẩm ở luồng nền: " + e.getMessage());
            }
        });
        navigationWorker.setDaemon(true);
        navigationWorker.start();
    }
}