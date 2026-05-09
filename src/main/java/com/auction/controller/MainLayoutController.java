package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;

public class MainLayoutController {

    @FXML
    private StackPane contentArea;

    @FXML
    private VBox buyerMenu;

    @FXML
    private VBox sellerMenu;

    @FXML
    private VBox roleBox;

    @FXML
    private Label lblRoleSidebar;

    @FXML
    private MenuButton roleMenuButton;

    // ================= BUTTON MENU =================

    @FXML
    private Button btnHome;

    @FXML
    private Button btnWallet;

    @FXML
    private Button btnAuction;

    @FXML
    private Button btnSelling;

    @FXML
    private Button btnCreateAuction;

    @FXML
    private Button btnHistory;

    @FXML
    private Button btnSettings;

    // ================= INIT =================

    @FXML
    public void initialize() {
        // Mặc định là người mua
        switchToBuyer();

        // Load trang chủ
        openHome();
    }

    // ================= LOAD PAGE (SỬA TẠI ĐÂY) =================

    private void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node page = loader.load();

            // Xóa nội dung cũ
            contentArea.getChildren().clear();

            // Kiểm tra và ép giãn trang con
            if (page instanceof Region) {
                Region region = (Region) page;

                // Ép trang con luôn có kích thước bằng với contentArea
                region.prefWidthProperty().bind(contentArea.widthProperty());
                region.prefHeightProperty().bind(contentArea.heightProperty());

                // Đảm bảo không bị giới hạn bởi MaxSize cũ trong FXML con
                region.setMaxWidth(Double.MAX_VALUE);
                region.setMaxHeight(Double.MAX_VALUE);
            }

            // Căn lề lên trên cùng để nội dung không bị trôi lơ lửng ở giữa
            StackPane.setAlignment(page, Pos.TOP_CENTER);

            contentArea.getChildren().add(page);

        } catch (Exception e) {
            System.err.println("Lỗi load file: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ================= ACTIVE BUTTON =================

    private void setActive(Button activeButton) {
        Button[] buttons = {
                btnHome, btnWallet, btnAuction, btnSelling,
                btnCreateAuction, btnHistory, btnSettings
        };

        for (Button btn : buttons) {
            btn.getStyleClass().remove("nav-button-active");
            if (!btn.getStyleClass().contains("nav-button")) {
                btn.getStyleClass().add("nav-button");
            }
        }

        activeButton.getStyleClass().remove("nav-button");
        if (!activeButton.getStyleClass().contains("nav-button-active")) {
            activeButton.getStyleClass().add("nav-button-active");
        }
    }

    // ================= MENU ACTIONS =================

    @FXML
    private void openHome() {
        loadPage("/view/MainView.fxml");
        setActive(btnHome);
    }

    @FXML
    private void openWallet() {
        loadPage("/view/WalletView.fxml");
        setActive(btnWallet);
    }

    @FXML
    private void openAuction() {
        loadPage("/view/ActiveAuctions.fxml");
        setActive(btnAuction);
    }

    @FXML
    private void openSelling() {
        loadPage("/view/MyProducts.fxml");
        setActive(btnSelling);
    }

    @FXML
    private void openCreateAuction() {
        loadPage("/view/CreateAuction.fxml");
        setActive(btnCreateAuction);
    }

    @FXML
    private void openHistory() {
        loadPage("/view/HistoryView.fxml");
        setActive(btnHistory);
    }

    @FXML
    private void openSettings() {
        loadPage("/view/Settings.fxml");
        setActive(btnSettings);
    }

    // ================= ROLE SWITCHING =================

    @FXML
    private void switchToBuyer() {
        lblRoleSidebar.setText("🛒 Người mua");
        roleMenuButton.setText("🔄 Người mua");

        roleBox.setStyle("-fx-background-color: #fae8ff; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #86198f; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #A21CAF; -fx-background-radius: 10; -fx-text-fill: white;");

        buyerMenu.setVisible(true);
        buyerMenu.setManaged(true);
        sellerMenu.setVisible(false);
        sellerMenu.setManaged(false);
    }

    @FXML
    private void switchToSeller() {
        lblRoleSidebar.setText("🏪 Người bán");
        roleMenuButton.setText("🔄 Người bán");

        roleBox.setStyle("-fx-background-color: #dbeafe; -fx-background-radius: 15; -fx-padding: 20;");
        lblRoleSidebar.setStyle("-fx-text-fill: #1d4ed8; -fx-font-size: 18; -fx-font-weight: bold;");
        roleMenuButton.setStyle("-fx-background-color: #2563eb; -fx-background-radius: 10; -fx-text-fill: white;");

        sellerMenu.setVisible(true);
        sellerMenu.setManaged(true);
        buyerMenu.setVisible(false);
        buyerMenu.setManaged(false);
    }
}