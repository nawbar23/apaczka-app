package com.belamila.ui;

import com.belamila.model.Package;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * Created by: Bartosz Nawrot
 * Date: 22.07.2020
 * Description:
 */
public class AcceptanceWindow {

    public enum Result { EXCEL, WEB_API }

    private static final Logger logger = LoggerFactory.getLogger(AcceptanceWindow.class);

    public Result verify(List<Package> packages) throws Exception {
        // TODO https://stackoverflow.com/questions/19588029/customize-listview-in-javafx-with-fxml
        Platform.runLater(() -> {
            try {
                URL url = new File("src/com/belamila/ui/fxml/acceptance_window.fxml").toURI().toURL();
                Parent root = FXMLLoader.load(url);
                Scene scene = new Scene(root);
                Stage window = new Stage();
                window.setScene(scene);
                window.show();

            } catch (IOException e) {
                logger.info("", e);
            }
        });
        //throw new RuntimeException("Unimplemented");


        for (Package p : packages) {
            if (p.getService().equals("INPOST")) {
                p.setInpostId("KRA01MP");
            }
        }

        return Result.WEB_API;
    }
}
