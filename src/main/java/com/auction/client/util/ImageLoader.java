package com.auction.client.util;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;

public class ImageLoader {
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/uet_uploads/";

    public static void tryLoadImageToView(ImageView imgView, String preferredFileName) {
        if (imgView == null) return;

        // 1. Xử lý ảnh dạng chuỗi mã hóa Base64
        if (preferredFileName != null && preferredFileName.startsWith("base64:")) {
            try {
                byte[] bytes = Base64.getDecoder().decode(preferredFileName.substring(7));
                imgView.setImage(new Image(new ByteArrayInputStream(bytes)));
                return;
            } catch (Exception e) {
                System.err.println("❌ Lỗi decode Base64: " + e.getMessage());
            }
        }

        // 2. Xử lý ảnh dạng URL trực tuyến (Http/Https)
        if (preferredFileName != null && (preferredFileName.startsWith("http://") || preferredFileName.startsWith("https://"))) {
            try {
                imgView.setImage(new Image(preferredFileName, true));
                return;
            } catch (Exception e) {
                System.err.println("❌ Load URL ảnh thất bại: " + e.getMessage());
            }
        }

        if (preferredFileName == null || preferredFileName.trim().isEmpty()) {
            preferredFileName = "default.png";
        }

        String[] extensions = {".png", ".jpg", ".jpeg"};
        boolean loaded = false;

        // 3. Quét tìm ảnh trong thư mục upload ngoài máy tính (Cấu hình động theo User Home)
        File directFile = new File(UPLOAD_DIR + preferredFileName);
        if (directFile.exists() && directFile.isFile()) {
            imgView.setImage(new Image(directFile.toURI().toString()));
            return;
        }

        for (String ext : extensions) {
            String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
            File extFile = new File(UPLOAD_DIR + fullFileName);
            if (extFile.exists()) {
                imgView.setImage(new Image(extFile.toURI().toString()));
                loaded = true;
                break;
            }
        }

        // 4. Tìm kiếm ảnh mặc định trong Resources của hệ thống
        if (!loaded) {
            for (String ext : extensions) {
                String fullFileName = preferredFileName.contains(".") ? preferredFileName : preferredFileName + ext;
                InputStream is = ImageLoader.class.getResourceAsStream("/com/auction/client/images/" + fullFileName);
                if (is != null) {
                    imgView.setImage(new Image(is));
                    loaded = true;
                    break;
                }
            }
        }

        // 5. Giải pháp cuối cùng: Dùng ảnh default.png hệ thống nếu không tìm thấy gì
        if (!loaded) {
            InputStream defaultIs = ImageLoader.class.getResourceAsStream("/com/auction/client/images/default.png");
            if (defaultIs != null) imgView.setImage(new Image(defaultIs));
        }
    }
}