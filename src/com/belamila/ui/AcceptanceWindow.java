package com.belamila.ui;

import com.belamila.model.Package;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Created by: Bartosz Nawrot
 * Date: 22.07.2020
 * Description:
 */
public class AcceptanceWindow implements Initializable {

    public enum Result { EXCEL, WEB_API }

    private static final Logger logger = LoggerFactory.getLogger(AcceptanceWindow.class);

    @FXML
    private ListView<Package> listView;

    @FXML
    private Button buttonExcel;

    @FXML
    private Button buttonWebApi;

    private ObservableList<Package> packageObservableList;

    @Setter
    private List<Package> packages;

    private static final Boolean condition = true;
    private static Result result;
    private static List<Package> packagesResult;

    public AcceptanceWindow() {
        packageObservableList = FXCollections.observableArrayList();
    }

    public static Result verify(List<Package> packages) throws Exception {
        Platform.runLater(() -> {
            try {
                URL url = new File("src/com/belamila/ui/fxml/acceptance_window.fxml").toURI().toURL();
                FXMLLoader fxmlLoader = new FXMLLoader(url);
                Parent root = fxmlLoader.load();
                AcceptanceWindow controller = fxmlLoader.getController();
                controller.setPackages(packages);
                Scene scene = new Scene(root);
                Stage window = new Stage();
                window.setScene(scene);
                window.show();

            } catch (IOException e) {
                logger.info("", e);
            }
        });

        try {
            synchronized (condition) {
                condition.wait();
            }
        } catch (InterruptedException ignored) {};

        if (result == null) {
            throw new RuntimeException("Acceptance window closed without result!");
        }

        packages.clear();
        packages.addAll(packagesResult);
        return result;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buttonWebApi.setOnAction(event -> onFinished(Result.WEB_API));
        buttonExcel.setOnAction(event -> onFinished(Result.EXCEL));
        Platform.runLater(() -> {
            logger.info("Initialize: {}", packages);
            packageObservableList.addAll(packages);
            listView.setItems(packageObservableList);
            listView.setCellFactory(studentListView ->
                    new PackageListViewCell(packageObservableList));
        });
    }

    private void onFinished(Result excel) {
        result = excel;
        packagesResult = new ArrayList<>(packageObservableList);
        synchronized (condition) {
            condition.notify();
        }
    }
}
