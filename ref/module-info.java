module org.marrok.gstockdz {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires com.dlsc.gemsfx;
    requires org.kordamp.ikonli.core;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.kordamp.ikonli.fontawesome;
    requires org.kordamp.ikonli.typicons;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires java.sql;
    requires javafx.swing;
    requires com.google.zxing;
    requires com.google.zxing.javase;
    requires jasperreports;
    requires jasperreports.pdf;
    requires jasperreports.excel.poi;
    requires com.zaxxer.hikari;
    requires atlantafx.base;
    // requires xmlgraphics.commons;

    requires fr.brouillard.oss.cssfx;
    requires java.desktop;
    // needed for jasper date format to not crash
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires org.apache.commons.net;
    requires ch.vorburger.mariadb4j;
    requires TafqeetJ;

    opens org.marrok.gstockdz to javafx.fxml;

    exports org.marrok.gstockdz;

    exports org.marrok.gstockdz.controller.login;

    opens org.marrok.gstockdz.controller.login to javafx.fxml;

    exports org.marrok.gstockdz.controller.stock_dashboard;

    opens org.marrok.gstockdz.controller.stock_dashboard to javafx.fxml;

    exports org.marrok.gstockdz.controller.enterprise_info;

    opens org.marrok.gstockdz.controller.enterprise_info to javafx.fxml;

    exports org.marrok.gstockdz.controller.article;

    opens org.marrok.gstockdz.controller.article to javafx.fxml;

    exports org.marrok.gstockdz.controller.bon_sortie;

    opens org.marrok.gstockdz.controller.bon_sortie to javafx.fxml;

    exports org.marrok.gstockdz.controller.bon_entree;

    opens org.marrok.gstockdz.controller.bon_entree to javafx.fxml;

    exports org.marrok.gstockdz.controller.settings;

    opens org.marrok.gstockdz.controller.settings to javafx.fxml;

    exports org.marrok.gstockdz.controller.category;

    opens org.marrok.gstockdz.controller.category to javafx.fxml;

    exports org.marrok.gstockdz.controller.employer;

    opens org.marrok.gstockdz.controller.employer to javafx.fxml;

    exports org.marrok.gstockdz.controller.service;

    opens org.marrok.gstockdz.controller.service to javafx.fxml;

    exports org.marrok.gstockdz.controller.bon_retour;

    opens org.marrok.gstockdz.controller.bon_retour to javafx.fxml;

    exports org.marrok.gstockdz.controller.bon_commande;

    opens org.marrok.gstockdz.controller.bon_commande to javafx.fxml;

    exports org.marrok.gstockdz.controller.location;

    opens org.marrok.gstockdz.controller.location to javafx.fxml;

    exports org.marrok.gstockdz.controller.fournisseur;

    opens org.marrok.gstockdz.controller.fournisseur to javafx.fxml;

    exports org.marrok.gstockdz.controller.inventaire;

    opens org.marrok.gstockdz.controller.inventaire to javafx.fxml;

    exports org.marrok.gstockdz.controller.users;

    opens org.marrok.gstockdz.controller.users to javafx.fxml;

    exports org.marrok.gstockdz.controller.license;

    opens org.marrok.gstockdz.controller.license to javafx.fxml;

    exports org.marrok.gstockdz.util;

    opens org.marrok.gstockdz.util to javafx.fxml;

    exports org.marrok.gstockdz.controller;

    opens org.marrok.gstockdz.controller to javafx.fxml;

    exports org.marrok.gstockdz.util.app_security;

    opens org.marrok.gstockdz.util.app_security to javafx.fxml;

    exports org.marrok.gstockdz.models;

    opens org.marrok.gstockdz.models to javafx.base, javafx.fxml;

    exports org.marrok.gstockdz.models.CustomModels;

    opens org.marrok.gstockdz.models.CustomModels to javafx.base, javafx.fxml;

    exports org.marrok.gstockdz.util.config;

    opens org.marrok.gstockdz.util.config to javafx.fxml;

    exports org.marrok.gstockdz.controller.shared;

    opens org.marrok.gstockdz.controller.shared to javafx.fxml;

}