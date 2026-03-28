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