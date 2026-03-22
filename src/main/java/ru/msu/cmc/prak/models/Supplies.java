package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "supplies")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Supplies implements CommonEntity<Long> {

    @Id
    @Column(name = "supply_id", nullable = false)
    private Long id;

    // связь с продуктом
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    @NonNull
    private Products product;

    @Column(name = "amount", nullable = false)
    @NonNull
    private BigDecimal amount;

    @Column(name = "time", nullable = false)
    @NonNull
    private LocalDateTime time;

    // связь с поставщиком
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    @NonNull
    private Providers provider;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    // обратная связь: одна поставка может быть связана с несколькими единицами товара
    @OneToMany(mappedBy = "supply", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductUnits> productUnits = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Supplies that = (Supplies) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}