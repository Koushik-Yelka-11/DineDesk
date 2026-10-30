package com.dinedesk.controller;

import com.dinedesk.database.Database;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Window;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class SettingsController extends NavController {

    @FXML
    private Button aboutButton;

    @FXML
    private void resetOrders() {

        // Step 1: Ask for the password in a custom dialog.
        Dialog<String> passwordDialog = new Dialog<>();

        passwordDialog.setTitle("Authentication Required");
        passwordDialog.setHeaderText(null);

        Window owner = aboutButton.getScene().getWindow();
        passwordDialog.initOwner(owner);

        DialogPane pane = passwordDialog.getDialogPane();

        // Increase the password dialog width
        pane.setPrefWidth(390);
        pane.setMinWidth(390);
        pane.setMaxWidth(390);
        applyDialogStyles(pane, "reset-password-dialog");

        Label title = new Label("Verify Your Password");
        title.getStyleClass().add("password-dialog-title");

        Label message = new Label(
                "Enter your admin password to reset all orders."
        );
        message.getStyleClass().add("password-dialog-message");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter admin password");
        passwordField.getStyleClass().add("password-input");

        // Password dialog content
        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setFillWidth(true);
        content.getStyleClass().add("password-dialog-content");

// Center the title and message
        title.setMaxWidth(Double.MAX_VALUE);
        title.setAlignment(Pos.CENTER);

        message.setMaxWidth(Double.MAX_VALUE);
        message.setAlignment(Pos.CENTER);
        message.setWrapText(true);

// Make the password field fill the available width
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setPrefHeight(44);

        content.getChildren().addAll(
                title,
                message,
                passwordField
        );

        pane.setContent(content);

        ButtonType verifyButton = new ButtonType(
                "Continue",
                ButtonBar.ButtonData.OK_DONE
        );

        ButtonType cancelButton = new ButtonType(
                "Cancel",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        pane.getButtonTypes().setAll(cancelButton, verifyButton);

        Button continueButton =
                (Button) pane.lookupButton(verifyButton);

        continueButton.getStyleClass().add("reset-confirm-button");

        Button cancel =
                (Button) pane.lookupButton(cancelButton);

        cancel.getStyleClass().add("cancel-button");

        // Do not close the dialog if the password is empty.
        continueButton.addEventFilter(
                javafx.event.ActionEvent.ACTION,
                event -> {
                    if (passwordField.getText().isBlank()) {
                        event.consume();
                        passwordField.setPromptText(
                                "Password is required"
                        );
                    }
                }
        );

        passwordDialog.setResultConverter(button -> {
            if (button == verifyButton) {
                return passwordField.getText();
            }
            return null;
        });

        Optional<String> passwordResult =
                passwordDialog.showAndWait();

        if (passwordResult.isEmpty()) {
            return;
        }

        // Step 2: Validate the password.
        // Replace this placeholder with your actual admin password
        // verification method.
        String enteredPassword = passwordResult.get();
        String adminPassword = "9959";

        if (!enteredPassword.equals(adminPassword)) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.initOwner(owner);
            error.setTitle("Authentication Failed");
            error.setHeaderText(null);
            error.setContentText(
                    "Incorrect password. Orders have not been deleted."
            );
            error.showAndWait();
            return;
        }

        // Step 3: Ask for final confirmation.
        // Step 3: Ask for final confirmation.
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);

        styleAlert(confirmation, owner, "reset-orders-dialog");

        confirmation.setTitle("Reset Orders");
        confirmation.setHeaderText(
                "Are you sure you want to reset all orders?"
        );
        confirmation.setContentText(
                "All saved orders and their order items will be permanently deleted."
        );

        ButtonType finalCancelButton = new ButtonType(
                "Cancel",
                ButtonBar.ButtonData.CANCEL_CLOSE
        );

        ButtonType resetButton = new ButtonType(
                "Reset All Orders",
                ButtonBar.ButtonData.OK_DONE
        );

        confirmation.getButtonTypes().setAll(
                finalCancelButton,
                resetButton
        );

// Apply custom button styles.
        Button finalCancel = (Button) confirmation.getDialogPane()
                .lookupButton(finalCancelButton);

        Button finalReset = (Button) confirmation.getDialogPane()
                .lookupButton(resetButton);

        finalCancel.getStyleClass().add("cancel-button");
        finalReset.getStyleClass().add("reset-confirm-button");

// Show the confirmation dialog.
        Optional<ButtonType> result = confirmation.showAndWait();

        if (result.isEmpty() || result.get() != resetButton) {
            return;
        }
        // Step 4: Reset orders only after password and confirmation.
        try (Connection connection = Database.connect()) {

            connection.setAutoCommit(false);

            try (Statement statement = connection.createStatement()) {

                statement.executeUpdate("DELETE FROM order_items");
                statement.executeUpdate("DELETE FROM orders");

                statement.executeUpdate(
                        "DELETE FROM sqlite_sequence WHERE name = 'orders'"
                );

                connection.commit();

            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }

            Alert success = new Alert(Alert.AlertType.INFORMATION);

            styleAlert(success, owner, "success-alert-dialog");

            success.setTitle("Reset Complete");
            success.setHeaderText("Orders reset successfully");
            success.setContentText(
                    "All orders have been deleted.\n" +
                            "The next Order ID will start from 1.\n" +
                            "Your menu remains unchanged."
            );

            success.showAndWait();

        } catch (SQLException e) {

            e.printStackTrace();

            Alert error = new Alert(Alert.AlertType.ERROR);
            error.initOwner(owner);
            error.setTitle("Reset Failed");
            error.setHeaderText("Could not reset orders");
            error.setContentText(e.getMessage());
            error.showAndWait();
        }
    }
    private void applyDialogStyles(DialogPane pane, String styleClass) {
        var stylesheet = getClass().getResource(
                "/com/dinedesk/css/styles.css"
        );

        if (stylesheet != null) {
            String css = stylesheet.toExternalForm();

            if (!pane.getStylesheets().contains(css)) {
                pane.getStylesheets().add(css);
            }
        }

        if (!pane.getStyleClass().contains(styleClass)) {
            pane.getStyleClass().add(styleClass);
        }
    }

    private void styleAlert(Alert alert, Window owner, String styleClass) {
        alert.initOwner(owner);
        alert.setGraphic(null);

        applyDialogStyles(alert.getDialogPane(), styleClass);

        alert.getDialogPane().getStyleClass().add("custom-alert-dialog");
    }
    @FXML
    private void handleAboutDineDesk() {

        String aboutText = """
                Welcome to DineDesk!

                Your reliable partner for smooth and efficient
                restaurant management.

                APPLICATION INFORMATION
                Application Name: DineDesk POS
                Brand: UdupiDesk
                Version: 1.0
                Application Type: Offline Restaurant Billing System
                Database: SQLite
                Platform: Desktop Application

                KEY FEATURES
                • Menu and category management
                • Quick order creation and billing
                • Automatic bill calculations
                • Daily token number generation
                • Kitchen Order Ticket (KOT) printing
                • Order history and search
                • Daily and monthly sales reports
                • Database backup and restore

                OUR GOAL
                To simplify restaurant operations through
                fast billing and reliable offline data storage.

                Thank you for choosing DineDesk!
                Have a great day and a successful business!
                """;

        Window owner = aboutButton.getScene().getWindow();

        Label title = new Label("About DineDesk");

        title.setStyle(
                "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #111827;"
        );

        Label content = new Label(aboutText);
        content.setWrapText(true);

        content.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-text-fill: #475569;" +
                        "-fx-line-spacing: 4px;"
        );

        ScrollPane scrollPane = new ScrollPane(content);

        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(320);

        scrollPane.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-background: transparent;"
        );

        Button closeButton = new Button("Close");

        closeButton.setStyle(
                "-fx-background-color: #0866FF;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8px;" +
                        "-fx-padding: 10px 24px;" +
                        "-fx-cursor: hand;"
        );

        Popup popup = new Popup();

        VBox card = new VBox(18, title, scrollPane, closeButton);

        card.setPadding(new Insets(26));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPrefWidth(480);

        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 16px;" +
                        "-fx-border-color: #DCE6F5;" +
                        "-fx-border-radius: 16px;"
        );

        StackPane wrapper = new StackPane(card);
        wrapper.setPadding(new Insets(10));

        popup.getContent().add(wrapper);

        popup.setAutoHide(true);
        popup.setHideOnEscape(true);

        closeButton.setOnAction(event -> popup.hide());

        popup.show(owner);

        popup.setX(
                owner.getX() +
                        (owner.getWidth() - wrapper.prefWidth(-1)) / 2
        );

        popup.setY(
                owner.getY() +
                        (owner.getHeight() - 440) / 2
        );
    }
}