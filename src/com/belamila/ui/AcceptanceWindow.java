package com.belamila.ui;

import com.belamila.backend.webapi.InPostWebApi;
import com.belamila.model.Package;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
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

import static com.belamila.model.Package.InpostStatus.DONE_INVALID;
import static com.belamila.model.Package.InpostStatus.DONE_VALID;

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
    private Button buttonWebApi;

    private final ObservableList<Package> packageObservableList;

    @Setter
    private List<Package> packages;

    @Setter
    private InPostWebApi inPostWebApi;

    private static final Boolean condition = true;
    private static Exception exception;
    private static Result result;
    private static List<Package> packagesResult;

    private static Stage window;

    public AcceptanceWindow() {
        packageObservableList = FXCollections.observableArrayList(
                pack -> new Observable[] { pack.getInpostStatus() });
    }

    public static Result verify(List<Package> packages, InPostWebApi inPostWebApi) throws Exception {
        Platform.runLater(() -> {
            exception = null;
            result = null;
            packagesResult = null;
            try {
                URL url = new File("src/fxml/acceptance_window.fxml").toURI().toURL();
                FXMLLoader fxmlLoader = new FXMLLoader(url);
                Parent root = fxmlLoader.load();
                AcceptanceWindow controller = fxmlLoader.getController();
                controller.setPackages(packages);
                controller.setInPostWebApi(inPostWebApi);
                Scene scene = new Scene(root);
                window = new Stage();
                window.setScene(scene);
                window.setTitle("Podsumowanie");
                window.setOnCloseRequest(event -> close());
                window.show();

            } catch (IOException e) {
                logger.warn("", e);
                exception = e;
                synchronized (condition) {
                    condition.notify();
                }
            }
        });

        try {
            synchronized (condition) {
                condition.wait();
            }
        } catch (InterruptedException ignored) {}

        Platform.runLater(() -> window.close());

        if (exception != null) {
            throw exception;
        }

        if (result == null) {
            throw new RuntimeException("Szkoda, że nie nadajemy");
        }

        packages.clear();
        packages.addAll(packagesResult);
        return result;
    }

    public static void close() {
        synchronized (condition) {
            condition.notify();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        buttonWebApi.setOnAction(event -> onFinished(Result.WEB_API));
        Platform.runLater(() -> {
            logger.info("Initialize: {}", packages);
            packages.forEach(p -> p.setInpostStatus(
                    new SimpleStringProperty(Package.InpostStatus.UNKNOWN.name())));
            packageObservableList.addAll(packages);
            listView.setItems(packageObservableList);
            listView.setCellFactory(studentListView ->
                    new com.belamila.ui.PackageListViewCell(packageObservableList));
        });
    }

    private void onFinished(Result res) {
        if (!validateInPost()) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "Wrong InPost ID", ButtonType.CLOSE);
            alert.showAndWait();
            return;
        }
        result = res;
        packagesResult = new ArrayList<>(packageObservableList);
        synchronized (condition) {
            condition.notify();
        }
    }

    private boolean validateInPost() {
        long failed = packageObservableList.stream()
                .filter(Package::isInPost) // InPost
                .filter(p -> !p.getInpostStatus().getValue().equals(DONE_VALID.name())) // Status not VALID
                .filter(p -> {
                    logger.info("InPost: {}", p.getId());
                    try {
                        inPostWebApi.verifyInPostId(p.getInPostId());
                        p.getInpostStatus().setValue(DONE_VALID.name());
                        return false;
                    } catch (IOException | RuntimeException e) {
                        logger.info("InPost validation failed: {}", e.getMessage());
                        p.getInpostStatus().setValue(DONE_INVALID.name());
                    }
                    return true;
                })
                .count();
        return failed == 0;
    }
}
