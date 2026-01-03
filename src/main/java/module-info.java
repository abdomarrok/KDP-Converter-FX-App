module com.boilerplate.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires jdk.jsobject;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.fontawesome5;
    requires org.apache.logging.log4j.core;
    requires org.apache.logging.log4j;
    requires atlantafx.base;
    requires jasperreports;
    requires jasperreports.pdf;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.xml;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires ch.vorburger.mariadb4j;
    requires org.mariadb.jdbc;
    requires org.slf4j;
    requires java.desktop;

    opens com.boilerplate.app.util.database to com.zaxxer.hikari;

    opens com.boilerplate.app to javafx.fxml;

    exports com.boilerplate.app;

    exports com.boilerplate.app.controller;

    opens com.boilerplate.app.controller to javafx.fxml;

    exports com.boilerplate.app.model;

    opens com.boilerplate.app.model to javafx.base; // Allow PropertyValueFactory

    opens com.boilerplate.app.service to javafx.web; // Allow JavaBridge access
}
