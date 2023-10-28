package com.belamila;

import com.belamila.backend.webapi.ApaczkaWebApi;
import com.belamila.backend.webapi.InPostWebApi;
import com.belamila.backend.webapi.WooWebApi;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application implements ProgressListener {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private TextArea summary;

    private ApaczkaWebApi apaczkaWebApi;
    private InPostWebApi inPostWebApi;

    private WooWebApi wooWebApi;

    @Override
    public void start(Stage primaryStage) {
        logger.info("Apaczka app started");

        Label label = new Label("Pamiętaj, żeby zawsze dobrze się bawić! (:");

        summary = new TextArea("");
        summary.setEditable(false);
        summary.setPrefRowCount(25);

        VBox view = new VBox();
        view.getChildren().addAll(label, summary);


        StackPane root = new StackPane();
        root.getChildren().add(view);

        Scene scene = new Scene(root, 700, 550);

        primaryStage.setTitle("Apaczka for Belamila :)");
        primaryStage.setScene(scene);
        primaryStage.resizableProperty().setValue(Boolean.FALSE);
        primaryStage.setOnCloseRequest(event -> {
            AcceptanceWindow.close();
            Platform.exit();
        });
        primaryStage.show();

        apaczkaWebApi = new ApaczkaWebApi(this);
        inPostWebApi = new InPostWebApi();
        wooWebApi = new WooWebApi(this);

        executorService.execute(this::run);
    }

    private void run() {
        try {
            executeLogic();
        } catch (Exception e) {
            logger.warn("", e);
            Platform.runLater(() -> {
                onProgressUpdated("Coś poszło bardzo nie tak :(");
                logger.info(e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.CLOSE);
                alert.showAndWait();
            });
        }
    }

    private void executeLogic() throws Exception {
        List<Package> packages = wooWebApi.fetchOrders();

        AcceptanceWindow.Result result = AcceptanceWindow.verify(packages, inPostWebApi);
        logger.info("Acceptance result: {}, packages: {}", result, packages);
        onProgressUpdated("Zaczynamy " + result.toString() + "...\n");

        if (result == AcceptanceWindow.Result.WEB_API) {
            String downloads = System.getProperty("user.home")+ "\\Downloads";
            apaczkaWebApi.issueOrdersAndDownloadCards(packages, downloads);
            return;
        }
        throw new RuntimeException("To się nie powinno zdażyć");
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
