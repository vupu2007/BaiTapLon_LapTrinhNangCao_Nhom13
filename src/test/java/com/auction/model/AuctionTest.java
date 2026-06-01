package com.auction.model;

import com.auction.shared.model.Auction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho Auction model.
 * Bao gồm: constructor, getter/setter, AuctionStatus enum.
 */

@DisplayName("Auction Model Tests")
class AuctionTest {

    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = new Auction();
        auction.setId(1);
        auction.setItemId("ITEM_001");
        auction.setSellerId(10);
        auction.setStartPrice(500_000.0);
        auction.setCurrentPrice(500_000.0);
        auction.setMinIncrement(50_000.0);
        auction.setWinnerId(null);
        auction.setStartTime(LocalDateTime.now().minusMinutes(10));
        auction.setEndTime(LocalDateTime.now().plusHours(2));
        auction.setStatus(Auction.AuctionStatus.RUNNING);
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


