package com.example.passman.ui;

import com.example.passman.dao.CategoryDAO;
import com.example.passman.dao.CredentialDAO;
import com.example.passman.model.Admin;
import com.example.passman.model.Category;
import com.example.passman.model.Credential;
import com.example.passman.util.CryptoUtil;
import com.example.passman.util.PasswordGenerator;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {
    private final Admin currentAdmin;
    private final char[] masterPassword;
    private final byte[] adminSalt;
    private final JFrame loginFrame;

    private final CredentialDAO credDAO = new CredentialDAO();
    private final CategoryDAO catDAO = new CategoryDAO();

    private final DefaultTableModel tableModel;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JList<Category> categoryList;
    private final DefaultListModel<Category> categoryListModel;
    private final JLabel statsLabel = new JLabel();
    private final JTextField searchField = new JTextField();

    public MainFrame(Admin admin, char[] masterPassword, JFrame loginFrame) {
        this.currentAdmin = admin;
        this.masterPassword = masterPassword;
        this.adminSalt = Base64.getDecoder().decode(admin.getSalt());
        this.loginFrame = loginFrame;

        this.tableModel = new DefaultTableModel(new String[]{"ID", "Service", "Identifiant", "Catégorie"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        this.table = new JTable(tableModel);
        this.sorter = new TableRowSorter<>(tableModel);
        this.categoryListModel = new DefaultListModel<>();
        this.categoryList = new JList<>(categoryListModel);

        setTitle("SécurIG2I - " + currentAdmin.getUsername());
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUI();
        loadCategories();
        loadCredentials();
    }

    private void initUI() {
        setLayout(new BorderLayout(5, 5));

        add(createToolBar(), BorderLayout.NORTH);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCategoryPanel(), createCredentialsPanel());
        splitPane.setDividerLocation(220);
        add(splitPane, BorderLayout.CENTER);

        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.add(statsLabel);
        add(statusBar, BorderLayout.SOUTH);
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setRollover(true);
        toolBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        toolBar.add(new JButton(new AbstractAction("Ajouter") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onAddOrEdit(null); }
        }));
        toolBar.add(new JButton(new AbstractAction("Modifier") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onEdit(); }
        }));
        toolBar.add(new JButton(new AbstractAction("Supprimer") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onDelete(); }
        }));
        toolBar.add(new JButton(new AbstractAction("Afficher Mdp") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onShow(); }
        }));
        toolBar.addSeparator();
        toolBar.add(new JButton(new AbstractAction("Générateur") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onGenerate(); }
        }));
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(new JButton(new AbstractAction("Déconnexion") {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { onLogout(); }
        }));
        return toolBar;
    }

    private JPanel createCategoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        categoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        categoryList.addListSelectionListener(e -> applyFilters());
        panel.add(new JScrollPane(categoryList), BorderLayout.CENTER);

        JButton manageCategoriesBtn = new JButton("Gérer les catégories");
        manageCategoriesBtn.addActionListener(e -> onManageCategories());
        panel.add(manageCategoriesBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createCredentialsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        if (table.getColumnModel().getColumnCount() > 0) {
            table.removeColumn(table.getColumnModel().getColumn(0));
        }

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilters(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilters(); }
        });

        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Rechercher: "), BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private void loadCategories() {
        categoryListModel.clear();
        Category all = new Category();
        all.setName("Toutes les catégories");
        categoryListModel.addElement(all);

        List<Category> categories = catDAO.findAll();
        categories.forEach(categoryListModel::addElement);
        categoryList.setSelectedIndex(0);
    }

    private void loadCredentials() {
        tableModel.setRowCount(0);
        List<Credential> credentials = credDAO.findByAdmin(currentAdmin);
        for (Credential c : credentials) {
            String categoryName = c.getCategory() != null ? c.getCategory().getName() : "";
            tableModel.addRow(new Object[]{c.getId(), c.getServiceName(), c.getLogin(), categoryName});
        }
        statsLabel.setText("Total d'identifiants : " + credentials.size());
    }

    private void applyFilters() {
        String searchText = searchField.getText();
        Category selectedCategory = categoryList.getSelectedValue();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (searchText != null && !searchText.trim().isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + searchText, 1, 2));
        }
        if (selectedCategory != null && selectedCategory.getId() != null) {
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(selectedCategory.getName()) + "$", 3));
        }

        sorter.setRowFilter(RowFilter.andFilter(filters));
    }

    private void onEdit() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner un identifiant à modifier.", "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Long id = (Long) tableModel.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
        Credential cred = credDAO.findById(id);
        onAddOrEdit(cred);
    }

    private void onDelete() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;
        Long id = (Long) tableModel.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Confirmer la suppression ?", "Suppression", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            credDAO.deleteById(id);
            loadCredentials();
        }
    }

    private void onShow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;
        Long id = (Long) tableModel.getValueAt(table.convertRowIndexToModel(selectedRow), 0);
        Credential cred = credDAO.findById(id);
        try {
            String decrypted = CryptoUtil.decrypt(masterPassword, cred.getEncryptedPassword(), adminSalt, cred.getIv());
            JOptionPane.showMessageDialog(this, "Mot de passe pour '" + cred.getServiceName() + "': " + decrypted, "Mot de passe", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur de déchiffrement.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onGenerate() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        JSpinner lengthSpinner = new JSpinner(new SpinnerNumberModel(16, 8, 128, 1));
        JCheckBox upperCheck = new JCheckBox("Majuscules (A-Z)", true);
        JCheckBox digitsCheck = new JCheckBox("Chiffres (0-9)", true);
        JCheckBox symbolsCheck = new JCheckBox("Symboles (!@#...)", true);
        JCheckBox ambiguousCheck = new JCheckBox("Éviter les caractères ambigus (O0Il1)", true);

        panel.add(new JLabel("Longueur:"));
        panel.add(lengthSpinner);
        panel.add(upperCheck);
        panel.add(digitsCheck);
        panel.add(symbolsCheck);
        panel.add(ambiguousCheck);

        int result = JOptionPane.showConfirmDialog(this, panel, "Générateur de mot de passe", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String generated = PasswordGenerator.generate(
                    (int) lengthSpinner.getValue(), upperCheck.isSelected(), digitsCheck.isSelected(),
                    symbolsCheck.isSelected(), ambiguousCheck.isSelected());
            JTextArea textArea = new JTextArea(generated);
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            JOptionPane.showMessageDialog(this, scrollPane, "Mot de passe généré", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onAddOrEdit(Credential cred) {
        boolean isEdit = cred != null;

        JTextField serviceField = new JTextField(isEdit ? cred.getServiceName() : "");
        JTextField loginField = new JTextField(isEdit ? cred.getLogin() : "");
        JPasswordField passwordField = new JPasswordField();
        JTextField urlField = new JTextField(isEdit ? cred.getUrl() : "");
        JComboBox<Category> categoryBox = new JComboBox<>(catDAO.findAll().toArray(new Category[0]));
        if (isEdit && cred.getCategory() != null) categoryBox.setSelectedItem(cred.getCategory());

        JPanel panel = new JPanel(new GridLayout(0, 1, 2, 2));
        panel.add(new JLabel("Service:")); panel.add(serviceField);
        panel.add(new JLabel("Identifiant (login/email):")); panel.add(loginField);
        panel.add(new JLabel("Mot de passe (laisser vide pour ne pas changer):")); panel.add(passwordField);
        panel.add(new JLabel("URL (facultatif):")); panel.add(urlField);
        panel.add(new JLabel("Catégorie:")); panel.add(categoryBox);

        int result = JOptionPane.showConfirmDialog(this, panel, isEdit ? "Modifier l'identifiant" : "Ajouter un identifiant", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            Credential toSave = isEdit ? cred : new Credential();
            toSave.setAdmin(currentAdmin);
            toSave.setServiceName(serviceField.getText());
            toSave.setLogin(loginField.getText());
            toSave.setUrl(urlField.getText());
            toSave.setCategory((Category) categoryBox.getSelectedItem());

            try {
                if (passwordField.getPassword().length > 0) {
                    CryptoUtil.Encrypted enc = CryptoUtil.encrypt(masterPassword, new String(passwordField.getPassword()), adminSalt);
                    toSave.setEncryptedPassword(enc.ciphertextBase64);
                    toSave.setIv(enc.ivBase64);
                }
                if (isEdit) credDAO.update(toSave); else credDAO.save(toSave);
                loadCredentials();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erreur de sauvegarde: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onManageCategories() {
        JDialog dialog = new JDialog(this, "Gérer les catégories", true);
        DefaultListModel<Category> model = new DefaultListModel<>();
        catDAO.findAll().forEach(model::addElement);
        JList<Category> list = new JList<>(model);

        JButton add = new JButton("Ajouter");
        add.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(dialog, "Nom de la catégorie:");
            if (name != null && !name.isEmpty()) {
                catDAO.save(new Category(name, ""));
                model.clear();
                catDAO.findAll().forEach(model::addElement);
            }
        });

        JButton delete = new JButton("Supprimer");
        delete.addActionListener(e -> {
            if (list.getSelectedValue() != null) {
                credDAO.findByAdmin(currentAdmin).stream()
                        .filter(c -> c.getCategory() != null && c.getCategory().getId().equals(list.getSelectedValue().getId()))
                        .forEach(c -> {
                            c.setCategory(null);
                            credDAO.update(c);
                        });
                catDAO.delete(list.getSelectedValue());
                model.clear();
                catDAO.findAll().forEach(model::addElement);
            }
        });

        JPanel buttons = new JPanel();
        buttons.add(add);
        buttons.add(delete);

        dialog.add(new JScrollPane(list));
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.setSize(300, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        loadCategories();
        loadCredentials();
    }

    private void onLogout() {
        this.dispose();
        loginFrame.setVisible(true);
    }
}