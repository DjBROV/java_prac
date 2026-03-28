package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductsDAOTest extends AbstractDAOTest {

    @Test
    void testGetByFilterAllNulls() {
        ProductCategories c1 = saveCategory(1L, "Техника");
        ProductCategories c2 = saveCategory(2L, "Мебель");
        saveProduct(1L, c1, "Ноутбук");
        saveProduct(2L, c2, "Стол");

        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder().build();
        List<Products> found = productsDAO.getByFilter(filter);
        assertEquals(2, found.size());
    }

    @Test
    void testGetByFilterAllConstraintsAndLargeTrue() {
        ProductCategories c1 = saveCategory(1L, "Электроника");
        ProductCategories c2 = saveCategory(2L, "Мебель");

        saveProduct(1L, c1, "Ноутбук Pro", UnitsType.kg, SizeType.large, 2, 100, "desc");
        saveProduct(2L, c2, "Стол", UnitsType.kg, SizeType.large, 1, 20, "desk");

        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder()
                .id(1L)
                .name("ноут")
                .categoryId(1L)
                .categoryName("электро")
                .unit(UnitsType.kg)
                .size(SizeType.large)
                .minStorageLife(Duration.ofDays(50))
                .maxStorageLife(Duration.ofDays(150))
                .large(true)
                .build();

        List<Products> found = productsDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetByFilterLargeFalseBranch() {
        ProductCategories c1 = saveCategory(1L, "Электроника");
        saveProduct(1L, c1, "Ноутбук", UnitsType.kg, SizeType.large, 1, 100, null);
        saveProduct(2L, c1, "Мышь", UnitsType.g, SizeType.small, 1, 100, null);

        ProductsDAO.Filter filter = ProductsDAO.getFilterBuilder()
                .large(false)
                .build();

        List<Products> found = productsDAO.getByFilter(filter);
        assertEquals(2, found.size());
    }



    @Test
    void testGetSuppliesForProduct() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Поставка");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2025, 1, 1, 10, 0), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 1, 2, 10, 0), true);

        List<Supplies> found = productsDAO.getSuppliesForProduct(product);
        assertEquals(2, found.size());
    }



    @Test
    void testGetOrdersForProduct() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Consumers consumer = saveConsumer(1L, "Иван");

        saveOrder(1L, product, consumer, BigDecimal.valueOf(5), LocalDateTime.of(2025, 2, 1, 10, 0), false);
        saveOrder(2L, product, consumer, BigDecimal.valueOf(7), LocalDateTime.of(2025, 2, 2, 10, 0), true);

        List<Orders> found = productsDAO.getOrdersForProduct(product);
        assertEquals(2, found.size());
    }


    @Test
    void testGetUnitsForProduct() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Поставка");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.valueOf(20), LocalDateTime.of(2025, 3, 1, 10, 0), false);
        ShelfsWorkload shelf = saveShelf(1L, 101, 10);

        saveUnit(1L, product, LocalDateTime.of(2025, 3, 1, 11, 0), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.of(2025, 3, 2, 11, 0), BigDecimal.TWO, shelf, supply, null);

        List<ProductUnits> found = productsDAO.getUnitsForProduct(product);
        assertEquals(2, found.size());
    }



    @Test
    void testGetCategoryNonNull() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");

        ProductCategories found = productsDAO.getCategory(product);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetCategoryNull() {
        assertNull(productsDAO.getCategory(null));
    }
}