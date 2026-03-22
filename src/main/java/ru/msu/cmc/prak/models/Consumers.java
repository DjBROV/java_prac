package ru.msu.cmc.prak.models;

import lombok.*;
import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "consumers")
@Getter
@Setter
@ToString
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Consumers implements CommonEntity<Long> {

    @Id
    @Column(name = "consumer_id", nullable = false)
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

    // обратная связь: один потребитель может сделать много заказов
    @OneToMany(mappedBy = "consumer", fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<Orders> orders = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Consumers that = (Consumers) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}