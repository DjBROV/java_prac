package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ShelfsWorkloadDAO;
import ru.msu.cmc.prak.models.ShelfsWorkload;
import ru.msu.cmc.prak.models.ProductUnits;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ShelfsWorkloadDAOImpl extends CommonDAOImpl<ShelfsWorkload, Long> implements ShelfsWorkloadDAO {

    // Предположим, что максимальная вместимость полки = 500 (можно вынести в настройки)
    private static final int DEFAULT_CAPACITY = 500;

    public ShelfsWorkloadDAOImpl() {
        super(ShelfsWorkload.class);
    }

    @Override
    public List<ShelfsWorkload> getByRoomNum(Integer roomNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<ShelfsWorkload> query = session.createQuery(
                    "FROM ShelfsWorkload WHERE roomNum = :room", ShelfsWorkload.class);
            query.setParameter("room", roomNum);
            return query.getResultList();
        }
    }

    @Override
    public List<ShelfsWorkload> getShelvesWithFreeSpace(int requiredUnits) {
        try (Session session = sessionFactory.openSession()) {
            // Предполагаем, что workloadCount — это текущая загрузка, и максимальная вместимость = 500.
            // В реальной системе в таблице должно быть поле capacity.
            Query<ShelfsWorkload> query = session.createQuery(
                    "FROM ShelfsWorkload WHERE (workloadCount + :required) <= :capacity", ShelfsWorkload.class);
            query.setParameter("required", requiredUnits);
            query.setParameter("capacity", DEFAULT_CAPACITY);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getUnitsOnShelf(ShelfsWorkload shelf) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "FROM ProductUnits WHERE shelf = :shelf", ProductUnits.class);
            query.setParameter("shelf", shelf);
            return query.getResultList();
        }
    }

    @Override
    public void updateWorkload(ShelfsWorkload shelf, int newWorkload) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            shelf.setWorkloadCount(newWorkload);
            session.update(shelf);
            session.getTransaction().commit();
        }
    }

    @Override
    public List<ShelfsWorkload> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<ShelfsWorkload> criteriaQuery = builder.createQuery(ShelfsWorkload.class);
            Root<ShelfsWorkload> root = criteriaQuery.from(ShelfsWorkload.class);

            List<Predicate> predicates = new ArrayList<>();
            if (filter.getRoomNum() != null) {
                predicates.add(builder.equal(root.get("roomNum"), filter.getRoomNum()));
            }
            if (filter.getMinWorkload() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("workloadCount"), filter.getMinWorkload()));
            }
            if (filter.getMaxWorkload() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("workloadCount"), filter.getMaxWorkload()));
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }
}