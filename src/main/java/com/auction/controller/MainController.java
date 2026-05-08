package com.auction.controller;

import com.auction.model.Account;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.service.MainService;
import com.auction.util.CurrentUser;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.List;

public class MainController {

    @FXML private Label balanceLabel;
    @FXML private Label ongoingLabel;
    @FXML private Label wonLabel;
    @FXML private Label welcomeLabel;
    @FXML private VBox hotItemsContainer;

    private MainService mainService;

    public MainController() {
        this.mainService = new MainService();
    }

    @FXML
    public void initialize() {
        Account current = CurrentUser.getUser();
        if (current != null) {
            if (welcomeLabel != null) {
                welcomeLabel.setText("Chào mừng, " + current.getUsername() + "!");
            }
            refreshDashboard();
        }
    }

    private void refreshDashboard() {
        Account current = CurrentUser.getUser();
        // Chỉ hiện balance nếu account đó là User (Bidder/Seller)
        if (current instanceof User) {
            balanceLabel.setText(String.format("%.0f VNĐ", ((User) current).getBalance()));
        } else {
            balanceLabel.setText("N/A");
        }

        ongoingLabel.setText(String.valueOf(mainService.getOngoingCount()));
        wonLabel.setText(String.valueOf(mainService.getWonCount()));
        loadHotAuctions();
    }

    private void loadHotAuctions() {
        hotItemsContainer.getChildren().clear();
        List<Item> items = mainService.getHotAuctions();
        for (Item item : items) {
            VBox card = createItemCard(item);
            hotItemsContainer.getChildren().add(card);
        }
    }

    private VBox createItemCard(Item item) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-border-color: #ddd; -fx-border-radius: 8;");
        Label name = new Label(item.getName());
        Label price = new Label("Giá: " + String.format("%.0f", item.getStartingPrice()) + " VNĐ");
        Button bidButton = new Button("Đấu giá ngay");
        bidButton.setOnAction(e -> System.out.println("Đang đấu giá: " + item.getName()));
        card.getChildren().addAll(name, price, bidButton);
        return card;
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        CurrentUser.logOut();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoginView.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}