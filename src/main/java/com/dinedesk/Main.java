
package com.dinedesk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    static Stage stage;

    @Override
    public void start(Stage s) throws Exception {
        stage = s;
        show("dashboard.fxml", "Dashboard");
        stage.show();
    }

    public static void show(String file, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("/com/dinedesk/view/" + file)
            );

            Scene scene = new Scene(loader.load(), 1280, 800);

            scene.getStylesheets().add(
                    Main.class.getResource(
                            "/com/dinedesk/css/styles.css"
                    ).toExternalForm()
            );

            stage.setTitle("DineDesk | " + title);
            stage.setScene(scene);

        } catch (Exception e) {
            e.printStackTrace();

            javafx.scene.control.Alert a =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            e.getMessage()
                    );

            a.showAndWait();
        }
    }

    public static void main(String[] a) {
        launch(a);
    }
}
