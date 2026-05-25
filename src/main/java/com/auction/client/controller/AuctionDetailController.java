package com.auction.client.controller;

import com.auction.client.service.AuctionDetailService;
import com.auction.client.util.CurrentAccount;
import com.auction.client.util.ImageLoader;
import com.auction.shared.model.Item;
import com.auction.shared.model.Auction;
import com.auction.shared.network.Response;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionDetailController {

    @FXML public Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
            lblStartPrice, lblSellerName, lblStartTime, lblEndTime,
            lblCurrentPrice, lblTopBidder;

    @FXML private ImageView imgProduct;
    @FXML private LineChart<Number, Number> chartPriceHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;
    @FXML private ToggleButton btnAutoBid;
    @FXML private VBox vboxBidHistoryContainer;

    private Auction currentAuction;
    private Item currentItem;
    private Timeline countdownTimeline;
    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;

    private boolean isAutoBidEnabled = false;
    private double maxAutoBidAmount = 0.0;
    private double autoBidIncrement = 0.0;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // Khởi tạo lớp nghiệp vụ điều phối mạng riêng biệt
    private final AuctionDetailService detailService = new AuctionDetailService();

    @FXML
    public void initialize() {
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Giá hiện tại");
        if (chartPriceHistory != null) {
            chartPriceHistory.getData().clear();
            chartPriceHistory.getData().add(priceSeries);
            chartPriceHistory.setAnimated(false);
        }

        if (btnSubmitBid != null) {
            btnSubmitBid.setOnAction(event -> handleManualBid());
        }

        if (btnAutoBid != null) {
            btnAutoBid.setOnAction(event -> handleAutoBidToggle());
        }
    }

    private void startCountdownClock(LocalDateTime endTime) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

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

                if (lblTimeRemaining != null) {
                    lblTimeRemaining.setText(String.format("%02dh %02dm %02ds", hours, minutes, seconds));
                }
            }
        }));

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

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

    private void disableBiddingFeatures(String message) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(true);
            txtBidAmount.setPromptText(message);
        }
        if (btnSubmitBid != null) btnSubmitBid.setDisable(true);
    }

    public void loadProductDetail(Item item) {
        if (item == null) return;
        this.currentItem = item;
        this.currentAuction = null;

        String formattedPrice = String.format("%,.0f đ", item.getStartingPrice());
        String sellerName = "Người bán #" + item.getOwnerId();

        Platform.runLater(() -> {
            fillTextFields(item.getName(), formattedPrice, item.getDescription(), sellerName, "--/--/---- --:--", "--/--/---- --:--");
            ImageLoader.tryLoadImageToView(imgProduct, item.getImagePath()); // Sử dụng Util chuẩn hóa
            checkBiddingPermissions(item.getOwnerId());

            priceSeries.getData().clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, item.getStartingPrice()));
        });
    }

    public void loadProductDetail(Auction auction) {
        if (auction == null) return;
        this.currentAuction = auction;
        this.currentItem = null;

        // 🚀 ĐÃ CHUẨN HÓA: Ủy quyền tải dữ liệu vật phẩm mạng chạy ngầm về lớp Service trung gian
        detailService.fetchItemByIdAsync(auction.getItemId(), finalItem -> {
            String imagePath = (finalItem != null) ? finalItem.getImagePath() : null;
            String pName = (auction.getProductName() != null) ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
            String startPriceStr = String.format("%,.0f đ", auction.getStartPrice());

            double currentPriceVal = auction.getCurrentPrice() > 0 ? auction.getCurrentPrice() : auction.getStartPrice();
            String currentPriceStr = String.format("%,.0f đ", currentPriceVal);

            String sellerName = "Người bán #" + auction.getSellerId();
            String startTimeStr = auction.getStartTime() != null ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
            String endTimeStr = auction.getEndTime() != null ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";
            String winnerText = (auction.getWinnerId() != null && auction.getWinnerId() > 0) ? "Thành viên #" + auction.getWinnerId() : "Chưa có";

            fillTextFields(pName, startPriceStr, "Mã phiên: " + auction.getId(), sellerName, startTimeStr, endTimeStr);

            if (lblCurrentPrice != null) lblCurrentPrice.setText(currentPriceStr);
            if (lblTopBidder != null) lblTopBidder.setText(winnerText);

            ImageLoader.tryLoadImageToView(imgProduct, imagePath); // Sử dụng Util chuẩn hóa
            startCountdownClock(auction.getEndTime());
            checkBiddingPermissions(auction.getSellerId());

            priceSeries.getData().clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, currentPriceVal));
        });
    }

    private void fillTextFields(String title, String price, String desc, String seller, String start, String end) {
        if (lblProductTitle != null) lblProductTitle.setText(title);
        if (lblInfoName != null) lblInfoName.setText(title);
        if (lblStartPrice != null) lblStartPrice.setText(price);
        if (lblCurrentPrice != null) lblCurrentPrice.setText(price);
        if (lblInfoDescription != null) lblInfoDescription.setText(desc);
        if (lblSellerName != null) lblSellerName.setText(seller);
        if (lblStartTime != null) lblStartTime.setText(start);
        if (lblEndTime != null) lblEndTime.setText(end);
    }

    /**
     * 🔥 ĐÃ SỬA TOÀN DIỆN: Đặt giá đồng bộ trực tiếp lên Server cơ sở dữ liệu
     */
    private void handleManualBid() {
        if (txtBidAmount == null || txtBidAmount.getText().trim().isEmpty()) {
            showAlert("Thông báo", "Vui lòng điền số tiền hợp lệ!");
            return;
        }

        if (currentAuction == null) {
            showAlert("Lỗi", "Phiên đấu giá chưa kích hoạt hoặc không tồn tại dữ liệu phiên mạng!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(txtBidAmount.getText().trim());
            double currentPrice = getCurrentPriceOnUI();

            if (bidAmount <= currentPrice) {
                showAlert("Lỗi đặt giá", "Giá đặt mới bắt buộc phải lớn hơn giá hiện tại!");
                return;
            }

            int bidderId = Integer.parseInt(CurrentAccount.getAccount().getId());
            String activeUser = CurrentAccount.getAccount().getUsername();

            // Gửi yêu cầu trả giá qua Socket Service lên Server kiểm tra túi tiền và DB
            detailService.sendBidRequestAsync(currentAuction.getId(), bidderId, bidAmount, response -> {
                if (response != null && response.isSuccess()) {
                    // Nếu Server đồng ý duyệt lệnh nạp giá, cập nhật UI real-time lập tức
                    processValidBidUpdate(activeUser, bidAmount);
                    txtBidAmount.clear();

                    if (isAutoBidEnabled) {
                        triggerAutoBidSimulation(bidAmount);
                    }
                } else {
                    String errorMsg = response != null ? response.getMessage() : "Mạng không phản hồi.";
                    showAlert("Đặt giá thất bại", "Server từ chối lệnh: " + errorMsg);
                }
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi dữ liệu", "Vui lòng nhập định dạng số!");
        }
    }

    private void processValidBidUpdate(String username, double amount) {
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0f đ", amount));
            if (lblTopBidder != null) lblTopBidder.setText(username);

            if (vboxBidHistoryContainer != null) {
                Label log = new Label(String.format("• [%s] Người dùng %s đặt mức giá %,.0f đ",
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")), username, amount));
                log.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                vboxBidHistoryContainer.getChildren().add(0, log);
            }

            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, amount));
        });
    }

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

    private void triggerAutoBidSimulation(double opponentBid) {
        if (opponentBid + autoBidIncrement <= maxAutoBidAmount) {
            Timeline autoBidTimer = new Timeline(new KeyFrame(Duration.seconds(2), event -> {
                double myNewBid = opponentBid + autoBidIncrement;
                processValidBidUpdate("AutoBot", myNewBid);
            }));
            autoBidTimer.play();
        }
    }

    private double getCurrentPriceOnUI() {
        if (lblCurrentPrice == null || lblCurrentPrice.getText() == null) return 0.0;
        try {
            return Double.parseDouble(lblCurrentPrice.getText().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        if (countdownTimeline != null) countdownTimeline.stop();
    }
}