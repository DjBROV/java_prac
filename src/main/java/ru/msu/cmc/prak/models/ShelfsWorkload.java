package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "shelfs_workload")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class ShelfsWorkload implements CommonEntity<Long> {

    @Id
    @Column(name = "shelf_num", nullable = false)
    private Long id;

    @Column(name = "room_num", nullable = false)
    @NonNull
    private Integer roomNum;

    @Column(name = "workload_count", nullable = false)
    @NonNull
    private Integer workloadCount;

    // обратная связь: одна полка может содержать много партий товара
    @OneToMany(mappedBy = "shelf", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ProductUnits> productUnits = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShelfsWorkload that = (ShelfsWorkload) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}