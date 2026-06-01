package com.auction.model;

import com.auction.shared.model.BidTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho BidTransaction model.
 * Bao gồm: constructor, getter/setter, bidderUsername, toString().
 */
@DisplayName("BidTransaction Model Tests")
class BidTransactionTest {

    private BidTransaction bid;
    private LocalDateTime bidTime;

    @BeforeEach
    void setUp() {
        bidTime = LocalDateTime.of(2026, 5, 8, 14, 30, 0);
        bid = new BidTransaction(1, 10, 5, 750_000, bidTime);
    }

    // ───────── Constructor & Getter ─────────

    @Test
    @DisplayName("Constructor đầy đủ khởi tạo đúng tất cả field")
    void testConstructorAndGetters() {
        assertEquals(1,         bid.getId());
        assertEquals(10,        bid.getAuctionId());
        assertEquals(5,         bid.getBidderId());
        assertEquals(750_000,   bid.getBidAmount());
        assertEquals(bidTime,   bid.getBidTime());
    }

    @Test
    @DisplayName("Constructor rỗng tạo object không null")
    void testEmptyConstructor() {
        BidTransaction empty = new BidTransaction();
        assertNotNull(empty);
    }

    // ───────── Setter ─────────

    @Test
    @DisplayName("setBidAmount cập nhật số tiền đặt giá đúng")
    void testSetBidAmount() {
        bid.setBidAmount(1_000_000);
        assertEquals(1_000_000, bid.getBidAmount());
    }

    @Test
    @DisplayName("setBidderId cập nhật ID người đặt giá đúng")
    void testSetBidderId() {
        bid.setBidderId(99);
        assertEquals(99, bid.getBidderId());
    }

    @Test
    @DisplayName("setAuctionId cập nhật ID phiên đấu giá đúng")
    void testSetAuctionId() {
        bid.setAuctionId(20);
        assertEquals(20, bid.getAuctionId());
    }

    @Test
    @DisplayName("setBidTime cập nhật thời gian đặt giá đúng")
    void testSetBidTime() {
        LocalDateTime newTime = LocalDateTime.of(2026, 6, 1, 10, 0, 0);
        bid.setBidTime(newTime);
        assertEquals(newTime, bid.getBidTime());
    }

    // ───────── bidderUsername ─────────
    // bidderUsername không có trong constructor — dùng setter khi load lịch sử từ DB

    @Test
    @DisplayName("bidderUsername mặc định là null khi dùng constructor đầy đủ")
    void testBidderUsername_DefaultNull() {
        // Constructor đầy đủ không nhận bidderUsername → mặc định null
        assertNull(bid.getBidderUsername(),
                "bidderUsername phai la null neu chua set");
    }

    @Test
    @DisplayName("setBidderUsername cập nhật đúng")
    void testSetBidderUsername() {
        bid.setBidderUsername("alice");
        assertEquals("alice", bid.getBidderUsername());
    }

    @Test
    @DisplayName("setBidderUsername với null → getBidderUsername trả null")
    void testSetBidderUsername_Null() {
        bid.setBidderUsername(null);
        assertNull(bid.getBidderUsername());
    }

    // ───────── toString() ─────────

    @Test
    @DisplayName("toString() chứa các thông tin quan trọng")
    void testToString() {
        String result = bid.toString();
        assertTrue(result.contains("id=1"));
        assertTrue(result.contains("auctionId=10"));
        assertTrue(result.contains("bidderId=5"));
        assertTrue(result.contains("bidAmount=750000.0"));
    }

    // ───────── Validation logic (giá trị biên) ─────────
    // BidTransaction là model thuần túy — không tự validate
    // Validation thuộc về AuctionService / Auction.placeBid()

    @Test
    @DisplayName("bidAmount = 0 vẫn set được (validation thuộc service layer)")
    void testBidAmountZero() {
        bid.setBidAmount(0);
        assertEquals(0, bid.getBidAmount(),
                "Model khong tu validate — service layer moi check");
    }

    @Test
    @DisplayName("bidAmount âm vẫn set được (validation thuộc service layer)")
    void testBidAmountNegative() {
        bid.setBidAmount(-100);
        assertEquals(-100, bid.getBidAmount(),
                "Model khong tu validate — service layer moi check");
    }
}