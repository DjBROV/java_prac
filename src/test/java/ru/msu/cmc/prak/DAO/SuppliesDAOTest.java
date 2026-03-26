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
public class SuppliesDAOTest {

    @Autowired
    private SuppliesDAO suppliesDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ProvidersDAO providersDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private ProductUnitsDAO productUnitsDAO;
    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetByProductId() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Товар");
        Providers provider = saveProvider(1L, "П1");

        saveSupply(1L, product, provider, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), false);

        List<Supplies> found = suppliesDAO.getByProductId(1L);
        assertEquals(1, found.size());
    }

    @Test
    void testGetByProviderId() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Товар");
        Providers provider = saveProvider(1L, "П1");

        saveSupply(1L, product, provider, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), false);

        List<Supplies> found = suppliesDAO.getByProviderId(1L);
        assertEquals(1, found.size());
    }

    @Test
    void testGetByCompleted() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Товар");
        Providers provider = saveProvider(1L, "П1");

        saveSupply(1L, product, provider, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), true);
        saveSupply(2L, product, provider, BigDecimal.valueOf(6), LocalDateTime.of(2026, 1, 2, 10, 0), false);

        List<Supplies> found = suppliesDAO.getByCompleted(true);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetByTimeRange() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Товар");
        Providers provider = saveProvider(1L, "П1");

        saveSupply(1L, product, provider, BigDecimal.valueOf(5), LocalDateTime.of(2026, 1, 1, 10, 0), false);
        saveSupply(2L, product, provider, BigDecimal.valueOf(6), LocalDateTime.of(2026, 2, 1, 10, 0), false);

        List<Supplies> found = suppliesDAO.getByTimeRange(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59)
        );
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilter() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "Поставщик A");

        saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        SuppliesDAO.Filter filter = SuppliesDAO.getFilterBuilder()
                .productName("Ноут")
                .providerName("Поставщик")
                .amountFrom(5)
                .amountTo(20)
                .timeFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .timeTo(LocalDateTime.of(2026, 1, 2, 0, 0))
                .completed(true)
                .build();

        List<Supplies> found = suppliesDAO.getByFilter(filter);
        assertEquals(1, found.size());
    }

    @Test
    void testGetProvider() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "Поставщик A");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        Providers found = suppliesDAO.getProvider(supply);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetProduct() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "Поставщик A");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2026, 1, 1, 10, 0), true);

        Products found = suppliesDAO.getProduct(supply);
        assertNotNull(found);
        assertEquals(1L, found.getId());
    }

    @Test
    void testGetProductUnitsForSupply() {
        ProductCategories category = saveCategory(1L, "Техника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "Поставщик A");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.valueOf(10), LocalDateTime.of(2026, 1, 1, 10, 0), true);
        ShelfsWorkload shelf = saveShelf(1L, 10, 100);

        saveUnit(1L, product, supply, shelf);
        saveUnit(2L, product, supply, shelf);

        List<ProductUnits> found = suppliesDAO.getProductUnitsForSupply(supply);
        assertEquals(2, found.size());
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
        product.setProduct_size(SizeType.large);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(90));
        productsDAO.save(product);
        return product;
    }

    private Providers saveProvider(Long id, String name) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName(name);
        provider.setPhoneNum("100");
        provider.setEmail("p" + id + "@test");
        providersDAO.save(provider);
        return provider;
    }

    private Supplies saveSupply(Long id, Products product, Providers provider,
                                BigDecimal amount, LocalDateTime time, boolean completed) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(amount);
        supply.setTime(time);
        supply.setCompleted(completed);
        suppliesDAO.save(supply);
        return supply;
    }

    private ShelfsWorkload saveShelf(Long id, Integer roomNum, Integer workload) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(roomNum);
        shelf.setWorkloadCount(workload);
        shelfsWorkloadDAO.save(shelf);
        return shelf;
    }

    private ProductUnits saveUnit(Long id, Products product, Supplies supply, ShelfsWorkload shelf) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setSupply(supply);
        unit.setShelf(shelf);
        unit.setArrival(LocalDateTime.of(2026, 1, 2, 10, 0));
        unit.setAmount(BigDecimal.valueOf(2));
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