package com.auction.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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

        // mặc định là người mua
        switchToBuyer();

        // load trang chủ
        openHome();
    }

    // ================= LOAD PAGE =================

    private void loadPage(String fxmlPath) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Node page = loader.load();

            contentArea.getChildren().clear();

            contentArea.getChildren().add(page);

        } catch (Exception e) {

            System.out.println("Không load được file: " + fxmlPath);

            e.printStackTrace();
        }
    }

    // ================= ACTIVE BUTTON =================

    private void setActive(Button activeButton) {

        Button[] buttons = {
                btnHome,
                btnWallet,
                btnAuction,
                btnSelling,
                btnCreateAuction,
                btnHistory,
                btnSettings
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

    // ================= MENU =================

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

    // ================= ROLE =================

    @FXML
    private void switchToBuyer() {

        // text
        lblRoleSidebar.setText("🛒 Người mua");

        roleMenuButton.setText("🔄 Người mua");

        // màu tím
        roleBox.setStyle("""
                -fx-background-color: #fae8ff;
                -fx-background-radius: 15;
                -fx-padding: 20;
                """);

        lblRoleSidebar.setStyle("""
                -fx-text-fill: #86198f;
                -fx-font-size: 18;
                -fx-font-weight: bold;
                """);

        roleMenuButton.setStyle("""
                -fx-background-color: #A21CAF;
                -fx-background-radius: 10;
                -fx-text-fill: white;
                """);

        // hiện menu buyer
        buyerMenu.setVisible(true);
        buyerMenu.setManaged(true);

        // ẩn menu seller
        sellerMenu.setVisible(false);
        sellerMenu.setManaged(false);
    }

    @FXML
    private void switchToSeller() {

        // text
        lblRoleSidebar.setText("🏪 Người bán");

        roleMenuButton.setText("🔄 Người bán");

        // màu xanh
        roleBox.setStyle("""
                -fx-background-color: #dbeafe;
                -fx-background-radius: 15;
                -fx-padding: 20;
                """);

        lblRoleSidebar.setStyle("""
                -fx-text-fill: #1d4ed8;
                -fx-font-size: 18;
                -fx-font-weight: bold;
                """);

        roleMenuButton.setStyle("""
                -fx-background-color: #2563eb;
                -fx-background-radius: 10;
                -fx-text-fill: white;
                """);

        // hiện menu seller
        sellerMenu.setVisible(true);
        sellerMenu.setManaged(true);

        // ẩn menu buyer
        buyerMenu.setVisible(false);
        buyerMenu.setManaged(false);
    }
}