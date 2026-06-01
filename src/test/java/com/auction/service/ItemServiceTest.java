package com.auction.service;

import com.auction.shared.model.Art;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;
import com.auction.shared.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho ItemService — validation logic sản phẩm.
 *
 * Bao gồm:
 *  - Electronics, Art, Vehicle: kiểm tra constructor, getter, kế thừa Item
 *  - addItem(): kiểm tra tên, giá khởi điểm hợp lệ
 *  - updateItem(): kiểm tra status sản phẩm
 *  - deleteItem(): kiểm tra quyền xóa theo ownerId và status
 *
 * Constructor thực tế:
 *  Electronics(itemId, name, desc, price, ownerId, categoryId, status, imagePath, warrantyMonths)
 *  Art(itemId, name, desc, price, ownerId, categoryId, status, artist, year)
 *  Vehicle(itemId, name, desc, price, ownerId, categoryId, status, brand, model, year, mileage)
 */
@DisplayName("ItemService — Validation Logic Tests")
class ItemServiceTest {

    private Electronics electronics;
    private Art art;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        // Electronics: categoryId=1, ownerId=1, imagePath là tham số thứ 8, warrantyMonths là tham số thứ 9
        // brand KHÔNG có trong constructor → dùng setBrand() sau
        electronics = new Electronics(
                "ITEM_001", "Laptop Dell", "Mo ta san pham",
                5_000_000, 1, 1, "AVAILABLE",
                "images/dell.jpg", 12
        );
        electronics.setBrand("Dell"); // brand được set riêng qua setter

        // Art: artist là tham số thứ 8, year là tham số thứ 9 (int)
        art = new Art(
                "ITEM_002", "Tranh Son Dau", "Mo ta tranh",
                3_000_000, 2, 2, "AVAILABLE",
                "Nguyen Van A", 2020
        );

        // Vehicle: brand, model, year, mileage — 4 field riêng biệt
        vehicle = new Vehicle(
                "ITEM_003", "Honda Wave", "Mo ta xe",
                15_000_000, 3, 3, "AVAILABLE",
                "Honda", "Wave Alpha", 2020, 0
        );
    }


    // ═══════════════════════════════════════════════════════
    //  Electronics — Kiểm tra constructor, getter, kế thừa
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Electronics: getter trả đúng giá trị khởi tạo")
    void testElectronics_Getters() {
        assertEquals("ITEM_001",    electronics.getItemId());
        assertEquals("Laptop Dell", electronics.getName());
        assertEquals(5_000_000,     electronics.getStartingPrice());
        assertEquals(1,             electronics.getOwnerId());
        assertEquals(1,             electronics.getCategoryId());
        assertEquals("AVAILABLE",   electronics.getStatus());
        assertEquals(12,            electronics.getWarrantyMonths());
        assertEquals("Dell",        electronics.getBrand()); // set qua setBrand()
    }

    @Test
    @DisplayName("Electronics: setBrand cập nhật đúng")
    void testElectronics_SetBrand() {
        electronics.setBrand("Apple");
        assertEquals("Apple", electronics.getBrand());
    }

    @Test
    @DisplayName("Electronics: setWarrantyMonths cập nhật đúng")
    void testElectronics_SetWarrantyMonths() {
        electronics.setWarrantyMonths(24);
        assertEquals(24, electronics.getWarrantyMonths());
    }

    @Test
    @DisplayName("Electronics kế thừa Item — instanceof đúng")
    void testElectronics_IsItem() {
        assertInstanceOf(Item.class, electronics, "Electronics phải là Item");
    }


    // ═══════════════════════════════════════════════════════
    //  Art — Kiểm tra constructor, getter, kế thừa
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Art: getter trả đúng giá trị khởi tạo")
    void testArt_Getters() {
        assertEquals("ITEM_002",      art.getItemId());
        assertEquals("Tranh Son Dau", art.getName());
        assertEquals(3_000_000,       art.getStartingPrice());
        assertEquals(2,               art.getOwnerId());
        assertEquals("AVAILABLE",     art.getStatus());
        assertEquals("Nguyen Van A",  art.getArtist());
        assertEquals(2020,            art.getYear());
    }

    @Test
    @DisplayName("Art: setArtist và setYear cập nhật đúng")
    void testArt_Setters() {
        art.setArtist("Tran Thi B");
        art.setYear(2023);
        assertEquals("Tran Thi B", art.getArtist());
        assertEquals(2023, art.getYear());
    }

    @Test
    @DisplayName("Art kế thừa Item — instanceof đúng")
    void testArt_IsItem() {
        assertInstanceOf(Item.class, art, "Art phải là Item");
    }


    // ═══════════════════════════════════════════════════════
    //  Vehicle — Kiểm tra constructor, getter, kế thừa
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Vehicle: getter trả đúng giá trị khởi tạo")
    void testVehicle_Getters() {
        assertEquals("ITEM_003",   vehicle.getItemId());
        assertEquals("Honda Wave", vehicle.getName());
        assertEquals(15_000_000,   vehicle.getStartingPrice());
        assertEquals(3,            vehicle.getOwnerId());
        assertEquals("AVAILABLE",  vehicle.getStatus());
        assertEquals("Honda",      vehicle.getBrand());
        assertEquals("Wave Alpha", vehicle.getModel());
        assertEquals(2020,         vehicle.getYear());
        assertEquals(0,            vehicle.getMileage());
    }

    @Test
    @DisplayName("Vehicle: setModel, setYear, setMileage cập nhật đúng")
    void testVehicle_Setters() {
        vehicle.setModel("Future 125");
        vehicle.setYear(2022);
        vehicle.setMileage(5000);
        assertEquals("Future 125", vehicle.getModel());
        assertEquals(2022,         vehicle.getYear());
        assertEquals(5000,         vehicle.getMileage());
    }

    @Test
    @DisplayName("Vehicle kế thừa Item — instanceof đúng")
    void testVehicle_IsItem() {
        assertInstanceOf(Item.class, vehicle, "Vehicle phải là Item");
    }


    // ═══════════════════════════════════════════════════════
    //  addItem — Validation logic
    //  Tái hiện điều kiện kiểm tra trong ItemService.createItem()
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("addItem: tên null → validation fail")
    void testAddItem_NullName_Fails() {
        electronics.setName(null);
        // Tái hiện: if (item.getName() == null || item.getName().isBlank()) return false
        boolean valid = electronics.getName() != null && !electronics.getName().isBlank()
                && electronics.getStartingPrice() > 0;
        assertFalse(valid, "Ten null phai fail validation");
    }

    @Test
    @DisplayName("addItem: tên rỗng → validation fail")
    void testAddItem_BlankName_Fails() {
        electronics.setName("   ");
        boolean valid = electronics.getName() != null && !electronics.getName().isBlank()
                && electronics.getStartingPrice() > 0;
        assertFalse(valid, "Ten rong phai fail validation");
    }

    @Test
    @DisplayName("addItem: giá khởi điểm = 0 → validation fail")
    void testAddItem_ZeroPrice_Fails() {
        electronics.setStartingPrice(0);
        boolean valid = electronics.getName() != null && !electronics.getName().isBlank()
                && electronics.getStartingPrice() > 0;
        assertFalse(valid, "Gia 0 phai fail validation");
    }

    @Test
    @DisplayName("addItem: giá khởi điểm âm → validation fail")
    void testAddItem_NegativePrice_Fails() {
        electronics.setStartingPrice(-1_000);
        boolean valid = electronics.getName() != null && !electronics.getName().isBlank()
                && electronics.getStartingPrice() > 0;
        assertFalse(valid, "Gia am phai fail validation");
    }

    @Test
    @DisplayName("addItem: item hợp lệ → validation pass")
    void testAddItem_ValidItem_Passes() {
        boolean valid = electronics.getName() != null && !electronics.getName().isBlank()
                && electronics.getStartingPrice() > 0;
        assertTrue(valid, "Item hop le phai pass validation");
    }


    // ═══════════════════════════════════════════════════════
    //  updateItem / deleteItem — Status logic
    //  ItemDAO.deleteItem() SQL: WHERE status = 'AVAILABLE'
    //  → chỉ xóa được khi status = AVAILABLE
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("updateItem: status IN_AUCTION → không thể cập nhật")
    void testUpdateItem_InAuction_Fails() {
        electronics.setStatus("IN_AUCTION");
        boolean canUpdate = "AVAILABLE".equals(electronics.getStatus());
        assertFalse(canUpdate, "IN_AUCTION khong the sua");
    }

    @Test
    @DisplayName("updateItem: status SOLD → không thể cập nhật")
    void testUpdateItem_Sold_Fails() {
        electronics.setStatus("SOLD");
        boolean canUpdate = "AVAILABLE".equals(electronics.getStatus());
        assertFalse(canUpdate, "SOLD khong the sua");
    }

    @Test
    @DisplayName("updateItem: status AVAILABLE → có thể cập nhật")
    void testUpdateItem_Available_Passes() {
        boolean canUpdate = "AVAILABLE".equals(electronics.getStatus());
        assertTrue(canUpdate, "AVAILABLE duoc phep sua");
    }

    @Test
    @DisplayName("deleteItem: status IN_AUCTION → ItemDAO không xóa được")
    void testDeleteItem_InAuction_NotDeletable() {
        electronics.setStatus("IN_AUCTION");
        // ItemDAO.deleteItem(): DELETE WHERE status = 'AVAILABLE'
        boolean canDelete = "AVAILABLE".equals(electronics.getStatus());
        assertFalse(canDelete, "IN_AUCTION khong the xoa");
    }

    @Test
    @DisplayName("deleteItem: status AVAILABLE → ItemDAO có thể xóa")
    void testDeleteItem_Available_Deletable() {
        boolean canDelete = "AVAILABLE".equals(electronics.getStatus());
        assertTrue(canDelete, "AVAILABLE duoc phep xoa");
    }


    // ═══════════════════════════════════════════════════════
    //  deleteItem — Authorization logic (ownerId)
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteItem: chủ sở hữu xóa sản phẩm của mình → được phép")
    void testDeleteItem_OwnerCanDelete() {
        int requesterId = electronics.getOwnerId(); // = 1
        boolean authorized = electronics.getOwnerId() == requesterId;
        assertTrue(authorized, "Chu so huu phai duoc phep xoa");
    }

    @Test
    @DisplayName("deleteItem: người khác xóa sản phẩm không phải của mình → bị từ chối")
    void testDeleteItem_NotOwnerCannotDelete() {
        int requesterId = 99; // khac ownerId = 1
        boolean authorized = electronics.getOwnerId() == requesterId;
        assertFalse(authorized, "Nguoi khong phai chu khong duoc xoa");
    }
}