package com.belamila.ui;

import com.belamila.model.Package;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * Created by: Bartosz Nawrot
 * Date: 13.08.2020
 * Description:
 */
public class PackageListViewCell extends ListCell<Package> {

    private static final Logger logger = LoggerFactory.getLogger(PackageListViewCell.class);

    @FXML
    private Label id;
    @FXML
    private Label receiver;
    @FXML
    private Label service;
    @FXML
    private TextField inpostId;

    @FXML
    private GridPane gridPane;

    private FXMLLoader mLLoader;

    @SneakyThrows
    @Override
    protected void updateItem(Package p, boolean empty) {
        super.updateItem(p, empty);
        if(empty || p == null) {
            setText(null);
            setGraphic(null);

        } else {
            logger.info("PackageListViewCell.updateItem: {}", p);

            if (mLLoader == null) {
                URL url = new File("src/com/belamila/ui/fxml/list_cell.fxml").toURI().toURL();
                mLLoader = new FXMLLoader(url);
                mLLoader.setController(this);

                try {
                    mLLoader.load();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            id.setText(p.getId());
            receiver.setText(p.getReceiver());
            service.setText(p.getService());

            if (p.getService().equals("DPD Classic")) {
                inpostId.setVisible(false);
            }

            setText(null);
            setGraphic(gridPane);
        }
    }
}
