package com.dinedesk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class BackupController extends NavController {

    /**
     * Existing backup-page action.
     * Called by the Backup button in your FXML.
     */
    @FXML
    private void backupDatabase() {
        boolean success = createDatabaseBackup();

        if (success) {
            info("Backup created successfully.");
        }
    }

    /**
     * Creates a database backup.
     *
     * @return true if the backup succeeds; false if cancelled or failed.
     */
    public boolean createDatabaseBackup() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Back Up DineDesk Database");
        fileChooser.setInitialFileName("dinedesk-backup.db");

        File outputFile = fileChooser.showSaveDialog(null);

        // User cancelled the file chooser.
        if (outputFile == null) {
            return false;
        }

        try {
            Path databasePath = Path.of("dinedesk.db");

            if (!Files.exists(databasePath)) {
                info("Backup failed: Database file was not found.");
                return false;
            }

            // Prevent overwriting the active database with its own backup.
            if (Files.isSameFile(databasePath, outputFile.toPath())) {
                info("Please select a different location for the backup.");
                return false;
            }

            Files.copy(
                    databasePath,
                    outputFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            return true;

        } catch (Exception e) {
            info("Backup failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Existing restore-page action.
     * Called by the Restore button in your FXML.
     */
    @FXML
    private void restoreDatabase() {
        FileChooser fileChooser = new FileChooser();

        fileChooser.setTitle("Restore DineDesk Database");

        File inputFile = fileChooser.showOpenDialog(null);

        if (inputFile == null) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "This replaces the current database. Continue?",
                ButtonType.YES,
                ButtonType.NO
        );

        confirmation.setTitle("Restore Database");
        confirmation.setHeaderText("Confirm Database Restore");

        if (confirmation.showAndWait().orElse(ButtonType.NO)
                != ButtonType.YES) {
            return;
        }

        try {
            Path sourcePath = inputFile.toPath();
            Path databasePath = Path.of("dinedesk.db");

            if (!Files.exists(sourcePath)) {
                info("Restore failed: Backup file was not found.");
                return;
            }

            if (Files.isSameFile(sourcePath, databasePath)) {
                info("The selected file is already the active database.");
                return;
            }

            Files.copy(
                    sourcePath,
                    databasePath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            info("Restore complete. Restart the application.");

        } catch (Exception e) {
            info("Restore failed: " + e.getMessage());
        }
    }

    /**
     * Displays an information dialog.
     */
    private void info(String message) {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                message
        );

        alert.setHeaderText(null);
        alert.showAndWait();
    }
}