package com.example.passman.dao;

import com.example.passman.model.Admin;
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

    // --- MÉTHODE MANQUANTE À AJOUTER ---
    // Elle remplace l'ancienne méthode "findAll"
    public List<Category> findByAdmin(Admin admin) {
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Category> q = em.createQuery("SELECT c FROM Category c WHERE c.admin = :admin ORDER BY c.name", Category.class);
        q.setParameter("admin", admin);
        List<Category> res = q.getResultList();
        em.close();
        return res;
    }
}