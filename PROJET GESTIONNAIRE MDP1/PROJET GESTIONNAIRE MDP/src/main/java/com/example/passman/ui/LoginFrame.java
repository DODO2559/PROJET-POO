package com.example.passman.ui;

import com.example.passman.dao.AdminDAO;
import com.example.passman.model.Admin;
import com.example.passman.util.CryptoUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Base64;
import java.util.List;

public class LoginFrame extends JFrame {

    JTextField champUtilisateur = new JTextField(20);
    JPasswordField champMotDePasse = new JPasswordField(20);
    AdminDAO daoAdmin = new AdminDAO();
    boolean estPremierUtilisateur;

    public LoginFrame() {
        verifierPremierUtilisateur();

        String titre = estPremierUtilisateur ? "Creation de l'administrateur" : "SecurIG2I - Connexion";
        setTitle(titre);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 320);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        JPanel panneauFormulaire = new JPanel(new GridBagLayout());
        panneauFormulaire.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints contraintes = new GridBagConstraints();
        contraintes.insets = new Insets(5, 5, 5, 5);
        contraintes.fill = GridBagConstraints.HORIZONTAL;

        JLabel labelTitre = new JLabel("Gestionnaire de Mots de Passe", SwingConstants.CENTER);
        labelTitre.setFont(new Font("Segoe UI", Font.BOLD, 20));

        contraintes.gridx = 0;
        contraintes.gridy = 0;
        contraintes.gridwidth = 2;
        contraintes.insets = new Insets(5, 5, 20, 5);
        panneauFormulaire.add(labelTitre, contraintes);

        contraintes.gridwidth = 1;
        contraintes.insets = new Insets(5, 5, 5, 5);

        contraintes.gridy = 1;
        contraintes.gridx = 0;
        JLabel labelUtilisateur = new JLabel("Identifiant:");
        panneauFormulaire.add(labelUtilisateur, contraintes);

        contraintes.gridx = 1;
        panneauFormulaire.add(champUtilisateur, contraintes);

        contraintes.gridy = 2;
        contraintes.gridx = 0;
        JLabel labelMdp = new JLabel("Mot de passe maitre:");
        panneauFormulaire.add(labelMdp, contraintes);

        contraintes.gridx = 1;
        panneauFormulaire.add(champMotDePasse, contraintes);

        String texteBouton = estPremierUtilisateur ? "Creer et se connecter" : "Deverrouiller";
        JButton boutonAction = new JButton(texteBouton);
        boutonAction.setFont(new Font("Segoe UI", Font.BOLD, 14));
        boutonAction.setBackground(new Color(60, 141, 188));
        boutonAction.setForeground(Color.WHITE);

        contraintes.gridy = 3;
        contraintes.gridx = 0;
        contraintes.gridwidth = 2;
        contraintes.anchor = GridBagConstraints.CENTER;
        contraintes.insets = new Insets(20, 5, 5, 5);
        panneauFormulaire.add(boutonAction, contraintes);


        boutonAction.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                gererAction(e);
            }
        });

        getRootPane().setDefaultButton(boutonAction);
        add(panneauFormulaire);
    }

    private void verifierPremierUtilisateur() {
        List<Admin> admins = daoAdmin.findAll();
        this.estPremierUtilisateur = admins.isEmpty();
        if (!estPremierUtilisateur) {
            champUtilisateur.setText(admins.get(0).getUsername());
            champUtilisateur.setEditable(false);
        }
    }

    private void gererAction(ActionEvent ev) {
        if (estPremierUtilisateur) {
            creerAdmin();
        } else {
            seConnecter();
        }
    }

    private void seConnecter() {
        char[] motDePasse = champMotDePasse.getPassword();
        if (motDePasse.length == 0) return;

        try {
            Admin admin = daoAdmin.findAll().get(0);
            byte[] sel = Base64.getDecoder().decode(admin.getSalt());

            if (CryptoUtil.verifyPassword(motDePasse, sel, admin.getPasswordHash())) {
                new MainFrame(admin, motDePasse, this).setVisible(true);
                this.setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Mot de passe incorrect.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(), "Erreur Critique", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void creerAdmin() {
        String nomUtilisateur = champUtilisateur.getText().trim();
        char[] motDePasse = champMotDePasse.getPassword();

        if (nomUtilisateur.isEmpty() || motDePasse.length < 8) {
            JOptionPane.showMessageDialog(this, "L'identifiant ne peut etre vide et le mot de passe doit faire 8 caracteres minimum.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String selBase64 = CryptoUtil.generateSaltBase64();
            byte[] sel = Base64.getDecoder().decode(selBase64);
            String hash = CryptoUtil.hashPassword(motDePasse, sel);

            Admin nouvelAdmin = new Admin(nomUtilisateur, hash, selBase64);
            daoAdmin.save(nouvelAdmin);

            JOptionPane.showMessageDialog(this, "Administrateur '" + nomUtilisateur + "' cree. Bienvenue !", "Succes", JOptionPane.INFORMATION_MESSAGE);

            new MainFrame(nouvelAdmin, motDePasse, this).setVisible(true);
            this.setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la creation: " + ex.getMessage(), "Erreur Critique", JOptionPane.ERROR_MESSAGE);
        }
    }
}