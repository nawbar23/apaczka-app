package com.belamila;

import com.belamila.backend.excel.ExcelBuilder;
import com.belamila.backend.parser.Parser;
import com.belamila.backend.webapi.ApaczkaWebApi;
import com.belamila.backend.webapi.InPostWebApi;
import com.belamila.model.Package;
import com.belamila.ui.AcceptanceWindow;
import com.belamila.ui.ProgressListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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

public class Main extends Application implements ProgressListener {

    private static final int ESTIMATED_PACKED_FEE_PLN = 15;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private TextArea summary;

    private ExcelBuilder excelBuilder;
    private ApaczkaWebApi apaczkaWebApi;
    private InPostWebApi inPostWebApi;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Apaczka app started");
        Label label = new Label("Drag and drop orders Wix CSV file");
        Label dropped = new Label("");
        summary = new TextArea("");
        summary.setEditable(false);
        summary.setPrefRowCount(25);
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
                summary.setText("");
                executorService.execute(() -> onDragHandle(file));
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        StackPane root = new StackPane();
        root.getChildren().add(dragTarget);

        Scene scene = new Scene(root, 700, 550);

        primaryStage.setTitle("Apaczka for Belamila :)");
        primaryStage.setScene(scene);
        primaryStage.resizableProperty().setValue(Boolean.FALSE);
        primaryStage.setOnCloseRequest(event -> {
            AcceptanceWindow.close();
            Platform.exit();
        });
        primaryStage.show();

        excelBuilder = new ExcelBuilder(this);
        apaczkaWebApi = new ApaczkaWebApi(this);
        inPostWebApi = new InPostWebApi();
    }

    private void onDragHandle(File file) {
        try {
            executeLogic(file);
        } catch (Exception e) {
            logger.warn("", e);
            Platform.runLater(() -> {
                onProgressUpdated("Finished after error :(");
                Alert alert = new Alert(Alert.AlertType.ERROR, e.toString(), ButtonType.CLOSE);
                alert.showAndWait();
            });
        }
    }

    private void executeLogic(File file) throws Exception {
        List<Package> packages = new Parser().parse(file);
        packages.sort((p1, p2) -> Integer.valueOf(p2.getId()).compareTo(Integer.valueOf(p1.getId())));

        AcceptanceWindow.Result result = AcceptanceWindow.verify(packages, inPostWebApi);
        logger.info("Acceptance result: {}, packages: {}", result, packages);
        onProgressUpdated("Starting " + result.toString() + "...\n");

        switch (result) {
            case EXCEL:
                excelBuilder.buildAndSafe(packages, file);
                return;
            case WEB_API:
                apaczkaWebApi.issueOrdersAndDownloadCards(packages, file);
                return;
            default:
                throw new RuntimeException("Incorrect window result");
        }
    }

    @Override
    public void onProgressUpdated(String message) {
        Platform.runLater(() -> {
            summary.setText(summary.getText() + message);
        });
    }

    @Override
    public void stop() {
        executorService.shutdown();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
