package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ProvidersDAO;
import ru.msu.cmc.prak.models.Providers;
import ru.msu.cmc.prak.models.Supplies;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
                    "FROM Providers WHERE name LIKE :name", Providers.class);
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public Providers getSingleByName(String name) {
        List<Providers> candidates = getAllByName(name);
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    @Override
    public List<Providers> getByPhoneNum(String phoneNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "FROM Providers WHERE phoneNum LIKE :phone", Providers.class);
            query.setParameter("phone", likeExpr(phoneNum));
            return query.getResultList();
        }
    }

    @Override
    public List<Providers> getByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            Query<Providers> query = session.createQuery(
                    "FROM Providers WHERE email LIKE :email", Providers.class);
            query.setParameter("email", likeExpr(email));
            return query.getResultList();
        }
    }

    @Override
    public List<Providers> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Providers> criteriaQuery = builder.createQuery(Providers.class);
            Root<Providers> root = criteriaQuery.from(Providers.class);

            List<Predicate> predicates = new ArrayList<>();

            if (filter.getId() != null) {
                predicates.add(builder.equal(root.get("id"), filter.getId()));
            }
            if (filter.getName() != null) {
                predicates.add(builder.like(root.get("name"), likeExpr(filter.getName())));
            }
            if (filter.getAddress() != null) {
                predicates.add(builder.like(root.get("address"), likeExpr(filter.getAddress())));
            }
            if (filter.getPhoneNum() != null) {
                predicates.add(builder.like(root.get("phoneNum"), likeExpr(filter.getPhoneNum())));
            }
            if (filter.getEmail() != null) {
                predicates.add(builder.like(root.get("email"), likeExpr(filter.getEmail())));
            }

            if (!predicates.isEmpty()) {
                criteriaQuery.where(predicates.toArray(new Predicate[0]));
            }

            return session.createQuery(criteriaQuery).getResultList();
        }
    }

    @Override
    public List<Supplies> getSuppliesFromProvider(Providers provider) {
        try (Session session = sessionFactory.openSession()) {
            Query<Supplies> query = session.createQuery(
                    "FROM Supplies WHERE provider = :prov", Supplies.class);
            query.setParameter("prov", provider);
            return query.getResultList();
        }
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}