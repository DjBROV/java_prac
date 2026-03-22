package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Orders implements CommonEntity<Long> {

    @Id
    @Column(name = "order_id", nullable = false)
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

    // связь с потребителем
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "consumer_id", nullable = false)
    @NonNull
    private Consumers consumer;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    // обратная связь: один заказ может быть связан с несколькими единицами товара
    @OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductUnits> productUnits = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Orders that = (Orders) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}