package com.dinedesk;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import com.dinedesk.database.Database;
public class Main extends Application {

    private static Stage stage;

    // Keep track of the current window state.
    private static boolean wasMaximized = false;
    private static boolean wasFullScreen = false;

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        // Initialize database and run pending migrations.
        Database.initialize();

        stage.setMinWidth(1000);
        stage.setMinHeight(650);

        show("dashboard.fxml", "Dashboard");

        stage.show();
    }

    public static void show(String file, String title) {

        try {
            // Remember the existing window state before changing pages.
            if (stage != null && stage.getScene() != null) {
                wasMaximized = stage.isMaximized();
                wasFullScreen = stage.isFullScreen();
            }

            String resourcePath = "/com/dinedesk/view/" + file;

            java.net.URL resource = Main.class.getResource(resourcePath);

            if (resource == null) {
                throw new IllegalStateException(
                        "FXML file not found: " + resourcePath
                                + "\nCheck the filename and location under src/main/resources."
                );
            }

            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            // Reuse the existing Scene instead of creating a new one
            // every time the user navigates to another page.
            Scene scene;

            if (stage.getScene() == null) {
                scene = new Scene(root, 1280, 800);

                scene.getStylesheets().add(
                        Main.class.getResource(
                                "/com/dinedesk/css/styles.css"
                        ).toExternalForm()
                );
                registerKeyboardShortcuts(scene);
                stage.setScene(scene);

            } else {
                scene = stage.getScene();

                // Replace the page content while retaining the Scene.
                scene.setRoot(root);
            }

            stage.setTitle("DineDesk | " + title);

            // Restore the previous window state.
            stage.setMaximized(wasMaximized);
            stage.setFullScreen(wasFullScreen);

        } catch (Exception e) {

            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("DineDesk Error");
            alert.setHeaderText("Unable to open page");
            alert.setContentText(
                    e.getMessage() == null
                            ? "An unexpected error occurred."
                            : e.getMessage()
            );

            alert.showAndWait();
        }
    }

    public static void main(String[] args) {

        launch(args);

    }
    private static void registerKeyboardShortcuts(Scene scene) {

        // Ctrl + Plus: New Order
        KeyCodeCombination newOrderShortcut =
                new KeyCodeCombination(
                        KeyCode.N,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + L: Orders
        KeyCodeCombination ordersShortcut =
                new KeyCodeCombination(
                        KeyCode.O,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + R: Reports
        KeyCodeCombination reportsShortcut =
                new KeyCodeCombination(
                        KeyCode.R,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + B: Backup & Restore
        KeyCodeCombination backupShortcut =
                new KeyCodeCombination(
                        KeyCode.B,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + S: Settings
        KeyCodeCombination settingsShortcut =
                new KeyCodeCombination(
                        KeyCode.S,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + H: Dashboard / Home
        KeyCodeCombination homeShortcut =
                new KeyCodeCombination(
                        KeyCode.H,
                        KeyCombination.CONTROL_DOWN
                );

        // Ctrl + M: Menu Management
        KeyCodeCombination menuShortcut =
                new KeyCodeCombination(
                        KeyCode.M,
                        KeyCombination.CONTROL_DOWN
                );

        // Register shortcuts
        scene.getAccelerators().put(
                newOrderShortcut,
                () -> show("NewOrder.fxml", "New Order")
        );

        scene.getAccelerators().put(
                ordersShortcut,
                () -> show("orders.fxml", "Orders")
        );

        scene.getAccelerators().put(
                reportsShortcut,
                () -> show("reports.fxml", "Reports")
        );

        scene.getAccelerators().put(
                backupShortcut,
                () -> show("backup.fxml", "Backup & Restore")
        );

        scene.getAccelerators().put(
                settingsShortcut,
                () -> show("settings.fxml", "Settings")
        );

        scene.getAccelerators().put(
                homeShortcut,
                () -> show("dashboard.fxml", "Dashboard")
        );

        scene.getAccelerators().put(
                menuShortcut,
                () -> show("menu.fxml", "Menu Management")
        );
    }
}
////hello koushik
//hi koushik