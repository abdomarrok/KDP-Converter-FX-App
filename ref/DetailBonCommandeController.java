package org.marrok.gstockdz.controller.bon_commande;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import io.github.osamabmaq.tafqeetj.converters.Tafqeet;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.view.JasperViewer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.marrok.gstockdz.models.BonCommande;
import org.marrok.gstockdz.models.CommandeItem;
import org.marrok.gstockdz.models.Fournisseur;
import org.marrok.gstockdz.util.GeneralUtil;
import org.marrok.gstockdz.util.database.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetailBonCommandeController {



    Logger logger = LogManager.getLogger(DetailBonCommandeController.class);

    @FXML
    private Label idLabel;
    @FXML
    private Label fournisseurIdLabel;
    @FXML
    private Label addressLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label documentNumLabel;
    public Label user_who_create;

    @FXML
    private TableView<CommandeItem> commandeTableView;
    public TableColumn<CommandeItem, String> articleColumn;
    public TableColumn<CommandeItem, Integer> quantityColumn;
    public TableColumn<CommandeItem, Double> priceColumn;
    public TableColumn<CommandeItem, Double> tvaRateColumn;
    public TableColumn<CommandeItem, Double> totalColumn;

    private BonCommande bonCommande;
    private Fournisseur currentFournisseur;
    private List<CommandeItem> itemList;

    private final FournisseurDbHelper fournisseurDbHelper = new FournisseurDbHelper();
    private final ArticleDbHelper articleDbHelper = new ArticleDbHelper();
    private final BonCommandeDbHelper bonCommandeDbHelper = new BonCommandeDbHelper();
    private final EntrpriseInfoDbHelper infoDbHelper = new EntrpriseInfoDbHelper();
    private final UserDbHelper udbhelper = new UserDbHelper();
    String userName = "";
    Map<String, Object> parameters = new HashMap<>();

    public DetailBonCommandeController() throws SQLException {}

    public void setBonCommande(BonCommande bonCommande) {
        logger.info("Set this BonCommande: " + bonCommande);
        this.bonCommande = bonCommande;
        currentFournisseur = fournisseurDbHelper.getFournisseurById(bonCommande.getIdFournisseur());
        populateDetails();
    }

    private void populateDetails() {
        if (bonCommande != null) {
            idLabel.setText(String.valueOf(bonCommande.getId()));
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
            dateLabel.setText(dateFormat.format(bonCommande.getDate()));
            documentNumLabel.setText(bonCommande.getNumero());
            userName = udbhelper.getUserNameById(bonCommande.getUser_id());
            user_who_create.setText(userName);

            // Fetch items
            itemList = bonCommandeDbHelper.getCommandeItemsByBonCommandeId(bonCommande.getId());
            commandeTableView.getItems().clear();
            commandeTableView.getItems().addAll(itemList);

            setupTableColumns();
        }

        if (currentFournisseur != null) {
            fournisseurIdLabel.setText(currentFournisseur.getName());
            addressLabel.setText(currentFournisseur.getAddress());
        }
    }

    private void setupTableColumns() {
        articleColumn.setCellValueFactory(cellData -> {
            int articleId = cellData.getValue().getIdArticle();
            String articleName = articleDbHelper.getArticleById(articleId).getName();
            return new SimpleStringProperty(articleName != null ? articleName : "Unknown Article");
        });

        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantite"));
        tvaRateColumn.setCellValueFactory(new PropertyValueFactory<>("tvaRate"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("prixUnitaire"));
        totalColumn.setCellValueFactory(new PropertyValueFactory<>("montant")); // quantity * unitPrice
    }

    @FXML
    private void handleEdit(ActionEvent event) {
        logger.info("Edit BonCommande clicked");
        // TODO: Open edit form
    }



    public void printBonCommande(ActionEvent event) {
        logger.info("printBonCommande");
        System.setProperty("jasper.reports.compile.temp", System.getProperty("java.io.tmpdir"));

        if (bonCommande == null) {
            GeneralUtil.showAlert(Alert.AlertType.WARNING, "تنبيه", "لا يوجد أمر شراء محدد للطباعة.");
            return;
        }

        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {

            // ✅ Load Jasper report safely
            InputStream reportStream = getClass().getResourceAsStream("/org/marrok/gstockdz/reports/Bon_Commande_Report.jrxml");
            if (reportStream == null) {
                logger.error("Report file not found: /org/marrok/gstockdz/reports/Bon_Commande_Report.jrxml");
                GeneralUtil.showAlert(Alert.AlertType.ERROR, "خطأ", "ملف التقرير غير موجود.");
                return;
            }

            // ✅ Company info
            InputStream logoStream = infoDbHelper.getLogo();
            String entrepriseName = infoDbHelper.getEnterpriseName();
            String ministryName = infoDbHelper.getMinistryName();
            String dairaName = infoDbHelper.getDairaName();

            // ✅ Barcode image
            BufferedImage barcodeImage = generateBarcodeImage(bonCommande.getNumero(), 150, 50);
            parameters.put("img_bar_code", barcodeImage);

            // ✅ Base parameters
            parameters.put("bon_commande_id", bonCommande.getId());
            if (logoStream != null) parameters.put("logo", ImageIO.read(logoStream));
            if (entrepriseName != null) parameters.put("entreprise_name", entrepriseName);
            if (ministryName != null) parameters.put("ministry_name", ministryName);
            if (dairaName != null) parameters.put("daira_name", dairaName);

            // ✅ Fetch items of this bon commande
            List<CommandeItem> items = bonCommandeDbHelper.getCommandeItemsByBonCommandeId(bonCommande.getId());

            double totalHT = 0.0;
            double totalTva9 = 0.0;
            double totalTva19 = 0.0;

            // ✅ Calculate per-rate totals
            for (CommandeItem item : items) {
                double montantHT = item.getQuantite() * item.getPrixUnitaire();
                totalHT += montantHT;

                if (item.getTvaRate() == 9.0) {
                    totalTva9 += montantHT * 0.09;
                } else if (item.getTvaRate() == 19.0) {
                    totalTva19 += montantHT * 0.19;
                }
            }

            double totalTVA = totalTva9 + totalTva19;
            double totalTTC = totalHT + totalTVA;

            // ✅ Add parameters for JasperReport
            parameters.put("total_ht", totalHT);
            parameters.put("total_tva9", totalTva9);
            parameters.put("total_tva19", totalTva19);
            parameters.put("total_tva", totalTVA);
            parameters.put("total_ttc", totalTTC);

            // ✅ Tafqeet (in words)
            try {
                long dinarPart = (long) totalTTC;
                int centPart = (int) Math.round((totalTTC - dinarPart) * 100);

                String tafqeetText;
                if (centPart > 0) {
                    tafqeetText = Tafqeet.getInstance().doTafqeet(dinarPart)
                            + " دينار و "
                            + Tafqeet.getInstance().doTafqeet(centPart)
                            + " سنتيم";
                } else {
                    tafqeetText = Tafqeet.getInstance().doTafqeet(dinarPart) + " دينار";
                }

                parameters.put("tafqeet_text", tafqeetText);
            } catch (Exception e) {
                logger.warn("Tafqeet conversion failed", e);
                parameters.put("tafqeet_text", "");
            }

            // ✅ Compile & show report
            JasperReport jasperReport = JasperCompileManager.compileReport(reportStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, connection);

            JasperViewer viewer = new JasperViewer(jasperPrint, false);
            viewer.setTitle("سند الطلب رقم " + bonCommande.getNumero());
            viewer.setVisible(true);

        } catch (SQLException sqlEx) {
            logger.error("SQL Error while printing BonCommande", sqlEx);
            GeneralUtil.showAlert(Alert.AlertType.ERROR, "خطأ في قاعدة البيانات", sqlEx.getMessage());
        } catch (Exception ex) {
            logger.error("Error generating BonCommande report", ex);
            GeneralUtil.showAlert(Alert.AlertType.ERROR, "خطأ", "حدث خطأ أثناء توليد التقرير: " + ex.getMessage());
        }
    }



    private BufferedImage generateBarcodeImage(String text, int width, int height) throws WriterException {
        BitMatrix bitMatrix = new com.google.zxing.MultiFormatWriter()
                .encode(text, BarcodeFormat.CODE_128, width, height);
        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }



}
