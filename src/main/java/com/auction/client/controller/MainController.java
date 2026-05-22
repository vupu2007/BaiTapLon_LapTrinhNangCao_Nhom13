package com.auction.client.controller;

import com.auction.server.service.MainService;
import com.auction.client.util.CurrentAccount;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Account;
import com.auction.shared.model.User;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel, ongoingLabel, wonLabel, welcomeLabel;
    @FXML private Button btnFilterAll, btnFilterActive, btnFilterUpcoming;
    @FXML private FlowPane flowPane;

    private MainService mainService = new MainService();
    private static MainController instance;
    private String currentFilter = "ALL";
    private final String UPLOAD_DIR = "C:/uet_uploads/"; // Thư mục lưu ảnh thật bên ngoài

    public MainController() {}

    @FXML
    public void initialize() {
        instance = this;
        Account current = CurrentAccount.getAccount();
        if (current != null) {
            if (welcomeLabel != null) welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            refreshDashboard();
        }
    }

    public static MainController getInstance() { return instance; }

    public void refreshDashboard() {
        Account current = CurrentAccount.getAccount();
        if (balanceLabel != null) {
            balanceLabel.setText(current instanceof User ? String.format("%.0f VNĐ", ((User) current).getBalance()) : "N/A");
        }
        if (ongoingLabel != null) ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        if (wonLabel != null) wonLabel.setText(String.valueOf(mainService.getWonCount()));
        handleFilterAll();
    }

    // Tải ảnh ngắn gọn: Ưu tiên quét thư mục upload ngoài trước, lỗi thì về default hệ thống
    private void tryLoadImageToView(ImageView imgView, String preferredFileName) {

        System.out.println("🖼️ tryLoad: " + preferredFileName);

        if (preferredFileName.startsWith("base64:")) {
            try {
                byte[] bytes = java.util.Base64.getDecoder().decode(preferredFileName.substring(7));
                imgView.setImage(new Image(new java.io.ByteArrayInputStream(bytes)));
                return;
            } catch (Exception e) {
                System.err.println("❌ Lỗi decode Base64: " + e.getMessage());
            }
        }

        if (preferredFileName.startsWith("http://") || preferredFileName.startsWith("https://")) {
            try {
                Image img = new Image(preferredFileName, true);
                img.errorProperty().addListener((obs, old, isError) -> {
                    if (isError) System.err.println("❌ Lỗi load URL: " + img.getException().getMessage());
                });
                imgView.setImage(img);
                return;
            } catch (Exception e) {
                System.err.println("❌ Load URL thất bại: " + e.getMessage());
            }
        }
            // Phần còn lại giữ nguyên...
        if (preferredFileName == null || preferredFileName.trim().isEmpty()) {
            preferredFileName = "default.png";
        }

        String[] extensions = {".png", ".jpg", ".jpeg"};
        boolean loaded = false;

        // 1. Quét kho ảnh động bên ngoài ổ đĩa trước (Giữ nguyên tên gốc đầy đủ nếu có extension)
        File directFile = new File(UPLOAD_DIR + preferredFileName);
        if (directFile.exists() && directFile.isFile()) {
            imgView.setImage(new Image(directFile.toURI().toString()));
            return;
        }

        // Thử ghép thêm đuôi nếu preferredFileName truyền vào chỉ là ID chuỗi thuần túy
        for (String ext : extensions) {
            String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
            File extFile = new File(UPLOAD_DIR + fullFileName);
            if (extFile.exists()) {
                imgView.setImage(new Image(extFile.toURI().toString()));
                loaded = true; break;
            }
        }

        // 2. Dự phòng: Tìm trong resources hệ thống nếu chưa có ảnh động ngoài
        if (!loaded) {
            for (String ext : extensions) {
                String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
                java.io.InputStream is = getClass().getResourceAsStream("/com/auction/client/images/" + fullFileName);
                if (is != null) {
                    imgView.setImage(new Image(is));
                    loaded = true; break;
                }
            }
        }

        // 3. Cuối cùng: Nếu không có ảnh nào, nạp ảnh mặc định dự phòng chống trắng màn hình
        if (!loaded) {
            java.io.InputStream defaultIs = getClass().getResourceAsStream("/com/auction/client/images/default.png");
            if (defaultIs != null) imgView.setImage(new Image(defaultIs));
        }
    }

    @FXML
    private void handleFilterAll() {
        currentFilter = "ALL"; setButtonActive(btnFilterAll);
        if (flowPane == null) return; flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Đang diễn ra"));
        }
    }

    @FXML
    private void handleFilterActive() {
        currentFilter = "ACTIVE"; setButtonActive(btnFilterActive);
        if (flowPane == null) return; flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Đang diễn ra"));
        }
    }

    @FXML
    private void handleFilterUpcoming() {
        currentFilter = "UPCOMING"; setButtonActive(btnFilterUpcoming);
        if (flowPane == null) return; flowPane.getChildren().clear();
        for (Item item : mainService.getHotAuctions()) {
            flowPane.getChildren().add(createItemCardWithStatus(item, "Sắp diễn ra"));
        }
    }

    // ĐÃ SỬA: Ưu tiên lấy đường dẫn ảnh thực tế (image/imageUrl) của Item từ DB
    private VBox createItemCardWithStatus(Item item, String statusText) {

        VBox card = new VBox(); card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane(); imageHolder.setPrefHeight(180);
        Region bgRegion = new Region(); bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView(); imgView.setFitWidth(300); imgView.setFitHeight(180); imgView.setPreserveRatio(false);

        // ĐÃ SỬA: Kiểm tra xem model Item của bạn có hàm getImage() hay không.
        // Nếu không có, nó sẽ tự động dùng tiếp cơ chế fallback theo itemId thông minh.
         String preferredName = item.getImagePath();

        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
            if (item instanceof Electronics) {
                String brand = ((Electronics) item).getBrand();
                if (brand != null && !brand.trim().isEmpty()) preferredName = brand.trim();
            }
        }
        tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15); clip.setArcHeight(15); imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label(statusText);
        statusLabel.setStyle(statusText.equals("Sắp diễn ra") ?
                "-fx-background-color: #dbeafe; -fx-text-fill: #2563eb; -fx-background-radius: 20; -fx-font-weight: bold;" :
                "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT); StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8); infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(item.getName()); nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label(item.getDescription() != null && !item.getDescription().isEmpty() ? item.getDescription() : "Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40); descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b")); descLabel.setWrapText(true);

        Region spacer = new Region(); spacer.setPrefHeight(10);
        HBox priceBox = new HBox(); priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:"); priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region(); HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", item.getStartingPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16)); priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button(statusText.equals("Sắp diễn ra") ? "Xem chi tiết" : "Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0)); bidButton.setOnAction(e -> showAuctionDetail(item));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    public void addAuctionToRealtimeUI(Auction newAuction) {
        Platform.runLater(() -> {
            if (flowPane != null && (currentFilter.equals("ALL") || currentFilter.equals("UPCOMING"))) {
                VBox card = createCardFromAuction(newAuction);
                flowPane.getChildren().add(0, card);
            }
        });
    }

    // ĐÃ SỬA: Đồng bộ hóa luồng đọc ảnh Real-time tương tự từ Auction Model
    private VBox createCardFromAuction(Auction auction) {
        VBox card = new VBox(); card.setPrefWidth(300);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-border-width: 1;");

        StackPane imageHolder = new StackPane(); imageHolder.setPrefHeight(180);
        Region bgRegion = new Region(); bgRegion.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 11 11 0 0;");
        imageHolder.getChildren().add(bgRegion);

        ImageView imgView = new ImageView(); imgView.setFitWidth(300); imgView.setFitHeight(180); imgView.setPreserveRatio(false);

        // ĐÃ SỬA: Ưu tiên tìm thuộc tính lưu ảnh thực tế của phiên đấu giá socket
        String preferredName = null;
        try {
            java.lang.reflect.Method getImgMethod = auction.getClass().getMethod("getImage");
            preferredName = (String) getImgMethod.invoke(auction);
        } catch (Exception e) {
            try {
                java.lang.reflect.Method getImgUrlMethod = auction.getClass().getMethod("getImageUrl");
                preferredName = (String) getImgUrlMethod.invoke(auction);
            } catch (Exception ex) {
                preferredName = auction.getItemId();
            }
        }

        if (preferredName == null || preferredName.trim().isEmpty()) {
            preferredName = "default.png";
        }
        tryLoadImageToView(imgView, preferredName);

        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(300, 180);
        clip.setArcWidth(15); clip.setArcHeight(15); imgView.setClip(clip);
        imageHolder.getChildren().add(imgView);

        Label statusLabel = new Label("Đang diễn ra");
        statusLabel.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-background-radius: 20; -fx-font-weight: bold;");
        statusLabel.setPadding(new Insets(5, 12, 5, 12));
        StackPane.setAlignment(statusLabel, Pos.TOP_RIGHT); StackPane.setMargin(statusLabel, new Insets(10, 10, 0, 0));
        imageHolder.getChildren().add(statusLabel);

        VBox infoBox = new VBox(8); infoBox.setPadding(new Insets(15));
        Label nameLabel = new Label(auction.getProductName() != null ? auction.getProductName() : "Sản phẩm mới lên sàn");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 16)); nameLabel.setTextFill(javafx.scene.paint.Color.valueOf("#1e293b"));
        Label descLabel = new Label("Sản phẩm chất lượng cao đang trong phiên đấu giá công khai.");
        descLabel.setPrefHeight(40); descLabel.setTextFill(javafx.scene.paint.Color.valueOf("#64748b")); descLabel.setWrapText(true);

        Region spacer = new Region(); spacer.setPrefHeight(10);
        HBox priceBox = new HBox(); priceBox.setAlignment(Pos.CENTER_LEFT);
        Label priceTitle = new Label("Giá hiện tại:"); priceTitle.setTextFill(javafx.scene.paint.Color.valueOf("#64748b"));
        Region priceSpacer = new Region(); HBox.setHgrow(priceSpacer, Priority.ALWAYS);
        Label priceValue = new Label(String.format("%,.0f đ", auction.getStartPrice()));
        priceValue.setFont(Font.font("System", FontWeight.BOLD, 16)); priceValue.setTextFill(javafx.scene.paint.Color.valueOf("#0284c7"));
        priceBox.getChildren().addAll(priceTitle, priceSpacer, priceValue);

        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
        bidButton.setPadding(new Insets(8, 0, 8, 0)); bidButton.setOnAction(e -> showAuctionDetail(auction));

        infoBox.getChildren().addAll(nameLabel, descLabel, spacer, priceBox, bidButton);
        card.getChildren().addAll(imageHolder, infoBox);
        return card;
    }

    private void setButtonActive(Button activeButton) {
        Button[] filterButtons = {btnFilterAll, btnFilterActive, btnFilterUpcoming};
        for (Button btn : filterButtons) {
            if (btn != null) btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
        }
        if (activeButton != null) activeButton.setStyle("-fx-background-color: #0ea5e9; -fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand; -fx-font-weight: bold; -fx-padding: 0 20;");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentAccount.logOut();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            ((Stage) ((Node) event.getSource()).getScene().getWindow()).getScene().setRoot(root);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void showAuctionDetail(Object productData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/AuctionDetail.fxml"));
            Parent detailView = loader.load();
            AuctionDetailController detailController = loader.getController();
            if (detailController != null) {
                if (productData instanceof Item) {
                    detailController.loadProductDetail((Item) productData);
                } else if (productData instanceof Auction) {
                    Auction auction = (Auction) productData;
                    String displayName = auction.getProductName() != null ? auction.getProductName() : "Sản phẩm mới";
                    detailController.lblProductTitle.setText(displayName + " (#" + auction.getItemId() + ")");
                    detailController.lblStartPrice.setText(String.format("%.0f đ", auction.getStartPrice()));
                }
            }
            if (MainLayoutController.getInstance() != null) {
                MainLayoutController.getInstance().setContent(detailView);
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}