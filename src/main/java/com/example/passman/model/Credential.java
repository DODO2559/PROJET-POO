package com.example.passman.model;

import jakarta.persistence.*;

@Entity
@Table(name = "credential")
public class Credential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String serviceName;
    private String login;

    @Lob
    private String encryptedPassword;

    private String iv;

    private String url;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(optional = false)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    public Credential() {}

    public Credential(String serviceName, String login, String encryptedPassword, String iv, String url, Category category, Admin admin) {
        this.serviceName = serviceName;
        this.login = login;
        this.encryptedPassword = encryptedPassword;
        this.iv = iv;
        this.url = url;
        this.category = category;
        this.admin = admin;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String s) { this.serviceName = s; }
    public String getLogin() { return login; }
    public void setLogin(String l) { this.login = l; }
    public String getEncryptedPassword() { return encryptedPassword; }
    public void setEncryptedPassword(String e) { this.encryptedPassword = e; }
    public String getIv() { return iv; }
    public void setIv(String iv) { this.iv = iv; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}