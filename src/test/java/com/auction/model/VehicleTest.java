package com.auction.model;

import com.auction.shared.model.Item;
import com.auction.shared.model.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class cho Vehicle (subclass của Item).
 * Bao gồm: constructor, getter/setter, kế thừa Item, printInfo().
 */
@DisplayName("Vehicle (Item) Tests")
public class VehicleTest {

    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        vehicle = new Vehicle(
                "item003", "Toyota Camry", "Xe sedan cao cap",
                500_000_000, 1, 3, "AVAILABLE",
                "Toyota", "Camry", 2020, 30000
        );
    }

    // ───────── Constructor & Getter ─────────

    @Test
    @DisplayName("Vehicle: constructor khởi tạo đúng tất cả field")
    public void testVehicleCreation() {
        assertEquals("item003",       vehicle.getItemId());
        assertEquals("Toyota Camry",  vehicle.getName());
        assertEquals(500_000_000,     vehicle.getStartingPrice());
        assertEquals("Toyota",        vehicle.getBrand());
        assertEquals("Camry",         vehicle.getModel());
        assertEquals(2020,            vehicle.getYear());
        assertEquals(30000,           vehicle.getMileage());
        assertEquals("AVAILABLE",     vehicle.getStatus());
    }

    @Test
    @DisplayName("Constructor rỗng tạo object không null")
    public void testEmptyConstructor() {
        Vehicle empty = new Vehicle();
        assertNotNull(empty);
    }

    // ───────── Setter ─────────

    @Test
    @DisplayName("Vehicle: setter cập nhật đúng brand, model, year, mileage")
    public void testVehicleSetters() {
        vehicle.setBrand("Honda");
        vehicle.setModel("Civic");
        vehicle.setYear(2022);
        vehicle.setMileage(10000);

        assertEquals("Honda",  vehicle.getBrand());
        assertEquals("Civic",  vehicle.getModel());
        assertEquals(2022,     vehicle.getYear());
        assertEquals(10000,    vehicle.getMileage());
    }

    @Test
    @DisplayName("setStatus() chuyển sang IN_AUCTION đúng")
    public void testSetStatusInAuction() {
        vehicle.setStatus("IN_AUCTION");
        assertEquals("IN_AUCTION", vehicle.getStatus());
    }

    @Test
    @DisplayName("setStatus() chuyển sang SOLD đúng")
    public void testSetStatusSold() {
        vehicle.setStatus("SOLD");
        assertEquals("SOLD", vehicle.getStatus());
    }

    // ───────── Kế thừa ─────────

    @Test
    @DisplayName("Vehicle kế thừa Item — instanceof đúng")
    public void testVehicleIsInstanceOfItem() {
        assertInstanceOf(Item.class, vehicle, "Vehicle phai la Item");
    }

    // ───────── printInfo() ─────────

    @Test
    @DisplayName("printInfo() không ném exception")
    public void testPrintInfoDoesNotThrow() {
        assertDoesNotThrow(() -> vehicle.printInfo());
    }
}