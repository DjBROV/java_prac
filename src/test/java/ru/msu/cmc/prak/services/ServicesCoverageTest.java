package ru.msu.cmc.prak.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.msu.cmc.prak.DAO.OrdersDAO;
import ru.msu.cmc.prak.DAO.ProductUnitsDAO;
import ru.msu.cmc.prak.DAO.ShelfsWorkloadDAO;
import ru.msu.cmc.prak.DAO.SuppliesDAO;
import ru.msu.cmc.prak.controllers.exceptions.BusinessRuleException;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServicesCoverageTest {

    private OrdersDAO ordersDAO;
    private ProductUnitsDAO productUnitsDAO;
    private SuppliesDAO suppliesDAO;
    private ShelfsWorkloadDAO shelfsWorkloadDAO;

    private OrdersService ordersService;
    private SuppliesService suppliesService;

    @BeforeEach
    void setUp() {
        ordersDAO = mock(OrdersDAO.class);
        productUnitsDAO = mock(ProductUnitsDAO.class);
        suppliesDAO = mock(SuppliesDAO.class);
        shelfsWorkloadDAO = mock(ShelfsWorkloadDAO.class);

        ordersService = new OrdersService(ordersDAO, productUnitsDAO);
        suppliesService = new SuppliesService(suppliesDAO, productUnitsDAO, shelfsWorkloadDAO);
    }

    private ProductCategories category(Long id) {
        ProductCategories category = new ProductCategories();
        category.setId(id);
        category.setName("Категория " + id);
        return category;
    }

    private Products product(Long id, SizeType size) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category(10L));
        product.setName("Товар " + id);
        product.setProduct_size(size);
        product.setUnit(UnitsType.kg);
        product.setUnitsForOne(1);
        return product;
    }

    private Products productWithoutSize(Long id) {
        Products product = new Products();
        product.setId(id);
        product.setCategory(category(10L));
        product.setName("Товар без размера " + id);
        product.setUnit(UnitsType.kg);
        product.setUnitsForOne(1);
        return product;
    }

    private Consumers consumer(Long id) {
        Consumers consumer = new Consumers();
        consumer.setId(id);
        consumer.setName("Покупатель " + id);
        consumer.setPhoneNum("70000000" + id);
        consumer.setEmail("consumer" + id + "@test.local");
        return consumer;
    }

    private Providers provider(Long id) {
        Providers provider = new Providers();
        provider.setId(id);
        provider.setName("Поставщик " + id);
        provider.setPhoneNum("80000000" + id);
        provider.setEmail("provider" + id + "@test.local");
        return provider;
    }

    private ShelfsWorkload shelf(Long id, int workload) {
        ShelfsWorkload shelf = new ShelfsWorkload();
        shelf.setId(id);
        shelf.setRoomNum(1);
        shelf.setWorkloadCount(workload);
        return shelf;
    }

    private Supplies supply(Long id,
                            Products product,
                            Providers provider,
                            BigDecimal amount,
                            boolean completed) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(amount);
        supply.setTime(LocalDateTime.of(2026, 4, 27, 10, 0));
        supply.setCompleted(completed);
        return supply;
    }

    private Supplies supplyWithoutTime(Long id,
                                       Products product,
                                       Providers provider,
                                       BigDecimal amount,
                                       boolean completed) {
        Supplies supply = new Supplies();
        supply.setId(id);
        supply.setProduct(product);
        supply.setProvider(provider);
        supply.setAmount(amount);
        // time намеренно не задаем: так покрывается ветка createUnitForSupply,
        // где arrival берется как LocalDateTime.now().
        supply.setCompleted(completed);
        return supply;
    }

    private Orders order(Long id,
                         Products product,
                         Consumers consumer,
                         BigDecimal amount,
                         boolean completed) {
        Orders order = new Orders();
        order.setId(id);
        order.setProduct(product);
        order.setConsumer(consumer);
        order.setAmount(amount);
        order.setTime(LocalDateTime.of(2026, 4, 27, 11, 0));
        order.setCompleted(completed);
        return order;
    }

    private ProductUnits unit(Long id,
                              Products product,
                              BigDecimal amount,
                              ShelfsWorkload shelf,
                              Supplies supply,
                              Orders order) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);
        unit.setProduct(product);
        unit.setAmount(amount);
        unit.setArrival(LocalDateTime.of(2026, 4, 27, 10, 0));
        unit.setShelf(shelf);
        unit.setSupply(supply);
        unit.setOrder(order);
        return unit;
    }

    private ProductUnits unitWithNullableRelations(Long id,
                                                   Products product,
                                                   BigDecimal amount,
                                                   ShelfsWorkload shelf,
                                                   Supplies supply,
                                                   Orders order) {
        ProductUnits unit = new ProductUnits();
        unit.setId(id);

        // В моделях product/shelf/supply/amount помечены @NonNull.
        // Чтобы проверить защитные ветки service-кода для null-связей,
        // используем no-args constructor и просто не вызываем соответствующие setter'ы.
        if (product != null) {
            unit.setProduct(product);
        }
        if (amount != null) {
            unit.setAmount(amount);
        }
        unit.setArrival(LocalDateTime.of(2026, 4, 27, 10, 0));
        if (shelf != null) {
            unit.setShelf(shelf);
        }
        if (supply != null) {
            unit.setSupply(supply);
        }
        unit.setOrder(order);
        return unit;
    }

    @Test
    void buildOrderShouldGenerateIdParseAmountAndTime() {
        Orders oldOrder = order(
                1001L,
                product(1L, SizeType.small),
                consumer(1L),
                BigDecimal.ONE,
                false
        );
        when(ordersDAO.getAll()).thenReturn(List.of(oldOrder));

        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);

        Orders result = ordersService.buildOrder(
                null,
                consumer,
                product,
                7,
                "2026-04-27T11:30"
        );

        assertEquals(1002L, result.getId());
        assertSame(consumer, result.getConsumer());
        assertSame(product, result.getProduct());
        assertEquals(0, BigDecimal.valueOf(7).compareTo(result.getAmount()));
        assertEquals(LocalDateTime.of(2026, 4, 27, 11, 30), result.getTime());
    }

    @Test
    void buildOrderShouldUseProvidedIdAndParseTime() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);

        Orders result = ordersService.buildOrder(
                55L,
                consumer,
                product,
                3,
                "2026-04-27T12:00"
        );

        assertEquals(55L, result.getId());
        assertSame(consumer, result.getConsumer());
        assertSame(product, result.getProduct());
        assertEquals(0, BigDecimal.valueOf(3).compareTo(result.getAmount()));
        assertEquals(LocalDateTime.of(2026, 4, 27, 12, 0), result.getTime());
        verify(ordersDAO, never()).getAll();
    }

    @Test
    void buildOrderWithNullAmountShouldThrowBecauseModelAmountIsNonNull() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);

        assertThrows(NullPointerException.class, () ->
                ordersService.buildOrder(77L, consumer, product, null, "2026-04-27T11:30")
        );
    }

    @Test
    void getAvailableAmountShouldReturnOnlyFreeAmountWhenExistingOrderIsNullOrCompleted() {
        ProductUnits freeFirst = unit(
                1L,
                product(1000L, SizeType.small),
                BigDecimal.valueOf(5),
                shelf(1L, 0),
                supply(1L, product(1000L, SizeType.small), provider(1L), BigDecimal.TEN, false),
                null
        );
        ProductUnits freeSecond = unit(
                2L,
                product(1000L, SizeType.small),
                BigDecimal.valueOf(7),
                shelf(1L, 0),
                supply(2L, product(1000L, SizeType.small), provider(1L), BigDecimal.TEN, false),
                null
        );

        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of(freeFirst, freeSecond));

        assertEquals(
                0,
                BigDecimal.valueOf(12).compareTo(
                        ordersService.getAvailableAmountForOrder(null, 1000L)
                )
        );

        Orders completedOrder = order(
                10L,
                product(1000L, SizeType.small),
                consumer(100L),
                BigDecimal.ONE,
                true
        );

        assertEquals(
                0,
                BigDecimal.valueOf(12).compareTo(
                        ordersService.getAvailableAmountForOrder(completedOrder, 1000L)
                )
        );

        verify(ordersDAO, never()).getProductUnitsForOrder(any());
    }

    @Test
    void getAvailableAmountShouldAddReservedAmountForEditedDraftOrder() {
        Products product = product(1000L, SizeType.small);
        Products otherProduct = product(1001L, SizeType.small);
        Consumers consumer = consumer(100L);
        Orders existingOrder = order(50L, product, consumer, BigDecimal.valueOf(4), false);

        ProductUnits freeUnit = unit(
                1L,
                product,
                BigDecimal.ONE,
                shelf(1L, 0),
                supply(1L, product, provider(1L), BigDecimal.TEN, false),
                null
        );

        ProductUnits reservedWithoutProduct = unitWithNullableRelations(
                2L,
                null,
                BigDecimal.valueOf(100),
                shelf(1L, 0),
                supply(2L, product, provider(1L), BigDecimal.TEN, false),
                existingOrder
        );
        ProductUnits reservedOtherProduct = unit(
                3L,
                otherProduct,
                BigDecimal.valueOf(8),
                shelf(1L, 0),
                supply(3L, otherProduct, provider(1L), BigDecimal.TEN, false),
                existingOrder
        );
        ProductUnits reservedSameProduct = unit(
                4L,
                product,
                BigDecimal.valueOf(4),
                shelf(1L, 0),
                supply(4L, product, provider(1L), BigDecimal.TEN, false),
                existingOrder
        );

        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of(freeUnit));
        when(ordersDAO.getProductUnitsForOrder(existingOrder))
                .thenReturn(List.of(
                        reservedWithoutProduct,
                        reservedOtherProduct,
                        reservedSameProduct
                ));

        BigDecimal available = ordersService.getAvailableAmountForOrder(existingOrder, 1000L);

        assertEquals(0, BigDecimal.valueOf(5).compareTo(available));
    }

    @Test
    void createOrderShouldReserveWholeUnitsWhenEachUnitFitsIntoRemainingAmount() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Orders order = order(1L, product, consumer, BigDecimal.valueOf(8), true);

        ShelfsWorkload shelf = shelf(1L, 0);
        Providers provider = provider(200L);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);

        ProductUnits first = unit(10L, product, BigDecimal.valueOf(5), shelf, supply, null);
        ProductUnits second = unit(11L, product, BigDecimal.valueOf(3), shelf, supply, null);

        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of(first, second));
        when(productUnitsDAO.getAll()).thenReturn(List.of(first, second));

        ordersService.createOrder(order, 1000L, BigDecimal.valueOf(8));

        assertFalse(order.isCompleted());
        assertSame(order, first.getOrder());
        assertSame(order, second.getOrder());

        verify(ordersDAO).save(order);
        verify(productUnitsDAO).update(first);
        verify(productUnitsDAO).update(second);
        verify(productUnitsDAO, never()).save(any(ProductUnits.class));
    }

    @Test
    void createOrderShouldSplitBigUnitAndBreakWhenRequiredAmountIsReserved() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Orders order = order(1L, product, consumer, BigDecimal.valueOf(3), false);

        ShelfsWorkload shelf = shelf(1L, 0);
        Providers provider = provider(200L);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);

        ProductUnits bigUnit = unit(10000L, product, BigDecimal.TEN, shelf, supply, null);
        ProductUnits unusedUnit = unit(10001L, product, BigDecimal.valueOf(5), shelf, supply, null);

        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of(bigUnit, unusedUnit));
        when(productUnitsDAO.getAll()).thenReturn(List.of(bigUnit, unusedUnit));

        ordersService.createOrder(order, 1000L, BigDecimal.valueOf(3));

        ArgumentCaptor<ProductUnits> savedUnitCaptor = ArgumentCaptor.forClass(ProductUnits.class);
        verify(productUnitsDAO).save(savedUnitCaptor.capture());

        ProductUnits reservedPart = savedUnitCaptor.getValue();
        assertEquals(10002L, reservedPart.getId());
        assertSame(product, reservedPart.getProduct());
        assertSame(shelf, reservedPart.getShelf());
        assertSame(supply, reservedPart.getSupply());
        assertSame(order, reservedPart.getOrder());
        assertEquals(0, BigDecimal.valueOf(3).compareTo(reservedPart.getAmount()));

        assertEquals(0, BigDecimal.valueOf(7).compareTo(bigUnit.getAmount()));
        assertNull(unusedUnit.getOrder());

        verify(productUnitsDAO).update(bigUnit);
        verify(productUnitsDAO, never()).update(unusedUnit);
    }

    @Test
    void updateOrderShouldReleaseBrokenReservedUnitAndUpdateOrder() {
        Products product = product(1000L, SizeType.small);
        Orders existingOrder = order(1L, product, consumer(100L), BigDecimal.ONE, false);
        Orders newOrder = order(1L, product, consumer(100L), BigDecimal.valueOf(2), true);

        ProductUnits brokenReservedUnit = unitWithNullableRelations(
                10L,
                null,
                BigDecimal.ONE,
                null,
                null,
                existingOrder
        );

        when(ordersDAO.getProductUnitsForOrder(existingOrder))
                .thenReturn(List.of(brokenReservedUnit));
        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of());
        when(productUnitsDAO.getAll()).thenReturn(List.of());

        ordersService.updateOrder(existingOrder, newOrder, 1000L, BigDecimal.ZERO);

        assertNull(brokenReservedUnit.getOrder());
        assertFalse(newOrder.isCompleted());

        verify(productUnitsDAO).update(brokenReservedUnit);
        verify(ordersDAO).update(newOrder);
    }

    @Test
    void updateOrderShouldMergeReleasedReservationWithMatchingFreeUnit() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Orders existingOrder = order(1L, product, consumer, BigDecimal.valueOf(4), false);
        Orders newOrder = order(1L, product, consumer, BigDecimal.valueOf(2), false);

        ShelfsWorkload shelf = shelf(1L, 0);
        Providers provider = provider(200L);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);

        ProductUnits reserved = unit(10L, product, BigDecimal.valueOf(4), shelf, supply, existingOrder);
        ProductUnits matchingFree = unit(11L, product, BigDecimal.valueOf(6), shelf, supply, null);

        when(ordersDAO.getProductUnitsForOrder(existingOrder)).thenReturn(List.of(reserved));
        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class)))
                .thenReturn(List.of(matchingFree), List.of());
        when(productUnitsDAO.getAll()).thenReturn(List.of());

        ordersService.updateOrder(existingOrder, newOrder, 1000L, BigDecimal.ZERO);

        assertEquals(0, BigDecimal.TEN.compareTo(matchingFree.getAmount()));
        verify(productUnitsDAO).update(matchingFree);
        verify(productUnitsDAO).deleteById(10L);
        verify(ordersDAO).update(newOrder);
    }

    @Test
    void deleteDraftOrderShouldReleaseReservationWithoutMatchingFreeUnit() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Orders order = order(1L, product, consumer, BigDecimal.valueOf(4), false);

        ShelfsWorkload shelf = shelf(1L, 0);
        Providers provider = provider(200L);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);

        ProductUnits reserved = unit(10L, product, BigDecimal.valueOf(4), shelf, supply, order);

        when(ordersDAO.getProductUnitsForOrder(order)).thenReturn(List.of(reserved));
        when(productUnitsDAO.getByFilter(any(ProductUnitsDAO.Filter.class))).thenReturn(List.of());

        ordersService.deleteOrder(order);

        assertNull(reserved.getOrder());
        verify(productUnitsDAO).update(reserved);
        verify(productUnitsDAO, never()).deleteById(10L);
        verify(ordersDAO).deleteById(1L);
    }


    @Test
    void deleteDraftOrderShouldReleaseReservationWhenShelfIsMissingButSupplyExists() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Providers provider = provider(200L);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);
        Orders order = order(1L, product, consumer, BigDecimal.ONE, false);

        ProductUnits reservedWithoutShelf = unitWithNullableRelations(
                10L,
                product,
                BigDecimal.ONE,
                null,
                supply,
                order
        );

        when(ordersDAO.getProductUnitsForOrder(order))
                .thenReturn(List.of(reservedWithoutShelf));

        ordersService.deleteOrder(order);

        assertNull(reservedWithoutShelf.getOrder());
        verify(productUnitsDAO).update(reservedWithoutShelf);
        verify(productUnitsDAO, never()).getByFilter(any(ProductUnitsDAO.Filter.class));
        verify(productUnitsDAO, never()).deleteById(10L);
        verify(ordersDAO).deleteById(1L);
    }

    @Test
    void deleteDraftOrderShouldReleaseReservationWhenProductIsMissingButSupplyAndShelfExist() {
        Products product = product(1000L, SizeType.small);
        Consumers consumer = consumer(100L);
        Providers provider = provider(200L);
        ShelfsWorkload shelf = shelf(1L, 0);
        Supplies supply = supply(300L, product, provider, BigDecimal.TEN, false);
        Orders order = order(1L, product, consumer, BigDecimal.ONE, false);

        ProductUnits reservedWithoutProduct = unitWithNullableRelations(
                11L,
                null,
                BigDecimal.ONE,
                shelf,
                supply,
                order
        );

        when(ordersDAO.getProductUnitsForOrder(order))
                .thenReturn(List.of(reservedWithoutProduct));

        ordersService.deleteOrder(order);

        assertNull(reservedWithoutProduct.getOrder());
        verify(productUnitsDAO).update(reservedWithoutProduct);
        verify(productUnitsDAO, never()).getByFilter(any(ProductUnitsDAO.Filter.class));
        verify(productUnitsDAO, never()).deleteById(11L);
        verify(ordersDAO).deleteById(1L);
    }

    @Test
    void deleteCompletedOrderShouldNotReleaseReservation() {
        Orders completedOrder = order(
                1L,
                product(1000L, SizeType.small),
                consumer(100L),
                BigDecimal.ONE,
                true
        );

        ordersService.deleteOrder(completedOrder);

        verify(ordersDAO, never()).getProductUnitsForOrder(any());
        verify(productUnitsDAO, never()).update(any(ProductUnits.class));
        verify(ordersDAO).deleteById(1L);
    }

    @Test
    void completeOrderShouldMarkOrderCompletedAndDeleteReservedUnits() {
        Orders order = order(
                1L,
                product(1000L, SizeType.small),
                consumer(100L),
                BigDecimal.ONE,
                false
        );
        ProductUnits first = unitWithNullableRelations(10L, null, BigDecimal.ONE, null, null, order);
        ProductUnits second = unitWithNullableRelations(11L, null, BigDecimal.ONE, null, null, order);

        when(ordersDAO.getProductUnitsForOrder(order)).thenReturn(List.of(first, second));

        ordersService.completeOrder(order);

        assertTrue(order.isCompleted());
        verify(ordersDAO).update(order);
        verify(productUnitsDAO).deleteById(10L);
        verify(productUnitsDAO).deleteById(11L);
    }

    @Test
    void buildSupplyShouldGenerateIdParseAmountAndTime() {
        Products product = product(1000L, SizeType.small);
        Providers provider = provider(200L);
        Supplies oldSupply = supply(1001L, product, provider, BigDecimal.ONE, false);

        when(suppliesDAO.getAll()).thenReturn(List.of(oldSupply));

        Supplies result = suppliesService.buildSupply(
                null,
                provider,
                product,
                9,
                "2026-04-27T10:30"
        );

        assertEquals(1002L, result.getId());
        assertSame(provider, result.getProvider());
        assertSame(product, result.getProduct());
        assertEquals(0, BigDecimal.valueOf(9).compareTo(result.getAmount()));
        assertEquals(LocalDateTime.of(2026, 4, 27, 10, 30), result.getTime());
    }

    @Test
    void buildSupplyShouldUseProvidedIdAndParseTime() {
        Products product = product(1000L, SizeType.small);
        Providers provider = provider(200L);

        Supplies result = suppliesService.buildSupply(
                77L,
                provider,
                product,
                5,
                "2026-04-27T12:00"
        );

        assertEquals(77L, result.getId());
        assertSame(provider, result.getProvider());
        assertSame(product, result.getProduct());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(result.getAmount()));
        assertEquals(LocalDateTime.of(2026, 4, 27, 12, 0), result.getTime());
        verify(suppliesDAO, never()).getAll();
    }

    @Test
    void buildSupplyWithNullAmountShouldThrowBecauseModelAmountIsNonNull() {
        assertThrows(NullPointerException.class, () ->
                suppliesService.buildSupply(
                        77L,
                        provider(200L),
                        product(1000L, SizeType.small),
                        null,
                        "2026-04-27T10:30"
                )
        );
    }

    @Test
    void calcRequiredWorkloadShouldCoverAllProductSizeWeights() {
        assertEquals(6, suppliesService.calcRequiredWorkload(null, 3));
        assertEquals(6, suppliesService.calcRequiredWorkload(productWithoutSize(1L), 3));
        assertEquals(6, suppliesService.calcRequiredWorkload(product(2L, SizeType.small), 3));
        assertEquals(30, suppliesService.calcRequiredWorkload(product(3L, SizeType.middle), 3));
        assertEquals(150, suppliesService.calcRequiredWorkload(product(4L, SizeType.large), 3));
    }

    @Test
    void findShelfForSupplyShouldReturnFirstShelfWithEnoughFreeSpace() {
        ShelfsWorkload tooFullShelf = shelf(1L, 490);
        ShelfsWorkload suitableShelf = shelf(2L, 450);

        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(tooFullShelf, suitableShelf));

        ShelfsWorkload result = suppliesService.findShelfForSupply(null, 50);

        assertSame(suitableShelf, result);
    }

    @Test
    void findShelfForSupplyShouldReturnNullWhenNoShelfHasEnoughSpace() {
        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(
                shelf(1L, 490),
                shelf(2L, 480)
        ));

        ShelfsWorkload result = suppliesService.findShelfForSupply(null, 50);

        assertNull(result);
    }

    @Test
    void findShelfForSupplyShouldAccountForWorkloadReleasedByEditedSupplyAndSkipBrokenUnits() {
        Products smallProduct = product(1000L, SizeType.small);
        Providers provider = provider(200L);
        ShelfsWorkload fullShelf = shelf(1L, 500);

        Supplies existingSupply = supply(
                3000L,
                smallProduct,
                provider,
                BigDecimal.TEN,
                false
        );

        ProductUnits withoutShelf = unitWithNullableRelations(
                1L,
                smallProduct,
                BigDecimal.TEN,
                null,
                existingSupply,
                null
        );
        ProductUnits withoutProduct = unitWithNullableRelations(
                2L,
                null,
                BigDecimal.TEN,
                fullShelf,
                existingSupply,
                null
        );
        ProductUnits withoutAmount = unitWithNullableRelations(
                3L,
                smallProduct,
                null,
                fullShelf,
                existingSupply,
                null
        );
        ProductUnits valid = unit(
                4L,
                smallProduct,
                BigDecimal.TEN,
                fullShelf,
                existingSupply,
                null
        );

        when(suppliesDAO.getProductUnitsForSupply(existingSupply))
                .thenReturn(List.of(withoutShelf, withoutProduct, withoutAmount, valid));
        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(fullShelf));

        ShelfsWorkload result = suppliesService.findShelfForSupply(existingSupply, 20);

        assertSame(fullShelf, result);
    }

    @Test
    void calcTotalFreeWorkloadShouldIgnoreCompletedExistingSupply() {
        Supplies completedSupply = supply(
                3000L,
                product(1000L, SizeType.small),
                provider(200L),
                BigDecimal.TEN,
                true
        );

        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(
                shelf(1L, 100),
                shelf(2L, 250)
        ));

        int totalFree = suppliesService.calcTotalFreeWorkload(completedSupply);

        assertEquals(650, totalFree);
        verify(suppliesDAO, never()).getProductUnitsForSupply(completedSupply);
    }

    @Test
    void calcTotalFreeWorkloadShouldIncludeWorkloadReleasedByEditedSupply() {
        Products middleProduct = product(1000L, SizeType.middle);
        Providers provider = provider(200L);
        ShelfsWorkload shelf = shelf(1L, 500);

        Supplies existingSupply = supply(
                3000L,
                middleProduct,
                provider,
                BigDecimal.valueOf(2),
                false
        );
        ProductUnits valid = unit(
                4L,
                middleProduct,
                BigDecimal.valueOf(2),
                shelf,
                existingSupply,
                null
        );

        when(suppliesDAO.getProductUnitsForSupply(existingSupply)).thenReturn(List.of(valid));
        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(shelf));

        int totalFree = suppliesService.calcTotalFreeWorkload(existingSupply);

        assertEquals(20, totalFree);
    }

    @Test
    void createSupplyShouldReserveSpaceSaveSupplyAndCreateUnitWithCurrentTimeWhenSupplyTimeIsNull() {
        Products product = product(1000L, SizeType.small);
        Providers provider = provider(200L);
        Supplies supply = supplyWithoutTime(3000L, product, provider, BigDecimal.valueOf(5), false);
        ShelfsWorkload shelf = shelf(1L, 100);

        when(productUnitsDAO.getAll()).thenReturn(List.of());

        suppliesService.createSupply(supply, 5, shelf, 10);

        verify(shelfsWorkloadDAO).updateWorkload(shelf, 110);
        verify(suppliesDAO).save(supply);

        ArgumentCaptor<ProductUnits> unitCaptor = ArgumentCaptor.forClass(ProductUnits.class);
        verify(productUnitsDAO).save(unitCaptor.capture());

        ProductUnits createdUnit = unitCaptor.getValue();
        assertEquals(10001L, createdUnit.getId());
        assertSame(product, createdUnit.getProduct());
        assertSame(shelf, createdUnit.getShelf());
        assertSame(supply, createdUnit.getSupply());
        assertNull(createdUnit.getOrder());
        assertEquals(0, BigDecimal.valueOf(5).compareTo(createdUnit.getAmount()));
        assertNotNull(createdUnit.getArrival());
    }

    @Test
    void createSupplyWithNullShelfShouldThrowBusinessRuleException() {
        Supplies supply = supply(
                3000L,
                product(1000L, SizeType.small),
                provider(200L),
                BigDecimal.valueOf(5),
                false
        );

        BusinessRuleException exception = assertThrows(
                BusinessRuleException.class,
                () -> suppliesService.createSupply(supply, 5, null, 10)
        );

        assertEquals("Не удалось выбрать полку для поставки", exception.getMessage());
        verifyNoInteractions(suppliesDAO);
        verifyNoInteractions(productUnitsDAO);
    }

    @Test
    void updateSupplyShouldReleaseOldUnitsReserveNewSpaceUpdateSupplyAndCreateNewUnit() {
        Products product = product(1000L, SizeType.small);
        Providers provider = provider(200L);
        ShelfsWorkload oldShelf = shelf(1L, 10);
        ShelfsWorkload selectedShelf = shelf(2L, 100);
        ShelfsWorkload refreshedShelf = shelf(2L, 100);

        Supplies existingSupply = supply(
                3000L,
                product,
                provider,
                BigDecimal.valueOf(5),
                false
        );
        Supplies newSupply = supply(
                3000L,
                product,
                provider,
                BigDecimal.valueOf(7),
                false
        );
        newSupply.setTime(LocalDateTime.of(2026, 4, 27, 12, 30));

        ProductUnits validOldUnit = unit(
                10L,
                product,
                BigDecimal.valueOf(5),
                oldShelf,
                existingSupply,
                null
        );
        ProductUnits withoutShelf = unitWithNullableRelations(
                11L,
                product,
                BigDecimal.ONE,
                null,
                existingSupply,
                null
        );
        ProductUnits withoutProduct = unitWithNullableRelations(
                12L,
                null,
                BigDecimal.ONE,
                oldShelf,
                existingSupply,
                null
        );
        ProductUnits withoutAmount = unitWithNullableRelations(
                13L,
                product,
                null,
                oldShelf,
                existingSupply,
                null
        );

        when(suppliesDAO.getProductUnitsForSupply(existingSupply))
                .thenReturn(List.of(validOldUnit, withoutShelf, withoutProduct, withoutAmount));
        when(shelfsWorkloadDAO.getById(2L)).thenReturn(refreshedShelf);
        when(productUnitsDAO.getAll()).thenReturn(List.of());

        suppliesService.updateSupply(existingSupply, newSupply, 7, selectedShelf, 14);

        verify(shelfsWorkloadDAO).updateWorkload(oldShelf, 0);
        verify(productUnitsDAO).deleteById(10L);
        verify(productUnitsDAO).deleteById(11L);
        verify(productUnitsDAO).deleteById(12L);
        verify(productUnitsDAO).deleteById(13L);

        verify(shelfsWorkloadDAO).updateWorkload(refreshedShelf, 114);
        verify(suppliesDAO).update(newSupply);

        ArgumentCaptor<ProductUnits> createdUnitCaptor = ArgumentCaptor.forClass(ProductUnits.class);
        verify(productUnitsDAO).save(createdUnitCaptor.capture());

        ProductUnits createdUnit = createdUnitCaptor.getValue();
        assertEquals(10001L, createdUnit.getId());
        assertSame(product, createdUnit.getProduct());
        assertSame(refreshedShelf, createdUnit.getShelf());
        assertSame(newSupply, createdUnit.getSupply());
        assertEquals(LocalDateTime.of(2026, 4, 27, 12, 30), createdUnit.getArrival());
        assertEquals(0, BigDecimal.valueOf(7).compareTo(createdUnit.getAmount()));
    }

    @Test
    void completeSupplyShouldMarkSupplyCompletedAndUpdateIt() {
        Supplies supply = supply(
                3000L,
                product(1000L, SizeType.small),
                provider(200L),
                BigDecimal.ONE,
                false
        );

        suppliesService.completeSupply(supply);

        assertTrue(supply.isCompleted());
        verify(suppliesDAO).update(supply);
    }

    @Test
    void deleteSupplyShouldReleaseReservedSpaceAndDeleteSupply() {
        Products product = product(1000L, SizeType.small);
        Providers provider = provider(200L);
        ShelfsWorkload shelf = shelf(1L, 100);
        Supplies supply = supply(3000L, product, provider, BigDecimal.TEN, false);
        ProductUnits unit = unit(10L, product, BigDecimal.TEN, shelf, supply, null);

        when(suppliesDAO.getProductUnitsForSupply(supply)).thenReturn(List.of(unit));

        suppliesService.deleteSupply(supply);

        verify(shelfsWorkloadDAO).updateWorkload(shelf, 80);
        verify(productUnitsDAO).deleteById(10L);
        verify(suppliesDAO).deleteById(3000L);
    }

    @Test
    void completeOrderShouldHandleOrderWithoutReservedUnits() {
        Orders order = order(
                2L,
                product(1000L, SizeType.small),
                consumer(100L),
                BigDecimal.ONE,
                false
        );

        when(ordersDAO.getProductUnitsForOrder(order)).thenReturn(List.of());

        ordersService.completeOrder(order);

        assertTrue(order.isCompleted());
        verify(ordersDAO).update(order);
        verify(productUnitsDAO, never()).deleteById(any());
    }

    @Test
    void deleteDraftOrderWithoutReservedUnitsShouldOnlyDeleteOrder() {
        Orders order = order(
                2L,
                product(1000L, SizeType.small),
                consumer(100L),
                BigDecimal.ONE,
                false
        );

        when(ordersDAO.getProductUnitsForOrder(order)).thenReturn(List.of());

        ordersService.deleteOrder(order);

        verify(productUnitsDAO, never()).update(any(ProductUnits.class));
        verify(productUnitsDAO, never()).deleteById(any());
        verify(ordersDAO).deleteById(2L);
    }

    @Test
    void calcTotalFreeWorkloadShouldHandleNonCompletedSupplyWithoutUnits() {
        Supplies existingSupply = supply(
                4000L,
                product(1000L, SizeType.small),
                provider(200L),
                BigDecimal.ONE,
                false
        );

        when(suppliesDAO.getProductUnitsForSupply(existingSupply)).thenReturn(List.of());
        when(shelfsWorkloadDAO.getAll()).thenReturn(List.of(shelf(1L, 250)));

        int totalFree = suppliesService.calcTotalFreeWorkload(existingSupply);

        assertEquals(250, totalFree);
    }

    @Test
    void deleteSupplyWithoutUnitsShouldOnlyDeleteSupply() {
        Supplies supply = supply(
                4000L,
                product(1000L, SizeType.small),
                provider(200L),
                BigDecimal.ONE,
                false
        );

        when(suppliesDAO.getProductUnitsForSupply(supply)).thenReturn(List.of());

        suppliesService.deleteSupply(supply);

        verify(shelfsWorkloadDAO, never()).updateWorkload(any(), anyInt());
        verify(productUnitsDAO, never()).deleteById(any());
        verify(suppliesDAO).deleteById(4000L);
    }

}
