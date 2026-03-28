package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProvidersDAO;
import ru.msu.cmc.prak.models.Providers;
import ru.msu.cmc.prak.models.Supplies;

import java.util.ArrayList;
import java.util.List;

@Repository
public class ProvidersDAOImpl extends CommonDAOImpl<Providers, Long> implements ProvidersDAO {

    public ProvidersDAOImpl() {
        super(Providers.class);
    }

    @Override
    public List<Providers> getAllByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "from Providers p where lower(p.name) like :name order by p.id",
                    Providers.class
            );
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public Providers getSingleByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "from Providers p where lower(p.name) = lower(:name)",
                    Providers.class
            );
            query.setParameter("name", name);
            query.setMaxResults(1);
            return query.uniqueResultOptional().orElse(null);
        }
    }

    @Override
    public List<Providers> getByPhoneNum(String phoneNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "from Providers p where lower(p.phoneNum) like :phone order by p.id",
                    Providers.class
            );
            query.setParameter("phone", likeExpr(phoneNum));
            return query.getResultList();
        }
    }

    @Override
    public List<Providers> getByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "from Providers p where lower(p.email) like :email order by p.id",
                    Providers.class
            );
            query.setParameter("email", likeExpr(email));
            return query.getResultList();
        }
    }

    @Override
    public List<Providers> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("from Providers p where 1=1");
            List<ParameterBinder> binders = new ArrayList<>();

            if (filter.getId() != null) {
                hql.append(" and p.id = :id");
                binders.add(q -> q.setParameter("id", filter.getId()));
            }
            if (filter.getName() != null) {
                hql.append(" and lower(p.name) like :name");
                binders.add(q -> q.setParameter("name", likeExpr(filter.getName())));
            }
            if (filter.getAddress() != null) {
                hql.append(" and lower(p.address) like :address");
                binders.add(q -> q.setParameter("address", likeExpr(filter.getAddress())));
            }
            if (filter.getPhoneNum() != null) {
                hql.append(" and lower(p.phoneNum) like :phoneNum");
                binders.add(q -> q.setParameter("phoneNum", likeExpr(filter.getPhoneNum())));
            }
            if (filter.getEmail() != null) {
                hql.append(" and lower(p.email) like :email");
                binders.add(q -> q.setParameter("email", likeExpr(filter.getEmail())));
            }

            hql.append(" order by p.id");

            Query<Providers> query = session.createQuery(hql.toString(), Providers.class);
            for (ParameterBinder binder : binders) {
                binder.bind(query);
            }
            return query.getResultList();
        }
    }

    @Override
    public List<Supplies> getSuppliesFromProvider(Providers provider) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "from Supplies s where s.provider = :provider order by s.time, s.id",
                    Supplies.class
            );
            query.setParameter("provider", provider);
            return query.getResultList();
        }
    }

    private String likeExpr(String value) {
        return "%" + value.toLowerCase() + "%";
    }

    @FunctionalInterface
    private interface ParameterBinder {
        void bind(Query<Providers> query);
    }
}