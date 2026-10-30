package com.dinedesk.controller;

import com.dinedesk.Main;
import com.dinedesk.database.Database;
import javafx.scene.layout.Region;
import javafx.print.PrinterJob;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.shape.Rectangle;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.scene.layout.TilePane;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class NewOrderController {

    @FXML private VBox sidebar;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;

    @FXML private TilePane menuGrid;
    @FXML private ScrollPane menuScrollPane;

    @FXML private ListView<CartEntry> cartListView;

    @FXML private Label menuCountLabel;
    @FXML private Label cartCountLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label totalLabel;

    private final ObservableList<MenuEntry> menuItems =
            FXCollections.observableArrayList();

    private final ObservableList<CartEntry> cartItems =
            FXCollections.observableArrayList();

    private final List<MenuEntry> allMenuItems = new ArrayList<>();

    private final NumberFormat currency =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private boolean sidebarCollapsed = false;

    // If an order is saved but printing fails, retain its receipt for a retry.
    // Retrying will print the same saved order rather than creating a duplicate.
    private String pendingReceipt;

    @FXML
    private void initialize() {

        // Configure the menu grid and make its columns adapt to available width.

// Configure the menu grid
        menuGrid.setHgap(12);
        menuGrid.setVgap(10);
        menuGrid.setPrefColumns(3);

// Let each row use the actual card height
        menuGrid.setPrefTileHeight(-1);

        menuGrid.setTileAlignment(Pos.TOP_LEFT);
        menuGrid.setAlignment(Pos.TOP_LEFT);
        menuGrid.setMinWidth(0);
        menuGrid.setMaxWidth(Double.MAX_VALUE);
        menuGrid.setPrefWidth(Region.USE_COMPUTED_SIZE);



        menuGrid.getChildren().clear();
        cartListView.setItems(cartItems);
        cartListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(CartEntry item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent;");
                    return;
                }

                Label nameLabel = new Label(item.name);
                nameLabel.setStyle(
                        "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #172033;");
                nameLabel.setWrapText(true);

                Label unitPriceLabel = new Label(
                        currency.format(item.price) + " × " + item.quantity);
                unitPriceLabel.setStyle(
                        "-fx-font-size: 12px;" +
                                "-fx-text-fill: #64748B;");

                VBox itemDetails = new VBox(4, nameLabel, unitPriceLabel);
                itemDetails.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(itemDetails, Priority.ALWAYS);

                Button minusButton = createQuantityButton("−", "Decrease " + item.name);
                minusButton.setOnAction(event -> {
                    if (item.quantity > 1) {
                        item.quantity--;
                    } else {
                        cartItems.remove(item);
                    }
                    cartListView.refresh();
                    updateTotals();
                });

                Label quantityLabel = new Label(String.valueOf(item.quantity));
                quantityLabel.setMinWidth(20);
                quantityLabel.setAlignment(Pos.CENTER);
                quantityLabel.setStyle(
                        "-fx-font-size: 14px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #172033;");

                Button plusButton = createQuantityButton("+", "Increase " + item.name);
                plusButton.setOnAction(event -> {
                    item.quantity++;
                    cartListView.refresh();
                    updateTotals();
                });

                HBox quantityControls = new HBox(8, minusButton, quantityLabel, plusButton);
                quantityControls.setAlignment(Pos.CENTER);

                Label lineTotalLabel = new Label(currency.format(item.getLineTotal()));
                lineTotalLabel.setMinWidth(78);
                lineTotalLabel.setAlignment(Pos.CENTER_RIGHT);
                lineTotalLabel.setStyle(
                        "-fx-font-size: 13px;" +
                                "-fx-font-weight: bold;" +
                                "-fx-text-fill: #172033;");

                HBox row = new HBox(12, itemDetails, quantityControls, lineTotalLabel);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(9, 6, 9, 6));
                row.setMinHeight(66);

                setText(null);
                setGraphic(row);
                setPadding(new Insets(2, 4, 2, 4));
                setStyle("-fx-background-color: transparent;");
            }
        });

        categoryCombo.getItems().add("All Categories");
        loadCategories();
        categoryCombo.getSelectionModel().selectFirst();

        loadMenuItems();

        categoryCombo.setOnAction(event -> filterMenu());

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterMenu());

        updateTotals();
    }

    private void loadCategories() {
        String sql = "SELECT name FROM categories ORDER BY id";

        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                categoryCombo.getItems().add(result.getString("name"));
            }

        } catch (Exception exception) {
            showError("Could not load categories", exception);
        }
    }
    private void loadMenuItems() {
        allMenuItems.clear();

        String sql = """
                SELECT m.id, m.name, m.price, m.category_id, c.name AS category_name
                FROM menu_items m
                JOIN categories c ON c.id = m.category_id
                ORDER BY c.id, m.name
                """;

        try (Connection connection = Database.connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            while (result.next()) {
                allMenuItems.add(new MenuEntry(
                        result.getInt("id"),
                        result.getInt("category_id"),
                        result.getString("name"),
                        result.getDouble("price"),
                        result.getString("category_name")
                ));
            }

            filterMenu();

        } catch (Exception exception) {
            showError("Could not load menu items", exception);
        }
    }

    private void filterMenu() {
        String search = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);

        String category = categoryCombo.getValue();

        menuItems.setAll(allMenuItems.stream()
                .filter(item -> item.name.toLowerCase(Locale.ROOT)
                        .contains(search))
                .filter(item -> category == null
                        || category.equals("All Categories")
                        || item.categoryName.equals(category))
                .toList());

        menuCountLabel.setText(menuItems.size() + " items");

        menuGrid.getChildren().clear();
        for (MenuEntry item : menuItems) {
            menuGrid.getChildren().add(createMenuCard(item));
        }
    }

    private VBox createMenuCard(MenuEntry item) {



        VBox card = new VBox(5);
        card.setPrefWidth(155);
        card.setMinWidth(155);
        card.setMaxWidth(155);

        card.setPrefHeight(Region.USE_COMPUTED_SIZE);
        card.setMinHeight(Region.USE_COMPUTED_SIZE);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        card.setPadding(new Insets(8));


        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-border-color: #E5EAF2;" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;");



        // Image container
        StackPane imageHolder = new StackPane();

        imageHolder.setPrefSize(120, 75);
        imageHolder.setMinSize(120, 75);
        imageHolder.setMaxSize(120, 75);

        imageHolder.setAlignment(Pos.CENTER);

        imageHolder.setStyle(
                "-fx-background-color: #F1F5F9;" +
                        "-fx-background-radius: 9;"
        );

// Load food image
        ImageView imageView = createMenuImage(item.name);

        if (imageView != null) {
            imageView.setFitWidth(120);
            imageView.setFitHeight(75);
            imageView.setPreserveRatio(false);
            imageView.setSmooth(true);

            // Match the imageHolder's rounded corners
            Rectangle clip = new Rectangle(120, 75);
            clip.setArcWidth(18);
            clip.setArcHeight(18);

            imageView.setClip(clip);

            StackPane.setAlignment(imageView, Pos.CENTER);
            imageHolder.getChildren().add(imageView);
        } else {
            Label placeholder = new Label("🍽");
            placeholder.setStyle("-fx-font-size: 28px;");
            imageHolder.getChildren().add(placeholder);
        }


        Label nameLabel = new Label(item.name);
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(Region.USE_PREF_SIZE);
        nameLabel.setStyle(
                "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #172033;");

        Label priceLabel = new Label(currency.format(item.price));
        priceLabel.setStyle(
                "-fx-font-size: 12px;" +
                        "-fx-text-fill: #64748B;");

        Label plusSymbol = new Label("+");
        plusSymbol.setStyle(
                "-fx-text-fill: #245DDB;" +
                        "-fx-font-size: 22px;" +
                        "-fx-font-weight: bold;");

        Button addButton = new Button();
        addButton.setGraphic(plusSymbol);
        addButton.setAccessibleText("Add " + item.name + " to current order");
        addButton.setFocusTraversable(false);
        addButton.setMinSize(36, 36);
        addButton.setPrefSize(36, 36);
        addButton.setMaxSize(36, 36);
        addButton.setStyle(
                "-fx-background-color: #EAF2FF;" +
                        "-fx-border-color: transparent;" +
                        "-fx-background-radius: 9;" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;");
        addButton.setOnAction(event -> addToCart(item));

        HBox bottomRow = new HBox(6, priceLabel, new javafx.scene.layout.Region(), addButton);
        bottomRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bottomRow.getChildren().get(1), Priority.ALWAYS);

        card.getChildren().addAll(imageHolder, nameLabel, bottomRow);

        card.setFillWidth(true);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.setAlignment(Pos.TOP_CENTER);

        return card;
    }

    private void addToCart(MenuEntry selected) {
        for (CartEntry entry : cartItems) {
            if (entry.menuItemId == selected.id) {
                entry.quantity++;
                cartListView.refresh();
                updateTotals();
                return;
            }
        }

        cartItems.add(new CartEntry(
                selected.id,
                selected.name,
                selected.price,
                1
        ));

        updateTotals();
    }

    /**
     * Loads an optional item photo from:
     * src/main/resources/com/dinedesk/images/menu/
     * Example: idly.jpg, masala-dosa.png
     * If no matching image exists, the menu card shows a food placeholder.
     */
    private ImageView createMenuImage(String itemName) {

        String fileBase = itemName.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        String[] extensions = {"jpg", "png", "jpeg"};

        for (String extension : extensions) {

            String resourcePath = "/com/dinedesk/images/menu/"
                    + fileBase + "." + extension;

            var imageUrl = getClass().getResource(resourcePath);

            if (imageUrl != null) {
                Image image = new Image(imageUrl.toExternalForm());

                ImageView imageView = new ImageView(image);
                imageView.setSmooth(true);

                return imageView;
            }
        }

        return null;
    }

    private Button createQuantityButton(String symbol, String accessibleText) {
        Label symbolLabel = new Label(symbol);
        symbolLabel.setStyle(
                "-fx-font-size: 17px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #245DDB;");

        Button button = new Button();
        button.setGraphic(symbolLabel);
        button.setAccessibleText(accessibleText);
        button.setFocusTraversable(false);
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);
        button.setMaxSize(30, 30);
        button.setStyle(
                "-fx-background-color: #EAF2FF;" +
                        "-fx-border-color: transparent;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 0;" +
                        "-fx-cursor: hand;");
        return button;
    }

    @FXML
    private void increaseSelectedQuantity() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showInfo("Select an item in the current order.");
            return;
        }

        selected.quantity++;
        cartListView.refresh();
        updateTotals();
    }

    @FXML
    private void decreaseSelectedQuantity() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected == null) {
            showInfo("Select an item in the current order.");
            return;
        }

        if (selected.quantity > 1) {
            selected.quantity--;
        } else {
            cartItems.remove(selected);
        }

        cartListView.refresh();
        updateTotals();
    }

    @FXML
    private void removeSelectedItem() {
        CartEntry selected = cartListView.getSelectionModel()
                .getSelectedItem();

        if (selected != null) {
            cartItems.remove(selected);
            updateTotals();
        } else {
            showInfo("Select an item to remove.");
        }
    }

    @FXML
    private void clearOrder() {
        if (cartItems.isEmpty()) {
            return;
        }

        Alert confirmation = new Alert(
                Alert.AlertType.CONFIRMATION,
                "Clear all items from the current order?",
                javafx.scene.control.ButtonType.YES,
                javafx.scene.control.ButtonType.NO
        );

        confirmation.setHeaderText("Clear current order");

        if (confirmation.showAndWait().orElse(
                javafx.scene.control.ButtonType.NO)
                == javafx.scene.control.ButtonType.YES) {

            cartItems.clear();
            updateTotals();
        }
    }

    private void updateTotals() {
        double total = cartItems.stream()
                .mapToDouble(CartEntry::getLineTotal)
                .sum();

        int quantity = cartItems.stream()
                .mapToInt(item -> item.quantity)
                .sum();

        subtotalLabel.setText(currency.format(total));
        totalLabel.setText(currency.format(total));
        cartCountLabel.setText(quantity + " items");
    }

    @FXML
    private void saveOrder() {
        // If a previous order was saved but could not be printed, retry that
        // receipt first. Never create a duplicate order on a print retry.
        if (pendingReceipt != null) {
            printPendingReceipt();
            return;
        }

        if (cartItems.isEmpty()) {
            showInfo("Add at least one item before printing the bill.");
            return;
        }

        // Snapshot the cart before saving so the receipt cannot change if the UI changes.
        List<CartEntry> receiptItems = new ArrayList<>();
        for (CartEntry item : cartItems) {
            receiptItems.add(new CartEntry(
                    item.menuItemId, item.name, item.price, item.quantity));
        }

        String date = LocalDate.now().toString();
        LocalTime now = LocalTime.now();
        String time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String displayDate = LocalDate.parse(date)
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String displayTime = now.format(DateTimeFormatter.ofPattern("hh:mm a"));

        String insertOrder = """
                INSERT INTO orders
                    (order_date, order_time, token_number, total_bill)
                VALUES (?, ?, ?, ?)
                """;
        String insertItem = """
                INSERT INTO order_items(order_id, menu_item_id, quantity, price)
                VALUES (?, ?, ?, ?)
                """;

        double total = receiptItems.stream()
                .mapToDouble(CartEntry::getLineTotal)
                .sum();

        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try {
                int token;
                try (PreparedStatement tokenQuery = connection.prepareStatement(
                        "SELECT COALESCE(MAX(token_number), 0) + 1 "
                                + "FROM orders WHERE order_date = ?")) {
                    tokenQuery.setString(1, date);
                    try (ResultSet result = tokenQuery.executeQuery()) {
                        result.next();
                        token = result.getInt(1);
                    }
                }

                int orderId;
                try (PreparedStatement orderStatement = connection.prepareStatement(
                        insertOrder, Statement.RETURN_GENERATED_KEYS)) {
                    orderStatement.setString(1, date);
                    orderStatement.setString(2, time);
                    orderStatement.setInt(3, token);
                    orderStatement.setDouble(4, total);
                    orderStatement.executeUpdate();

                    try (ResultSet keys = orderStatement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new java.sql.SQLException(
                                    "Could not retrieve the saved order ID.");
                        }
                        orderId = keys.getInt(1);
                    }
                }

                try (PreparedStatement itemStatement =
                             connection.prepareStatement(insertItem)) {
                    for (CartEntry item : receiptItems) {
                        itemStatement.setInt(1, orderId);
                        itemStatement.setInt(2, item.menuItemId);
                        itemStatement.setInt(3, item.quantity);
                        itemStatement.setDouble(4, item.price);
                        itemStatement.addBatch();
                    }
                    itemStatement.executeBatch();
                }

                connection.commit();

                pendingReceipt = buildReceipt(
                        orderId, token, displayDate, displayTime, receiptItems, total);

                // Clear the submitted cart after the database commit. If printing
                // fails, pendingReceipt remains available for a safe retry while a
                // new customer can start a separate order.
                cartItems.clear();
                updateTotals();

                // The order is committed before printing. A printer error must not
                // roll back or duplicate this order.
                printPendingReceipt();

            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }

        } catch (Exception exception) {
            showError("Could not save order", exception);
        }
    }

    private String buildReceipt(int orderId, int token, String date, String time,
                                List<CartEntry> items, double total) {
        StringBuilder receipt = new StringBuilder();
        final int width = 32;
        receipt.append(center("UDUPIDESK", width)).append('\n');
        receipt.append(center("Restaurant Billing", width)).append('\n');
        receipt.append("--------------------------------\n");
        receipt.append(String.format(Locale.US, "Bill No : %d%n", orderId));
        receipt.append(String.format(Locale.US, "Token   : %03d%n", token));
        receipt.append("Date    : ").append(date).append('\n');
        receipt.append("Time    : ").append(time).append('\n');
        receipt.append("--------------------------------\n");
        receipt.append(String.format(Locale.US, "%-15s %3s %10s%n", "Item", "Qty", "Amount"));
        receipt.append("--------------------------------\n");

        for (CartEntry item : items) {
            String itemName = item.name;
            // Wrap long item names so the receipt remains readable on narrow paper.
            while (itemName.length() > 15) {
                receipt.append(itemName, 0, 15).append('\n');
                itemName = itemName.substring(15);
            }
            receipt.append(String.format(Locale.US, "%-15s %3d %10.2f%n",
                    itemName, item.quantity, item.getLineTotal()));
        }

        receipt.append("--------------------------------\n");
        receipt.append(String.format(Locale.US, "Subtotal:%22.2f%n", total));
        receipt.append(String.format(Locale.US, "TOTAL:%25.2f%n", total));
        receipt.append("--------------------------------\n");
        receipt.append(center("Thank you! Visit Again.", width)).append('\n');
        receipt.append(center("Powered by UdupiDesk", width)).append('\n');
        receipt.append("\n\n");
        return receipt.toString();
    }

    private String center(String value, int width) {
        if (value.length() >= width) return value;
        int leftPadding = (width - value.length()) / 2;
        return " ".repeat(leftPadding) + value;
    }


    private void printPendingReceipt() {
        if (pendingReceipt == null) {
            return;
        }

        Path pdf;

        // STEP 1: Always generate and save the PDF first.
        try {
            pdf = createReceiptPdf(pendingReceipt);

            System.out.println("====================================");
            System.out.println("DineDesk receipt PDF generated:");
            System.out.println(pdf.toAbsolutePath());
            System.out.println("File exists: " + Files.exists(pdf));
            System.out.println("====================================");

        } catch (Exception exception) {
            showError("Could not generate receipt PDF", exception);
            return;
        }

        // STEP 2: Attempt to print using the computer's configured printer.
        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {
            openReceiptPdf(pdf);

            showInfo(
                    "The order has been saved, but no printer is available.\n\n"
                            + "Your bill has been saved as a PDF at:\n"
                            + pdf.toAbsolutePath()
                            + "\n\nConnect a printer to print the bill."
            );

            // Keep pendingReceipt so printing can be retried later.
            return;
        }

        Text receiptText = new Text(pendingReceipt);
        receiptText.setFont(Font.font("Monospaced", 9));
        receiptText.setWrappingWidth(250);

        boolean printed = false;

        try {
            printed = job.printPage(receiptText);

            if (printed) {
                printed = job.endJob();
            } else {
                job.cancelJob();
            }

        } catch (Exception exception) {
            exception.printStackTrace();

            try {
                job.cancelJob();
            } catch (Exception ignored) {
                // The print job may already have ended.
            }

            printed = false;
        }

        // STEP 3: Handle the result.
        if (printed) {
            pendingReceipt = null;

            showInfo(
                    "Bill printed successfully!\n\n"
                            + "PDF copy saved at:\n"
                            + pdf.toAbsolutePath()
            );

        } else {
            openReceiptPdf(pdf);

            showInfo(
                    "The order has been saved, but printing failed.\n\n"
                            + "Your PDF bill is available at:\n"
                            + pdf.toAbsolutePath()
                            + "\n\nCheck your printer connection and try printing again."
            );

            // Keep pendingReceipt so a retry will not create a duplicate order.
        }
    }

    private void openReceiptPdf(Path pdf) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdf.toFile());
            } else {
                System.out.println(
                        "Open this PDF manually: " + pdf.toAbsolutePath()
                );
            }
        } catch (Exception exception) {
            System.err.println("Could not open receipt PDF.");
            System.err.println("PDF location: " + pdf.toAbsolutePath());
            exception.printStackTrace();
        }
    }

    /** Creates a compact, dependency-free thermal-style PDF receipt. */
    private Path createReceiptPdf(String receipt) throws IOException {
        Path folder = Paths.get("Bills");
        Files.createDirectories(folder);

        int billNo = 0;
        int tokenNo = 0;
        for (String line : receipt.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Bill No")) billNo = parseNumberAfterColon(trimmed);
            if (trimmed.startsWith("Token")) tokenNo = parseNumberAfterColon(trimmed);
        }
        String filename = String.format(Locale.ROOT, "Bill_%05d_Token_%03d.pdf", billNo, tokenNo);
        Path output = folder.resolve(filename);

        // PDF standard fonts are used so the receipt needs no extra font files.
        // Replace the rupee glyph with Rs. for compatibility with built-in Helvetica.
        String[] lines = receipt.replace("₹", "Rs.").split("\\R", -1);
        float pageWidth = 226f; // approximately 80 mm
        float topMargin = 18f;
        float lineHeight = 12f;
        float pageHeight = Math.max(180f, topMargin * 2 + lines.length * lineHeight);

        StringBuilder content = new StringBuilder("BT\n/F1 8 Tf\n");
        float y = pageHeight - topMargin;
        for (String line : lines) {
            String safe = line.replace("\t", "    ");
            if (safe.length() > 34) {
                for (int start = 0; start < safe.length(); start += 34) {
                    String part = safe.substring(start, Math.min(start + 34, safe.length()));
                    content.append("1 0 0 1 8 ").append(y).append(" Tm (")
                            .append(escapePdf(part)).append(") Tj\n");
                    y -= lineHeight;
                }
            } else {
                content.append("1 0 0 1 8 ").append(y).append(" Tm (")
                        .append(escapePdf(safe)).append(") Tj\n");
                y -= lineHeight;
            }
        }
        content.append("ET\n");

        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);
        String[] objects = new String[] {
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 " + pageWidth + " " + pageHeight + "] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
                "<< /Length " + stream.length + " >>\nstream\n" + content + "endstream"
        };

        try (var out = Files.newOutputStream(output)) {
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            buffer.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
            java.util.List<Integer> offsets = new java.util.ArrayList<>();
            offsets.add(0);
            for (int i = 0; i < objects.length; i++) {
                offsets.add(buffer.size());
                buffer.write((i + 1 + " 0 obj\n").getBytes(StandardCharsets.US_ASCII));
                buffer.write(objects[i].getBytes(StandardCharsets.US_ASCII));
                buffer.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
            }
            int xrefOffset = buffer.size();
            buffer.write(("xref\n0 " + (objects.length + 1) + "\n0000000000 65535 f \n")
                    .getBytes(StandardCharsets.US_ASCII));
            for (int i = 1; i < offsets.size(); i++) {
                buffer.write(String.format(Locale.ROOT, "%010d 00000 n \n", offsets.get(i))
                        .getBytes(StandardCharsets.US_ASCII));
            }
            buffer.write(("trailer\n<< /Size " + (objects.length + 1) + " /Root 1 0 R >>\nstartxref\n"
                    + xrefOffset + "\n%%EOF\n").getBytes(StandardCharsets.US_ASCII));
            buffer.writeTo(out);
        }
        return output;
    }

    private int parseNumberAfterColon(String line) {
        try {
            return Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String escapePdf(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
                .replaceAll("[^\\x20-\\x7E]", "?");
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("DineDesk");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, Exception exception) {
        exception.printStackTrace();

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("DineDesk");
        alert.setHeaderText(title);
        alert.setContentText(exception.getMessage() == null
                ? "An unexpected error occurred."
                : exception.getMessage());
        alert.showAndWait();
    }

    @FXML
    private void toggleSidebar() {
        sidebarCollapsed = !sidebarCollapsed;

        double width = sidebarCollapsed ? 76 : 245;
        sidebar.setPrefWidth(width);
        sidebar.setMinWidth(width);
        sidebar.setMaxWidth(width);
    }

    @FXML
    private void openDashboard() {
        Main.show("dashboard.fxml", "Dashboard");
    }

    @FXML
    private void openNewOrder() {
        Main.show("NewOrder.fxml", "New Order");
    }

    @FXML
    private void openOrders() {
        Main.show("orders.fxml", "Orders");
    }

    @FXML
    private void openMenu() {
        Main.show("menu.fxml", "Menu Management");
    }

    @FXML
    private void openReports() {
        Main.show("reports.fxml", "Reports");
    }

    @FXML
    private void openSettings() {
        Main.show("settings.fxml", "Settings");
    }

    @FXML
    private void openBackup() {
        Main.show("backup.fxml", "Backup & Restore");
    }

    private static class MenuEntry {
        private final int id;
        private final int categoryId;
        private final String name;
        private final double price;
        private final String categoryName;

        private MenuEntry(int id, int categoryId, String name,
                          double price, String categoryName) {
            this.id = id;
            this.categoryId = categoryId;
            this.name = name;
            this.price = price;
            this.categoryName = categoryName;
        }
    }

    private static class CartEntry {
        private final int menuItemId;
        private final String name;
        private final double price;
        private int quantity;

        private CartEntry(int menuItemId, String name,
                          double price, int quantity) {
            this.menuItemId = menuItemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        private double getLineTotal() {
            return price * quantity;
        }
    }
}
