package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;
import ru.msu.cmc.prak.models.Providers;
import ru.msu.cmc.prak.models.Supplies;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProvidersDAOTest extends AbstractDAOTest {

    @Test
    void testGetAllByNameFound() {
        saveProvider(1L, "ООО Альфа");
        saveProvider(2L, "ООО Альфа Логистик");
        assertEquals(2, providersDAO.getAllByName("альфа").size());
    }

    @Test
    void testGetAllByNameNotFound() {
        saveProvider(1L, "ООО Альфа");
        assertTrue(providersDAO.getAllByName("бета").isEmpty());
    }

    @Test
    void testGetSingleByNameFound() {
        saveProvider(1L, "ООО Альфа");
        Providers found = providersDAO.getSingleByName("ООО Альфа");
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetSingleByNameNotFound() {
        saveProvider(1L, "ООО Альфа");
        assertNull(providersDAO.getSingleByName("ООО Бета"));
    }

    @Test
    void testGetByPhoneNum() {
        saveProvider(1L, "ООО Альфа", null, null, "12345", "a@test");
        saveProvider(2L, "ООО Бета", null, null, "99999", "b@test");
        assertEquals(1, providersDAO.getByPhoneNum("123").size());
    }

    @Test
    void testGetByEmail() {
        saveProvider(1L, "ООО Альфа", null, null, "12345", "alpha@test");
        saveProvider(2L, "ООО Бета", null, null, "99999", "beta@test");
        assertEquals(1, providersDAO.getByEmail("alpha").size());
    }

    @Test
    void testGetByFilterAllNulls() {
        saveProvider(1L, "ООО Альфа");
        saveProvider(2L, "ООО Бета");
        ProvidersDAO.Filter filter = ProvidersDAO.getFilterBuilder().build();
        assertEquals(2, providersDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterWithAllFields() {
        saveProvider(1L, "ООО Альфа", "desc", "Москва", "12345", "alpha@test");
        saveProvider(2L, "ООО Бета", "desc2", "Казань", "99999", "beta@test");

        ProvidersDAO.Filter filter = ProvidersDAO.getFilterBuilder()
                .id(1L)
                .name("альф")
                .address("моск")
                .phoneNum("123")
                .email("alpha")
                .build();

        List<Providers> found = providersDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetSuppliesFromProvider() {
        Providers provider = saveProvider(1L, "ООО Альфа");
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 4, 1, 10, 0), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(15), LocalDateTime.of(2025, 4, 2, 10, 0), true);

        List<Supplies> found = providersDAO.getSuppliesFromProvider(provider);
        assertEquals(2, found.size());
    }
}