package com.example.passman.dao;

import com.example.passman.model.Admin;
import com.example.passman.model.Credential;
import com.example.passman.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.List;

public class CredentialDAO {
    public void save(Credential cred) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        em.persist(cred);
        em.getTransaction().commit();
        em.close();
    }

    public void update(Credential cred) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        em.merge(cred);
        em.getTransaction().commit();
        em.close();
    }

    public void deleteById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        em.getTransaction().begin();
        Credential cred = em.find(Credential.class, id);
        if (cred != null) {
            em.remove(cred);
        }
        em.getTransaction().commit();
        em.close();
    }

    public Credential findById(Long id) {
        EntityManager em = JPAUtil.getEntityManager();
        Credential credential = em.find(Credential.class, id);
        em.close();
        return credential;
    }

    public List<Credential> findByAdmin(Admin admin) {
        EntityManager em = JPAUtil.getEntityManager();
        TypedQuery<Credential> q = em.createQuery("SELECT c FROM Credential c WHERE c.admin = :admin ORDER BY c.serviceName", Credential.class);
        q.setParameter("admin", admin);
        List<Credential> res = q.getResultList();
        em.close();
        return res;
    }
}