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
import javafx.fxml.FXMLLoader;

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

        // Biến cục bộ để lưu giữ chuỗi dữ liệu ảnh Base64 đồng bộ sang trang chi tiết
        final String[] sharedImageHolder = {imageNameInitial};

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

                // Đọc dữ liệu ảnh và chuyển Base64 ngầm
                if (productImgFile != null) {
                    byte[] fileBytes = Files.readAllBytes(productImgFile.toPath());
                    String base64 = java.util.Base64.getEncoder().encodeToString(fileBytes);
                    sharedImageHolder[0] = "base64:" + base64;
                }
                newItem.setImagePath(sharedImageHolder[0]);

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
                // 🔥 ĐÓNG GÓI ĐẦY ĐỦ THÔNG TIN ĐỂ TRUYỀN SANG TRANG CHI TIẾT KHÔNG BỊ TRỐNG
                Auction newAuction = new Auction();
                newAuction.setItemId(itemId);
                newAuction.setProductName(name); // Thêm tên SP
                newAuction.setStartPrice(finalStartPrice);
                newAuction.setCurrentPrice(finalStartPrice);
                newAuction.setStartTime(startTime);
                newAuction.setEndTime(endTime); // Thêm thời gian kết thúc
                newAuction.setSellerId(ownerId);
                newAuction.setStatus(Auction.AuctionStatus.OPEN);
                newAuction.setAccount(CurrentAccount.getAccount()); // Gán luôn account người bán để hiện tên chính xác

                // Đồng bộ dùng mẹo qua Reflection hoặc thuộc tính động của ảnh nếu có thể
                try {
                    java.lang.reflect.Method setImgMethod = newAuction.getClass().getMethod("setProductName", String.class);
                    setImgMethod.invoke(newAuction, name);
                } catch(Exception ex) {}

                if (MainController.getInstance() != null) {
                    MainController.getInstance().addAuctionToRealtimeUI(newAuction);
                }

                // Hiện Alert thông báo cho người dùng thành công
                showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công và đã được đưa lên sàn!");

                // Sao lưu lại object hoàn chỉnh trước khi clear Form nhập liệu
                final Auction auctionToNavigate = newAuction;
                final String finalProductImage = sharedImageHolder[0];

                // Reset trắng form nhập liệu gốc
                handleCancel();

                // 🌟 CHÌA KHÓA VÀNG: Đợi 300ms cho DB ổn định rồi ép chuyển THẲNG sang tab Chi tiết sản phẩm
                PauseTransition pause = new PauseTransition(Duration.millis(300));
                pause.setOnFinished(pEvent -> {
                    if (MainLayoutController.getInstance() != null) {
                        System.out.println("🔄 Đang chuyển hướng trực tiếp sang màn hình Chi tiết sản phẩm vừa tạo...");

                        // Gọi hàm Object thông minh của MainLayoutController để lật trang và đẩy dữ liệu toàn vẹn
                        MainLayoutController.getInstance().openAuctionDetailWithObject(auctionToNavigate);

                        // Đồng thời bồi thêm hàm nạp ảnh Base64 trực tiếp vào View để đảm bảo hình ảnh hiển thị ngay tắp lự
                        try {
                            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AuctionDetailView.fxml"));
                            if (getClass().getResource("/view/AuctionDetailView.fxml") == null) {
                                loader = new FXMLLoader(getClass().getResource("/view/AuctionDetail.fxml"));
                            }
                            // Truyền thủ công bằng chuỗi 8 tham số phòng hờ Realtime chưa kịp nạp luồng DB
                            MainLayoutController.getInstance().openAuctionDetail(
                                    name,
                                    String.format("%,.0f đ", finalStartPrice),
                                    null,
                                    finalProductImage,
                                    "Trạng thái phiên: OPEN",
                                    CurrentAccount.getAccount().getUsername(),
                                    startTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                                    endTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            );
                        } catch (Exception err) {
                            err.printStackTrace();
                        }
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