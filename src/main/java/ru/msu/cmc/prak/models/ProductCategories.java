package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "product_categories")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class ProductCategories implements CommonEntity<Long> {

    @Id
    @Column(name = "category_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    @NonNull
    private String name;

    // обратная связь: одна категория может иметь много продуктов
    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Products> products = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductCategories that = (ProductCategories) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}