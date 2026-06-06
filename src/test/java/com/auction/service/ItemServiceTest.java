package com.auction.service;

import com.auction.server.dao.AuctionDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.service.ItemService;
import com.auction.shared.model.User;
import com.auction.shared.model.Auction;
import com.auction.shared.model.Bidder;
import com.auction.shared.model.Electronics;
import com.auction.shared.model.Item;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cho ItemService — validation logic sản phẩm.
 *
 * Bao gồm:
 *  - Electronics: kiểm tra constructor, getter, setter, kế thừa Item (đại diện cho Art, Vehicle)
 *  - addItem(): validation tên và giá khởi điểm (dùng Mockito mock DAO)
 *  - deleteItem(): phân quyền Admin/User và trạng thái phiên (dùng Mockito mock DAO)
 *
 * Lý do test đại diện Electronics:
 *  Art và Vehicle có cùng cấu trúc kế thừa từ Item,
 *  logic validation giống nhau → không cần lặp lại.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ItemService — Validation & Logic Tests")
class ItemServiceTest {

    // ════════════════════════════════════════
    //  Mock DAO — thay thế DB thật
    // ════════════════════════════════════════

    @Mock
    private ItemDAO itemDAO;

    @Mock
    private AuctionDAO auctionDAO;

    // ItemService sẽ dùng 2 mock DAO trên thay vì DB thật
    @InjectMocks
    private ItemService itemService;

    // Object dùng chung cho các test
    private Electronics electronics;
    private Bidder seller;
    private User fakeAdmin;


    @BeforeEach
    void setUp() {
        // Electronics đại diện cho tất cả Item subclass
        electronics = new Electronics(
                "ITEM_001", "Laptop Dell", "Mo ta san pham",
                5_000_000, 1, 1, "AVAILABLE",
                "images/dell.jpg", 12
        );
        electronics.setBrand("Dell");

        // Seller — người dùng thông thường
        seller = new Bidder("1", "alice", "pass", "alice@mail.com", 2_000_000);

        // Admin — có quyền cao nhất
        fakeAdmin = new User("99", "admin", "pass", "admin@mail.com", "ADMIN", 0) {
            @Override public String displayRole() { return "Admin"; }
            @Override public void navigateDashboard() {}
        };
    }


    // ═══════════════════════════════════════════════════════
    //  Electronics — Đại diện cho Item subclass
    //  Kiểm tra constructor, getter, setter, kế thừa
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
        assertEquals("Dell",        electronics.getBrand());
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
    @DisplayName("Electronics: setStartingPrice cập nhật đúng")
    void testElectronics_SetStartingPrice() {
        electronics.setStartingPrice(10_000_000);
        assertEquals(10_000_000, electronics.getStartingPrice());
    }

    @Test
    @DisplayName("Electronics: setStatus cập nhật đúng")
    void testElectronics_SetStatus() {
        electronics.setStatus("IN_AUCTION");
        assertEquals("IN_AUCTION", electronics.getStatus());
    }

    @Test
    @DisplayName("Electronics kế thừa Item — instanceof đúng")
    void testElectronics_IsItem() {
        assertInstanceOf(Item.class, electronics, "Electronics phai la Item");
    }

    @Test
    @DisplayName("Electronics: constructor rỗng tạo object không null")
    void testElectronics_EmptyConstructor() {
        Electronics empty = new Electronics();
        assertNotNull(empty);
    }


    // ═══════════════════════════════════════════════════════
    //  addItem() — Validation logic
    //  Gọi thật ItemService, mock DAO để không cần DB
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("addItem: tên null → trả false, không gọi DAO")
    void testAddItem_NullName() {
        electronics.setName(null);
        boolean result = itemService.addItem(electronics);
        assertFalse(result);
        // Validation fail sớm → không bao giờ gọi xuống DAO
        verify(itemDAO, never()).insertItem(any());
    }

    @Test
    @DisplayName("addItem: tên rỗng → trả false, không gọi DAO")
    void testAddItem_BlankName() {
        electronics.setName("   ");
        boolean result = itemService.addItem(electronics);
        assertFalse(result);
        verify(itemDAO, never()).insertItem(any());
    }

    @Test
    @DisplayName("addItem: giá khởi điểm = 0 → trả false, không gọi DAO")
    void testAddItem_ZeroPrice() {
        electronics.setStartingPrice(0);
        boolean result = itemService.addItem(electronics);
        assertFalse(result);
        verify(itemDAO, never()).insertItem(any());
    }

    @Test
    @DisplayName("addItem: giá khởi điểm âm → trả false, không gọi DAO")
    void testAddItem_NegativePrice() {
        electronics.setStartingPrice(-1_000_000);
        boolean result = itemService.addItem(electronics);
        assertFalse(result);
        verify(itemDAO, never()).insertItem(any());
    }

    @Test
    @DisplayName("addItem: item hợp lệ → gọi DAO và trả true")
    void testAddItem_ValidItem() {
        when(itemDAO.insertItem(electronics)).thenReturn(true);
        boolean result = itemService.addItem(electronics);
        assertTrue(result);
        // Phải gọi đúng 1 lần
        verify(itemDAO, times(1)).insertItem(electronics);
    }

    @Test
    @DisplayName("addItem: DAO insertItem thất bại → trả false")
    void testAddItem_DAOFails() {
        when(itemDAO.insertItem(electronics)).thenReturn(false);
        boolean result = itemService.addItem(electronics);
        assertFalse(result);
    }


    // ═══════════════════════════════════════════════════════
    //  deleteItem() — Phân quyền & Trạng thái phiên
    //  Admin: xóa tuốt
    //  User thường: phụ thuộc trạng thái phiên đấu giá
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("Admin xóa item → luôn thành công, không check auction")
    void testDeleteItem_AdminCanDelete() throws SQLException {
        when(itemDAO.deleteItem("ITEM_001")).thenReturn(true);

        boolean result = itemService.deleteItem("ITEM_001", fakeAdmin);

        assertTrue(result);
        // Admin không check auction → không bao giờ gọi auctionDAO
        verify(auctionDAO, never()).getAuctionByItemId(any());
    }

    @Test
    @DisplayName("Item không tồn tại → deleteItem trả false")
    void testDeleteItem_ItemNotFound() throws SQLException {
        when(itemDAO.getItemById("ITEM_999")).thenReturn(null);

        boolean result = itemService.deleteItem("ITEM_999", seller);

        assertFalse(result);
    }

    @Test
    @DisplayName("Phiên đang RUNNING → không cho xóa")
    void testDeleteItem_AuctionRunning() throws SQLException {
        Auction runningAuction = new Auction();
        runningAuction.setStatus(Auction.AuctionStatus.RUNNING);

        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001")).thenReturn(runningAuction);

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertFalse(result);
    }

    @Test
    @DisplayName("Phiên đã FINISHED → không cho xóa")
    void testDeleteItem_AuctionFinished() throws SQLException {
        Auction finishedAuction = new Auction();
        finishedAuction.setStatus(Auction.AuctionStatus.FINISHED);

        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001")).thenReturn(finishedAuction);

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertFalse(result);
    }

    @Test
    @DisplayName("Phiên đã CANCELED → không cho xóa")
    void testDeleteItem_AuctionCanceled() throws SQLException {
        Auction canceledAuction = new Auction();
        canceledAuction.setStatus(Auction.AuctionStatus.CANCELED);

        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001")).thenReturn(canceledAuction);

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertFalse(result);
    }

    @Test
    @DisplayName("Phiên OPEN → cho phép xóa")
    void testDeleteItem_AuctionOpen() throws SQLException {
        Auction openAuction = new Auction();
        openAuction.setStatus(Auction.AuctionStatus.OPEN);

        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001")).thenReturn(openAuction);
        when(itemDAO.deleteItem("ITEM_001")).thenReturn(true);

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertTrue(result);
    }

    @Test
    @DisplayName("Không có phiên nào → cho phép xóa")
    void testDeleteItem_NoAuction() throws SQLException {
        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001")).thenReturn(null);
        when(itemDAO.deleteItem("ITEM_001")).thenReturn(true);

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertTrue(result);
    }

    @Test
    @DisplayName("SQLException khi check auction → deleteItem trả false")
    void testDeleteItem_SQLException() throws SQLException {
        when(itemDAO.getItemById("ITEM_001")).thenReturn(electronics);
        when(auctionDAO.getAuctionByItemId("ITEM_001"))
                .thenThrow(new SQLException("DB error"));

        boolean result = itemService.deleteItem("ITEM_001", seller);

        assertFalse(result);
    }
}