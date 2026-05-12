package com.auction.server.service;

import com.auction.server.dao.ItemDAO;
import com.auction.shared.model.Item;

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

        return itemDAO.insertItem(item);
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
        // Chỉ chủ sở hữu mới được xóa
        if (existing.getOwnerId() != requesterId) {
            System.err.println("Bạn không có quyền xóa sản phẩm này!");
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
}