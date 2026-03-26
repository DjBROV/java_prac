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
public class ProductUnitsDAOTest {

    @Autowired
    private ProductUnitsDAO productUnitsDAO;
    @Autowired
    private ProductsDAO productsDAO;
    @Autowired
    private ProductCategoriesDAO productCategoriesDAO;
    @Autowired
    private SuppliesDAO suppliesDAO;
    @Autowired
    private ProvidersDAO providersDAO;
    @Autowired
    private ShelfsWorkloadDAO shelfsWorkloadDAO;
    @Autowired
    private OrdersDAO ordersDAO;
    @Autowired
    private ConsumersDAO consumersDAO;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void testGetByProductId() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 1, 10, 0), BigDecimal.valueOf(2));

        List<ProductUnits> found = productUnitsDAO.getByProductId(f.product.getId());
        assertEquals(1, found.size());
    }

    @Test
    void testGetByShelfNum() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 1, 10, 0), BigDecimal.valueOf(2));

        List<ProductUnits> found = productUnitsDAO.getByShelfNum(f.shelf.getId());
        assertEquals(1, found.size());
    }

    @Test
    void testGetBySupplyId() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 1, 10, 0), BigDecimal.valueOf(2));

        List<ProductUnits> found = productUnitsDAO.getBySupplyId(f.supply.getId());
        assertEquals(1, found.size());
    }

    @Test
    void testGetByOrderId() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, f.order, LocalDateTime.of(2026, 1, 1, 10, 0), BigDecimal.valueOf(2));

        List<ProductUnits> found = productUnitsDAO.getByOrderId(f.order.getId());
        assertEquals(1, found.size());
    }

    @Test
    void testGetByArrivalRange() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(2));
        saveUnit(2L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 2, 10, 10, 0), BigDecimal.valueOf(2));

        List<ProductUnits> found = productUnitsDAO.getByArrivalRange(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 31, 23, 59)
        );
        assertEquals(1, found.size());
    }

    @Test
    void testGetByFilter() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));

        ProductUnitsDAO.Filter filter = ProductUnitsDAO.getFilterBuilder()
                .productId(f.product.getId())
                .supplyId(f.supply.getId())
                .supplierId(f.provider.getId())
                .shelfNum(f.shelf.getId())
                .roomNum(f.shelf.getRoomNum())
                .minAmount(1)
                .maxAmount(10)
                .arrivalFrom(LocalDateTime.of(2026, 1, 1, 0, 0))
                .arrivalTo(LocalDateTime.of(2026, 1, 31, 23, 59))
                .reserved(false)
                .build();

        List<ProductUnits> found = productUnitsDAO.getByFilter(filter);
        assertEquals(1, found.size());
    }

    @Test
    void testGetProduct() {
        Fixture f = prepareFixture();
        ProductUnits unit = saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));

        Products found = productUnitsDAO.getProduct(unit);
        assertNotNull(found);
        assertEquals(f.product.getId(), found.getId());
    }

    @Test
    void testGetShelf() {
        Fixture f = prepareFixture();
        ProductUnits unit = saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));

        ShelfsWorkload found = productUnitsDAO.getShelf(unit);
        assertNotNull(found);
        assertEquals(f.shelf.getId(), found.getId());
    }

    @Test
    void testGetSupply() {
        Fixture f = prepareFixture();
        ProductUnits unit = saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));

        Supplies found = productUnitsDAO.getSupply(unit);
        assertNotNull(found);
        assertEquals(f.supply.getId(), found.getId());
    }

    @Test
    void testGetOrder() {
        Fixture f = prepareFixture();
        ProductUnits unit = saveUnit(1L, f.product, f.supply, f.shelf, f.order, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));

        Orders found = productUnitsDAO.getOrder(unit);
        assertNotNull(found);
        assertEquals(f.order.getId(), found.getId());
    }

    @Test
    void testGetFreeUnits() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));
        saveUnit(2L, f.product, f.supply, f.shelf, f.order, LocalDateTime.of(2026, 1, 11, 10, 0), BigDecimal.valueOf(5));

        List<ProductUnits> found = productUnitsDAO.getFreeUnits();
        assertEquals(1, found.size());
        assertNull(found.getFirst().getOrder());
    }

    @Test
    void testGetReservedUnits() {
        Fixture f = prepareFixture();
        saveUnit(1L, f.product, f.supply, f.shelf, null, LocalDateTime.of(2026, 1, 10, 10, 0), BigDecimal.valueOf(5));
        saveUnit(2L, f.product, f.supply, f.shelf, f.order, LocalDateTime.of(2026, 1, 11, 10, 0), BigDecimal.valueOf(5));

        List<ProductUnits> found = productUnitsDAO.getReservedUnits();
        assertEquals(1, found.size());
        assertNotNull(found.getFirst().getOrder());
    }

    private Fixture prepareFixture() {
        ProductCategories category = new ProductCategories();
        category.setId(1L);
        category.setName("Техника");
        productCategoriesDAO.save(category);

        Products product = new Products();
        product.setId(1L);
        product.setCategory(category);
        product.setName("Ноутбук");
        product.setUnit(UnitsType.kg);
        product.setProduct_size(SizeType.large);
        product.setUnitsForOne(1);
        product.setStorageLife(Duration.ofDays(90));
        productsDAO.save(product);

        Providers provider = new Providers();
        provider.setId(1L);
        provider.setName("Поставщик");
        provider.setPhoneNum("123");
        provider.setEmail("p@test");
        providersDAO.save(provider);

        Supplies supply = new Supplies();
        supply.setId(1L);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(BigDecimal.valueOf(10));
        supply.setTime(LocalDateTime.of(2026, 1, 1, 9, 0));
        suppliesDAO.save(supply);

        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(1L);
        shelf.setRoomNum(100);
        shelf.setWorkloadCount(50);
        shelfsWorkloadDAO.save(shelf);

        Consumers consumer = new Consumers();
        consumer.setId(1L);
        consumer.setName("Клиент");
        consumer.setPhoneNum("555");
        consumer.setEmail("c@test");
        consumersDAO.save(consumer);

        Orders order = new Orders();
        order.setId(1L);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(BigDecimal.valueOf(3));
        order.setTime(LocalDateTime.of(2026, 1, 1, 12, 0));
        ordersDAO.save(order);

        return new Fixture(product, provider, supply, shelf, order);
    }

    private ProductUnits saveUnit(Long id, Products product, Supplies supply, ShelfsWorkload shelf,
                                  Orders order, LocalDateTime arrival, BigDecimal amount) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setSupply(supply);
        unit.setShelf(shelf);
        unit.setOrder(order);
        unit.setArrival(arrival);
        unit.setAmount(amount);
        productUnitsDAO.save(unit);
        return unit;
    }

    private record Fixture(Products product, Providers provider, Supplies supply, ShelfsWorkload shelf, Orders order) {}

    @BeforeAll
    @AfterEach
    void annihilation() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.createNativeQuery("TRUNCATE TABLE product_units CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE orders CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE supplies CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE consumers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE providers CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE shelfs_workload CASCADE").executeUpdate();
            session.createNativeQuery("TRUNCATE TABLE product_categories CASCADE").executeUpdate();
            session.getTransaction().commit();
        }
    }
}