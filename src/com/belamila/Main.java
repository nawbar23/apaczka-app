package com.belamila;

import com.belamila.backend.excel.ExcelBuilder;
import com.belamila.backend.parser.Parser;
import com.belamila.backend.webapi.ApaczkaWebApi;
import com.belamila.model.Package;
import com.belamila.ui.AcceptanceWindow;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private Label summary;

    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("Drag and drop orders Wix CSV file");
        Label dropped = new Label("");
        summary = new Label("");
        VBox dragTarget = new VBox();
        dragTarget.getChildren().addAll(label, dropped, summary);
        dragTarget.setOnDragOver(event -> {
            if (event.getGestureSource() != dragTarget
                    && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });

        dragTarget.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;
            if (db.hasFiles()) {
                logger.info("Dropped event {}", db.getString());
                File file = db.getFiles().get(0);
                dropped.setText(file.toString());
                executorService.execute(() -> onDragHandle(file));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);

        Scene scene = new Scene(root, 500, 250);

        primaryStage.setTitle("Apaczka for Belamila :)");
        primaryStage.setScene(scene);
        primaryStage.resizableProperty().setValue(Boolean.FALSE);
        primaryStage.show();
    }

    private void onDragHandle(File file) {
        Alert.AlertType alertType;
        String message;
        try {
            message = executeLogic(file);
            alertType = Alert.AlertType.INFORMATION;
        } catch (Exception e) {
            logger.warn("", e);
            message = e.toString();
            alertType = Alert.AlertType.ERROR;
        }
        Alert.AlertType finalAlertType = alertType;
        String finalMessage = message;
        Platform.runLater(() -> {
            Alert alert = new Alert(finalAlertType, finalMessage, ButtonType.CLOSE);
            alert.showAndWait();
            Platform.exit();
        });
    }

    private String executeLogic(File file) throws Exception {
        List<Package> packages = new Parser().parse(file);
        AcceptanceWindow.Result result = AcceptanceWindow.verify(packages);
        logger.info("Acceptance result: {}, packages: {}", result, packages);
        switch (result) {
            case EXCEL:
                new ExcelBuilder().buildAndSafe(packages, file);
                return "Generated " + packages.size() + " EXCEL orders! :)";
            case WEB_API:
                new ApaczkaWebApi().issueOrdersAndDownloadCards(packages, file);
                return "Issued " + packages.size() + " orders via WebApi :)";
            default:
                throw new RuntimeException("Incorrect window result");
        }
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
