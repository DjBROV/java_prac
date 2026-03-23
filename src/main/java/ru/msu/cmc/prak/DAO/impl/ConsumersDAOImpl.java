package ru.msu.cmc.prak.DAO.impl;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;
import ru.msu.cmc.prak.DAO.ConsumersDAO;
import ru.msu.cmc.prak.models.Consumers;
import ru.msu.cmc.prak.models.Orders;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ConsumersDAOImpl extends CommonDAOImpl<Consumers, Long> implements ConsumersDAO {

    public ConsumersDAOImpl() {
        super(Consumers.class);
    }

    @Override
    public List<Consumers> getAllByName(String name) {
        try (Session session = sessionFactory.openSession()) {
            Query<Consumers> query = session.createQuery(
                    "FROM Consumers WHERE name LIKE :name", Consumers.class);
            query.setParameter("name", likeExpr(name));
            return query.getResultList();
        }
    }

    @Override
    public Consumers getSingleByName(String name) {
        List<Consumers> candidates = getAllByName(name);
        return candidates.size() == 1 ? candidates.getFirst() : null;
    }

    @Override
    public List<Consumers> getByPhoneNum(String phoneNum) {
        try (Session session = sessionFactory.openSession()) {
            Query<Consumers> query = session.createQuery(
                    "FROM Consumers WHERE phoneNum LIKE :phone", Consumers.class);
            query.setParameter("phone", likeExpr(phoneNum));
            return query.getResultList();
        }
    }

    @Override
    public List<Consumers> getByEmail(String email) {
        try (Session session = sessionFactory.openSession()) {
            Query<Consumers> query = session.createQuery(
                    "FROM Consumers WHERE email LIKE :email", Consumers.class);
            query.setParameter("email", likeExpr(email));
            return query.getResultList();
        }
    }

    @Override
    public List<Consumers> getByFilter(Filter filter) {
        try (Session session = sessionFactory.openSession()) {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<Consumers> criteriaQuery = builder.createQuery(Consumers.class);
            Root<Consumers> root = criteriaQuery.from(Consumers.class);

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
    public List<Orders> getOrdersByConsumer(Consumers consumer) {
        try (Session session = sessionFactory.openSession()) {
            Query<Orders> query = session.createQuery(
                    "FROM Orders WHERE consumer = :cons", Orders.class);
            query.setParameter("cons", consumer);
            return query.getResultList();
        }
    }

    private String likeExpr(String param) {
        return "%" + param + "%";
    }
}