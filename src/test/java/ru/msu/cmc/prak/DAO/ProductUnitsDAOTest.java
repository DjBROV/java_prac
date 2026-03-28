package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductUnitsDAOTest extends AbstractDAOTest {

    @Test
    void testGetByProductId() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products p1 = saveProduct(1L, category, "Ноутбук");
        Products p2 = saveProduct(2L, category, "Мышь");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply1 = saveSupply(1L, p1, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Supplies supply2 = saveSupply(2L, p2, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, p1, LocalDateTime.now(), BigDecimal.ONE, shelf, supply1, null);
        saveUnit(2L, p2, LocalDateTime.now(), BigDecimal.ONE, shelf, supply2, null);

        assertEquals(1, productUnitsDAO.getByProductId(1L).size());
    }

    @Test
    void testGetByShelfNum() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload s1 = saveShelf(1L, 1, 0);
        ShelfsWorkload s2 = saveShelf(2L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, s1, supply, null);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.ONE, s2, supply, null);

        assertEquals(1, productUnitsDAO.getByShelfNum(1L).size());
    }

    @Test
    void testGetBySupplyId() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies s1 = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Supplies s2 = saveSupply(2L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, s1, null);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, s2, null);

        assertEquals(1, productUnitsDAO.getBySupplyId(1L).size());
    }

    @Test
    void testGetByOrderId() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        Orders o1 = saveOrder(1L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        Orders o2 = saveOrder(2L, product, consumer, BigDecimal.ONE, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, o1);
        saveUnit(2L, product, LocalDateTime.now(), BigDecimal.ONE, shelf, supply, o2);

        assertEquals(1, productUnitsDAO.getByOrderId(1L).size());
    }

    @Test
    void testGetByArrivalRange() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 1, 0);

        saveUnit(1L, product, LocalDateTime.of(2025, 1, 10, 10, 0), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.of(2025, 2, 10, 10, 0), BigDecimal.ONE, shelf, supply, null);

        List<ProductUnits> found = productUnitsDAO.getByArrivalRange(
                LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 1, 31, 23, 59)
        );
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilterAllNullsReservedNullBranchAndBdNullBranch() {
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

        assertEquals(1, productUnitsDAO.getFreeUnits().size());
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

        assertEquals(1, productUnitsDAO.getReservedUnits().size());
    }
}