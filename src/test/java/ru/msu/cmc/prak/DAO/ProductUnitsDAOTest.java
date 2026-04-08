package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductUnitsDAOTest extends AbstractDAOTest {

    @Test
    void testGetByIdWithFetchedRelations() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 10, 0);

        ProductUnits saved = saveUnit(1L, product, LocalDateTime.of(2025, 1, 1, 10, 0), BigDecimal.valueOf(3), shelf, supply, order);

        ProductUnits found = productUnitsDAO.getById(saved.getId());

        assertNotNull(found);
        assertEquals(1L, found.getId());
        assertNotNull(found.getProduct());
        assertNotNull(found.getShelf());
        assertNotNull(found.getSupply());
        assertNotNull(found.getOrder());
        assertNotNull(found.getSupply().getProvider());
    }

    @Test
    void testGetByIdNotFound() {
        assertNull(productUnitsDAO.getById(999L));
    }

    @Test
    void testGetByFilterAllNullsReservedNullBranch() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.TWO, shelf, supply, null);

        ProductUnitsDAO.Filter filter = ProductUnitsDAO.getFilterBuilder().build();
        assertEquals(2, productUnitsDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterReservedFalseBranch() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 10, 0);

        saveUnit(1L, product, LocalDateTime.of(2025, 1, 1, 10, 0), BigDecimal.valueOf(3), shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.of(2025, 1, 2, 10, 0), BigDecimal.valueOf(5), shelf, supply, order);

        ProductUnitsDAO.Filter filter = ProductUnitsDAO.getFilterBuilder()
                .productId(1L)
                .supplyId(1L)
                .supplierId(1L)
                .shelfNum(1L)
                .roomNum(10)
                .minAmount(1)
                .maxAmount(4)
                .arrivalFrom(LocalDateTime.of(2025, 1, 1, 0, 0))
                .arrivalTo(LocalDateTime.of(2025, 1, 1, 23, 59))
                .reserved(false)
                .build();

        List<ProductUnits> found = productUnitsDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertNull(found.getFirst().getOrder());
    }

    @Test
    void testGetByFilterReservedTrueBranch() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 10, 0);

        saveUnit(1L, product, LocalDateTime.of(2025, 1, 1, 10, 0), BigDecimal.valueOf(3), shelf, supply, order);

        ProductUnitsDAO.Filter filter = ProductUnitsDAO.getFilterBuilder()
                .reserved(true)
                .build();

        List<ProductUnits> found = productUnitsDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertNotNull(found.getFirst().getOrder());
    }

    @Test
    void testGetProductNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        ProductUnits unit = saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);

        assertNotNull(productUnitsDAO.getProduct(unit));
    }

    @Test
    void testGetProductNull() {
        assertNull(productUnitsDAO.getProduct(null));
    }

    @Test
    void testGetShelfNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        ProductUnits unit = saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);

        assertNotNull(productUnitsDAO.getShelf(unit));
    }

    @Test
    void testGetShelfNull() {
        assertNull(productUnitsDAO.getShelf(null));
    }

    @Test
    void testGetSupplyNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        ProductUnits unit = saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);

        assertNotNull(productUnitsDAO.getSupply(unit));
    }

    @Test
    void testGetSupplyNull() {
        assertNull(productUnitsDAO.getSupply(null));
    }

    @Test
    void testGetOrderNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        ProductUnits unit = saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, order);

        assertNotNull(productUnitsDAO.getOrder(unit));
    }

    @Test
    void testGetOrderNullArgument() {
        assertNull(productUnitsDAO.getOrder(null));
    }

    @Test
    void testGetOrderNullValueInsideUnit() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);
        ProductUnits unit = saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);

        assertNull(productUnitsDAO.getOrder(unit));
    }

    @Test
    void testGetFreeUnits() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, order);

        List<ProductUnits> free = productUnitsDAO.getFreeUnits();
        assertEquals(1, free.size());
        assertNull(free.getFirst().getOrder());
    }

    @Test
    void testGetReservedUnits() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders order = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, order);

        List<ProductUnits> reserved = productUnitsDAO.getReservedUnits();
        assertEquals(1, reserved.size());
        assertNotNull(reserved.getFirst().getOrder());
    }

    @Test
    void testGetFreeUnitsEmpty() {
        assertTrue(productUnitsDAO.getFreeUnits().isEmpty());
    }

    @Test
    void testGetReservedUnitsEmpty() {
        assertTrue(productUnitsDAO.getReservedUnits().isEmpty());
    }
}