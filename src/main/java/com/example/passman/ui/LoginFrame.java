package com.example.passman.ui;

import com.example.passman.dao.AdminDAO;
import com.example.passman.model.Admin;
import com.example.passman.util.CryptoUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Base64;
import java.util.List;

public class LoginFrame extends JFrame {
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final AdminDAO adminDAO = new AdminDAO();
    private boolean isFirstUser;

    public LoginFrame() {
        checkIfFirstUser();

        String title = isFirstUser ? "Création de l'administrateur" : "SécurIG2I - Connexion";
        setTitle(title);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 320);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel("Gestionnaire de Mots de Passe", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(5, 5, 20, 5);
        formPanel.add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel userLabel = new JLabel("Identifiant:");
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(userField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel passLabel = new JLabel("Mot de passe maître:");
        formPanel.add(passLabel, gbc);

        gbc.gridx = 1;
        formPanel.add(passwordField, gbc);

        String buttonText = isFirstUser ? "Créer et se connecter" : "Déverrouiller";
        JButton actionButton = new JButton(buttonText);
        actionButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        actionButton.setBackground(new Color(60, 141, 188));
        actionButton.setForeground(Color.WHITE);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 5, 5, 5);
        formPanel.add(actionButton, gbc);

        actionButton.addActionListener(this::onAction);
        getRootPane().setDefaultButton(actionButton);
        add(formPanel);
    }

    private void checkIfFirstUser() {
        List<Admin> admins = adminDAO.findAll();
        this.isFirstUser = admins.isEmpty();
        if (!isFirstUser) {
            userField.setText(admins.get(0).getUsername());
            userField.setEditable(false);
        }
    }

    private void onAction(ActionEvent ev) {
        if (isFirstUser) {
            onCreateAdmin();
        } else {
            onLogin();
        }
    }

    private void onLogin() {
        char[] pwd = passwordField.getPassword();
        if (pwd.length == 0) return;

        try {
            Admin admin = adminDAO.findAll().get(0);
            byte[] salt = Base64.getDecoder().decode(admin.getSalt());
            if (CryptoUtil.verifyPassword(pwd, salt, admin.getPasswordHash())) {
                new MainFrame(admin, pwd, this).setVisible(true);
                this.setVisible(false);
            } else {
                JOptionPane.showMessageDialog(this, "Mot de passe incorrect.", "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur: " + ex.getMessage(), "Erreur Critique", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCreateAdmin() {
        String username = userField.getText().trim();
        char[] pwd = passwordField.getPassword();
        if (username.isEmpty() || pwd.length < 8) {
            JOptionPane.showMessageDialog(this, "L'identifiant ne peut être vide et le mot de passe doit faire 8 caractères minimum.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String saltB64 = CryptoUtil.generateSaltBase64();
            byte[] salt = Base64.getDecoder().decode(saltB64);
            String hash = CryptoUtil.hashPassword(pwd, salt);
            Admin newAdmin = new Admin(username, hash, saltB64);
            adminDAO.save(newAdmin);

            JOptionPane.showMessageDialog(this, "Administrateur '" + username + "' créé. Bienvenue !", "Succès", JOptionPane.INFORMATION_MESSAGE);
            new MainFrame(newAdmin, pwd, this).setVisible(true);
            this.setVisible(false);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la création: " + ex.getMessage(), "Erreur Critique", JOptionPane.ERROR_MESSAGE);
        }
    }
}