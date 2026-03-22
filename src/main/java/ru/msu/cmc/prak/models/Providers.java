package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "providers")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Providers implements CommonEntity<Long> {

    @Id
    @Column(name = "provider_id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false)
    @NonNull
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_num")
    @NonNull
    private String phoneNum;

    @Column(name = "email")
    @NonNull
    private String email;

    // обратная связь: один поставщик может иметь много поставок
    @OneToMany(mappedBy = "provider", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Supplies> supplies = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Providers that = (Providers) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}