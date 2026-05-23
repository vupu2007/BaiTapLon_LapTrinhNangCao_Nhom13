package com.auction.client.controller;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.BidDAO;
import com.auction.shared.model.Account;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Item;
import com.auction.shared.model.Auction;
import com.auction.client.util.CurrentAccount;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

/**
 * Controller màn hình chi tiết phiên đấu giá (AuctionDetail.fxml).
 * Xử lý: hiển thị thông tin sản phẩm, đồng hồ đếm ngược,
 * đặt giá thủ công, auto-bid, lưu lịch sử vào DB, biểu đồ giá.
 */
public class AuctionDetailController {

    // ===== FXML Labels hiển thị thông tin phiên đấu giá =====
    @FXML public Label lblProductTitle;     // Tên sản phẩm (tiêu đề to)
    @FXML public Label lblTimeRemaining;    // Đồng hồ đếm ngược
    @FXML public Label lblInfoName;         // Tên sản phẩm (trong phần thông tin)
    @FXML public Label lblInfoDescription;  // Mô tả sản phẩm
    @FXML public Label lblStartPrice;       // Giá khởi điểm
    @FXML public Label lblSellerName;       // Tên người bán
    @FXML public Label lblStartTime;        // Thời gian bắt đầu
    @FXML public Label lblEndTime;          // Thời gian kết thúc
    @FXML public Label lblCurrentPrice;     // Giá hiện tại (cập nhật realtime)
    @FXML public Label lblTopBidder;        // Người đang dẫn đầu
    @FXML private Label lblAuctionId;       // Mã phiên đấu giá

    // ===== Các thành phần UI khác =====
    @FXML private ImageView imgProduct;                          // Ảnh sản phẩm
    @FXML private LineChart<Number, Number> chartPriceHistory;   // Biểu đồ lịch sử giá
    @FXML private TextField txtBidAmount;                        // Ô nhập giá đặt
    @FXML private Button btnSubmitBid;                           // Nút đặt giá
    @FXML private ToggleButton btnAutoBid;                       // Nút bật/tắt auto-bid
    @FXML private VBox vboxBidHistoryContainer;                  // Danh sách lịch sử đặt giá

    // ===== State nội bộ =====
    private Auction currentAuction;   // Phiên đấu giá đang xem — dùng khi lưu bid vào DB
    private Item currentItem;         // Sản phẩm tương ứng (có thể null)
    private Timeline countdownTimeline;                         // Timer đếm ngược
    private XYChart.Series<Number, Number> priceSeries;         // Chuỗi dữ liệu biểu đồ
    private int bidCount = 0;         // Số lần đặt giá (dùng làm trục X biểu đồ)

    private final com.auction.server.dao.ItemDAO itemDAO = new com.auction.server.dao.ItemDAO();

    private boolean isBidding = false; // ⚠️ Chặn double-click

    // ===== Auto-bid settings =====
    private boolean isAutoBidEnabled = false;   // Trạng thái auto-bid
    private double maxAutoBidAmount = 0.0;      // Giá tối đa chấp nhận auto-bid
    private double autoBidIncrement = 0.0;      // Bước tăng giá mỗi lần auto-bid

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // KHỞI TẠO

    /**
     * Gọi tự động bởi JavaFX sau khi load FXML.
     * Khởi tạo biểu đồ và gắn sự kiện cho các nút.
     *
     * ⚠️ QUAN TRỌNG: btnSubmitBid và btnAutoBid trong FXML không có thuộc tính
     * onAction="#..." nên BẮT BUỘC phải setOnAction ở đây.
     * Nếu xóa 2 dòng setOnAction này thì nút sẽ không hoạt động.
     */
    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá hiện tại");
        if (chartPriceHistory != null) {
            chartPriceHistory.getData().clear();
            chartPriceHistory.getData().add(priceSeries);
            chartPriceHistory.setAnimated(false); // Tắt animation để cập nhật mượt hơn
        }

        // Gắn sự kiện nút — KHÔNG được xóa 2 dòng này vì FXML không có onAction
        if (btnSubmitBid != null) btnSubmitBid.setOnAction(e -> handleManualBid());
        if (btnAutoBid != null) btnAutoBid.setOnAction(e -> handleAutoBidToggle());
    }

     // LOAD DỮ LIỆU

    /**
     * Load chi tiết từ một Item (không có Auction — dùng cho màn hình xem nhanh).
     */
    public void loadProductDetail(Item item) {
        if (item == null) return;
        System.out.println("🔍 loadProductDetail Item: name=" + item.getName() + " | imagePath=" + item.getImagePath());

        this.currentItem = item;
        this.currentAuction = null; // Không có phiên đấu giá → không lưu bid được

        String formattedPrice = String.format("%,.0f đ", item.getStartingPrice());
        String sellerName = "Người bán #" + item.getOwnerId();

        fillTextFields(item.getName(), formattedPrice, item.getDescription(), sellerName,
                "--/--/---- --:--", "--/--/---- --:--");

        Platform.runLater(() -> {
            tryLoadImageToView(imgProduct, item.getImagePath());
            checkBiddingPermissions(item.getOwnerId());
            priceSeries.getData().clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, item.getStartingPrice()));
        });

    }

    /**
     * Load chi tiết từ một Auction — hàm chính khi bấm vào card đấu giá.
     * Lấy thêm Item từ DB để hiển thị ảnh và mô tả.
     */
    public void loadProductDetail(Auction auction) {
        if (auction == null) return;

        // ⚠️ Phải gán currentAuction trước — processValidBidUpdate() dùng biến này để lưu DB
        this.currentAuction = auction;
        this.currentItem = null;

        Item item = itemDAO.getItemById(auction.getItemId());
        System.out.println("🔍 AuctionDetail itemId=" + auction.getItemId()
                + " | item=" + item + " | imagePath=" + (item != null ? item.getImagePath() : "null"));
        String imagePath = (item != null) ? item.getImagePath() : null;

        String pName = (auction.getProductName() != null)
                ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
        String startPriceStr = String.format("%,.0f đ", auction.getStartPrice());

        // Ưu tiên currentPrice, fallback về startPrice nếu chưa có ai đặt
        double currentPriceVal = auction.getCurrentPrice() > 0
                ? auction.getCurrentPrice() : auction.getStartPrice();
        String currentPriceStr = String.format("%,.0f đ", currentPriceVal);

        // Lấy tên người bán từ DB thay vì chỉ hiện ID
        com.auction.server.dao.AccountDAO accountDAO = new com.auction.server.dao.AccountDAO();
        com.auction.shared.model.Account seller = accountDAO.getAccountById(auction.getSellerId());
        String sellerName = (seller != null) ? seller.getUsername() : "Người bán #" + auction.getSellerId();

        String startTimeStr = auction.getStartTime() != null
                ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
        String endTimeStr = auction.getEndTime() != null
                ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";
        String description = (item != null && item.getDescription() != null) ? item.getDescription() : "";

        fillTextFields(pName, startPriceStr, description, sellerName, startTimeStr, endTimeStr);
        if (lblAuctionId != null) lblAuctionId.setText(String.valueOf(auction.getId()));
        if (lblCurrentPrice != null) lblCurrentPrice.setText(currentPriceStr);

        // Hiển thị người đang dẫn đầu (nếu có)
        if (lblTopBidder != null) {
            lblTopBidder.setText(auction.getWinnerId() != null && auction.getWinnerId() > 0
                    ? "Thành viên #" + auction.getWinnerId() : "Chưa có");
        }

        Platform.runLater(() -> {
            tryLoadImageToView(imgProduct, imagePath);
            startCountdownClock(auction.getEndTime());
            checkBiddingPermissions(auction.getSellerId());
            priceSeries.getData().clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, currentPriceVal));
        });

             new Thread(() -> {
                BidDAO bidDAO = new BidDAO();
                List<BidTransaction> history = bidDAO.getBidsByAuction(auction.getId());
                Platform.runLater(() -> {
                    // Reset biểu đồ
                    priceSeries.getData().clear();
                    bidCount = 0;

                    // Điểm khởi đầu = giá khởi điểm
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, auction.getStartPrice()));

                    // Rebuild từ lịch sử DB — đã sort ASC nên thứ tự đúng
                    for (BidTransaction b : history) {
                        bidCount++;
                        priceSeries.getData().add(new XYChart.Data<>(bidCount, b.getBidAmount()));
                    }

                    // Rebuild lịch sử text
                    if (vboxBidHistoryContainer != null) {
                        vboxBidHistoryContainer.getChildren().clear();
                        for (int i = history.size() - 1; i >= 0; i--) {
                            BidTransaction b = history.get(i);
                            Label log = new Label(String.format("• [%s] %s đặt mức giá %,.0f đ",
                                    b.getBidTime().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                                    b.getBidderUsername(),
                                    b.getBidAmount()));
                            log.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                            vboxBidHistoryContainer.getChildren().add(0, log);
                        }
                    }
                });
         }).start();
    }

    /**
     * Điền dữ liệu vào tất cả các Label thông tin.
     */
    private void fillTextFields(String title, String price, String desc,
                                String seller, String start, String end) {
        if (lblProductTitle != null) lblProductTitle.setText(title);
        if (lblInfoName != null) lblInfoName.setText(title);
        if (lblStartPrice != null) lblStartPrice.setText(price);
        if (lblCurrentPrice != null) lblCurrentPrice.setText(price);
        if (lblInfoDescription != null) lblInfoDescription.setText(desc);
        if (lblSellerName != null) lblSellerName.setText(seller);
        if (lblStartTime != null) lblStartTime.setText(start);
        if (lblEndTime != null) lblEndTime.setText(end);
    }

     // ĐẶT GIÁ

    /**
     * Xử lý khi người dùng nhấn nút "Đặt giá ngay".
     * Validate input rồi gọi processValidBidUpdate().
     */
    private void handleManualBid() {
        // ⚠️ Nếu đang xử lý bid thì bỏ qua click tiếp theo
        if (isBidding) return;
        isBidding = true;

        try {
            if (txtBidAmount == null || txtBidAmount.getText().trim().isEmpty()) {
                showAlert("Thông báo", "Vui lòng điền số tiền hợp lệ!");
                return;
            }

            double bidAmount = Double.parseDouble(txtBidAmount.getText().trim());
            double currentPrice = getCurrentPriceOnUI();

            if (bidAmount <= currentPrice) {
                showAlert("Lỗi đặt giá", "Giá đặt mới bắt buộc phải lớn hơn giá hiện tại!");
                return;
            }

            String activeUser = CurrentAccount.getAccount() != null
                    ? CurrentAccount.getAccount().getUsername() : "Ẩn danh";
            processValidBidUpdate(activeUser, bidAmount);

            if (isAutoBidEnabled) triggerAutoBidSimulation(bidAmount);

        } catch (NumberFormatException e) {
            showAlert("Lỗi dữ liệu", "Vui lòng nhập định dạng số!");
        } finally {
            // ✅ Reset sau 500ms — đủ để chặn double-click nhưng không khoá lâu
            new Thread(() -> {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                isBidding = false;
            }).start();
        }
    }

    /**
     * Lưu lịch sử đặt giá vào DB rồi cập nhật UI.
     *
     * ⚠️ Chỉ lưu DB khi CẢ HAI điều kiện đúng:
     *   - currentAccount != null  → người dùng đã đăng nhập
     *   - currentAuction != null  → đang xem một phiên đấu giá cụ thể
     * Nếu một trong hai null thì bỏ qua bước lưu DB (chỉ update UI).
     */
    private void processValidBidUpdate(String username, double amount) {
        Account currentAccount = CurrentAccount.getAccount();

        // Debug — xem biến nào null thì biết lý do không lưu được DB
        System.out.println("DEBUG account=" + currentAccount + " | auction=" + currentAuction);

        if (currentAccount != null && currentAuction != null) {
            BidTransaction bid = new BidTransaction();
            bid.setAuctionId(currentAuction.getId());
            // ⚠️ getId() trả về String nên phải parse sang int
            bid.setBidderId(Integer.parseInt(String.valueOf(currentAccount.getId())));
            bid.setBidAmount(amount);

            BidDAO bidDAO = new BidDAO();
            boolean saved = bidDAO.insertBid(bid);
            // Cập nhật giá hiện tại trong bảng Auctions
            AuctionDAO auctionDAO = new AuctionDAO();
            auctionDAO.updateCurrentPrice(currentAuction.getId(), amount,
                    Integer.parseInt(String.valueOf(currentAccount.getId())));
            System.out.println("DEBUG insertBid result=" + saved); // true = lưu thành công
            if (!saved) {
                showAlert("Lỗi", "Không thể lưu lịch sử đặt giá!");
                return;
            }
        }

        // Cập nhật UI trên JavaFX Application Thread
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0f đ", amount));
            if (lblTopBidder != null) lblTopBidder.setText(username);

            // Thêm dòng log vào lịch sử (mới nhất lên đầu)
            if (vboxBidHistoryContainer != null) {
                Label log = new Label(String.format("• [%s] Người dùng %s đặt mức giá %,.0f đ",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                        username, amount));
                log.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                vboxBidHistoryContainer.getChildren().add(0, log);
            }

            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, amount));
        });
    }

     // AUTO-BID

    /**
     * Xử lý khi người dùng bật/tắt auto-bid.
     * Nếu bật: hỏi mức giá tối đa, mặc định tăng 50,000đ mỗi lần.
     */
    private void handleAutoBidToggle() {
        if (btnAutoBid == null) return;
        if (btnAutoBid.isSelected()) {
            TextInputDialog dialog = new TextInputDialog("5000000");
            dialog.setTitle("Thiết lập Auto-Bid");
            dialog.setHeaderText("Hệ thống tự động nâng giá khi có đối thủ cạnh tranh.");
            dialog.setContentText("Nhập mức giá tối đa bạn có thể trả:");
            var result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    this.maxAutoBidAmount = Double.parseDouble(result.get());
                    this.autoBidIncrement = 50000.0;
                    this.isAutoBidEnabled = true;
                    btnAutoBid.setText("Bật");
                } catch (Exception e) {
                    btnAutoBid.setSelected(false);
                }
            } else {
                btnAutoBid.setSelected(false);
            }
        } else {
            this.isAutoBidEnabled = false;
            btnAutoBid.setText("Tắt");
        }
    }

    /**
     * Tự động đặt giá cao hơn đối thủ sau 2 giây.
     * Chỉ thực hiện nếu giá mới vẫn trong ngưỡng maxAutoBidAmount.
     */
    private void triggerAutoBidSimulation(double opponentBid) {
        if (opponentBid + autoBidIncrement <= maxAutoBidAmount) {
            Timeline autoBidTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                double myNewBid = opponentBid + autoBidIncrement;

                // ⚠️ AutoBot chỉ update UI — không lưu DB (bid thật đã lưu ở handleManualBid)
                Platform.runLater(() -> {
                    if (lblCurrentPrice != null)
                        lblCurrentPrice.setText(String.format("%,.0f đ", myNewBid));
                    if (lblTopBidder != null)
                        lblTopBidder.setText("AutoBot");
                    if (vboxBidHistoryContainer != null) {
                        Label log = new Label(String.format("• [%s] AutoBot tự động đặt %,.0f đ",
                                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), myNewBid));
                        log.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                        vboxBidHistoryContainer.getChildren().add(0, log);
                    }
                    bidCount++;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, myNewBid));
                });
            }));
            autoBidTimer.play();
        }
    }

    // ĐỒNG HỒ ĐẾM NGƯỢC

    /**
     * Bắt đầu đồng hồ đếm ngược đến thời điểm kết thúc phiên.
     * Tự động dừng và vô hiệu hóa đặt giá khi hết giờ.
     */
    private void startCountdownClock(LocalDateTime endTime) {
        if (countdownTimeline != null) countdownTimeline.stop(); // Dừng timer cũ nếu có

        if (endTime == null) {
            if (lblTimeRemaining != null) lblTimeRemaining.setText("Vô thời hạn");
            return;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            java.time.Duration duration = java.time.Duration.between(LocalDateTime.now(), endTime);
            if (duration.isZero() || duration.isNegative()) {
                if (lblTimeRemaining != null) {
                    lblTimeRemaining.setText("00h 00m 00s (Đã kết thúc)");
                    lblTimeRemaining.setStyle("-fx-text-fill: #ff4d4d; -fx-font-weight: bold;");
                }
                disableBiddingFeatures("Phiên đấu giá đã kết thúc!");
                countdownTimeline.stop();
            } else {
                long hours = duration.toHours();
                long minutes = duration.toMinutesPart();
                long seconds = duration.toSecondsPart();
                if (lblTimeRemaining != null)
                    lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    // KIỂM TRA QUYỀN

    /**
     * Kiểm tra người dùng hiện tại có được phép đặt giá không.
     * Vô hiệu hóa nếu: chưa đăng nhập hoặc là chủ sở hữu sản phẩm.
     */
    private void checkBiddingPermissions(int sellerId) {
        if (CurrentAccount.getAccount() == null) {
            disableBiddingFeatures("Vui lòng đăng nhập hệ thống!");
            return;
        }
        try {
            int currentUserId = Integer.parseInt(CurrentAccount.getAccount().getId());
            if (currentUserId == sellerId) {
                disableBiddingFeatures("Bạn là chủ sở hữu sản phẩm này!");
                if (btnSubmitBid != null) btnSubmitBid.setText("Sản phẩm của bạn");
            } else {
                if (txtBidAmount != null) txtBidAmount.setDisable(false);
                if (btnSubmitBid != null) {
                    btnSubmitBid.setDisable(false);
                    btnSubmitBid.setText("Đặt giá ngay");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi kiểm tra quyền: " + e.getMessage());
        }
    }

    /** Vô hiệu hóa ô nhập giá và nút đặt giá, hiển thị thông báo lý do. */
    private void disableBiddingFeatures(String message) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(true);
            txtBidAmount.setPromptText(message);
        }
        if (btnSubmitBid != null) btnSubmitBid.setDisable(true);
    }

     // TIỆN ÍCH

    /** Lấy giá hiện tại đang hiển thị trên UI (parse từ text label). */
    private double getCurrentPriceOnUI() {
        if (lblCurrentPrice == null || lblCurrentPrice.getText() == null) return 0.0;
        try {
            return Double.parseDouble(lblCurrentPrice.getText().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * Load ảnh vào ImageView từ nhiều nguồn theo thứ tự ưu tiên:
     * 1. Base64 (lưu trong DB)
     * 2. URL online (http/https)
     * 3. File local (C:/uet_uploads/)
     * 4. Resource trong project (/images/)
     * 5. Fallback: uet_logo.png
     */
    private void tryLoadImageToView(ImageView imgView, String imagePath) {
        if (imgView == null) return;
        try {
            if (imagePath == null || imagePath.trim().isEmpty()) {
                imgView.setImage(new Image(getClass().getResourceAsStream("/images/uet_logo.png")));
                return;
            }
            if (imagePath.startsWith("base64:")) {
                byte[] bytes = Base64.getDecoder().decode(imagePath.substring(7));
                imgView.setImage(new Image(new ByteArrayInputStream(bytes)));
                return;
            }
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                imgView.setImage(new Image(imagePath, true)); // true = load bất đồng bộ
                return;
            }
            File file = new File("C:/uet_uploads/" + imagePath);
            if (file.exists()) {
                imgView.setImage(new Image(file.toURI().toString()));
                return;
            }
            InputStream is = getClass().getResourceAsStream("/images/" + imagePath);
            if (is != null) {
                imgView.setImage(new Image(is));
            } else {
                imgView.setImage(new Image(getClass().getResourceAsStream("/images/uet_logo.png")));
            }
        } catch (Exception e) {
            System.err.println("Không tải được ảnh: " + e.getMessage());
        }
    }

    /** Hiển thị hộp thoại thông báo đơn giản. */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /** Gọi khi nhấn nút "← Quay lại" — dừng đồng hồ đếm ngược để tránh memory leak. */
    @FXML
    private void handleBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
    }
}