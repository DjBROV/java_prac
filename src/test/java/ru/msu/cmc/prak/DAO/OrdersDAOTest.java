package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class OrdersDAOTest extends AbstractDAOTest {

    @Test
    void testGetByIdWithFetchedRelations() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders saved = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2025, 1, 1, 10, 0), false);

        Orders found = ordersDAO.getById(saved.getId());

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertNotNull(found.getProduct());
        assertEquals("Ноутбук", found.getProduct().getName());
        assertNotNull(found.getConsumer());
        assertEquals("Иван", found.getConsumer().getName());
    }

    @Test
    void testGetByIdNotFound() {
        assertNull(ordersDAO.getById(999L));
    }

    @Test
    void testGetByFilterAllNulls() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(7), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder().build();
        assertEquals(2, ordersDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterAllConstraints() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2025, 1, 1, 10, 0), true);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(15), LocalDateTime.of(2025, 2, 1, 10, 0), false);

        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder()
                .id(1L)
                .productId(1L)
                .consumerId(1L)
                .productName("ноут")
                .consumerName("ива")
                .amountFrom(1)
                .amountTo(10)
                .timeFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .timeTo(LocalDateTime.of(2025, 1, 2, 0, 0))
                .completed(true)
                .build();

        List<Orders> found = ordersDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetByFilterOnlyCompletedFalseBranch() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");

        saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveOrder(2L, product, consumer, BigDecimal.ONE, LocalDateTime.of(2025, 1, 2, 10, 0), true);

        OrdersDAO.Filter filter = OrdersDAO.getFilterBuilder()
                .completed(false)
                .build();

        List<Orders> found = ordersDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertFalse(found.getFirst().isCompleted());
    }

    @Test
    void testGetConsumerNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.now(), false);

        assertNotNull(ordersDAO.getConsumer(order));
        assertEquals(1L, ordersDAO.getConsumer(order).getId());
    }

    @Test
    void testGetConsumerNull() {
        assertNull(ordersDAO.getConsumer(null));
    }

    @Test
    void testGetProductNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.now(), false);

        assertNotNull(ordersDAO.getProduct(order));
        assertEquals(1L, ordersDAO.getProduct(order).getId());
    }

    @Test
    void testGetProductNull() {
        assertNull(ordersDAO.getProduct(null));
    }

    @Test
    void testGetProductUnitsForOrder() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.valueOf(20), LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 100, 10);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.now(), false);

        saveUnit(1L, product, LocalDateTime.of(2025, 1, 1, 12, 0), BigDecimal.ONE, shelf, supply, order);
        saveUnit(2L, product, LocalDateTime.of(2025, 1, 2, 12, 0), BigDecimal.TWO, shelf, supply, order);

        List<ProductUnits> units = ordersDAO.getProductUnitsForOrder(order);
        assertEquals(2, units.size());
        assertNotNull(units.getFirst().getProduct());
        assertNotNull(units.getFirst().getShelf());
        assertNotNull(units.getFirst().getSupply());
        assertNotNull(units.getFirst().getOrder());
    }

    @Test
    void testGetProductUnitsForOrderEmpty() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);

        List<ProductUnits> units = ordersDAO.getProductUnitsForOrder(order);
        assertTrue(units.isEmpty());
    }
}