package ru.msu.cmc.prak.DAO;

import lombok.Builder;
import lombok.Getter;
import ru.msu.cmc.prak.models.ShelfsWorkload;
import ru.msu.cmc.prak.models.ProductUnits;
import java.util.List;

public interface ShelfsWorkloadDAO extends CommonDAO<ShelfsWorkload, Long> {

    /**
     * Поиск полок по номеру комнаты.
     */
    List<ShelfsWorkload> getByRoomNum(Integer roomNum);

    /**
     * Поиск полок с достаточным свободным местом для размещения указанного количества.
     * Используется при создании поставки для резервирования места.
     * @param requiredUnits количество единиц, которые нужно разместить
     * @return список полок, где свободное место >= requiredUnits
     */
    List<ShelfsWorkload> getShelvesWithFreeSpace(int requiredUnits);

    /**
     * Получение всех товарных единиц, хранящихся на данной полке.
     */
    List<ProductUnits> getUnitsOnShelf(ShelfsWorkload shelf);

    /**
     * Обновление значения загруженности полки.
     * Используется при резервировании места (уменьшение свободного места)
     * и при удалении поставки (освобождение места).
     */
    void updateWorkload(ShelfsWorkload shelf, int newWorkload);

    List<ShelfsWorkload> getByFilter(Filter filter);

    @Builder
    @Getter
    class Filter {
        private Integer roomNum;
        private Integer minWorkload;  // минимальная загруженность
        private Integer maxWorkload;  // максимальная загруженность
    }

    static Filter.FilterBuilder getFilterBuilder() {
        return Filter.builder();
    }
}