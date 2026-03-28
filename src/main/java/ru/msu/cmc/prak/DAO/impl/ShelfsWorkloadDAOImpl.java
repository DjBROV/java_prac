package ru.msu.cmc.prak.DAO.impl;

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

    private static final int SHELF_CAPACITY = 500;

    public ShelfsWorkloadDAOImpl() {
        super(ShelfsWorkload.class);
    }

    @Override
    public List<ShelfsWorkload> getByRoomNum(Integer roomNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<ShelfsWorkload> query = session.createQuery(
                    "from ShelfsWorkload s where s.roomNum = :roomNum order by s.id",
                    ShelfsWorkload.class
            );
            query.setParameter("roomNum", roomNum);
            return query.getResultList();
        }
    }

    @Override
    public List<ShelfsWorkload> getShelvesWithFreeSpace(int requiredUnits) {
        try (Session session = sessionFactory.openSession()) {
            Query<ShelfsWorkload> query = session.createQuery(
                    "from ShelfsWorkload s where (s.workloadCount + :requiredUnits) <= :capacity order by s.id",
                    ShelfsWorkload.class
            );
            query.setParameter("requiredUnits", requiredUnits);
            query.setParameter("capacity", SHELF_CAPACITY);
            return query.getResultList();
        }
    }

    @Override
    public List<ProductUnits> getUnitsOnShelf(ShelfsWorkload shelf) {
        try (Session session = sessionFactory.openSession()) {
            Query<ProductUnits> query = session.createQuery(
                    "from ProductUnits u where u.shelf = :shelf order by u.arrival, u.id",
                    ProductUnits.class
            );
            query.setParameter("shelf", shelf);
            return query.getResultList();
        }
    }

    @Override
    public void updateWorkload(ShelfsWorkload shelf, int newWorkload) {
        try (Session session = sessionFactory.openSession()) {
            var tx = session.beginTransaction();
            try {
                ShelfsWorkload managed = session.find(ShelfsWorkload.class, shelf.getId());
                if (managed != null) {
                    managed.setWorkloadCount(newWorkload);
                }
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
            StringBuilder hql = new StringBuilder("from ShelfsWorkload s where 1=1");
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getRoomNum() != null) {
                hql.append(" and s.roomNum = :roomNum");
                binders.add(q -> q.setParameter("roomNum", filter.getRoomNum()));
            }
            if (filter.getMinWorkload() != null) {
                hql.append(" and s.workloadCount >= :minWorkload");
                binders.add(q -> q.setParameter("minWorkload", filter.getMinWorkload()));
            }
            if (filter.getMaxWorkload() != null) {
                hql.append(" and s.workloadCount <= :maxWorkload");
                binders.add(q -> q.setParameter("maxWorkload", filter.getMaxWorkload()));
            }

            hql.append(" order by s.id");

            Query<ShelfsWorkload> query = session.createQuery(hql.toString(), ShelfsWorkload.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<ShelfsWorkload> query);
    }
}