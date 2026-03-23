package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.time.Duration;
import java.util.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Products implements CommonEntity<Long> {

    @Id
    @Column(name = "product_id", nullable = false)
    private Long id;

    // связь с категорией (многие продукты → одна категория)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @NonNull
    private ProductCategories category;

    @Column(name = "name", nullable = false)
    @NonNull
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit")
    private UnitsType unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "size")
    private SizeType product_size;

    @Column(name = "units_for_one")
    private Integer unitsForOne;

    @Column(name = "storage_life")
    private Duration storageLife;

    // обратные связи (для полноты)
    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Supplies> supplies = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Orders> orders = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductUnits> productUnits = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Products that = (Products) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}