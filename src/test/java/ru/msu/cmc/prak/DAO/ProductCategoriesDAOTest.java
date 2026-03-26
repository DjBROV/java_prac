package ru.msu.cmc.prak.DAO;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.msu.cmc.prak.models.ProductCategories;
import ru.msu.cmc.prak.models.Products;
import ru.msu.cmc.prak.models.SizeType;
import ru.msu.cmc.prak.models.UnitsType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application.properties")
public class ProductCategoriesDAOTest {

    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;

    @Autowired
    private ProductsDAO productsDAO;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

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
    void testGetAllByName() {
        ProductCategories cat1 = new ProductCategories();
        cat1.setId(1L);
        cat1.setName("Электроника");

        ProductCategories cat2 = new ProductCategories();
        cat2.setId(2L);
        cat2.setName("Бытовая электроника");

        productCategoriesDAO.save(cat1);
        productCategoriesDAO.save(cat2);

        List<ProductCategories> found = productCategoriesDAO.getAllByName("электроника");
        assertEquals(2, found.size());
    }

    @Test
    void testGetSingleByName() {
        ProductCategories cat = new ProductCategories();
        cat.setId(1L);
        cat.setName("Электроника");
        productCategoriesDAO.save(cat);

        ProductCategories found = productCategoriesDAO.getSingleByName("Электроника");
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetByFilter() {
        ProductCategories cat1 = new ProductCategories();
        cat1.setId(1L);
        cat1.setName("Электроника");

        ProductCategories cat2 = new ProductCategories();
        cat2.setId(2L);
        cat2.setName("Бытовая химия");

        productCategoriesDAO.save(cat1);
        productCategoriesDAO.save(cat2);

        ProductCategoriesDAO.Filter filter = ProductCategoriesDAO.getFilterBuilder()
                .name("Электроника")
                .build();

        List<ProductCategories> filtered = productCategoriesDAO.getByFilter(filter);
        assertEquals(1, filtered.size());
        assertEquals(1L, filtered.getFirst().getId());
    }

    @Test
    void testGetProductsInCategory() {
        ProductCategories category = new ProductCategories();
        category.setId(1L);
        category.setName("Электроника");
        productCategoriesDAO.save(category);

        Products product1 = new Products();
        product1.setId(100L);
        product1.setCategory(category);
        product1.setName("Смартфон");
        product1.setUnit(UnitsType.kg);
        product1.setProduct_size(SizeType.small);
        product1.setUnitsForOne(1);
        product1.setStorageLife(Duration.ofDays(730));

        Products product2 = new Products();
        product2.setId(101L);
        product2.setCategory(category);
        product2.setName("Ноутбук");
        product2.setUnit(UnitsType.kg);
        product2.setProduct_size(SizeType.large);
        product2.setUnitsForOne(1);
        product2.setStorageLife(Duration.ofDays(730));

        productsDAO.save(product1);
        productsDAO.save(product2);

        List<Products> productsInCategory = productCategoriesDAO.getProductsInCategory(category);
        assertEquals(2, productsInCategory.size());
    }

    @Test
    void testCountProductsInCategory() {
        ProductCategories category = new ProductCategories();
        category.setId(1L);
        category.setName("Электроника");
        productCategoriesDAO.save(category);

        Products product1 = new Products();
        product1.setId(100L);
        product1.setCategory(category);
        product1.setName("Смартфон");
        product1.setUnit(UnitsType.kg);
        product1.setProduct_size(SizeType.small);
        product1.setUnitsForOne(1);
        product1.setStorageLife(Duration.ofDays(730));

        Products product2 = new Products();
        product2.setId(101L);
        product2.setCategory(category);
        product2.setName("Ноутбук");
        product2.setUnit(UnitsType.kg);
        product2.setProduct_size(SizeType.large);
        product2.setUnitsForOne(1);
        product2.setStorageLife(Duration.ofDays(730));

        productsDAO.save(product1);
        productsDAO.save(product2);

        long count = productCategoriesDAO.countProductsInCategory(category);
        assertEquals(2, count);
    }

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}