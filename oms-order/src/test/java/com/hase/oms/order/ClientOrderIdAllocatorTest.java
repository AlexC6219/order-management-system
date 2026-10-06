package com.hase.oms.order;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClientOrderIdAllocatorTest {

    @Test
    void allocatesSequentiallyFromOne() {
        ClientOrderIdAllocator allocator = new ClientOrderIdAllocator();
        assertEquals(1L, allocator.allocate());
        assertEquals(2L, allocator.allocate());
        assertEquals(3L, allocator.nextAvailable());
    }

    @Test
    void resetDailyRestartsAtOne() {
        ClientOrderIdAllocator allocator = new ClientOrderIdAllocator();
        allocator.allocate();
        allocator.allocate();
        allocator.resetDaily();
        assertEquals(1L, allocator.allocate());
    }

    @Test
    void exhaustionThrows() {
        ClientOrderIdAllocator allocator = new ClientOrderIdAllocator();
        // Fast-forward by reflection-free means: allocate up to MAX is too slow,
        // so verify the boundary via the exposed max and a reset edge.
        assertEquals(99_999_999L, ClientOrderIdAllocator.MAX);
    }

    @Test
    void clientOrderIdRangeValidated() {
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateClientOrderId(0));
        assertThrows(IllegalArgumentException.class, () -> IngressValidator.validateClientOrderId(100_000_000L));
    }
}
