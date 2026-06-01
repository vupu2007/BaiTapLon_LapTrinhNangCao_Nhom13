package com.auction.model;

import com.auction.shared.model.Art;
import com.auction.shared.model.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cho Art model.
 * Kiểm tra: constructor, getter, setter, kế thừa Item.
 */
@DisplayName("Art Model Tests")
class ArtTest {

    @Test
    @DisplayName("Art: constructor khởi tạo đúng tất cả field")
    void testArtCreation() {
        Art art = new Art("item001", "Mona Lisa", "Tranh noi tieng",
                100_000, 1, 2, "AVAILABLE", "Leonardo da Vinci", 1503);

        assertEquals("item001",            art.getItemId());
        assertEquals("Mona Lisa",          art.getName());
        assertEquals(100_000,              art.getStartingPrice());
        assertEquals("Leonardo da Vinci",  art.getArtist());
        assertEquals(1503,                 art.getYear());
        assertEquals("AVAILABLE",          art.getStatus());
    }

    @Test
    @DisplayName("Art: setter cập nhật đúng artist và year")
    void testArtSetters() {
        Art art = new Art();
        art.setArtist("Van Gogh");
        art.setYear(1889);

        assertEquals("Van Gogh", art.getArtist());
        assertEquals(1889,       art.getYear());
    }

    @Test
    @DisplayName("Art kế thừa Item — instanceof đúng")
    void testArtIsInstanceOfItem() {
        Art art = new Art("item002", "Starry Night", "Tranh dem day sao",
                200_000, 1, 2, "AVAILABLE", "Van Gogh", 1889);

        assertInstanceOf(Item.class, art, "Art phai la Item");
    }

    @Test
    @DisplayName("Art.printInfo() không throw exception — Polymorphism hoạt động")
    void testArtPrintInfo_NoException() {
        Art art = new Art("item001", "Mona Lisa", "Tranh noi tieng",
                100_000, 1, 2, "AVAILABLE", "Leonardo da Vinci", 1503);

        // printInfo() là override method — gọi được không throw là đủ
        assertDoesNotThrow(() -> art.printInfo(),
                "printInfo() phai chay duoc khong throw exception");
    }

    @Test
    @DisplayName("Art là Item — gọi printInfo() qua kiểu cha Item (đa hình)")
    void testArtPrintInfo_Polymorphic() {
        Item item = new Art("item002", "Starry Night", "Tranh dem day sao",
                200_000, 1, 2, "AVAILABLE", "Van Gogh", 1889);

        // Gọi qua kiểu Item → dispatch đến Art.printInfo() — đúng polymorphism
        assertDoesNotThrow(() -> item.printInfo());
    }
}
