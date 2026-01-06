package com.example.passman.ui;

import com.example.passman.dao.CategoryDAO;
import com.example.passman.dao.CredentialDAO;
import com.example.passman.model.Admin;
import com.example.passman.model.Category;
import com.example.passman.model.Credential;
import com.example.passman.util.CryptoUtil;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class MainFrame extends JFrame {

    Admin adminActuel;
    char[] motDePasseMaitre;
    byte[] selAdmin;
    JFrame fenetreLogin;

    CredentialDAO daoIdentifiant = new CredentialDAO();
    CategoryDAO daoCategorie = new CategoryDAO();

    DefaultTableModel modeleTableau;
    JTable tableau;
    TableRowSorter<DefaultTableModel> trieur;
    JList<Category> listeCategories;
    DefaultListModel<Category> modeleListeCategories;
    JLabel labelStats = new JLabel();
    JTextField champRecherche = new JTextField();

    JPanel conteneurBas;
    CardLayout layoutCartesBas = new CardLayout();

    JPanel panneauDetails;
    JPanel panneauEditeurDivise;

    JLabel lblService = new JLabel();
    JLabel lblLogin = new JLabel();
    JLabel lblUrl = new JLabel();
    JLabel lblCategorie = new JLabel();

    JPanel panneauFormulaire;
    JPanel panneauGenerateur;

    JTextField champService = new JTextField(20);
    JTextField champLogin = new JTextField(20);
    JPasswordField champMotDePasse = new JPasswordField(20);
    JTextField champUrl = new JTextField(20);
    JComboBox<Category> boiteCategorie = new JComboBox<>();
    JButton boutonSauvegarder = new JButton();

    Credential identifiantActuel = null;
    JSplitPane separationPrincipale;

    static Map<String, String> NOMS = new HashMap<>();
    static List<String> ADJECTIFS_M = new ArrayList<>();
    static List<String> VERBES = new ArrayList<>();
    static List<String> DETERMINANTS_M = new ArrayList<>();
    static List<String> DETERMINANTS_F = new ArrayList<>();
    static SecureRandom ALEATOIRE = new SecureRandom();

    static String CHIFFRES = "0123456789";
    static String SYMBOLES = "!@#$%&*()-_=+[]";

    public MainFrame(Admin admin, char[] mdpMaitre, JFrame fenetreLogin) {
        this.adminActuel = admin;
        this.motDePasseMaitre = mdpMaitre;
        this.selAdmin = Base64.getDecoder().decode(admin.getSalt());
        this.fenetreLogin = fenetreLogin;

        String[] colonnes = {"ID", "Service", "Identifiant", "Categorie"};
        this.modeleTableau = new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        this.tableau = new JTable(modeleTableau);
        this.trieur = new TableRowSorter<>(modeleTableau);
        this.modeleListeCategories = new DefaultListModel<>();
        this.listeCategories = new JList<>(modeleListeCategories);

        chargerListesMots();

        this.conteneurBas = new JPanel(layoutCartesBas);

        this.panneauDetails = creerPanneauDetails();

        this.panneauFormulaire = creerPanneauFormulaire();
        this.panneauGenerateur = creerPanneauGenerateur();

        this.panneauEditeurDivise = new JPanel(new BorderLayout());
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panneauFormulaire, panneauGenerateur);
        split.setResizeWeight(0.5);
        this.panneauEditeurDivise.add(split, BorderLayout.CENTER);

        setTitle("SecurIG2I - " + adminActuel.getUsername());
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initialiserInterface();
        chargerCategories();
        chargerIdentifiants();
    }

    private void initialiserInterface() {
        setLayout(new BorderLayout(5, 5));
        add(creerBarreOutils(), BorderLayout.NORTH);

        conteneurBas.add(panneauDetails, "DETAILS");
        conteneurBas.add(panneauEditeurDivise, "EDITEUR");

        JPanel panneauPrincipalIdentifiants = creerPanneauIdentifiants();
        this.separationPrincipale = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panneauPrincipalIdentifiants, conteneurBas);
        separationPrincipale.setResizeWeight(0.7);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, creerPanneauCategorie(), separationPrincipale);
        splitPane.setDividerLocation(220);
        add(splitPane, BorderLayout.CENTER);

        JPanel barreStatut = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barreStatut.add(labelStats);
        add(barreStatut, BorderLayout.SOUTH);

        tableau.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    selectionnerIdentifiant();
                }
            }
        });

        rendreBasVisible(false);
    }

    private JToolBar creerBarreOutils() {
        JToolBar barre = new JToolBar();
        barre.setFloatable(false);
        barre.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JButton btnAjouter = new JButton("Ajouter");
        btnAjouter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ajouterNouveau();
            }
        });
        barre.add(btnAjouter);

        JButton btnSupprimer = new JButton("Supprimer");
        btnSupprimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                supprimer();
            }
        });
        barre.add(btnSupprimer);

        barre.add(Box.createHorizontalGlue());

        JButton btnVerrouiller = new JButton("Verrouiller");
        btnVerrouiller.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seDeconnecter();
            }
        });
        barre.add(btnVerrouiller);

        return barre;
    }

    private JPanel creerPanneauDetails() {
        JPanel panneau = new JPanel(new BorderLayout(10, 10));
        panneau.setBorder(BorderFactory.createTitledBorder("Informations de l'identifiant"));

        JPanel grilleInfo = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        Font policeLabel = new Font("Segoe UI", Font.BOLD, 14);
        Font policeValeur = new Font("Segoe UI", Font.PLAIN, 14);

        gbc.gridy = 0;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel l1 = new JLabel("Service :"); l1.setFont(policeLabel);
        grilleInfo.add(l1, gbc);

        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        lblService.setFont(policeValeur);
        grilleInfo.add(lblService, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel l2 = new JLabel("Identifiant :"); l2.setFont(policeLabel);
        grilleInfo.add(l2, gbc);

        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        lblLogin.setFont(policeValeur);
        grilleInfo.add(lblLogin, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel l3 = new JLabel("URL :"); l3.setFont(policeLabel);
        grilleInfo.add(l3, gbc);

        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        lblUrl.setFont(policeValeur);
        grilleInfo.add(lblUrl, gbc);
        gbc.gridy = 3;
        gbc.gridx = 0; gbc.anchor = GridBagConstraints.EAST;
        JLabel l4 = new JLabel("Categorie :"); l4.setFont(policeLabel);
        grilleInfo.add(l4, gbc);

        gbc.gridx = 1; gbc.anchor = GridBagConstraints.WEST;
        lblCategorie.setFont(policeValeur);
        grilleInfo.add(lblCategorie, gbc);

        JPanel panneauActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        JButton btnCopieMdp = new JButton("Copier le mot de passe");
        JButton btnCopieUrl = new JButton("Copier l'URL");
        JButton btnOuvrirUrl = new JButton("Ouvrir le lien");

        btnCopieMdp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copierMotDePasse();
            }
        });

        btnCopieUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!lblUrl.getText().equals("-")) {
                    copierDansPressePapiers(lblUrl.getText(), "URL");
                }
            }
        });

        btnOuvrirUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ouvrirLien();
            }
        });

        panneauActions.add(btnCopieMdp);
        panneauActions.add(btnCopieUrl);
        panneauActions.add(btnOuvrirUrl);

        gbc.gridy = 4; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        grilleInfo.add(panneauActions, gbc);

        JPanel barreBas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnModifier = new JButton("Modifier");
        JButton btnFermer = new JButton("Fermer");

        btnModifier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passerEnEdition();
            }
        });

        btnFermer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                rendreBasVisible(false);
            }
        });

        barreBas.add(btnModifier);
        barreBas.add(btnFermer);

        panneau.add(grilleInfo, BorderLayout.CENTER);
        panneau.add(barreBas, BorderLayout.SOUTH);

        return panneau;
    }

    private JPanel creerPanneauGenerateur() {
        JPanel panneau = new JPanel(new BorderLayout(10, 10));
        panneau.setBorder(BorderFactory.createTitledBorder("Generateur de Mot de Passe Hybride"));

        JPanel panneauPhrase = new JPanel(new BorderLayout(5, 5));
        JTextField champPhrase = new JTextField();
        champPhrase.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JButton btnGenererPhrase = new JButton("Generer Phrase");
        panneauPhrase.add(new JLabel("1. Phrase de base (editable) :"), BorderLayout.NORTH);
        panneauPhrase.add(champPhrase, BorderLayout.CENTER);
        panneauPhrase.add(btnGenererPhrase, BorderLayout.EAST);

        JPanel panneauComplexite = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        panneauComplexite.setBorder(BorderFactory.createTitledBorder("2. Parametres de Complexite"));

        panneauComplexite.add(new JLabel("Majuscules:"));
        JSpinner spinMaj = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));
        panneauComplexite.add(spinMaj);

        panneauComplexite.add(Box.createHorizontalStrut(15));

        panneauComplexite.add(new JLabel("Chiffres:"));
        JSpinner spinChiffres = new JSpinner(new SpinnerNumberModel(2, 0, 20, 1));
        panneauComplexite.add(spinChiffres);

        panneauComplexite.add(Box.createHorizontalStrut(15));

        panneauComplexite.add(new JLabel("Symboles:"));
        JSpinner spinSymboles = new JSpinner(new SpinnerNumberModel(1, 0, 20, 1));
        panneauComplexite.add(spinSymboles);

        JPanel panneauResultat = new JPanel(new BorderLayout(5, 5));
        JTextField champResultat = new JTextField();
        champResultat.setEditable(false);
        champResultat.setFont(new Font("Monospaced", Font.BOLD, 16));

        JButton btnAppliquer = new JButton("Appliquer & Ameliorer");
        panneauResultat.add(new JLabel("3. Mot de passe final :"), BorderLayout.NORTH);
        panneauResultat.add(champResultat, BorderLayout.CENTER);
        panneauResultat.add(btnAppliquer, BorderLayout.EAST);

        btnGenererPhrase.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                List<String> clesNoms = new ArrayList<>(NOMS.keySet());
                String nom = clesNoms.get(ALEATOIRE.nextInt(clesNoms.size()));
                String genre = NOMS.get(nom);

                String adj = ADJECTIFS_M.get(ALEATOIRE.nextInt(ADJECTIFS_M.size()));
                if ("f".equals(genre) && !adj.endsWith("e")) {
                    adj += "e";
                }

                String determinant;
                if ("f".equals(genre)) {
                    determinant = DETERMINANTS_F.get(ALEATOIRE.nextInt(DETERMINANTS_F.size()));
                } else {
                    determinant = DETERMINANTS_M.get(ALEATOIRE.nextInt(DETERMINANTS_M.size()));
                }

                String verbe = VERBES.get(ALEATOIRE.nextInt(VERBES.size()));
                champPhrase.setText(determinant + "-" + nom + "-" + adj + "-" + verbe);
                champResultat.setText("");
            }
        });

        btnAppliquer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String phraseBase = champPhrase.getText();
                if (phraseBase.isEmpty()) {
                    JOptionPane.showMessageDialog(panneau, "Veuillez d'abord generer une phrase de base.");
                    return;
                }

                StringBuilder sb = new StringBuilder(phraseBase);
                int nbMaj = (int) spinMaj.getValue();
                int nbChiffres = (int) spinChiffres.getValue();
                int nbSymboles = (int) spinSymboles.getValue();

                List<Integer> indices = new ArrayList<>();
                for (int i = 0; i < sb.length(); i++) {
                    if (Character.isLetterOrDigit(sb.charAt(i))) {
                        indices.add(i);
                    }
                }
                Collections.shuffle(indices);

                if (nbMaj + nbChiffres + nbSymboles > indices.size()) {
                    JOptionPane.showMessageDialog(panneau, "Le nombre total de modifications depasse la longueur de la phrase.");
                    return;
                }

                int compteur = 0;
                for (int i = 0; i < nbMaj; i++) {
                    int index = indices.get(compteur++);
                    sb.setCharAt(index, Character.toUpperCase(sb.charAt(index)));
                }
                for (int i = 0; i < nbChiffres; i++) {
                    int index = indices.get(compteur++);
                    sb.setCharAt(index, CHIFFRES.charAt(ALEATOIRE.nextInt(CHIFFRES.length())));
                }
                for (int i = 0; i < nbSymboles; i++) {
                    int index = indices.get(compteur++);
                    sb.setCharAt(index, SYMBOLES.charAt(ALEATOIRE.nextInt(SYMBOLES.length())));
                }
                champResultat.setText(sb.toString());
            }
        });

        JPanel panneauControle = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnUtiliser = new JButton("Utiliser le Mot de Passe Final");

        btnUtiliser.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!champResultat.getText().isEmpty()) {
                    champMotDePasse.setText(champResultat.getText());
                }
            }
        });

        panneauControle.add(btnUtiliser);

        JPanel panneauCentral = new JPanel();
        panneauCentral.setLayout(new BoxLayout(panneauCentral, BoxLayout.PAGE_AXIS));
        panneauCentral.add(panneauPhrase);
        panneauCentral.add(Box.createRigidArea(new Dimension(0, 10)));
        panneauCentral.add(panneauComplexite);
        panneauCentral.add(Box.createRigidArea(new Dimension(0, 10)));
        panneauCentral.add(panneauResultat);
        panneauCentral.add(Box.createVerticalGlue());

        panneau.add(panneauCentral, BorderLayout.CENTER);
        panneau.add(panneauControle, BorderLayout.SOUTH);

        return panneau;
    }

    private void copierDansPressePapiers(String texte, String type) {
        StringSelection selection = new StringSelection(texte);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);

        String texteOriginal = labelStats.getText();
        labelStats.setText(type + " copie(e) dans le presse-papiers !");
        Timer timer = new Timer(3000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                labelStats.setText(texteOriginal);
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void ouvrirLien() {
        String texteUrl = lblUrl.getText();
        if (texteUrl == null || texteUrl.isEmpty() || texteUrl.equals("-")) {
            return;
        }

        if (!texteUrl.startsWith("http://") && !texteUrl.startsWith("https://")) {
            texteUrl = "https://" + texteUrl;
        }

        try {
            Desktop.getDesktop().browse(new URI(texteUrl));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Impossible d'ouvrir le lien : " + ex.getMessage());
        }
    }

    private static void chargerListesMots() {
        if (NOMS.isEmpty()) {
            List<String> lignes = chargerFichier("/nouns_mf.txt");
            for(String ligne : lignes) {
                String[] parts = ligne.split(":");
                if (parts.length == 2) NOMS.put(parts[0], parts[1]);
            }
        }
        if (ADJECTIFS_M.isEmpty()) ADJECTIFS_M.addAll(chargerFichier("/adjectives_m.txt"));
        if (VERBES.isEmpty()) VERBES.addAll(chargerFichier("/verbs_conjugated.txt"));
        if (DETERMINANTS_M.isEmpty()) DETERMINANTS_M.addAll(chargerFichier("/determinants_m.txt"));
        if (DETERMINANTS_F.isEmpty()) DETERMINANTS_F.addAll(chargerFichier("/determinants_f.txt"));
    }

    private static List<String> chargerFichier(String cheminRessource) {
        List<String> lignes = new ArrayList<>();
        try {
            InputStream is = MainFrame.class.getResourceAsStream(cheminRessource);
            if(is == null) return lignes;

            BufferedReader lecteur = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String ligne;
            while ((ligne = lecteur.readLine()) != null) {
                lignes.add(ligne);
            }
            lecteur.close();
        } catch (Exception e) {
            System.err.println("Erreur de chargement du fichier : " + cheminRessource);
        }
        return lignes;
    }

    private JPanel creerPanneauFormulaire() {
        JPanel panneau = new JPanel(new BorderLayout(10, 10));
        panneau.setBorder(BorderFactory.createTitledBorder("Edition de l'identifiant"));

        JPanel grille = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; grille.add(new JLabel("Service:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; grille.add(champService, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; grille.add(new JLabel("Identifiant:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; grille.add(champLogin, gbc);

        JPanel panneauUrl = new JPanel(new BorderLayout(5, 5));
        panneauUrl.add(champUrl, BorderLayout.CENTER);
        JButton btnColler = new JButton("Coller");

        btnColler.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                    if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                        champUrl.setText(text);
                    }
                } catch (Exception ex) { }
            }
        });

        panneauUrl.add(btnColler, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 3; grille.add(new JLabel("URL:"), gbc);
        gbc.gridx = 1; grille.add(panneauUrl, gbc);

        gbc.gridx = 0; gbc.gridy = 4; grille.add(new JLabel("Categorie:"), gbc);
        gbc.gridx = 1; grille.add(boiteCategorie, gbc);

        JPanel panneauMdp = new JPanel(new BorderLayout(5, 5));
        panneauMdp.add(champMotDePasse, BorderLayout.CENTER);
        JButton btnCopier = new JButton("Copier");

        btnCopier.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copierMotDePasse();
            }
        });
        panneauMdp.add(btnCopier, BorderLayout.EAST);

        gbc.gridx = 0; gbc.gridy = 2; grille.add(new JLabel("Mot de passe:"), gbc);
        gbc.gridx = 1; grille.add(panneauMdp, gbc);

        JPanel panneauBoutons = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        boutonSauvegarder.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sauvegarder();
            }
        });

        JButton btnAnnuler = new JButton("Annuler");
        btnAnnuler.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                annulerEdition();
            }
        });

        panneauBoutons.add(btnAnnuler);
        panneauBoutons.add(boutonSauvegarder);

        panneau.add(grille, BorderLayout.CENTER);
        panneau.add(panneauBoutons, BorderLayout.SOUTH);

        return panneau;
    }

    private void selectionnerIdentifiant() {
        int rangeeSelectionnee = tableau.getSelectedRow();
        if (rangeeSelectionnee == -1) {
            rendreBasVisible(false);
            return;
        }
        Long id = (Long) modeleTableau.getValueAt(tableau.convertRowIndexToModel(rangeeSelectionnee), 0);
        this.identifiantActuel = daoIdentifiant.findById(id);

        lblService.setText(identifiantActuel.getServiceName());
        lblLogin.setText(identifiantActuel.getLogin());

        if (identifiantActuel.getUrl() == null || identifiantActuel.getUrl().isEmpty()) {
            lblUrl.setText("-");
        } else {
            lblUrl.setText(identifiantActuel.getUrl());
        }

        if (identifiantActuel.getCategory() != null) {
            lblCategorie.setText(identifiantActuel.getCategory().getName());
        } else {
            lblCategorie.setText("Aucune");
        }

        layoutCartesBas.show(conteneurBas, "DETAILS");
        rendreBasVisible(true);
    }

    private void passerEnEdition() {
        if (identifiantActuel == null) return;

        champService.setText(identifiantActuel.getServiceName());
        champLogin.setText(identifiantActuel.getLogin());
        champUrl.setText(identifiantActuel.getUrl());
        champMotDePasse.setText("");
        champMotDePasse.setToolTipText("Laissez vide pour ne pas changer");

        mettreAJourBoiteCategorie();
        if (identifiantActuel.getCategory() != null) {
            boiteCategorie.setSelectedItem(identifiantActuel.getCategory());
        } else {
            boiteCategorie.setSelectedIndex(-1);
        }

        boutonSauvegarder.setText("Enregistrer");
        layoutCartesBas.show(conteneurBas, "EDITEUR");
    }

    private void annulerEdition() {
        if (identifiantActuel != null) {
            layoutCartesBas.show(conteneurBas, "DETAILS");
        } else {
            rendreBasVisible(false);
        }
    }

    private void ajouterNouveau() {
        tableau.clearSelection();
        this.identifiantActuel = null;

        champService.setText("");
        champLogin.setText("");
        champMotDePasse.setText("");
        champMotDePasse.setToolTipText(null);
        champUrl.setText("");
        mettreAJourBoiteCategorie();

        boutonSauvegarder.setText("Creer");

        layoutCartesBas.show(conteneurBas, "EDITEUR");
        rendreBasVisible(true);
        champService.requestFocusInWindow();
    }

    private void sauvegarder() {
        boolean estEdition = (identifiantActuel != null);
        Credential aSauver;
        if (estEdition) {
            aSauver = identifiantActuel;
        } else {
            aSauver = new Credential();
        }

        if (champService.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Le nom du service est obligatoire.", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        aSauver.setAdmin(adminActuel);
        aSauver.setServiceName(champService.getText().trim());
        aSauver.setLogin(champLogin.getText().trim());
        aSauver.setUrl(champUrl.getText().trim());
        aSauver.setCategory((Category) boiteCategorie.getSelectedItem());

        try {
            if (champMotDePasse.getPassword().length > 0) {
                CryptoUtil.Encrypted enc = CryptoUtil.encrypt(motDePasseMaitre, new String(champMotDePasse.getPassword()), selAdmin);
                aSauver.setEncryptedPassword(enc.ciphertextBase64);
                aSauver.setIv(enc.ivBase64);
            } else if (!estEdition) {
                JOptionPane.showMessageDialog(this, "Un mot de passe est requis pour un nouvel identifiant.", "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (estEdition) {
                daoIdentifiant.update(aSauver);
            } else {
                daoIdentifiant.save(aSauver);
            }

            chargerIdentifiants();

            if (estEdition) {
                selectionnerIdentifiant();
            } else {
                rendreBasVisible(false);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur de sauvegarde: " + ex.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void rendreBasVisible(boolean visible) {
        if (!visible) {
            tableau.clearSelection();
            identifiantActuel = null;
        }
        conteneurBas.setVisible(visible);

        if (visible) {
            SwingUtilities.invokeLater(() -> separationPrincipale.setDividerLocation(0.60));
        }

        separationPrincipale.revalidate();
        separationPrincipale.repaint();
    }

    private void copierMotDePasse() {
        if (identifiantActuel == null) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner un identifiant.");
            return;
        }
        try {
            String dechiffre = CryptoUtil.decrypt(motDePasseMaitre, identifiantActuel.getEncryptedPassword(), selAdmin, identifiantActuel.getIv());
            copierDansPressePapiers(dechiffre, "Mot de passe pour '" + identifiantActuel.getServiceName() + "'");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erreur de dechiffrement.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mettreAJourBoiteCategorie() {
        Object selection = boiteCategorie.getSelectedItem();
        boiteCategorie.removeAllItems();
        List<Category> categories = daoCategorie.findByAdmin(adminActuel);
        for (Category c : categories) {
            boiteCategorie.addItem(c);
        }
        if (selection != null) {
            boiteCategorie.setSelectedItem(selection);
        }
    }

    private JPanel creerPanneauCategorie() {
        JPanel panneau = new JPanel(new BorderLayout());
        listeCategories.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        listeCategories.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            @Override
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {
                appliquerFiltres();
            }
        });

        panneau.add(new JScrollPane(listeCategories), BorderLayout.CENTER);

        JPanel panneauGestion = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton btnAjouter = new JButton("+");
        JButton btnSupprimer = new JButton("-");

        btnAjouter.setToolTipText("Ajouter une categorie");
        btnSupprimer.setToolTipText("Supprimer la categorie selectionnee");

        btnAjouter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ajouterCategorie();
            }
        });

        btnSupprimer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                supprimerCategorie();
            }
        });

        panneauGestion.add(btnAjouter);
        panneauGestion.add(btnSupprimer);
        panneau.add(panneauGestion, BorderLayout.SOUTH);

        return panneau;
    }

    private void ajouterCategorie() {
        String nom = JOptionPane.showInputDialog(this, "Nom de la nouvelle categorie:", "Ajouter une categorie", JOptionPane.PLAIN_MESSAGE);
        if (nom != null && !nom.trim().isEmpty()) {
            Category nouvelleCat = new Category(nom, "");
            nouvelleCat.setAdmin(adminActuel);
            daoCategorie.save(nouvelleCat);
            chargerCategories();
        }
    }

    private void supprimerCategorie() {
        Category selection = listeCategories.getSelectedValue();
        if (selection == null || selection.getId() == null) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner une categorie a supprimer dans la liste.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Supprimer la categorie '" + selection.getName() + "' ?\nLes identifiants associes ne seront plus categorises.", "Confirmation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            List<Credential> identifiants = daoIdentifiant.findByAdmin(adminActuel);
            for (Credential c : identifiants) {
                if (c.getCategory() != null && c.getCategory().getId().equals(selection.getId())) {
                    c.setCategory(null);
                    daoIdentifiant.update(c);
                }
            }
            daoCategorie.delete(selection);
            chargerCategories();
            chargerIdentifiants();
        }
    }

    private JPanel creerPanneauIdentifiants() {
        JPanel panneau = new JPanel(new BorderLayout(5, 5));
        tableau.setRowSorter(trieur);
        tableau.setFillsViewportHeight(true);
        if (tableau.getColumnModel().getColumnCount() > 0) {
            tableau.removeColumn(tableau.getColumnModel().getColumn(0));
        }

        champRecherche.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { appliquerFiltres(); }
            @Override public void removeUpdate(DocumentEvent e) { appliquerFiltres(); }
            @Override public void changedUpdate(DocumentEvent e) { appliquerFiltres(); }
        });

        JPanel panneauRecherche = new JPanel(new BorderLayout());
        panneauRecherche.add(new JLabel("Rechercher: "), BorderLayout.WEST);
        panneauRecherche.add(champRecherche, BorderLayout.CENTER);
        panneau.add(panneauRecherche, BorderLayout.NORTH);
        panneau.add(new JScrollPane(tableau), BorderLayout.CENTER);
        return panneau;
    }

    private void chargerCategories() {
        modeleListeCategories.clear();
        Category tout = new Category();
        tout.setName("Toutes les categories");
        modeleListeCategories.addElement(tout);
        List<Category> categories = daoCategorie.findByAdmin(adminActuel);
        for (Category c : categories) {
            modeleListeCategories.addElement(c);
        }
        listeCategories.setSelectedIndex(0);
    }

    private void chargerIdentifiants() {
        modeleTableau.setRowCount(0);
        List<Credential> credentials = daoIdentifiant.findByAdmin(adminActuel);
        for (Credential c : credentials) {
            String nomCat = "";
            if (c.getCategory() != null) {
                nomCat = c.getCategory().getName();
            }
            modeleTableau.addRow(new Object[]{c.getId(), c.getServiceName(), c.getLogin(), nomCat});
        }
        labelStats.setText("Total d'identifiants : " + credentials.size());
    }

    private void appliquerFiltres() {
        String texte = champRecherche.getText();
        Category categorieSelectionnee = listeCategories.getSelectedValue();
        List<RowFilter<Object, Object>> filtres = new ArrayList<>();

        if (texte != null && !texte.trim().isEmpty()) {
            filtres.add(RowFilter.regexFilter("(?i)" + texte, 1, 2));
        }
        if (categorieSelectionnee != null && categorieSelectionnee.getId() != null) {
            filtres.add(RowFilter.regexFilter("^" + Pattern.quote(categorieSelectionnee.getName()) + "$", 3));
        }
        trieur.setRowFilter(RowFilter.andFilter(filtres));
    }

    private void supprimer() {
        int rangeeSelectionnee = tableau.getSelectedRow();
        if (rangeeSelectionnee == -1) {
            JOptionPane.showMessageDialog(this, "Veuillez selectionner un identifiant a supprimer.");
            return;
        }
        Long id = (Long) modeleTableau.getValueAt(tableau.convertRowIndexToModel(rangeeSelectionnee), 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Confirmer la suppression ?", "Suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            daoIdentifiant.deleteById(id);
            chargerIdentifiants();
            rendreBasVisible(false);
        }
    }

    private void seDeconnecter() {
        this.dispose();
        fenetreLogin.setVisible(true);
    }
}