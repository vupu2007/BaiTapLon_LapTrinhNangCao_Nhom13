package com.auction.client.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageLoader {
    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/uet_uploads/";

    // 1. THREAD POOL: Chỉ cho phép tối đa 4 luồng load ảnh cùng lúc để bảo vệ CPU
    private static final ExecutorService IMAGE_LOAD_POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true); // Tự động tắt luồng khi tắt app
        thread.setPriority(Thread.MIN_PRIORITY);
        return thread;
    });

    // 2. CACHE (Caffeine): Giữ lại tối đa 100 ảnh gần nhất trong RAM, tự giải phóng sau 10 phút không dùng
    private static final Cache<String, Image> IMAGE_CACHE = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build();

    // 3. PLACEHOLDER: Ảnh mặc định tải sẵn vào RAM để hiện ngay lập tức lúc đang chờ ảnh thật
    private static Image defaultImage;
    static {
        try (InputStream is = ImageLoader.class.getResourceAsStream("/com/auction/client/images/default.png")) {
            if (is != null) defaultImage = new Image(is, 150, 150, true, true);
        } catch (Exception e) {
            System.err.println("❌ Không thể tải ảnh default hệ thống: " + e.getMessage());
        }
    }

    public static void tryLoadImageToView(ImageView imgView, String preferredFileName) {
        if (imgView == null) return;

        // BƯỚC 1: Hiển thị ngay ảnh chờ (Placeholder) để giao diện không bị trống trải
        imgView.setImage(defaultImage);

        if (preferredFileName == null || preferredFileName.trim().isEmpty()) {
            return; // Nếu không có tên file thì giữ nguyên ảnh mặc định
        }

        // BƯỚC 2: Kiểm tra xem ảnh này đã từng load và lưu trong Cache (RAM) chưa
        Image cachedImage = IMAGE_CACHE.getIfPresent(preferredFileName);
        if (cachedImage != null) {
            imgView.setImage(cachedImage); // Có sẵn trong RAM -> Hiện ngay lập tức (0ms)
            return;
        }

        // BƯỚC 3: Nếu chưa có trong Cache -> Đẩy nhiệm vụ tải ảnh vào Thread Pool để xử lý ngầm
        IMAGE_LOAD_POOL.submit(() -> {
            Image loadedImage = null;
            try {
                // Xử lý Base64
                if (preferredFileName.startsWith("base64:")) {
                    byte[] bytes = Base64.getDecoder().decode(preferredFileName.substring(7));
                    loadedImage = new Image(new ByteArrayInputStream(bytes), 200, 200, true, true);
                }
                // Xử lý URL mạng
                else if (preferredFileName.startsWith("http://") || preferredFileName.startsWith("https://")) {
                    // Load trực tiếp qua URL của JavaFX (đồng bộ trong luồng ngầm này)
                    loadedImage = new Image(preferredFileName, 200, 200, true, true, false);
                }
                // Xử lý File từ ổ cứng
                else {
                    File directFile = new File(UPLOAD_DIR + preferredFileName);
                    if (directFile.exists() && directFile.isFile()) {
                        loadedImage = new Image(directFile.toURI().toString(), 200, 200, true, true, false);
                    } else {
                        // Tìm trong Resources
                        String resPath = "/com/auction/client/images/" + preferredFileName;
                        try (InputStream is = ImageLoader.class.getResourceAsStream(resPath)) {
                            if (is != null) loadedImage = new Image(is, 200, 200, true, true);
                        }
                    }
                }

                // BƯỚC 4: Nếu tải thành công, lưu vào Cache và cập nhật UI
                if (loadedImage != null && !loadedImage.isError()) {
                    IMAGE_CACHE.put(preferredFileName, loadedImage); // Lưu vào RAM cho lần sau

                    Image finalLoadedImage = loadedImage;
                    Platform.runLater(() -> imgView.setImage(finalLoadedImage)); // Đẩy về UI thread
                }
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi tải ngầm ảnh [" + preferredFileName + "]: " + e.getMessage());
            }
        });
    }
}