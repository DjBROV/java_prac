package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConsumersDAOTest extends AbstractDAOTest {

    @Test
    void testGetAllByNameFound() {
        saveConsumer(1L, "Иван");
        saveConsumer(2L, "Иван Петров");
        assertEquals(2, consumersDAO.getAllByName("иван").size());
    }

    @Test
    void testGetAllByNameNotFound() {
        saveConsumer(1L, "Иван");
        assertTrue(consumersDAO.getAllByName("мария").isEmpty());
    }

    @Test
    void testGetSingleByNameFound() {
        saveConsumer(1L, "Иван");
        Consumers found = consumersDAO.getSingleByName("Иван");
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetSingleByNameNotFound() {
        saveConsumer(1L, "Иван");
        assertNull(consumersDAO.getSingleByName("Мария"));
    }

    @Test
    void testGetByPhoneNum() {
        saveConsumer(1L, "Иван", null, null, "12345", "a@test");
        saveConsumer(2L, "Петр", null, null, "99999", "b@test");

        List<Consumers> found = consumersDAO.getByPhoneNum("123");
        assertEquals(1, found.size());
    }

    @Test
    void testGetByEmail() {
        saveConsumer(1L, "Иван", null, null, "12345", "ivan@test");
        saveConsumer(2L, "Петр", null, null, "99999", "petr@test");

        List<Consumers> found = consumersDAO.getByEmail("ivan");
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilterAllNulls() {
        saveConsumer(1L, "Иван");
        saveConsumer(2L, "Петр");

        ConsumersDAO.Filter filter = ConsumersDAO.getFilterBuilder().build();
        assertEquals(2, consumersDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterWithAllFields() {
        saveConsumer(1L, "Иван", "desc", "Москва", "12345", "ivan@test");
        saveConsumer(2L, "Петр", "desc2", "Казань", "99999", "petr@test");

        ConsumersDAO.Filter filter = ConsumersDAO.getFilterBuilder()
                .id(1L)
                .name("ива")
                .address("моск")
                .phoneNum("123")
                .email("ivan")
                .build();

        List<Consumers> found = consumersDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetOrdersByConsumer() {
        Consumers consumer = saveConsumer(1L, "Иван");
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(3), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(4), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        List<Orders> orders = consumersDAO.getOrdersByConsumer(consumer);
        assertEquals(2, orders.size());
    }
}