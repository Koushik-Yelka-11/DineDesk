
package com.dinedesk.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.*;

public class BackupController extends NavController {

    @FXML
    private void backupDatabase() {
        FileChooser f = new FileChooser();
        f.setInitialFileName("dinedesk-backup.db");
        File out = f.showSaveDialog(null);

        if (out == null) return;

        try {
            Files.copy(
                    Path.of("dinedesk.db"),
                    out.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            info("Backup created.");
        } catch (Exception e) {
            info("Backup failed: " + e.getMessage());
        }
    }

    @FXML
    private void restoreDatabase() {
        FileChooser f = new FileChooser();
        File in = f.showOpenDialog(null);

        if (in == null) return;

        Alert a = new Alert(
                Alert.AlertType.CONFIRMATION,
                "This replaces the current database. Continue?",
                ButtonType.YES,
                ButtonType.NO
        );

        if (a.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;

        try {
            Files.copy(
                    in.toPath(),
                    Path.of("dinedesk.db"),
                    StandardCopyOption.REPLACE_EXISTING
            );
            info("Restore complete. Restart the application.");
        } catch (Exception e) {
            info("Restore failed: " + e.getMessage());
        }
    }

    private void info(String s) {
        new Alert(Alert.AlertType.INFORMATION, s).showAndWait();
    }
}
