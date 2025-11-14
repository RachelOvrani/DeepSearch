package project.UI.View;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import project.UI.Controller.IndexController;
import project.Common.ProjectLogger;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * חלון בחירת כוננים לאינדוקס
 */
public class DriveSelectionWindow {
    private final IndexController indexController;
    private Stage settingsStage;

    // משתנים עבור ניהול מצב הכוננים
    private final Set<String> originalSelectedDrives;
    private final Set<String> currentSelectedDrives;
    private final AtomicBoolean hasChanges;
    private Button startIndexButton;

    /**
     * קונסטרקטור
     * @param controller בקר האינדוקס
     */
    public DriveSelectionWindow(IndexController controller) {
        this.indexController = controller;

        // אתחול המשתנים כאן במקום בפונקציה אחרת
        this.originalSelectedDrives = new HashSet<>();
        this.currentSelectedDrives = new HashSet<>();
        this.hasChanges = new AtomicBoolean(false);

        ProjectLogger.info("DriveSelectionWindow initialized");
    }

    /**
     * פתיחת חלון הגדרות משופר - עם בחירת כוננים
     */
    public void show() {
        settingsStage = new Stage();
        settingsStage.initModality(Modality.APPLICATION_MODAL);
        settingsStage.setTitle("הגדרות חיפוש");
        settingsStage.setMinWidth(450);
        settingsStage.setMinHeight(400);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.getStyleClass().add("settings-window");

        // כותרת ראשית עם עטיפה ליישור לימין
        Label titleLabel = new Label("בחירת כוננים לאינדוקס");
        titleLabel.getStyleClass().add("settings-title");
        titleLabel.setTextAlignment(TextAlignment.LEFT);
        titleLabel.setAlignment(Pos.CENTER_LEFT);
        titleLabel.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        HBox titleBox = new HBox(titleLabel);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        titleBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);

        VBox drivesSection = createDrivesSection();
        HBox mainButtonsBox = createMainButtonsBox();

        layout.getChildren().addAll(
                titleBox,
                new Separator(),
                drivesSection,
                mainButtonsBox
        );

        Scene scene = new Scene(layout, 450, 400);
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource("/css/setting-window-css.css")
                ).toExternalForm()
        );

        settingsStage.setScene(scene);
        settingsStage.showAndWait();
    }

    /**
     * יצירת סקציית הכוננים
     * @return VBox עם רשימת הכוננים
     */
    private VBox createDrivesSection() {
        VBox drivesSection = new VBox(10);
        Label drivesTitle = new Label("כוננים זמינים:");
        drivesTitle.getStyleClass().add("section-title");

        // ScrollPane לרשימת הכוננים
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setPrefHeight(200);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("drives-scroll");

        VBox drivesContainer = new VBox(8);
        drivesContainer.setPadding(new Insets(10));
        drivesContainer.getStyleClass().add("drives-container");

        List<String> allDrivers = indexController.getDrivers();
        List<String> indexedDrivers = indexController.getIndexedPaths();

        // אתחול הסטים עם הנתונים הנוכחיים
        originalSelectedDrives.clear();
        originalSelectedDrives.addAll(indexedDrivers);

        currentSelectedDrives.clear();
        currentSelectedDrives.addAll(originalSelectedDrives);

        hasChanges.set(false);

        // יצירת הכפתור כאן כדי שיהיה זמין
        startIndexButton = new Button("התחל אינדוקס");
        startIndexButton.getStyleClass().add("primary-button");
        startIndexButton.setPrefWidth(125);
        startIndexButton.setDisable(true);

        for (String drivePath : allDrivers) {
            HBox driveBox = createDriveBox(drivePath, indexedDrivers);
            drivesContainer.getChildren().add(driveBox);
        }

        scrollPane.setContent(drivesContainer);
        drivesSection.getChildren().addAll(drivesTitle, scrollPane);

        return drivesSection;
    }

    /**
     * יצירת בוקס לכונן בודד
     */
    private HBox createDriveBox(String drivePath, List<String> indexedDrivers) {
        HBox driveBox = new HBox(10);
        driveBox.setAlignment(Pos.CENTER_RIGHT);
        driveBox.getStyleClass().add("drive-item");

        CheckBox driveCheckBox = new CheckBox();
        driveCheckBox.getStyleClass().add("drive-checkbox");

        boolean isIndexed = indexedDrivers.contains(drivePath);
        driveCheckBox.setSelected(isIndexed);

        Label driveLabel = new Label(drivePath);
        driveLabel.getStyleClass().add("drive-label");

        Label driveIcon = new Label("💾");
        driveIcon.getStyleClass().add("drive-icon");

        driveBox.getChildren().addAll(driveLabel, driveIcon, driveCheckBox);

        driveCheckBox.setOnAction(e -> {
            if (driveCheckBox.isSelected()) {
                currentSelectedDrives.add(drivePath);
            } else {
                currentSelectedDrives.remove(drivePath);
            }

            updateButtonState();
        });

        return driveBox;
    }

    /**
     * יצירת בוקס הכפתורים הראשיים
     */
    private HBox createMainButtonsBox() {
        HBox mainButtonsBox = new HBox(15);
        mainButtonsBox.setAlignment(Pos.CENTER);
        mainButtonsBox.setPadding(new Insets(20, 0, 0, 0));

        Button cancelButton = new Button("ביטול");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(event -> settingsStage.close());

        // אם הכפתור עדיין לא נוצר, ניצור אותו כאן
        if (startIndexButton == null) {
            startIndexButton = new Button("התחל אינדוקס");
            startIndexButton.getStyleClass().add("primary-button");
            startIndexButton.setPrefWidth(125);
            startIndexButton.setDisable(true);
        }

        setupStartIndexButtonAction();

        mainButtonsBox.getChildren().addAll(cancelButton, startIndexButton);
        return mainButtonsBox;
    }

    /**
     * עדכון מצב הכפתור
     */
    private void updateButtonState() {
        boolean changed = !currentSelectedDrives.equals(originalSelectedDrives);
        hasChanges.set(changed);

        if (startIndexButton != null) {
            boolean needsIndexing = indexController.needsIndexing(new ArrayList<>(currentSelectedDrives));
            boolean isIndexing = indexController.isIndexingInProgress();

            // הכפתור יהיה זמין רק אם יש שינויים ואין אינדוקס פעיל
            startIndexButton.setDisable(!changed || isIndexing);

            // עדכון טקסט הכפתור
            if (isIndexing) {
                startIndexButton.setText("מאנדקס...");
            } else if (needsIndexing && changed) {
                startIndexButton.setText("התחל אינדוקס");
            } else if (changed) {
                startIndexButton.setText("עדכן בחירה");
            } else {
                startIndexButton.setText("התחל אינדוקס");
            }
        }
    }

    /**
     * הגדרת פעולת כפתור התחלת האינדוקס
     */
    private void setupStartIndexButtonAction() {
        startIndexButton.setOnAction(event -> {
            if (!hasChanges.get()) return;

            if (currentSelectedDrives.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING,
                        "יש לבחור לפחות כונן אחד לאינדוקס!", ButtonType.OK);
                alert.showAndWait();
                return;
            }

            boolean needsIndexing = indexController.needsIndexing(new ArrayList<>(currentSelectedDrives));
            String message;

            if (needsIndexing) {
                message = "האם אתה בטוח שברצונך להתחיל אינדוקס עבור " + currentSelectedDrives.size() + " כוננים?\n" +
                        "כוננים נבחרים: " + String.join(", ", currentSelectedDrives) + "\n" +
                        "פעולה זו עלולה לקחת זמן רב.";
            } else {
                message = "האם אתה בטוח שברצונך לעדכן את בחירת הכוננים?\n" +
                        "כוננים נבחרים: " + String.join(", ", currentSelectedDrives);
            }

            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);

            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    indexController.startIndexing(new ArrayList<>(currentSelectedDrives));
                    settingsStage.close();
                }
            });
        });
    }
}