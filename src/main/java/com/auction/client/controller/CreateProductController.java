package com.auction.client.controller;

import com.auction.client.util.CurrentAccount;
import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Electronics;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CreateProductController {

    @FXML private TextField productNameField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField startPriceField;
    // Thuộc tính hình ảnh mới
    @FXML private Label lblImagePath;

    // Thuộc tính thời gian dạng bảng chọn mới
    @FXML private DatePicker startDatePicker;
    @FXML private ComboBox<String> startHourCombo;
    @FXML private ComboBox<String> startMinuteCombo;

    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> endHourCombo;
    @FXML private ComboBox<String> endMinuteCombo;

    // 🟢 ĐÃ THÊM: Biến toàn cục để lưu vết file ảnh vật lý mà người dùng chọn
    private File productImgFile = null;

    // Khởi tạo đối tượng DAO để làm việc với Database
    private final ItemDAO itemDAO = new ItemDAO();

    @FXML
    public void initialize() {
        // 1. Khởi tạo danh sách giờ đầy đủ (00 -> 23)
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }

        // 2. Khởi tạo danh sách phút ĐẦY ĐỦ từ 00 đến 59
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }

        // Đổ dữ liệu vào các ComboBox trên giao diện
        startHourCombo.setItems(hours);
        endHourCombo.setItems(hours);

        startMinuteCombo.setItems(minutes);
        endMinuteCombo.setItems(minutes);

        // Đặt ngày mặc định cho bảng chọn ngày
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1)); // Ngày kết thúc mặc định là ngày hôm sau

        // Đặt giờ/phút mặc định xuất hiện sẵn trên giao diện để tránh bị trống ô
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
            // 🟢 ĐÃ CẬP NHẬT: Lưu tệp tin vào biến toàn cục của Class để hàm tạo sản phẩm sử dụng sao chép
            this.productImgFile = selectedFile;
            lblImagePath.setText(selectedFile.getName());
            System.out.println("Đường dẫn file ảnh tĩnh để xử lý sau: " + selectedFile.getAbsolutePath());
        }
    }

    @FXML
    private void handleCreateAuction() {
        // Hàm này chạy khi bấm nút "Tạo phiên đấu giá"
        String name = productNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        String priceText = startPriceField.getText().trim();

        // 1. Kiểm tra validation cơ bản tránh rỗng hoặc lỗi số
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

        // 2. Thu thập và định dạng chuỗi Thời gian chuẩn cho cơ sở dữ liệu (MySQL DATETIME)
        LocalDate startDate = startDatePicker.getValue();
        int startHour = Integer.parseInt(startHourCombo.getValue());
        int startMinute = Integer.parseInt(startMinuteCombo.getValue());
        LocalDateTime startTime = LocalDateTime.of(startDate, LocalTime.of(startHour, startMinute));

        LocalDate endDate = endDatePicker.getValue();
        int endHour = Integer.parseInt(endHourCombo.getValue());
        int endMinute = Integer.parseInt(endMinuteCombo.getValue());
        LocalDateTime endTime = LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute));

        // Kiểm tra logic thời gian kết thúc phải sau thời gian bắt đầu
        if (endTime.isBefore(startTime)) {
            showAlert(Alert.AlertType.WARNING, "Lỗi thời gian", "Thời gian kết thúc phiên đấu giá phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        // Định dạng chuỗi ngày giờ TRƯỚC KHI gọi DAO để tránh lỗi "cannot find symbol"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime.format(formatter);
        String endTimeStr = endTime.format(formatter);

        try {
            // Tự động sinh ID duy nhất, ngẫu nhiên cho sản phẩm
            String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            // Lấy ID của người dùng hiện tại đang đăng nhập hệ thống
            int ownerId = Integer.parseInt(CurrentAccount.getAccount().getId());

            // 3. ĐÓNG GÓI DỮ LIỆU SẢN PHẨM (Có kèm tên hình ảnh động)
            Electronics newItem = new Electronics();
            newItem.setItemId(itemId);
            newItem.setName(name);
            newItem.setDescription(description);
            newItem.setStartingPrice(startPrice);
            newItem.setCategoryId(1);
            newItem.setOwnerId(ownerId);
            newItem.setStatus("IN_AUCTION");

            // Lấy tên file ảnh từ Label giao diện gửi xuống DB qua trường Brand tạm thời
            String imageName = lblImagePath.getText();

            if (imageName.equals("Chưa chọn tệp nào") || imageName.isEmpty()) {
                imageName = null;
            }

            newItem.setImagePath(imageName);
            newItem.setBrand("");

            // 4. THỰC THI GHI VÀO DATABASE QUA TẦNG DAO
            boolean isItemSaved = itemDAO.insertItem(newItem);
            if (isItemSaved) {

                // 🟢 ĐÃ THÊM: LUỒNG TỰ ĐỘNG SAO CHÉP FILE ẢNH VẬT LÝ VÀO HỆ THỐNG
                if (productImgFile != null && !imageName.equals("default.png")) {
                    try {
                        File uploadDir = new File("C:/uet_uploads/");
                        if (!uploadDir.exists()) uploadDir.mkdirs();
                        Files.copy(productImgFile.toPath(),
                                new File(uploadDir, imageName).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                        System.out.println("✅ Đã lưu ảnh: " + imageName);
                    } catch (Exception imgEx) {
                        System.err.println("⚠️ Lỗi lưu ảnh: " + imgEx.getMessage());
                    }
                }
                // Kích hoạt phiên đấu giá tương ứng sang bảng Auctions (Lúc này đã nhận được startTimeStr and endTimeStr)
                boolean isAuctionStarted = itemDAO.startAuction(itemId, ownerId, startPrice, startTimeStr, endTimeStr);

                if (isAuctionStarted) {
                    // 5. ĐÓNG GÓI THÀNH ĐỐI TƯỢNG AUCTION THẬT VÀ ĐẨY LÊN GIAO DIỆN REALTIME
                    Auction newAuction = new Auction();
                    newAuction.setItemId(itemId);
                    newAuction.setStartPrice(startPrice);
                    newAuction.setCurrentPrice(startPrice);
                    newAuction.setStartTime(startTime);
                    newAuction.setStatus(Auction.AuctionStatus.OPEN);

                    // Đồng bộ đẩy thông tin lên giao diện Trang chủ của MainController ngay tức thì
                    if (MainController.getInstance() != null) {
                        MainController.getInstance().addAuctionToRealtimeUI(newAuction);
                    }

                    // Hiện thông báo thành công cho người dùng
                    showAlert(Alert.AlertType.INFORMATION, "Thông báo", "Tạo phiên đấu giá cho sản phẩm [" + name + "] thành công và đã được đưa lên sàn!");

                    // Xóa sạch form để sẵn sàng cho lần nhập tiếp theo
                    handleCancel();
                } else {
                    showAlert(Alert.AlertType.ERROR, "Lỗi hệ thống", "Không thể kích hoạt phiên đấu giá trên sàn đấu giá!");
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể lưu thông tin sản phẩm vào cơ sở dữ liệu!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Lỗi không xác định", "Đã xảy ra lỗi trong quá trình xử lý: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        // 1. Xóa rỗng các ô nhập văn bản
        productNameField.clear();
        descriptionArea.clear();
        startPriceField.clear();

        // 2. Đặt lại nhãn hình ảnh về trạng thái ban đầu và xóa vết file cũ
        lblImagePath.setText("Chưa chọn tệp nào");
        this.productImgFile = null; // Giải phóng bộ nhớ biến ảnh

        // 3. Đặt lại Ngày về mặc định (Ngày hôm nay và ngày mai)
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(1));

        // 4. Đặt lại Giờ và Phút về giá trị mặc định ban đầu
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