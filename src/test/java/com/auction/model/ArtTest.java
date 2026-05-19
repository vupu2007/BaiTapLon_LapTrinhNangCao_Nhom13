package com.auction.model;

import com.auction.shared.model.Art;
import com.auction.shared.model.Item;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArtTest {

    @Test
    public void testArtCreation() {
        Art art = new Art("item001", "Mona Lisa", "Tranh nổi tiếng",
                100000, 1, 2, "AVAILABLE", "Leonardo da Vinci", 1503);

        assertEquals("item001", art.getItemId());
        assertEquals("Mona Lisa", art.getName());
        assertEquals(100000, art.getStartingPrice());
        assertEquals("Leonardo da Vinci", art.getArtist());
        assertEquals(1503, art.getYear());
        assertEquals("AVAILABLE", art.getStatus());
    }

    @Test
    public void testArtSetters() {
        Art art = new Art();
        art.setArtist("Van Gogh");
        art.setYear(1889);

        assertEquals("Van Gogh", art.getArtist());
        assertEquals(1889, art.getYear());
    }

    @Test
    public void testArtIsInstanceOfItem() {
        Art art = new Art("item002", "Starry Night", "Tranh đêm đầy sao",
                200000, 1, 2, "AVAILABLE", "Van Gogh", 1889);

        assertInstanceOf(Item.class, art); // Kiểm tra kế thừa
    }
}