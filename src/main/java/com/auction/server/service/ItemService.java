package com.auction.server.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.util.DatabaseConnection;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics; // ✅ THÊM IMPORT LỚP CON

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();

    // 1. Seller thêm sản phẩm mới
    public boolean addItem(Item item) {
        if (item.getName() == null || item.getName().isBlank()) {
            System.err.println("Tên sản phẩm không được để trống!");
            return false;
        }
        if (item.getStartingPrice() <= 0) {
            System.err.println("Giá khởi điểm phải lớn hơn 0!");
            return false;
        }

        // 🟢 ĐÃ SỬA: Ép kiểu an toàn sang Electronics để khớp với hàm insertItem(Electronics item) trong DAO
        if (item instanceof Electronics) {
            return itemDAO.insertItem((Electronics) item);
        } else {
            // Trường hợp dự phòng nếu Client truyền Object Item gốc
            // Tạo nhanh một bản sao Electronics để vận chuyển dữ liệu xuống DB không bị lỗi
            Electronics elec = new Electronics();
            elec.setItemId(item.getItemId());
            elec.setName(item.getName());
            elec.setDescription(item.getDescription());
            elec.setStartingPrice(item.getStartingPrice());
            elec.setCategoryId(item.getCategoryId());
            elec.setOwnerId(item.getOwnerId());
            elec.setStatus(item.getStatus());
            elec.setBrand("default.png"); // Tên ảnh mặc định nếu không có thuộc tính brand

            return itemDAO.insertItem(elec);
        }
    }

    // 2. Seller cập nhật thông tin sản phẩm
    public boolean updateItem(Item item) { // (Nếu code bạn đang là Electronics item thì giữ nguyên nhé)
        // 1. Cập nhật Items
        boolean itemOk = itemDAO.updateItem(item);

        // 👉 Kiểm tra xem Client có gửi cái StartTimeStr lên không
        if (itemOk && item.getStartTimeStr() != null) {

            // 2. Chọc xuống bảng Auctions để cập nhật nốt Giá và Thời gian
            String sql = "UPDATE Auctions SET start_price = ?, current_price = ?, start_time = ?, end_time = ? WHERE item_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setDouble(1, item.getStartingPrice());
                pstmt.setDouble(2, item.getStartingPrice()); // Cho giá hiện tại = giá khởi điểm mới luôn

                // 👉 Lấy data từ 2 hàm có sẵn của bạn:
                pstmt.setString(3, item.getStartTimeStr());
                pstmt.setString(4, item.getEndTimeStr());

                pstmt.setString(5, item.getItemId());

                pstmt.executeUpdate();
            } catch (SQLException e) {
                System.err.println("❌ Lỗi đồng bộ Auctions: " + e.getMessage());
            }
        }
        return itemOk;
    }

    // 3. Seller xóa sản phẩm
    public boolean deleteItem(String itemId) {
        Item existing = itemDAO.getItemById(itemId);
        if (existing == null) return false;

        try {
            Auction auction = auctionDAO.getAuctionByItemId(itemId);
            // Có auction nhưng không phải OPEN → không cho xóa
            if (auction != null && auction.getStatus() != Auction.AuctionStatus.OPEN) return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return itemDAO.deleteItem(itemId);
    }
    // 4. Lấy tất cả sản phẩm (hiển thị danh sách)
    public List<Item> getAllItems() {
        return itemDAO.getAllItems();
    }

    // 5. Lấy sản phẩm theo ID
    public Item getItemById(String itemId) {
        return itemDAO.getItemById(itemId);
    }

    // 6. Lấy sản phẩm của một Seller
    public List<Item> getItemsByOwner(int ownerId) {
        return itemDAO.getItemsByOwner(ownerId);
    }

    // ── Alias methods cho ClientHandler ──────────────────────────────────────

    // createItem: alias cua addItem (ClientHandler goi ten nay)
    public boolean createItem(Item item) {
        return addItem(item);
    }

}