package com.example.passman.dao;

import com.example.passman.model.Admin;
import com.example.passman.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.Collections;
import java.util.List;

public class AdminDAO {
    public Admin findByUsername(String username) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Admin> q = em.createQuery("SELECT a FROM Admin a WHERE a.username = :u", Admin.class);
            q.setParameter("u", username);
            return q.getSingleResult();
        } catch (NoResultException e) {
            return null;
        } finally {
            em.close();
        }
    }

    public List<Admin> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            TypedQuery<Admin> q = em.createQuery("SELECT a FROM Admin a", Admin.class);
            return q.getResultList();
        } catch (Exception e) {
            return Collections.emptyList();
        } finally {
            em.close();
        }
    }

    public void save(Admin a) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        em.persist(a);
        em.getTransaction().commit();
        em.close();
    }
}