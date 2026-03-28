package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SuppliesDAOTest extends AbstractDAOTest {

    @Test
    void testGetByProductId() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products p1 = saveProduct(1L, category, "Ноутбук");
        Products p2 = saveProduct(2L, category, "Мышь");
        Providers provider = saveProvider(1L, "ООО Альфа");

        saveSupply(1L, p1, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveSupply(2L, p2, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        assertEquals(1, suppliesDAO.getByProductId(1L).size());
    }

    @Test
    void testGetByProviderId() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers p1 = saveProvider(1L, "ООО Альфа");
        Providers p2 = saveProvider(2L, "ООО Бета");

        saveSupply(1L, product, p1, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveSupply(2L, product, p2, BigDecimal.valueOf(20), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        assertEquals(1, suppliesDAO.getByProviderId(1L).size());
    }

    @Test
    void testGetByCompleted() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        assertEquals(1, suppliesDAO.getByCompleted(true).size());
        assertEquals(1, suppliesDAO.getByCompleted(false).size());
    }

    @Test
    void testGetByTimeRange() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 2, 1, 10, 0), true);

        List<Supplies> found = suppliesDAO.getByTimeRange(
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 31, 23, 59)
        );
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilterAllNulls() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.now(), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(20), LocalDateTime.now(), true);

        SuppliesDAO.Filter filter = SuppliesDAO.getFilterBuilder().build();
        assertEquals(2, suppliesDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterAllConstraints() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), true);
        saveSupply(2L, product, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 2, 1, 10, 0), false);

        SuppliesDAO.Filter filter = SuppliesDAO.getFilterBuilder()
                .id(1L)
                .productId(1L)
                .providerId(1L)
                .productName("ноут")
                .providerName("альфа")
                .amountFrom(1)
                .amountTo(15)
                .timeFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .timeTo(LocalDateTime.of(2025, 1, 2, 0, 0))
                .completed(true)
                .build();

        List<Supplies> found = suppliesDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetProviderNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);

        assertNotNull(suppliesDAO.getProvider(supply));
        assertEquals(1L, suppliesDAO.getProvider(supply).getId());
    }

    @Test
    void testGetProviderNull() {
        assertNull(suppliesDAO.getProvider(null));
    }

    @Test
    void testGetProductNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);

        assertNotNull(suppliesDAO.getProduct(supply));
        assertEquals(1L, suppliesDAO.getProduct(supply).getId());
    }

    @Test
    void testGetProductNull() {
        assertNull(suppliesDAO.getProduct(null));
    }

    @Test
    void testGetProductUnitsForSupply() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 100, 5);

        saveUnit(1L, product, LocalDateTime.of(2025, 5, 1, 10, 0), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.of(2025, 5, 2, 10, 0), BigDecimal.TWO, shelf, supply, null);

        List<ProductUnits> units = suppliesDAO.getProductUnitsForSupply(supply);
        assertEquals(2, units.size());
    }
}