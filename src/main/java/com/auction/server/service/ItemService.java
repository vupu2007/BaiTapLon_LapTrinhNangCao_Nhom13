package com.auction.server.service;

import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Item;
import com.auction.shared.model.Electronics; // ✅ THÊM IMPORT LỚP CON

import java.util.List;

public class ItemService {

    private final ItemDAO itemDAO = new ItemDAO();

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
    public boolean updateItem(Item item) {
        Item existing = itemDAO.getItemById(item.getItemId());
        if (existing == null) {
            System.err.println("Sản phẩm không tồn tại!");
            return false;
        }
        // Chỉ sửa được khi sản phẩm chưa vào đấu giá
        if (!"AVAILABLE".equals(existing.getStatus())) {
            System.err.println("Không thể sửa sản phẩm đang đấu giá hoặc đã bán!");
            return false;
        }

        return itemDAO.updateItem(item);
    }

    // 3. Seller xóa sản phẩm (chỉ xóa được khi AVAILABLE)
    public boolean deleteItem(String itemId, int requesterId) {
        Item existing = itemDAO.getItemById(itemId);
        if (existing == null) {
            System.err.println("Sản phẩm không tồn tại!");
            return false;
        }
        if (existing.getOwnerId() != requesterId) {
            System.err.println("Bạn không có quyền xóa sản phẩm này!");
            return false;
        }
         if (!"AVAILABLE".equals(existing.getStatus()) && !"OPEN".equals(existing.getAuctionStatus())) {
            System.err.println("Không thể xóa sản phẩm đang đấu giá!");
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

    // deleteItem(int): ClientHandler truyen int id, nhung DAO dung String
    // requesterId mac dinh la owner (da kiem tra o ClientHandler)
    public boolean deleteItem(int itemId) {
        return itemDAO.deleteItem(String.valueOf(itemId));
    }
}