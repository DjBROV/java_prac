package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "product_units")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductUnits implements CommonEntity<Long> {

    @Id
    @Column(name = "units_id", nullable = false)
    private Long id;

    // связь с продуктом
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NonNull
    private Products product;

    @Column(name = "arrival", nullable = false)
    @NonNull
    private LocalDateTime arrival;

    @Column(name = "amount", nullable = false)
    @NonNull
    private BigDecimal amount;

    // связь с полкой
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelf_num", nullable = false)
    @NonNull
    private ShelfsWorkload shelf;

    // связь с поставкой
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supply_id", nullable = false)
    @NonNull
    private Supplies supply;

    // связь с заказом (может быть null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Orders order;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductUnits that = (ProductUnits) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}