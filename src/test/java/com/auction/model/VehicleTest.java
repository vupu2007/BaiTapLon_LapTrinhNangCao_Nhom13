package com.auction.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VehicleTest {

    @Test
    public void testVehicleCreation() {
        Vehicle vehicle = new Vehicle("item003", "Toyota Camry", "Xe sedan cao cấp",
                500000000, 1, 3, "AVAILABLE",
                "Toyota", "Camry", 2020, 30000);

        assertEquals("item003", vehicle.getItemId());
        assertEquals("Toyota Camry", vehicle.getName());
        assertEquals(500000000, vehicle.getStartingPrice());
        assertEquals("Toyota", vehicle.getBrand());
        assertEquals("Camry", vehicle.getModel());
        assertEquals(2020, vehicle.getYear());
        assertEquals(30000, vehicle.getMileage());
        assertEquals("AVAILABLE", vehicle.getStatus());
    }

    @Test
    public void testVehicleSetters() {
        Vehicle vehicle = new Vehicle();
        vehicle.setBrand("Honda");
        vehicle.setModel("Civic");
        vehicle.setYear(2022);
        vehicle.setMileage(10000);

        assertEquals("Honda", vehicle.getBrand());
        assertEquals("Civic", vehicle.getModel());
        assertEquals(2022, vehicle.getYear());
        assertEquals(10000, vehicle.getMileage());
    }

    @Test
    public void testVehicleIsInstanceOfItem() {
        Vehicle vehicle = new Vehicle("item004", "Honda Civic", "Xe đời mới",
                400000000, 1, 3, "AVAILABLE",
                "Honda", "Civic", 2022, 10000);

        assertInstanceOf(Item.class, vehicle); // Kiểm tra kế thừa
    }
}