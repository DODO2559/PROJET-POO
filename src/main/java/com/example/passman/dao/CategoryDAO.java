package com.example.passman.dao;

import com.example.passman.model.Category;
import com.example.passman.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;

public class CategoryDAO {
    public void save(Category c) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        em.persist(c);
        em.getTransaction().commit();
        em.close();
    }

    public void delete(Category c) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        Category managedCategory = em.find(Category.class, c.getId());
        if (managedCategory != null) {
            em.remove(managedCategory);
        }
        em.getTransaction().commit();
        em.close();
    }

    public List<Category> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Category> q = em.createQuery("SELECT c FROM Category c ORDER BY c.name", Category.class);
        List<Category> res = q.getResultList();
        em.close();
        return res;
    }
}