package com.auction;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.net.URL;

public class MainApp {

    public static class AppUI extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            // 1. Sửa lại đường dẫn dùng dấu gạch chéo /
            // Giả sử file của bạn nằm ở: src/main/resources/view/LoginView.fxml
            URL fxmlLocation = getClass().getResource("/view/LoginView.fxml");

            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file LoginView.fxml!");
                System.err.println("Đường dẫn đang quét: src/main/resources/view/LoginView.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);

            primaryStage.setTitle("Hệ thống đấu giá trực tuyến - Nhóm 13");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
            primaryStage.show();
        }
    }

    public static void main(String[] args) {
        Application.launch(AppUI.class, args);
    }
}