package com.auction.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class MainApp {

    private static Stage mainStage;

    public static class AppUI extends Application {
        @Override
        public void start(Stage primaryStage) throws Exception {
            mainStage = primaryStage;

            URL fxmlLocation = getClass().getResource("/view/LoginView.fxml");
            if (fxmlLocation == null) {
                System.err.println("LỖI: Không tìm thấy file LoginView.fxml!");
                return;
            }
            Parent root = FXMLLoader.load(fxmlLocation);
            Scene scene = new Scene(root);

            primaryStage.setTitle("Hệ thống đấu giá trực tuyến - Nhóm 13");
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true); // Phóng to ngay từ đầu
            primaryStage.show();
        }

        @Override
        public void stop() {
            System.out.println("Ứng dụng Client đã đóng an toàn.");
        }
    }

    // 🌟 HÀM ĐỔI MÀN HÌNH ĐÃ SỬA CHỮA TOÀN DIỆN
    public static void changeScene(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                URL fxmlLocation = MainApp.class.getResource(fxmlPath);
                if (fxmlLocation == null) {
                    System.err.println("LỖI: Không tìm thấy file FXML tại: " + fxmlPath);
                    return;
                }
                Parent root = FXMLLoader.load(fxmlLocation);

                // 1. Lấy lớp nền (Scene) hiện tại đang full màn hình sẵn
                Scene currentScene = mainStage.getScene();

                if (currentScene != null) {
                    // 🛠️ THẦN CHÚ: Ép cái giao diện mới (StackPane) phải tự động co giãn
                    // khít 100% theo chiều rộng và chiều cao của toàn màn hình hiện tại.
                    if (root instanceof javafx.scene.layout.Region) {
                        javafx.scene.layout.Region region = (javafx.scene.layout.Region) root;
                        region.prefWidthProperty().bind(currentScene.widthProperty());
                        region.prefHeightProperty().bind(currentScene.heightProperty());
                    }

                    // 2. Tiến hành thay ruột giao diện
                    currentScene.setRoot(root);
                }
            } catch (IOException e) {
                System.err.println("LỖI: Không thể chuyển sang màn hình " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        ClientConnection.connect();
        Application.launch(AppUI.class, args);
    }
}