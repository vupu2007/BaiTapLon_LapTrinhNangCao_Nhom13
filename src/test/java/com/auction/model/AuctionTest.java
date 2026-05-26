package com.auction.model;

import com.auction.shared.model.Auction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho Auction model.
 * Bao gồm: constructor, getter/setter, isActive(), chuyển trạng thái.
 */
@DisplayName("Auction Model Tests")
class AuctionTest {

    private Auction auction;
    private final LocalDateTime FUTURE = LocalDateTime.now().plusHours(2);
    private final LocalDateTime PAST   = LocalDateTime.now().minusHours(1);

    @BeforeEach
    void setUp() {
        // 🚀 KHỞI TẠO QUA BUILDER: Vừa sạch code, vừa không lo lỗi lệch tham số constructor!
        auction = Auction.builder()
                .id(1)
                .title("ITEM_001") // Nếu trong class đặt tên là itemId thì sửa thành .itemId("ITEM_001")
                .sellerId(10)
                .startPrice(500000.0)
                .currentPrice(500000.0)
                .minIncrement(50000.0)
                .winnerId(null)
                .startTime(LocalDateTime.now().minusMinutes(10))
                .endTime(FUTURE)
                .status(Auction.AuctionStatus.RUNNING)
                // .thuocTinhThu11(null) // Đền bù nốt thuộc tính số 11 của ông vào đây nếu cần
                .build();
    }

    // ───────── Constructor & Getter ─────────

    @Test
    @DisplayName("Constructor đầy đủ khởi tạo đúng tất cả field")
    void testConstructorAndGetters() {
        assertEquals(1, auction.getId());
        assertEquals("ITEM_001", auction.getItemId());
        assertEquals(10, auction.getSellerId());
        assertEquals(500_000, auction.getStartPrice());
        assertEquals(500_000, auction.getCurrentPrice());
        assertEquals(50_000, auction.getMinIncrement());
        assertNull(auction.getWinnerId());
        assertEquals(Auction.AuctionStatus.RUNNING, auction.getStatus());
    }

    @Test
    @DisplayName("Constructor rỗng tạo object không null")
    void testEmptyConstructor() {
        Auction empty = new Auction();
        assertNotNull(empty);
    }

    // ───────── Setter ─────────

    @Test
    @DisplayName("setCurrentPrice cập nhật giá hiện tại đúng")
    void testSetCurrentPrice() {
        auction.setCurrentPrice(700_000);
        assertEquals(700_000, auction.getCurrentPrice());
    }

    @Test
    @DisplayName("setWinnerId cập nhật người thắng đúng")
    void testSetWinnerId() {
        auction.setWinnerId(99);
        assertEquals(99, auction.getWinnerId());
    }

    @Test
    @DisplayName("setStatus thay đổi trạng thái đúng")
    void testSetStatus() {
        auction.setStatus(Auction.AuctionStatus.FINISHED);
        assertEquals(Auction.AuctionStatus.FINISHED, auction.getStatus());
    }

    // ───────── isActive() ─────────

    @Test
    @DisplayName("isActive() = true khi RUNNING và endTime chưa đến")
    void testIsActive_WhenRunningAndNotExpired() {
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setEndTime(FUTURE);
        assertTrue(auction.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi status là OPEN (chưa bắt đầu)")
    void testIsActive_WhenStatusOpen() {
        auction.setStatus(Auction.AuctionStatus.OPEN);
        assertFalse(auction.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi status là FINISHED")
    void testIsActive_WhenStatusFinished() {
        auction.setStatus(Auction.AuctionStatus.FINISHED);
        assertFalse(auction.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi status là CANCELED")
    void testIsActive_WhenStatusCanceled() {
        auction.setStatus(Auction.AuctionStatus.CANCELED);
        assertFalse(auction.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi RUNNING nhưng endTime đã qua")
    void testIsActive_WhenRunningButExpired() {
        auction.setStatus(Auction.AuctionStatus.RUNNING);
        auction.setEndTime(PAST);
        assertFalse(auction.isActive());
    }

    @Test
    @DisplayName("isActive() = false khi status là PAID")
    void testIsActive_WhenStatusPaid() {
        auction.setStatus(Auction.AuctionStatus.PAID);
        assertFalse(auction.isActive());
    }

    // ───────── AuctionStatus enum ─────────

    @Test
    @DisplayName("Tất cả 5 trạng thái AuctionStatus tồn tại đúng")
    void testAuctionStatusValues() {
        Auction.AuctionStatus[] statuses = Auction.AuctionStatus.values();
        assertEquals(5, statuses.length);
        assertNotNull(Auction.AuctionStatus.valueOf("OPEN"));
        assertNotNull(Auction.AuctionStatus.valueOf("RUNNING"));
        assertNotNull(Auction.AuctionStatus.valueOf("FINISHED"));
        assertNotNull(Auction.AuctionStatus.valueOf("PAID"));
        assertNotNull(Auction.AuctionStatus.valueOf("CANCELED"));
    }

    // ───────── minIncrement ─────────

    @Test
    @DisplayName("setMinIncrement cập nhật bước giá tối thiểu đúng")
    void testSetMinIncrement() {
        auction.setMinIncrement(100_000);
        assertEquals(100_000, auction.getMinIncrement());
    }
}
