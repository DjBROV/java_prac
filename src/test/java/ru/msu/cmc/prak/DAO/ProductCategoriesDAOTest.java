package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProductCategoriesDAOTest extends AbstractDAOTest {

    @Test
    void testCRUD() {
        ProductCategories category = new ProductCategories();
        category.setId(1L);
        category.setName("Электроника");
        productCategoriesDAO.save(category);

        ProductCategories saved = productCategoriesDAO.getById(1L);
        assertNotNull(saved);
        assertEquals("Электроника", saved.getName());

        saved.setName("Бытовая техника");
        productCategoriesDAO.update(saved);

        ProductCategories updated = productCategoriesDAO.getById(1L);
        assertEquals("Бытовая техника", updated.getName());

        productCategoriesDAO.delete(updated);
        assertNull(productCategoriesDAO.getById(1L));
    }

    @Test
    void testGetAll() {
        List<ProductCategories> categories = new ArrayList<>();

        ProductCategories cat1 = new ProductCategories();
        cat1.setId(1L);
        cat1.setName("Электроника");

        ProductCategories cat2 = new ProductCategories();
        cat2.setId(2L);
        cat2.setName("Бытовая химия");

        categories.add(cat1);
        categories.add(cat2);
        productCategoriesDAO.saveCollection(categories);

        List<ProductCategories> all = (List<ProductCategories>) productCategoriesDAO.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void testGetByFilterWithName() {
        saveCategory(1L, "Электроника");
        saveCategory(2L, "Бытовая химия");

        ProductCategoriesDAO.Filter filter = ProductCategoriesDAO.getFilterBuilder()
                .name("электро")
                .build();

        List<ProductCategories> filtered = productCategoriesDAO.getByFilter(filter);
        assertEquals(1, filtered.size());
        assertEquals(1L, filtered.getFirst().getId());
    }

    @Test
    void testGetByFilterWithoutNameReturnsAll() {
        saveCategory(1L, "Электроника");
        saveCategory(2L, "Бытовая химия");

        ProductCategoriesDAO.Filter filter = ProductCategoriesDAO.getFilterBuilder().build();

        List<ProductCategories> filtered = productCategoriesDAO.getByFilter(filter);
        assertEquals(2, filtered.size());
    }



    @Test
    void testGetProductsInCategory() {
        ProductCategories category = saveCategory(1L, "Электроника");
        saveProduct(100L, category, "Смартфон");
        saveProduct(101L, category, "Ноутбук");

        List<Products> productsInCategory = productCategoriesDAO.getProductsInCategory(category);
        assertEquals(2, productsInCategory.size());
    }



    @Test
    void testCountProductsInCategoryNonZero() {
        ProductCategories category = saveCategory(1L, "Электроника");
        saveProduct(100L, category, "Смартфон");
        saveProduct(101L, category, "Ноутбук");

        long count = productCategoriesDAO.countProductsInCategory(category);
        assertEquals(2, count);
    }

    @Test
    void testCountProductsInCategoryZero() {
        ProductCategories category = saveCategory(1L, "Электроника");
        assertEquals(0, productCategoriesDAO.countProductsInCategory(category));
    }
}