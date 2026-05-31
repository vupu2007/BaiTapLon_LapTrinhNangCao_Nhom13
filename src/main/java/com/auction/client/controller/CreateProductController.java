package com.auction.client.controller;

import com.auction.client.service.CreateProductService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CreateProductController {

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startPriceField;
    @FXML private Label lblImagePath;

    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;

    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;

    private File productImgFile = null;
    private final CreateProductService productService = new CreateProductService();


    @FXML
    public void initialize() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) hours.add(String.format("%02d", i));

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) minutes.add(String.format("%02d", i));

        startHourCombo.setItems(hours);
        endHourCombo.setItems(hours);
        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(minutes);

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        startHourCombo.setValue("08");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn hình ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        if (lblImagePath.getScene() != null) {
            Stage stage = (Stage) lblImagePath.getScene().getWindow();
            File selectedFile = fileChooser.showOpenDialog(stage);

            if (selectedFile != null) {
                this.productImgFile = selectedFile;
                lblImagePath.setText(selectedFile.getName());
            }
        }
    }

    private boolean isCreating = false;
    @FXML
    private void handleCreateAuction() {
        if (isCreating) return;
        isCreating = true;
        if (CurrentAccount.getAccount() == null) {
            isCreating = false;
            showAlert(Alert.AlertType.ERROR, "Lỗi phân quyền", "Vui lòng đăng nhập để tạo phiên đấu giá!");
            return;
        }

        String name = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceText = startPriceField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Tên sản phẩm và Giá khởi điểm!");
            return;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(priceText);
            if (startPrice <= 0) {
                showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Giá khởi điểm phải là một số hợp lệ!");
            return;
        }

        LocalDateTime startTime = LocalDateTime.of(startDatePicker.getValue(),
                LocalTime.of(Integer.parseInt(startHourCombo.getValue()), Integer.parseInt(startMinuteCombo.getValue())));
        LocalDateTime endTime = LocalDateTime.of(endDatePicker.getValue(),
                LocalTime.of(Integer.parseInt(endHourCombo.getValue()), Integer.parseInt(endMinuteCombo.getValue())));

        if (startTime.isBefore(LocalDateTime.now())) {
            showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian bắt đầu không thể nằm trong quá khứ!");
            return;
        }
        if (endTime.isBefore(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // Gọi Service gửi gói tin lên mạng bất đồng bộ
        productService.createAuctionPipelineAsync(itemId, name, description, startPrice, ownerId,
                productImgFile, startTimeStr, endTimeStr, response -> {

                    Platform.runLater(() -> {
                        isCreating = false; // ← unlock sau khi nhận response
                        if (response != null && response.isSuccess()) {
                            Auction newAuction = (response.getData() instanceof Auction)
                                    ? (Auction) response.getData()
                                    : new Auction();

                            if (!(response.getData() instanceof Auction)) {
                                newAuction.setItemId(itemId);
                                newAuction.setStartPrice(startPrice);
                                newAuction.setCurrentPrice(startPrice);
                                newAuction.setStartTime(startTime);
                                newAuction.setEndTime(endTime);
                                newAuction.setSellerId(ownerId);
                                newAuction.setStatus(Auction.AuctionStatus.OPEN);
                            }
                            newAuction.setProductName(name);

                            showAlert(Alert.AlertType.INFORMATION, "Thành công", "Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công!");
                            handleCancel();

                            final Auction finalAuction = newAuction;
                            PauseTransition pause = new PauseTransition(Duration.millis(300));
                            pause.setOnFinished(pEvent -> navigateToDetailView(finalAuction));
                            pause.play();
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Máy chủ không phản hồi.";
                            showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể kích hoạt phiên: " + errorMsg);
                        }
                    });
                });
    }

    /**
     * 🧠 KỸ THUẬT SCENE LOOKUP: Tự định vị vùng hiển thị của Layout cha để đổi trang mà không cần gọi Singleton
     */
    private void navigateToDetailView(Auction newAuction) {
        if (productNameField.getScene() == null) return;

        Parent root = productNameField.getScene().getRoot();
        Node layoutCenter = root.lookup("#contentArea");

        if (layoutCenter instanceof StackPane contentArea) {
            try {
                // Xác định đường dẫn file chi tiết thích hợp
                String path = getClass().getResource("/view/AuctionDetailView.fxml") != null
                        ? "/view/AuctionDetailView.fxml" : "/view/AuctionDetail.fxml";

                FXMLLoader loader = new FXMLLoader(getClass().getResource(path));
                Parent detailView = loader.load();

                // Truyền Model dữ liệu trực tiếp vào Controller mới vừa nạp
                AuctionDetailController detailController = loader.getController();
                if (detailController != null) {
                    detailController.loadProductDetail(newAuction);
                }

                // Chèn đè giao diện chi tiết vào trung tâm màn hình chính
                contentArea.getChildren().setAll(detailView);
                System.out.println("🎯 [Navigation] Đã chuyển tiếp sang trang chi tiết sản phẩm mới tạo.");
            } catch (IOException e) {
                System.err.println("❌ Không thể nạp trang chi tiết sản phẩm: " + e.getMessage());
            }
        } else {
            System.err.println("❌ Không thể định vị được vùng chứa trung tâm #contentArea.");
        }
    }

    @FXML
    private void handleCancel() {
        productNameField.clear();
        descriptionArea.clear();
        startPriceField.clear();
        lblImagePath.setText("Chưa chọn tệp nào");
        this.productImgFile = null;

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));
        startHourCombo.setValue("08");
        startMinuteCombo.setValue("00");
        endHourCombo.setValue("21");
        endMinuteCombo.setValue("00");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        if (Platform.isFxApplicationThread()) {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(content);
            alert.showAndWait();
        } else {
            Platform.runLater(() -> {
                Alert alert = new Alert(type);
                alert.setTitle(title);
                alert.setHeaderText(null);
                alert.setContentText(content);
                alert.showAndWait();
            });
        }
    }
}