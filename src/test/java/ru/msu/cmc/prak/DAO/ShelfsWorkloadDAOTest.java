package ru.msu.cmc.prak.DAO;

import org.junit.jupiter.api.Test;
import ru.msu.cmc.prak.models.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShelfsWorkloadDAOTest extends AbstractDAOTest {

    @Test
    void testGetShelvesWithFreeSpace() {
        saveShelf(1L, 100, 100);
        saveShelf(2L, 100, 490);

        List<ShelfsWorkload> found = shelfsWorkloadDAO.getShelvesWithFreeSpace(20);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testGetUnitsOnShelf() {
        ProductCategories category = saveCategory(1L, "Электроника");
        Products product = saveProduct(1L, category, "Ноутбук");
        Providers provider = saveProvider(1L, "ООО Альфа");
        Supplies supply = saveSupply(1L, product, provider, BigDecimal.TEN, LocalDateTime.now(), false);
        ShelfsWorkload shelf = saveShelf(1L, 100, 10);

        saveUnit(1L, product, LocalDateTime.of(2025, 1, 1, 10, 0), BigDecimal.ONE, shelf, supply, null);
        saveUnit(2L, product, LocalDateTime.of(2025, 1, 2, 10, 0), BigDecimal.TWO, shelf, supply, null);

        List<ProductUnits> units = shelfsWorkloadDAO.getUnitsOnShelf(shelf);
        assertEquals(2, units.size());
    }

    @Test
    void testUpdateWorkloadManagedFoundBranch() {
        saveShelf(1L, 100, 10);

        ShelfsWorkload detached = new ShelfsWorkload();
        detached.setId(1L);
        detached.setRoomNum(100);
        detached.setWorkloadCount(10);
        shelfsWorkloadDAO.updateWorkload(detached, 77);

        ShelfsWorkload updated = shelfsWorkloadDAO.getById(1L);
        assertNotNull(updated);
    }

    @Test
    void testUpdateWorkloadCatchBranch() {
        assertThrows(NullPointerException.class, () -> shelfsWorkloadDAO.updateWorkload(null, 77));
    }

    @Test
    void testUpdateWorkloadManagedNotFoundBranch() {
        ShelfsWorkload detached = new ShelfsWorkload();
        detached.setId(999L);
        detached.setRoomNum(100);
        detached.setWorkloadCount(10);

        shelfsWorkloadDAO.updateWorkload(detached, 77);

        assertNull(shelfsWorkloadDAO.getById(999L));
    }

    @Test
    void testGetByFilterAllNulls() {
        saveShelf(1L, 100, 10);
        saveShelf(2L, 200, 20);

        ShelfsWorkloadDAO.Filter filter = ShelfsWorkloadDAO.getFilterBuilder().build();
        assertEquals(2, shelfsWorkloadDAO.getByFilter(filter).size());
    }

    @Test
    void testGetByFilterAllConstraints() {
        saveShelf(1L, 100, 10);
        saveShelf(2L, 200, 20);

        ShelfsWorkloadDAO.Filter filter = ShelfsWorkloadDAO.getFilterBuilder()
                .roomNum(100)
                .minWorkload(5)
                .maxWorkload(15)
                .build();

        List<ShelfsWorkload> found = shelfsWorkloadDAO.getByFilter(filter);
        assertEquals(1, found.size());
        assertEquals(1L, found.getFirst().getId());
    }

    @Test
    void testDeleteByIdExistingEntity() {
        saveShelf(1L, 100, 10);

        assertNotNull(shelfsWorkloadDAO.getById(1L));

        shelfsWorkloadDAO.deleteById(1L);

        assertNull(shelfsWorkloadDAO.getById(1L));
    }

    @Test
    void testDeleteByIdNonExistingEntity() {
        assertDoesNotThrow(() -> shelfsWorkloadDAO.deleteById(999L));
        assertNull(shelfsWorkloadDAO.getById(999L));
    }
}