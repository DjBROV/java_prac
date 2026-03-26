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
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application.properties")
public class ProvidersDAOTest {

    @Autowired
    private ProvidersDAO providersDAO;
    @Autowired
    private SuppliesDAO suppliesDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetAllByName() {
        saveProvider(1L, "ООО Альфа");
        saveProvider(2L, "ООО Альфа Плюс");

        List<Providers> found = providersDAO.getAllByName("Альфа");
        assertEquals(2, found.size());
    }

    @Test
    void testGetSingleByName() {
        saveProvider(1L, "Бета");

        Providers found = providersDAO.getSingleByName("Бета");
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetByPhoneNum() {
        saveProvider(1L, "A", "555123", "a@test");
        saveProvider(2L, "B", "555999", "b@test");

        List<Providers> found = providersDAO.getByPhoneNum("555");
        assertEquals(2, found.size());
    }

    @Test
    void testGetByEmail() {
        saveProvider(1L, "A", "111", "alpha@test");
        saveProvider(2L, "B", "222", "beta@test");

        List<Providers> found = providersDAO.getByEmail("alpha");
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetByFilter() {
        Providers provider = new Providers();
        provider.setId(1L);
        provider.setName("Поставщик");
        provider.setAddress("Казань");
        provider.setPhoneNum("7000");
        provider.setEmail("sup@test");
        providersDAO.save(provider);

        ProvidersDAO.Filter filter = ProvidersDAO.getFilterBuilder()
                .name("Постав")
                .address("Каз")
                .phoneNum("700")
                .email("sup")
                .build();

        List<Providers> found = providersDAO.getByFilter(filter);
        assertEquals(1, found.size());
    }

    @Test
    void testGetSuppliesFromProvider() {
        Providers provider = saveProvider(1L, "Поставщик");
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Монитор");

        saveSupply(1L, product, provider);
        saveSupply(2L, product, provider);

        List<Supplies> found = providersDAO.getSuppliesFromProvider(provider);
        assertEquals(2, found.size());
    }

    private Providers saveProvider(Long id, String name) {
        return saveProvider(id, name, "111", "p" + id + "@test");
    }

    private Providers saveProvider(Long id, String name, String phone, String email) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setPhoneNum(phone);
        provider.setEmail(email);
        providersDAO.save(provider);
        return provider;
    }

    private ProductCategories saveCategory(Long id, String name) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName(name);
        productCategoriesDAO.save(category);
        return category;
    }

    private Products saveProduct(Long id, ProductCategories category, String name) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category);
        product.setName(name);
        product.setUnit(UnitsType.kg);
        product.setProduct_size(SizeType.middle);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(40));
        productsDAO.save(product);
        return product;
    }

    private Supplies saveSupply(Long id, Products product, Providers provider) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(BigDecimal.valueOf(7));
        supply.setTime(LocalDateTime.of(2026, 1, 2, 10, 0));
        suppliesDAO.save(supply);
        return supply;
    }

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE supplies CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE providers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}