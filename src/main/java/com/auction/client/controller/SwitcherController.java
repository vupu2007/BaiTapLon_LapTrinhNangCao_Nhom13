package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SwitcherController {

    // 1. Chỉ khai báo MỘT LẦN duy nhất các biến này - Giữ nguyên
    @FXML private VBox roleBox;        // Khung Sidebar (chứa chữ Tư cách hiện tại)
    @FXML private VBox buyerMenu;      // Nhóm nút cho người mua
    @FXML private VBox sellerMenu;     // Nhóm nút cho người bán
    @FXML private Label lblRoleSidebar; // Nhãn "Người mua/bán"
    @FXML private Label lblRoleTitle;   // Nhãn nhỏ "Tư cách hiện tại"

    /**
     * Hàm xử lý logic chuyển đổi giao diện Sidebar
     */
    public void updateRoleUI(boolean isBuyer) {
        String color = isBuyer ? "#fae8ff" : "#e0f2fe";
        String textColor = isBuyer ? "#86198f" : "#0369a1";
        String roleText = isBuyer ? "🛒 Người mua" : "🏪 Người bán";

        // Cập nhật phong cách chung
        if (roleBox != null) roleBox.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 15; -fx-padding: 20;");
        if (lblRoleSidebar != null) {
            lblRoleSidebar.setText(roleText);
            lblRoleSidebar.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold; -fx-font-size: 18;");
        }
        if (lblRoleTitle != null) lblRoleTitle.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 11;");

        // Toggle menu (Sử dụng kỹ thuật ẩn hiện thông minh)
        updateMenuVisibility(buyerMenu, isBuyer);
        updateMenuVisibility(sellerMenu, !isBuyer);
    }

    private void updateMenuVisibility(VBox menu, boolean visible) {
        if (menu != null) {
            menu.setVisible(visible);
            menu.setManaged(visible);
        }
    }
}