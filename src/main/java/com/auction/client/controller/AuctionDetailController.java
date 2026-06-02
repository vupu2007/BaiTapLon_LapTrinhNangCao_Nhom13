package com.auction.client.controller;

import com.auction.client.service.AuctionDetailService;
import com.auction.client.util.CurrentAccount;
import com.auction.client.util.ImageLoader;
import com.auction.client.network.ClientSocket;
import com.auction.shared.model.BidTransaction;
import com.auction.shared.model.Observer;
import com.auction.shared.model.Item;
import com.auction.shared.model.Auction;
import com.auction.shared.network.Request;
import com.auction.shared.network.Response;
import com.auction.shared.network.MessageType;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuctionDetailController implements Observer {

    @FXML public Label lblProductTitle, lblTimeRemaining, lblInfoName, lblInfoDescription,
            lblStartPrice, lblSellerName, lblStartTime, lblEndTime,
            lblCurrentPrice, lblTopBidder;

    @FXML private ImageView imgProduct;
    @FXML private LineChart<Number, Number> chartPriceHistory;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnSubmitBid;
    @FXML private ToggleButton btnAutoBid;
    @FXML private VBox vboxBidHistoryContainer;
    @FXML private Label lblAuctionId;

    // === CÁC THÀNH PHẦN MỚI THÊM ĐỂ HIỂN THỊ NGƯỜI CHIẾN THẮNG ===
    @FXML private VBox vboxWinnerSection;
    @FXML private Label lblWinnerName;
    @FXML private Label lblWinnerPrice;

    @FXML private Label lblTimeRemainingTitle;


    private Auction currentAuction;
    private Item currentItem;
    private Timeline countdownTimeline;
    private XYChart.Series<Number, Number> priceSeries;
    private int bidCount = 0;

    private boolean isAutoBidEnabled = false;
    private double maxAutoBidAmount = 0.0;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final AuctionDetailService detailService = new AuctionDetailService();

    private String lastEndTimeStr = ""; // Lính gác nhớ thời gian kết thúc cũ

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

    private double lastProcessedPrice = -1;


    @Override
    public void update(double newPrice, String usernameAndTimeToParse) {
        String finalUsername = usernameAndTimeToParse;
        String newEndTimeStr = null;

        if (usernameAndTimeToParse != null && usernameAndTimeToParse.contains("|")) {
            String[] parts = usernameAndTimeToParse.split("\\|");
            finalUsername = parts[0];
            newEndTimeStr = parts[1];
        }

        // 🎯 TẠO CÁC BIẾN FINAL ĐỂ FIX TRIỆT ĐỂ LỖI LAMBDA EXPRESSION
        final String targetUser = finalUsername;
        final String targetEndTimeStr = newEndTimeStr;

        // 🤖 LUỒNG AUTOBID: Chạy độc lập hoàn toàn
        Platform.runLater(() -> {
            if (isAutoBidEnabled && CurrentAccount.getAccount() != null && !targetUser.equals(CurrentAccount.getAccount().getUsername())) {
                triggerAutoBidSystem(newPrice);
            }
        });

        // 🛑 LUỒNG CHẶN LẶP LOG GIAO DIỆN
        if (newPrice == lastProcessedPrice) {
            return;
        }
        lastProcessedPrice = newPrice;

        // 💻 LUỒNG CẬP NHẬT GIAO DIỆN CHÍNH
        Platform.runLater(() -> {
            if (lblCurrentPrice != null) lblCurrentPrice.setText(String.format("%,.0f đ", newPrice));
            if (lblTopBidder != null) lblTopBidder.setText(targetUser);

            if (targetEndTimeStr != null && !targetEndTimeStr.isEmpty()) {
                try {
                    java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                    startCountdownClock(java.time.LocalDateTime.parse(targetEndTimeStr, dtf));
                } catch (Exception e) { System.err.println("❌ Lỗi: " + e.getMessage()); }
            }

            if (vboxBidHistoryContainer != null) {
                // Nhật ký giá thường
                Label log = new Label(String.format("• [%s] %s đặt mức giá %,.0f đ", LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")), targetUser, newPrice));
                log.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                vboxBidHistoryContainer.getChildren().add(0, log);

                // Thông báo chữ cam ngắn gọn mỗi khi gia hạn thành công
                if (targetEndTimeStr != null && !targetEndTimeStr.isEmpty()) {

                    // NẾU thời gian kết thúc vừa nhận được KHÁC với thời gian kết thúc cũ (tức là bị gia hạn)
                    if (!targetEndTimeStr.equals(this.lastEndTimeStr)) {

                        // Nếu là lần đầu mở form (lính gác chưa có dữ liệu) thì chỉ lưu lại, KHÔNG in thông báo
                        if (this.lastEndTimeStr.isEmpty()) {
                            this.lastEndTimeStr = targetEndTimeStr;
                        }
                        // Nếu đã có dữ liệu cũ rồi mà nay lại bị đổi -> Đích thị là Anti-snipping!
                        else {
                            this.lastEndTimeStr = targetEndTimeStr; // Cập nhật lại thời gian mới

                            Label alertLog = new Label(String.format("⚡ [%s] [GIA HẠN] Đặt giá giây cuối, phiên đấu giá tăng thêm 60 giây!",
                                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))));
                            alertLog.setStyle("-fx-font-size: 14px; -fx-padding: 5px; -fx-text-fill: #d35400; -fx-font-weight: bold;");

                            Platform.runLater(() -> {
                                vboxBidHistoryContainer.getChildren().add(0, alertLog);
                            });
                        }
                    }
                }
            }
            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, newPrice));
        });
    }
    private void startCountdownClock(LocalDateTime endTime) {
        if (countdownTimeline != null) countdownTimeline.stop();
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

                // GỌI HÀM HIỂN THỊ Ô NGƯỜI THẮNG CUỘC KHI HẾT GIỜ
                showWinnerSection();

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

    /**
     * HÀM MỚI: Xử lý hiển thị thông tin người thắng cuộc và cấu hình lại trạng thái giao diện
     */
    private void showWinnerSection() {
        if (vboxWinnerSection != null && lblWinnerName != null && lblWinnerPrice != null) {
            String winner = (lblTopBidder != null) ? lblTopBidder.getText() : "Chưa có";
            String finalPrice = (lblCurrentPrice != null) ? lblCurrentPrice.getText() : "0 đ";

            // Nếu hết giờ mà nhãn vẫn là mặc định thì xem như không ai đặt
            if ("Chưa có".equals(winner) || "Chưa xác định".equals(winner)) {
                lblWinnerName.setText("Không có người tham gia");
                lblWinnerPrice.setText("Phiên đấu giá không thành công");
            } else {
                lblWinnerName.setText(winner);
                lblWinnerPrice.setText("Giá chung cuộc: " + finalPrice);
            }

            // Hiển thị ô thông tin lên màn hình
            vboxWinnerSection.setManaged(true);
            vboxWinnerSection.setVisible(true);
        }
    }

    private void checkBiddingPermissions(int sellerId) {
        if (CurrentAccount.getAccount() == null) {
            disableBiddingFeatures("Vui lòng đăng nhập!"); return;
        }
        if (currentAuction != null && currentAuction.getStartTime() != null
                && java.time.LocalDateTime.now().isBefore(currentAuction.getStartTime())) {
            disableBiddingFeatures("Phiên đấu giá chưa bắt đầu!"); return;
        }
        try {
            int uid = Integer.parseInt(CurrentAccount.getAccount().getId());
            if (uid == sellerId) {
                disableBiddingFeatures("Bạn là chủ sở hữu!");
                if (btnSubmitBid != null) btnSubmitBid.setText("Sản phẩm của bạn");
            } else {
                if (txtBidAmount != null) txtBidAmount.setDisable(false);
                if (btnSubmitBid != null) { btnSubmitBid.setDisable(false); btnSubmitBid.setText("Đặt giá ngay"); }
            }
        } catch (Exception e) { System.err.println("Lỗi kiểm tra quyền: " + e.getMessage()); }
    }

    private void disableBiddingFeatures(String message) {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(true);
            txtBidAmount.setPromptText(message);
        }
        if (btnSubmitBid != null) btnSubmitBid.setDisable(true);
        if (btnAutoBid != null) btnAutoBid.setDisable(true);
    }

    public void loadProductDetail(Item item) {

        if (item == null) return;
        this.currentItem = item;
        this.currentAuction = null;

        String formattedPrice = String.format("%,.0f đ", item.getStartingPrice());

        String fetchedSeller;
        try {
            Response sellerResp = ClientSocket.getInstance().sendRequest(
                    new Request(MessageType.GET_ACCOUNT_BY_ID, item.getOwnerId()));
            com.auction.shared.model.Account seller = (sellerResp != null && sellerResp.isSuccess())
                    ? (com.auction.shared.model.Account) sellerResp.getData() : null;
            fetchedSeller = (seller != null) ? seller.getUsername() : "Người bán #" + item.getOwnerId();
        } catch (Exception e) {
            fetchedSeller = "Người bán #" + item.getOwnerId();
        }
        final String sellerName = fetchedSeller;

        Platform.runLater(() -> {
            fillTextFields(item.getName(), formattedPrice, item.getDescription(), sellerName, "--/--/---- --:--", "--/--/---- --:--");
            ImageLoader.tryLoadImageToView(imgProduct, item.getImagePath());
            checkBiddingPermissions(item.getOwnerId());

            priceSeries.getData().clear();
            bidCount = 0;
            priceSeries.getData().add(new XYChart.Data<>(bidCount, item.getStartingPrice()));
        });


        // Fetch auction từ server
        detailService.fetchAuctionByItemIdAsync(item.getId(), a -> {
            System.out.println("Auction nhận được: " + a);
            if (a != null) {
                this.currentAuction = a;
                ClientSocket.getInstance().addAuctionObserver(a.getId(), this);

                Platform.runLater(() -> {
                    if (a.getStartTime() != null && LocalDateTime.now().isBefore(a.getStartTime())) {
                        startCountdownClock(a.getStartTime());
                    } else {
                        startCountdownClock(a.getEndTime());
                    }                    if (lblCurrentPrice != null)
                        lblCurrentPrice.setText(String.format("%,.0f đ", a.getCurrentPrice()));
                });
                detailService.fetchBidHistoryAsync(a.getId(), bids -> {
                    Platform.runLater(() -> {
                        if (vboxBidHistoryContainer != null && bids != null) {
                            vboxBidHistoryContainer.getChildren().clear();
                            for (BidTransaction bid : bids) {
                                Label label = new Label(String.format("• %s đặt %,.0f đ",
                                        bid.getBidderUsername(), bid.getBidAmount()));
                                label.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                                vboxBidHistoryContainer.getChildren().add(label);
                                bidCount++;
                                priceSeries.getData().add(new XYChart.Data<>(bidCount, bid.getBidAmount()));
                            }
                        }
                    });
                });
            }
        });
    }
    public void loadProductDetail(Auction auction) {
        if (auction == null) return;
        System.out.println("seller=" + auction.getSellerName() + " desc=" + auction.getDescription());
        if (auction.getProductName() == null) {
            auction.setProductName("");
        }
        if (auction.getSellerName() == null) {
            auction.setSellerName("Người bán #" + auction.getSellerId());
        }

        this.currentAuction = auction;
        this.currentItem = null;

        ClientSocket.getInstance().addAuctionObserver(auction.getId(), this);

        detailService.fetchItemByIdAsync(auction.getItemId(), finalItem -> {
            String imagePath = (finalItem != null) ? finalItem.getImagePath() : null;
            String pName = (auction.getProductName() != null) ? auction.getProductName() : "Sản phẩm #" + auction.getItemId();
            String startPriceStr = String.format("%,.0f đ", auction.getStartPrice());

            double currentPriceVal = auction.getCurrentPrice() > 0 ? auction.getCurrentPrice() : auction.getStartPrice();
            String currentPriceStr = String.format("%,.0f đ", currentPriceVal);

            String fetchedSeller;
            try {
                Response sellerResp = ClientSocket.getInstance().sendRequest(
                        new Request(MessageType.GET_ACCOUNT_BY_ID, auction.getSellerId()));
                com.auction.shared.model.Account seller = (sellerResp != null && sellerResp.isSuccess())
                        ? (com.auction.shared.model.Account) sellerResp.getData() : null;
                fetchedSeller = (seller != null) ? seller.getUsername() : "Người bán #" + auction.getSellerId();
            } catch (Exception e) {
                fetchedSeller = "Người bán #" + auction.getSellerId();
            }
            final String sellerName = fetchedSeller;            String startTimeStr = auction.getStartTime() != null ? auction.getStartTime().format(dateTimeFormatter) : "--/--/---- --:--";
            String endTimeStr = auction.getEndTime() != null ? auction.getEndTime().format(dateTimeFormatter) : "--/--/---- --:--";

            String winnerText;
            if (auction.getWinnerId() != null && auction.getWinnerId() > 0) {
                try {
                    Response winnerResp = ClientSocket.getInstance().sendRequest(
                            new Request(MessageType.GET_ACCOUNT_BY_ID, auction.getWinnerId()));
                    com.auction.shared.model.Account winner = (winnerResp != null && winnerResp.isSuccess())
                            ? (com.auction.shared.model.Account) winnerResp.getData() : null;
                    winnerText = (winner != null) ? winner.getUsername() : "Thành viên #" + auction.getWinnerId();
                } catch (Exception e) {
                    winnerText = "Thành viên #" + auction.getWinnerId();
                }
            } else {
                winnerText = "Chưa có";
            }
            final String finalWinnerText = winnerText;
            Platform.runLater(() -> {
                fillTextFields(pName, startPriceStr, (finalItem != null && finalItem.getDescription() != null) ? finalItem.getDescription() : "", sellerName, startTimeStr, endTimeStr);
                if (lblAuctionId != null) lblAuctionId.setText(String.valueOf(auction.getId()));
                if (lblCurrentPrice != null) lblCurrentPrice.setText(currentPriceStr);
                if (lblTopBidder != null) lblTopBidder.setText(finalWinnerText);
                ImageLoader.tryLoadImageToView(imgProduct, imagePath);
                boolean notStarted = auction.getStartTime() != null && LocalDateTime.now().isBefore(auction.getStartTime());
                startCountdownClock(notStarted ? auction.getStartTime() : auction.getEndTime());
                if (lblTimeRemainingTitle != null) lblTimeRemainingTitle.setText(notStarted ? "Bắt đầu sau" : "Thời gian còn lại");
                checkBiddingPermissions(auction.getSellerId());
            });
        });
        detailService.fetchBidHistoryAsync(auction.getId(), bids -> {
            Platform.runLater(() -> {
                if (vboxBidHistoryContainer != null && bids != null) {
                    vboxBidHistoryContainer.getChildren().clear();
                    priceSeries.getData().clear(); // THÊM
                    bidCount = 0;
                    priceSeries.getData().add(new XYChart.Data<>(bidCount, auction.getStartPrice()));                    for (BidTransaction bid : bids) {
                        Label label = new Label(String.format("• [%s] %s đặt %,.0f đ",
                                bid.getBidTime() != null
                                        ? bid.getBidTime().plusHours(7).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
                                        : "--:--:--",
                                bid.getBidderUsername(),
                                bid.getBidAmount()));
                        label.setStyle("-fx-font-size: 14px; -fx-padding: 5px;");
                        vboxBidHistoryContainer.getChildren().add(label);
                        bidCount++;
                        priceSeries.getData().add(new XYChart.Data<>(bidCount, bid.getBidAmount()));
                    }
                }
            });
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

    private void handleManualBid() {
        if (txtBidAmount == null || txtBidAmount.getText().trim().isEmpty()) {
            showAlert("Thông báo", "Vui lòng điền số tiền hợp lệ!");
            return;
        }
        if (currentAuction == null) {
            showAlert("Lỗi", "Phiên đấu giá chưa kích hoạt!");
            return;
        }

        try {
            double bidAmount = Double.parseDouble(txtBidAmount.getText().trim());
            double currentPrice = getCurrentPriceOnUI();
            double minBid = currentAuction.getStartPrice() * 0.1;

            if (bidAmount < currentPrice + minBid) {
                showAlert("Lỗi đặt giá", "Giá đặt phải cao hơn giá hiện tại ít nhất "
                        + String.format("%,.0f đ", minBid) + " (10% giá khởi điểm)!");
                return;
            }

            int bidderId = Integer.parseInt(CurrentAccount.getAccount().getId());

            detailService.sendBidRequestAsync(currentAuction.getId(), bidderId, bidAmount, response -> {
                Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        txtBidAmount.clear();
                        showAlert("Thành công", "Đặt giá thành công!");
                    } else {
                        String errorMsg = response != null ? response.getMessage() : "Mạng không phản hồi.";
                        showAlert("Đặt giá thất bại", errorMsg);
                    }
                });
            });

        } catch (NumberFormatException e) {
            showAlert("Lỗi dữ liệu", "Vui lòng nhập định dạng số!");
        }
    }

    private void handleAutoBidToggle() {
        if (btnAutoBid == null) return;

        if (btnAutoBid.isSelected()) {
            TextInputDialog dialog = new TextInputDialog("5000000");
            dialog.setTitle("Thiết lập Auto-Bid");
            dialog.setHeaderText("Hệ thống tự động nâng giá tối thiểu 10% giá khởi điểm khi có đối thủ vượt bạn.");            dialog.setContentText("Nhập mức giá tối đa bạn có thể trả:");

            var result = dialog.showAndWait();
            if (result.isPresent()) {
                try {
                    double inputMax = Double.parseDouble(result.get());
                    double minBid = currentAuction.getStartPrice() * 0.1;

                    if (inputMax < getCurrentPriceOnUI() + minBid) {
                        showAlert("Lỗi thiết lập", "Mức giới hạn phải cao hơn giá hiện tại ít nhất "
                                + String.format("%,.0f đ", minBid) + " (10% giá khởi điểm)!");
                        btnAutoBid.setSelected(false);
                        return;
                    }
                    this.maxAutoBidAmount = inputMax;
                    this.isAutoBidEnabled = true;
                    btnAutoBid.setText("Auto-Bid: BẬT");

                    if (!CurrentAccount.getAccount().getUsername().equals(lblTopBidder.getText())) {
                        triggerAutoBidSystem(getCurrentPriceOnUI());
                    }
                } catch (Exception e) {
                    btnAutoBid.setSelected(false);
                }
            } else {
                btnAutoBid.setSelected(false);
            }
        } else {
            this.isAutoBidEnabled = false;
            btnAutoBid.setText("Auto-Bid: TẮT");
        }
    }

    private void triggerAutoBidSystem(double currentOpponentBid) {
        // 🎯 TÍNH BƯỚC GIÁ ĐỘNG 10% TỪ GIÁ KHỞI ĐIỂM CÓ SẴN TRONG AUCTION
        double stepPrice = 50000.0; // Mặc định phòng hờ nếu object bị null

        if (currentAuction != null) {
            // Gọi thẳng hàm lấy giá khởi điểm của phiên đấu giá hiện tại
            stepPrice = currentAuction.getStartPrice() * 0.10;
        }

        // Mức giá Robot sẽ tự động đặt trả đòn bằng: Giá đối thủ vừa đặt + Bước giá 10% chuẩn
        double myNewPrice = currentOpponentBid + stepPrice;

        if (myNewPrice <= maxAutoBidAmount) {
            Timeline autoBidDelay = new Timeline(new KeyFrame(Duration.seconds(1.5), event -> {
                if (currentAuction == null || CurrentAccount.getAccount() == null) return;

                int bidderId = Integer.parseInt(CurrentAccount.getAccount().getId());

                detailService.sendBidRequestAsync(currentAuction.getId(), bidderId, myNewPrice, response -> {
                    if (response != null && !response.isSuccess()) {
                        System.err.println("❌ Auto-Bid bị Server từ chối: " + response.getMessage());
                    }
                });
            }));
            autoBidDelay.play();
        } else {
            isAutoBidEnabled = false;
            if (btnAutoBid != null) {
                btnAutoBid.setSelected(false);
                btnAutoBid.setText("Auto-Bid: TẮT");
            }
            System.out.println("-> Giá phòng đã vượt quá giới hạn Auto-Bid tối đa của bạn.");
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
        if (currentAuction != null) {
            ClientSocket.getInstance().removeAuctionObserver(currentAuction.getId(), this);
        }

        if (txtBidAmount.getScene() == null) return;

        Platform.runLater(() -> {
            try {
                String homeFxmlPath = "/view/MainView.fxml";
                java.net.URL fxmlLocation = getClass().getResource(homeFxmlPath);
                if (fxmlLocation == null) {
                    System.err.println("❌ LỖI: Không tìm thấy tệp FXML trang chủ tại: " + homeFxmlPath);
                    return;
                }

                FXMLLoader loader = new FXMLLoader(fxmlLocation);
                Node homeView = loader.load();

                Parent root = txtBidAmount.getScene().getRoot();
                Node layoutCenter = root.lookup("#contentArea");

                if (layoutCenter instanceof StackPane contentArea) {
                    contentArea.getChildren().setAll(homeView);
                    System.out.println("🎯 [Navigation] Quay lại trang chủ thành công qua cơ chế Scene Graph Lookup.");
                } else {
                    System.err.println("❌ Không tìm thấy vùng chứa #contentArea trên giao diện hiện hành.");
                }

            } catch (Exception e) {
                System.err.println("❌ Lỗi nghiêm trọng khi quay lại trang chủ: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}