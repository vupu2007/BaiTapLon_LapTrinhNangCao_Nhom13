package com.auction.client.controller;

import com.auction.client.service.CreateProductService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
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

    // Nạp tầng Service xử lý mạng bất đồng bộ
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

        Stage stage = (Stage) lblImagePath.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            this.productImgFile = selectedFile;
            lblImagePath.setText(selectedFile.getName());
        }
    }

    @FXML
    private void handleCreateAuction() {
        String name = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceText = startPriceField.getText().trim();

        // 1. Kiểm tra tính hợp lệ của dữ liệu đầu vào
        if (name.isEmpty() || priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Tên sản phẩm và Giá khởi điểm!");
            return;
        }

        double startPrice;
        try {
            startPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Giá khởi điểm phải là một số hợp lệ!");
            return;
        }

        LocalDateTime startTime = LocalDateTime.of(startDatePicker.getValue(),
                LocalTime.of(Integer.parseInt(startHourCombo.getValue()), Integer.parseInt(startMinuteCombo.getValue())));
        LocalDateTime endTime = LocalDateTime.of(endDatePicker.getValue(),
                LocalTime.of(Integer.parseInt(endHourCombo.getValue()), Integer.parseInt(endMinuteCombo.getValue())));

        if (endTime.isBefore(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

        // 2. Ủy quyền tác vụ mạng chạy ngầm thông qua lớp Service trung gian
        productService.createAuctionPipelineAsync(itemId, name, description, startPrice, ownerId,
                productImgFile, startTimeStr, endTimeStr, response -> {

                    if (response != null && response.isSuccess()) {
                        // 3. Khởi tạo đối tượng Model để đẩy trực tiếp lên màn hình thời gian thực Client
                        Auction newAuction = new Auction();
                        newAuction.setItemId(itemId);
                        newAuction.setProductName(name);
                        newAuction.setStartPrice(startPrice);
                        newAuction.setCurrentPrice(startPrice);
                        newAuction.setStartTime(startTime);
                        newAuction.setEndTime(endTime);
                        newAuction.setSellerId(ownerId);
                        newAuction.setStatus(Auction.AuctionStatus.OPEN);
                        newAuction.setAccount(CurrentAccount.getAccount());

                        if (MainController.getInstance() != null) {
                            MainController.getInstance().addAuctionToRealtimeUI(newAuction);
                        }

                        showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công!");
                        handleCancel(); // Reset form sạch sẽ

                        // Trì hoãn 300ms rồi chuyển luồng mượt mà sang màn hình chi tiết sản phẩm vừa tạo
                        PauseTransition pause = new PauseTransition(Duration.millis(300));
                        pause.setOnFinished(pEvent -> {
                            if (MainLayoutController.getInstance() != null) {
                                MainLayoutController.getInstance().openAuctionDetailWithObject(newAuction);
                            }
                        });
                        pause.play();

                    } else {
                        String errorMsg = (response != null) ? response.getMessage() : "Mạng không phản hồi.";
                        showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể kích hoạt phiên: " + errorMsg);
                    }
                });
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
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}