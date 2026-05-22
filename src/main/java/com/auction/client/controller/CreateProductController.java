package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Electronics;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
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
    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }

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
            System.out.println("Đường dẫn file ảnh tĩnh để xử lý sau: " + selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleCreateAuction() {
        String name = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceText = startPriceField.getText().trim();

        if (name.isEmpty() || priceText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Vui lòng nhập đầy đủ Tên sản phẩm và Giá khởi điểm!");
            return;
        }

        double startPrice = 0;
        try {
            startPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Lỗi nhập liệu", "Giá khởi điểm phải là một số hợp lệ!");
            return;
        }

        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourCombo.getValue());
        int startMinute = Integer.parseInt(startMinuteCombo.getValue());
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));

        LocalDate endDate = endDatePicker.getValue();
        int endHour = Integer.parseInt(endHourCombo.getValue());
        int endMinute = Integer.parseInt(endMinuteCombo.getValue());
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute));

        if (endTime.isBefore(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phiên đấu giá phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        // Chuẩn bị dữ liệu ban đầu
        String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());
        double finalStartPrice = startPrice;
        String imageNameInitial = lblImagePath.getText().equals("Chưa chọn tệp nào") || lblImagePath.getText().isEmpty() ? null : lblImagePath.getText();

        // 🚀 TỐI ƯU ĐA LUỒNG: Đẩy việc đọc file ảnh nặng và ghi DB xuống luồng ngầm
        Task<Boolean> databaseTask = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                Electronics newItem = new Electronics();
                newItem.setItemId(itemId);
                newItem.setName(name);
                newItem.setDescription(description);
                newItem.setStartingPrice(finalStartPrice);
                newItem.setCategoryId(1);
                newItem.setOwnerId(ownerId);
                newItem.setStatus("IN_AUCTION");
                newItem.setBrand("");

                String finalImageName = imageNameInitial;
                // Đọc dữ liệu ảnh và chuyển Base64 ngầm
                if (productImgFile != null) {
                    byte[] fileBytes = Files.readAllBytes(productImgFile.toPath());
                    String base64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
                    finalImageName = "base64:" + base64;
                }
                newItem.setImagePath(finalImageName);

                // Ghi đồng thời vào Database qua tầng DAO
                boolean isItemSaved = itemDAO.insertItem(newItem);
                if (!isItemSaved) return false;

                return itemDAO.startAuction(itemId, ownerId, finalStartPrice, startTimeStr, endTimeStr);
            }
        };

        // KHI LUỒNG NGẦM CHẠY XỬ LÝ DATABASE THÀNH CÔNG
        databaseTask.setOnSucceeded(event -> {
            boolean success = databaseTask.getValue();
            if (success) {
                // Đóng gói mô hình đẩy lên thời gian thực trang chủ
                Auction newAuction = new Auction();
                newAuction.setItemId(itemId);
                newAuction.setStartPrice(finalStartPrice);
                newAuction.setCurrentPrice(finalStartPrice);
                newAuction.setStartTime(startTime);
                newAuction.setStatus(Auction.AuctionStatus.OPEN);

                if (MainController.getInstance() != null) {
                    MainController.getInstance().addAuctionToRealtimeUI(newAuction);
                }

                // Hiện Alert thông báo cho người dùng
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công và đã được đưa lên sàn!");

                // Reset trắng form nhập liệu
                handleCancel();

                // 🌟 CHÌA KHÓA VÀNG: Trì hoãn 300ms đợi DB commit hoàn chỉnh rồi mới chuyển trang nhảy số
                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(pEvent -> {
                    if (MainLayoutController.getInstance() != null) {
                        System.out.println("🔄 DB ổn định! Đang tự động nhảy sang tab Đang bán để refresh...");
                        MainLayoutController.getInstance().openSelling();
                    }
                });
                pause.play();

            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể kích hoạt phiên đấu giá trên sàn đấu giá!");
            }
        });

        // KHI LUỒNG NGẦM GẶP LỖI
        databaseTask.setOnFailed(event -> {
            Throwable e = databaseTask.getException();
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Đã xảy ra lỗi trong quá trình xử lý Database ngầm: " + e.getMessage());
        });

        // Kích hoạt chạy luồng ngầm tách biệt luồng UI
        Thread thread = new Thread(databaseTask);
        thread.setDaemon(true);
        thread.start();
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

        System.out.println("Đã xóa toàn bộ form và đặt lại về mặc định!");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}