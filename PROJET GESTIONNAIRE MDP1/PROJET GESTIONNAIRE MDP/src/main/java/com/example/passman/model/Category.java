package com.example.passman.model;

import jakarta.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "category")
public class Category {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    private String description;

    // --- AJOUT DE CETTE RELATION ---
    // Elle lie une catégorie à un utilisateur spécifique
    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @OneToMany(mappedBy = "category", cascade = CascadeType.PERSIST)
    private List<Credential> credentials;

    public Category() {}
    public Category(String name, String description) {
        this.name = name; this.description = description;
    }

    // --- GETTERS ET SETTERS ---
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String n) { this.name = n; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }

    // --- MÉTHODES MANQUANTES À AJOUTER ---
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Category category = (Category) o;
        return Objects.equals(id, category.id) && Objects.equals(name, category.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}