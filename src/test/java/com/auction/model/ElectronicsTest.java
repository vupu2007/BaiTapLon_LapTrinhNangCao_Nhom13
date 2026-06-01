package com.auction.model;

import com.auction.shared.model.Electronics;
import com.auction.shared.model.Entity;
import com.auction.shared.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho Electronics (subclass của Item).
 * Bao gồm: constructor, getter/setter, kế thừa, printInfo(), trạng thái.
 */
@DisplayName("Electronics (Item) Tests")
class ElectronicsTest {

    private Electronics electronics;

    @BeforeEach
    void setUp() {
        // Tham số thứ 8 là imagePath — truyền đường dẫn ảnh, không phải brand
        electronics = new Electronics(
                "ITEM_001",
                "Laptop Dell XPS",
                "Laptop cao cap",
                15_000_000,
                5,                    // ownerId
                1,                    // categoryId
                "AVAILABLE",
                "images/dell.jpg",    // imagePath (tham số thứ 8)
                24                    // warrantyMonths (tham số thứ 9)
        );
        // brand KHÔNG có trong constructor → set riêng qua setter
        electronics.setBrand("Dell");
    }

    // ───────── Constructor & Getter ─────────

    @Test
    @DisplayName("Constructor đầy đủ khởi tạo đúng tất cả field")
    void testConstructorAndGetters() {
        assertEquals("ITEM_001",        electronics.getItemId());
        assertEquals("Laptop Dell XPS", electronics.getName());
        assertEquals("Laptop cao cap",  electronics.getDescription());
        assertEquals(15_000_000,        electronics.getStartingPrice());
        assertEquals(5,                 electronics.getOwnerId());
        assertEquals(1,                 electronics.getCategoryId());
        assertEquals("AVAILABLE",       electronics.getStatus());
        assertEquals("images/dell.jpg", electronics.getImagePath());
        assertEquals(24,                electronics.getWarrantyMonths());
        // brand được set qua setBrand() trong setUp()
        assertEquals("Dell",            electronics.getBrand());
    }

    @Test
    @DisplayName("Constructor rỗng tạo object không null")
    void testEmptyConstructor() {
        Electronics e = new Electronics();
        assertNotNull(e);
    }

    // ───────── getId() từ Entity interface ─────────

    @Test
    @DisplayName("getId() trả về itemId (thực thi từ Entity interface)")
    void testGetId() {
        // Item.getId() trả về itemId — thực thi interface Entity
        assertEquals("ITEM_001", electronics.getId());
    }

    // ───────── Setter ─────────

    @Test
    @DisplayName("setName() cập nhật tên đúng")
    void testSetName() {
        electronics.setName("MacBook Pro");
        assertEquals("MacBook Pro", electronics.getName());
    }

    @Test
    @DisplayName("setStartingPrice() cập nhật giá khởi điểm đúng")
    void testSetStartingPrice() {
        electronics.setStartingPrice(20_000_000);
        assertEquals(20_000_000, electronics.getStartingPrice());
    }

    @Test
    @DisplayName("setBrand() cập nhật thương hiệu đúng")
    void testSetBrand() {
        electronics.setBrand("Apple");
        assertEquals("Apple", electronics.getBrand());
    }

    @Test
    @DisplayName("setWarrantyMonths() cập nhật thời gian bảo hành đúng")
    void testSetWarrantyMonths() {
        electronics.setWarrantyMonths(12);
        assertEquals(12, electronics.getWarrantyMonths());
    }

    // ───────── Trạng thái Item ─────────

    @Test
    @DisplayName("setStatus() chuyển sang IN_AUCTION đúng")
    void testSetStatusInAuction() {
        electronics.setStatus("IN_AUCTION");
        assertEquals("IN_AUCTION", electronics.getStatus());
    }

    @Test
    @DisplayName("setStatus() chuyển sang SOLD đúng")
    void testSetStatusSold() {
        electronics.setStatus("SOLD");
        assertEquals("SOLD", electronics.getStatus());
    }

    // ───────── Kế thừa & Đa hình ─────────

    @Test
    @DisplayName("Electronics là instance của Item và Entity")
    void testInheritance() {
        assertInstanceOf(Item.class,   electronics, "Electronics phai la Item");
        assertInstanceOf(Entity.class, electronics, "Electronics phai implement Entity");
    }

    @Test
    @DisplayName("printInfo() không ném exception")
    void testPrintInfoDoesNotThrow() {
        // printInfo() là abstract method bắt buộc override — kiểm tra không crash
        assertDoesNotThrow(() -> electronics.printInfo());
    }

    // ───────── Giá trị biên ─────────

    @Test
    @DisplayName("startingPrice = 0 vẫn set được (validation thuộc service layer)")
    void testStartingPriceZero() {
        electronics.setStartingPrice(0);
        assertEquals(0, electronics.getStartingPrice(),
                "Model khong tu validate — service layer moi check");
    }

    @Test
    @DisplayName("warrantyMonths = 0 là hợp lệ về model")
    void testWarrantyMonthsZero() {
        electronics.setWarrantyMonths(0);
        assertEquals(0, electronics.getWarrantyMonths());
    }
}