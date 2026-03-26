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
public class ShelfsWorkloadDAOTest {

    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;
    @Autowired
    private ProductUnitsDAO productUnitsDAO;
    @Autowired
    private SuppliesDAO suppliesDAO;
    @Autowired
    private ProvidersDAO providersDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetByRoomNum() {
        saveShelf(1L, 10, 50);
        saveShelf(2L, 10, 100);

        List<ShelfsWorkload> found = shelfsWorkloadDAO.getByRoomNum(10);
        assertEquals(2, found.size());
    }

    @Test
    void testGetShelvesWithFreeSpace() {
        saveShelf(1L, 10, 100);
        saveShelf(2L, 10, 490);

        List<ShelfsWorkload> found = shelfsWorkloadDAO.getShelvesWithFreeSpace(20);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetUnitsOnShelf() {
        ShelfsWorkload shelf = saveShelf(1L, 10, 50);
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Мышь");
        Providers provider = saveProvider(1L, "Поставщик");
        Supplies supply = saveSupply(1L, product, provider);

        saveUnit(1L, product, supply, shelf);
        saveUnit(2L, product, supply, shelf);

        List<ProductUnits> found = shelfsWorkloadDAO.getUnitsOnShelf(shelf);
        assertEquals(2, found.size());
    }

    @Test
    void testUpdateWorkload() {
        ShelfsWorkload shelf = saveShelf(1L, 10, 50);

        shelfsWorkloadDAO.updateWorkload(shelf, 77);

        ShelfsWorkload updated = shelfsWorkloadDAO.getById(1L);
        assertNotNull(updated);
        assertEquals(77, updated.getWorkloadCount());
    }

    @Test
    void testGetByFilter() {
        saveShelf(1L, 10, 50);
        saveShelf(2L, 20, 200);

        ShelfsWorkloadDAO.Filter filter = ShelfsWorkloadDAO.getFilterBuilder()
                .roomNum(10)
                .minWorkload(40)
                .maxWorkload(100)
                .build();

        List<ShelfsWorkload> found = shelfsWorkloadDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    private ShelfsWorkload saveShelf(Long id, Integer roomNum, Integer workload) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(roomNum);
        shelf.setWorkloadCount(workload);
        shelfsWorkloadDAO.save(shelf);
        return shelf;
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
        product.setProduct_size(SizeType.small);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(10));
        productsDAO.save(product);
        return product;
    }

    private Providers saveProvider(Long id, String name) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setPhoneNum("123");
        provider.setEmail("p" + id + "@test");
        providersDAO.save(provider);
        return provider;
    }

    private Supplies saveSupply(Long id, Products product, Providers provider) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(BigDecimal.valueOf(8));
        supply.setTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        suppliesDAO.save(supply);
        return supply;
    }

    private ProductUnits saveUnit(Long id, Products product, Supplies supply, ShelfsWorkload shelf) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setSupply(supply);
        unit.setShelf(shelf);
        unit.setArrival(LocalDateTime.of(2026, 1, 1, 11, 0));
        unit.setAmount(BigDecimal.valueOf(1));
        productUnitsDAO.save(unit);
        return unit;
    }

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE product_units CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE supplies CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE providers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE shelfs_workload CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}