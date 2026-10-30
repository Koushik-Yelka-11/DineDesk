package com.dinedesk.controller;

import com.dinedesk.database.Database;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.print.PrinterJob;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class MenuController extends NavController {

    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private Label photoNameLabel;
    @FXML private Button choosePhotoButton;
    @FXML private Button saveButton;
    @FXML private VBox categoryContainer;

    private File selectedImage;
    @FXML
    private void chooseItemPhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Menu Item Photo");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "Image Files",
                        "*.png", "*.jpg", "*.jpeg", "*.webp"
                )
        );

        if (choosePhotoButton == null || choosePhotoButton.getScene() == null) {
            return;
        }

        selectedImage = fileChooser.showOpenDialog(
                choosePhotoButton.getScene().getWindow()
        );

        if (selectedImage != null) {
            photoNameLabel.setText(selectedImage.getName());
        }
    }
    private String saveMenuImage(String itemName, File source)
            throws IOException {

        // Save to the project's resource folder so the image is available to the app.
        Path imageDirectory = Path.of(
                "src", "main", "resources", "com", "dinedesk", "images", "menu"
        );
        Files.createDirectories(imageDirectory);

        String extension = getImageExtension(source.getName());

        String filename = itemName.trim()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-zA-Z0-9._-]", "")
                + extension;

        Path destination = imageDirectory.resolve(filename);

        Files.copy(
                source.toPath(),
                destination,
                StandardCopyOption.REPLACE_EXISTING
        );

        return destination.toString();
    }

    private String getImageExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex < 0) {
            return ".png";
        }

        return filename.substring(dotIndex).toLowerCase();
    }
    private MenuRow selectedRow;
    private final List<TableView<MenuRow>> categoryTables = new ArrayList<>();

    public static class MenuRow {
        public final IntegerProperty id = new SimpleIntegerProperty();
        public final IntegerProperty catId = new SimpleIntegerProperty();
        public final StringProperty name = new SimpleStringProperty();
        public final StringProperty category = new SimpleStringProperty();
        public final DoubleProperty price = new SimpleDoubleProperty();

        public MenuRow(int id, int categoryId, String name, String category, double price) {
            this.id.set(id);
            this.catId.set(categoryId);
            this.name.set(name);
            this.category.set(category);
            this.price.set(price);
        }
    }

    private static class CategoryInfo {
        final int id;
        final String name;

        CategoryInfo(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }


    @FXML
    private void initialize() {
        categoryCombo.setPromptText("Select category");
        categoryCombo.getSelectionModel().clearSelection();
        loadCategoriesIntoCombo();
        load();
    }

    private void loadCategoriesIntoCombo() {
        categoryCombo.getItems().clear();

        String sql = "SELECT name FROM categories ORDER BY name COLLATE NOCASE";
        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            while (resultSet.next()) {
                categoryCombo.getItems().add(resultSet.getString("name"));
            }
            // Do not select an item by default: the prompt remains visible.
            categoryCombo.getSelectionModel().clearSelection();
            categoryCombo.setValue(null);
        } catch (SQLException e) {
            error(e);
        }
    }

    private void load() {
        categoryContainer.getChildren().clear();
        categoryTables.clear();
        selectedRow = null;

        Map<Integer, CategoryInfo> categories = new LinkedHashMap<>();
        Map<Integer, List<MenuRow>> itemsByCategory = new LinkedHashMap<>();

        String categorySql = "SELECT id, name FROM categories ORDER BY id";
        String itemSql =
                "SELECT m.id, m.category_id, m.name, c.name AS category_name, m.price " +
                        "FROM menu_items m " +
                        "JOIN categories c ON c.id = m.category_id " +
                        "ORDER BY m.name COLLATE NOCASE";

        try (Connection c = Database.connect()) {
            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(categorySql)) {
                while (r.next()) {
                    int id = r.getInt("id");
                    String name = r.getString("name");
                    categories.put(id, new CategoryInfo(id, name));
                    itemsByCategory.put(id, new ArrayList<>());
                }
            }

            try (Statement s = c.createStatement();
                 ResultSet r = s.executeQuery(itemSql)) {
                while (r.next()) {
                    int categoryId = r.getInt("category_id");
                    String categoryName = r.getString("category_name");
                    MenuRow row = new MenuRow(
                            r.getInt("id"),
                            categoryId,
                            r.getString("name"),
                            categoryName,
                            r.getDouble("price")
                    );

                    itemsByCategory.computeIfAbsent(categoryId, key -> new ArrayList<>()).add(row);
                    categories.putIfAbsent(categoryId, new CategoryInfo(categoryId, categoryName));
                }
            }

            for (Map.Entry<Integer, CategoryInfo> entry : categories.entrySet()) {
                int categoryId = entry.getKey();
                CategoryInfo category = entry.getValue();
                List<MenuRow> rows = itemsByCategory.getOrDefault(categoryId, List.of());
                categoryContainer.getChildren().add(createCategorySection(category, rows));
            }

        } catch (Exception e) {
            error(e);
        }
    }

    private TitledPane createCategorySection(CategoryInfo category, List<MenuRow> rows) {
        Label categoryLabel = new Label(category.name);
        categoryLabel.getStyleClass().add("menu-category-title");

        Label countLabel = new Label(rows.size() + (rows.size() == 1 ? " item" : " items"));
        countLabel.getStyleClass().add("menu-category-count");

        HBox heading = new HBox(10, categoryLabel, countLabel);
        heading.getStyleClass().add("menu-category-heading");
        heading.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        TableView<MenuRow> table = new TableView<>();
        table.getStyleClass().add("menu-category-table");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No items in this category yet"));
        table.setPrefHeight(Math.max(95, Math.min(280, rows.size() * 42.0 + 45)));
        table.setMinHeight(95);

        TableColumn<MenuRow, String> nameColumn = new TableColumn<>("Item");
        nameColumn.setCellValueFactory(data -> data.getValue().name);
        nameColumn.setMinWidth(150);

        TableColumn<MenuRow, Number> priceColumn = new TableColumn<>("Price (₹)");
        priceColumn.setCellValueFactory(data -> data.getValue().price);
        priceColumn.setMinWidth(95);
        priceColumn.setMaxWidth(150);
        priceColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Number price, boolean empty) {
                super.updateItem(price, empty);
                setText(empty || price == null ? null : String.format("₹ %.2f", price.doubleValue()));
            }
        });

        TableColumn<MenuRow, MenuRow> deleteColumn = new TableColumn<>("");
        deleteColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue()));
        deleteColumn.setMinWidth(80);
        deleteColumn.setPrefWidth(80);
        deleteColumn.setMaxWidth(80);
        deleteColumn.setSortable(false);
        deleteColumn.setReorderable(false);
        deleteColumn.setResizable(false);

        deleteColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.setText("Delete");
                deleteButton.setMinWidth(65);
                deleteButton.setPrefWidth(65);
                deleteButton.setMaxWidth(65);

                deleteButton.setStyle(
                        "-fx-background-color: #FFF1F2;" +
                                "-fx-text-fill: #DC2626;" +
                                "-fx-font-size: 11px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-padding: 5 4 5 4;" +
                                "-fx-background-radius: 6;"
                );
                deleteButton.getStyleClass().add("menu-item-delete-button");
                deleteButton.setTooltip(new Tooltip("Delete this menu item"));
                deleteButton.setFocusTraversable(false);
                deleteButton.setOnAction(event -> {
                    MenuRow row = getItem();
                    if (row != null) {
                        deleteItem(row);
                    }
                });
            }

            @Override
            protected void updateItem(MenuRow row, boolean empty) {
                super.updateItem(row, empty);
                setGraphic(empty || row == null ? null : deleteButton);
                setText(null);
            }
        });

        table.getColumns().addAll(nameColumn, priceColumn, deleteColumn);
        table.setItems(FXCollections.observableArrayList(rows));

        // Selecting a row can still populate the form, but is not required for deletion.
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldRow, newRow) -> {
            if (newRow != null) {
                selectedRow = newRow;
                nameField.setText(newRow.name.get());
                priceField.setText(String.valueOf(newRow.price.get()));
                categoryCombo.setValue(newRow.category.get());
            }
        });

        categoryTables.add(table);

        VBox content = new VBox(table);
        content.getStyleClass().add("menu-category-content");

        TitledPane pane = new TitledPane();
        pane.setGraphic(heading);
        pane.setText("");
        pane.setContent(content);
        pane.setExpanded(true);
        pane.getStyleClass().add("menu-category-pane");
        pane.setAnimated(false);

        return pane;
    }

    @FXML
    private void saveItem() {
        try {
            String itemName = nameField.getText() == null ? "" : nameField.getText().trim();
            String priceText = priceField.getText() == null ? "" : priceField.getText().trim();
            String selectedCategory = categoryCombo.getValue();

            if (itemName.isBlank()) {
                throw new Exception("Enter an item name.");
            }
            if (selectedCategory == null || selectedCategory.isBlank()) {
                throw new Exception("Please select a category.");
            }
            if (priceText.isBlank()) {
                throw new Exception("Enter the item price.");
            }
            if (selectedImage == null || !selectedImage.isFile()) {
                throw new Exception("Please choose a photo for this menu item.");
            }

            double price = Double.parseDouble(priceText);
            if (!Double.isFinite(price) || price < 0) {
                throw new Exception("Enter a valid price greater than or equal to ₹0.");
            }

            // Copy the selected image as Item-Name.ext into resources/com/dinedesk/images/menu.
            saveMenuImage(itemName, selectedImage);

            String sql = "INSERT INTO menu_items(category_id, name, price) " +
                    "SELECT id, ?, ? FROM categories WHERE name = ?";

            try (Connection c = Database.connect();
                 PreparedStatement p = c.prepareStatement(sql)) {
                p.setString(1, itemName);
                p.setDouble(2, price);
                p.setString(3, selectedCategory);

                int affectedRows = p.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("The selected category was not found.");
                }
            }

            clearFields();
            load();
        } catch (NumberFormatException e) {
            error(new Exception("Enter a valid numeric price, for example 80 or 80.50."));
        } catch (Exception e) {
            error(e);
        }
    }

    /**
     * Deletes the specific menu item represented by the clicked trash icon.
     */
    private void deleteItem(MenuRow row) {
        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Delete \"" + row.name.get() + "\" from the menu?",
                ButtonType.CANCEL,
                ButtonType.OK
        );
        confirmation.setHeaderText("Delete menu item");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM menu_items WHERE id = ?")) {

            p.setInt(1, row.id.get());
            int affectedRows = p.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("The menu item was not found. It may already have been deleted.");
            }

            clearFields();
            load();

        } catch (Exception e) {
            error(e);
        }
    }

    @FXML
    private void clearFields() {
        nameField.clear();
        priceField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        categoryCombo.setValue(null);
        selectedImage = null;
        if (photoNameLabel != null) {
            photoNameLabel.setText("No photo selected");
        }
        selectedRow = null;

        for (TableView<MenuRow> table : categoryTables) {
            table.getSelectionModel().clearSelection();
        }
    }

    private void error(Exception e) {
        new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait();
    }
    private void printBillDirectly(Node billContent) {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob == null) {
            showAlert(
                    Alert.AlertType.ERROR,
                    "Printer Error",
                    "No printer is available. Please connect or configure a printer."
            );
            return;
        }

        boolean printed = printerJob.printPage(billContent);

        if (printed) {
            printerJob.endJob();

            showAlert(
                    Alert.AlertType.INFORMATION,
                    "Print Successful",
                    "The bill has been sent to the printer."
            );
        } else {
            printerJob.cancelJob();

            showAlert(
                    Alert.AlertType.ERROR,
                    "Print Failed",
                    "Unable to print the bill. Please check your printer."
            );
        }
    }

    private void showAlert(
            Alert.AlertType type,
            String title,
            String message) {

        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
