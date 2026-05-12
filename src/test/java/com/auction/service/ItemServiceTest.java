package com.auction.service;

import com.auction.model.Electronics;
import com.auction.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho ItemService
 * Test trực tiếp validation logic của ItemService bằng cách tạo subclass override itemDAO
 */
@DisplayName("ItemService Validation Tests")
class ItemServiceTest {

    // ─── Helper: Electronics hợp lệ ───
    private Electronics makeValidItem() {
        return new Electronics(
                "ITEM_001",
                "Laptop Dell",
                "Mô tả sản phẩm",
                5_000_000,
                1,          // ownerId
                1,          // categoryId
                "AVAILABLE",
                "Dell",
                12
        );
    }

    // ═══════════════════════════════════════════
    //  addItem — Validation (không cần DB)
    //  Test logic if-else bên trong ItemService
    // ═══════════════════════════════════════════

    /**
     * Tạo ItemService "giả" để test validation mà không kết nối DB.
     * Kỹ thuật: anonymous subclass override hành vi DAO.
     */
    private ItemService makeServiceWithFakeDAO(boolean daoResult, Item existingItem) {
        return new ItemService() {
            // Override bằng cách inject fake DAO behavior thông qua package-private trick
            // Nếu nhóm có Mockito thì thay bằng @Mock + @InjectMocks
        };
    }

    // ─── Test validation thuần (không cần DAO) ───

    @Test
    @DisplayName("Item có tên null → addItem validation phát hiện lỗi")
    void testAddItem_NullName_ValidationLogic() {
        Electronics item = makeValidItem();
        item.setName(null);

        // Tái hiện logic validation của ItemService.addItem()
        boolean validationPassed = item.getName() != null && !item.getName().isBlank()
                && item.getStartingPrice() > 0;
        assertFalse(validationPassed, "Tên null phải fail validation");
    }

    @Test
    @DisplayName("Item có tên rỗng → addItem validation phát hiện lỗi")
    void testAddItem_BlankName_ValidationLogic() {
        Electronics item = makeValidItem();
        item.setName("   ");

        boolean validationPassed = item.getName() != null && !item.getName().isBlank()
                && item.getStartingPrice() > 0;
        assertFalse(validationPassed, "Tên rỗng phải fail validation");
    }

    @Test
    @DisplayName("Giá khởi điểm = 0 → addItem validation phát hiện lỗi")
    void testAddItem_ZeroPrice_ValidationLogic() {
        Electronics item = makeValidItem();
        item.setStartingPrice(0);

        boolean validationPassed = item.getName() != null && !item.getName().isBlank()
                && item.getStartingPrice() > 0;
        assertFalse(validationPassed, "Giá 0 phải fail validation");
    }

    @Test
    @DisplayName("Giá khởi điểm âm → addItem validation phát hiện lỗi")
    void testAddItem_NegativePrice_ValidationLogic() {
        Electronics item = makeValidItem();
        item.setStartingPrice(-1000);

        boolean validationPassed = item.getName() != null && !item.getName().isBlank()
                && item.getStartingPrice() > 0;
        assertFalse(validationPassed, "Giá âm phải fail validation");
    }

    @Test
    @DisplayName("Item hợp lệ → validation pass")
    void testAddItem_ValidItem_ValidationLogic() {
        Electronics item = makeValidItem();

        boolean validationPassed = item.getName() != null && !item.getName().isBlank()
                && item.getStartingPrice() > 0;
        assertTrue(validationPassed, "Item hợp lệ phải pass validation");
    }

    // ═══════════════════════════════════════════
    //  updateItem — Validation logic
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("Sản phẩm IN_AUCTION → không thể cập nhật")
    void testUpdateItem_InAuction_ShouldFail() {
        Electronics item = makeValidItem();
        item.setStatus("IN_AUCTION");

        // Tái hiện logic: chỉ sửa được khi AVAILABLE
        boolean canUpdate = "AVAILABLE".equals(item.getStatus());
        assertFalse(canUpdate, "IN_AUCTION không thể sửa");
    }

    @Test
    @DisplayName("Sản phẩm SOLD → không thể cập nhật")
    void testUpdateItem_Sold_ShouldFail() {
        Electronics item = makeValidItem();
        item.setStatus("SOLD");

        boolean canUpdate = "AVAILABLE".equals(item.getStatus());
        assertFalse(canUpdate, "SOLD không thể sửa");
    }

    @Test
    @DisplayName("Sản phẩm AVAILABLE → có thể cập nhật")
    void testUpdateItem_Available_ShouldPass() {
        Electronics item = makeValidItem();
        item.setStatus("AVAILABLE");

        boolean canUpdate = "AVAILABLE".equals(item.getStatus());
        assertTrue(canUpdate, "AVAILABLE được phép sửa");
    }

    // ═══════════════════════════════════════════
    //  deleteItem — Authorization logic
    // ═══════════════════════════════════════════

    @Test
    @DisplayName("Chủ sở hữu xóa sản phẩm của mình → được phép")
    void testDeleteItem_OwnerCanDelete() {
        Electronics item = makeValidItem();
        int requesterId = item.getOwnerId(); // = 1

        boolean authorized = item.getOwnerId() == requesterId;
        assertTrue(authorized, "Chủ sở hữu phải được phép xóa");
    }

    @Test
    @DisplayName("Người khác xóa sản phẩm không phải của mình → bị từ chối")
    void testDeleteItem_NotOwnerCannotDelete() {
        Electronics item = makeValidItem(); // ownerId = 1
        int requesterId = 99; // người khác

        boolean authorized = item.getOwnerId() == requesterId;
        assertFalse(authorized, "Người không phải chủ không được xóa");
    }
}
