package ru.msu.cmc.prak.DAO.impl;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ShelfsWorkloadDAO;
import ru.msu.cmc.prak.models.ProductUnits;
import ru.msu.cmc.prak.models.ShelfsWorkload;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ShelfsWorkloadDAOImpl extends CommonDAOImpl<ShelfsWorkload, Long> implements ShelfsWorkloadDAO {

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
            var tx = session.beginTransaction();
            try {
                shelf.setWorkloadCount(newWorkload);
                session.merge(shelf);
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    tx.rollback();
                }
                throw e;
            }
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