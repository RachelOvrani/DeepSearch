package project.UI.View;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import project.UI.Controller.IndexController;
import project.UI.Controller.SearchController;
import project.UI.Model.SearchModel;

import java.util.*;

// קומפוננטת חיפוש
public class SearchPane {
    private final VBox searchPane;
    private final TextField searchField;
    private final SearchController searchController;
    private final IndexController indexController;
    private final SearchModel searchModel;
    private final Label statusLabel;
    private DriveSelectionWindow driveSelectionWindow;
    private ProgressIndicator progressIndicator;
    private Button searchButton;


    // קונסטרקטור - יוצר את פאנל החיפוש
    public SearchPane(SearchController searchController, IndexController indexController) {
        this.searchController = searchController;
        this.indexController = indexController;
        this.searchModel = SearchModel.getInstance();
        this.statusLabel = new Label("התחל חיפוש.");
        this.driveSelectionWindow = new DriveSelectionWindow(indexController);

        searchPane = new VBox(10);
        searchPane.setPadding(new Insets(10));
        searchPane.getStyleClass().add("search-pane");

        // כותרת
        Text title = new Text("חיפוש קבצים ותיקיות");
        title.getStyleClass().add("search-title");

        // שורת חיפוש
        HBox searchBar = new HBox(10);
        searchBar.getStyleClass().add("search-bar");

        // שדה הקלדה לחיפוש
        searchField = new TextField();
        searchField.setPromptText("הקלד מילות מפתח לחיפוש...");
        searchField.setPrefWidth(300);
        searchField.getStyleClass().add("search-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox.setMargin(searchField, new Insets(5, 30, 5, 10));

        // קישור שדה החיפוש למודל
        bindSearchField();

        // כפתור חיפוש
        searchButton = new Button("חפש");
        searchButton.setPrefSize(80, 30);
        searchButton.setDefaultButton(true);
        searchButton.getStyleClass().add("search-button");
        searchButton.setOnAction(e -> searchController.performSearch());

        // כפתור הגדרות
        Button settingsButton = createSettingsButton();

        searchBar.getChildren().addAll(searchButton, searchField, settingsButton);

        // יצירת Progress Indicator לאינדוקס
        progressIndicator = new ProgressIndicator();
        progressIndicator.setMaxSize(20, 20);
        progressIndicator.setVisible(false);

        // עדכון מחלקת סגנון ללייבל הסטטוס
        statusLabel.getStyleClass().add("status-label");

        // קישור לייבל הסטטוס למודל
        bindStatusLabel();

        // חיבור לסטטוס האינדוקס
        bindToIndexingStatus();

        // קישור כפתור החיפוש למצב הזמינות
        bindSearchButton();

        // יצירת קונטיינר לסטטוס עם Progress Indicator
        HBox statusContainer = new HBox(10);
        statusContainer.setAlignment(Pos.CENTER_LEFT);
        statusContainer.setPadding(new Insets(0, 0, 5, 110));
        statusContainer.getChildren().addAll(progressIndicator, statusLabel);

        searchPane.getChildren().addAll(title, searchBar, statusContainer);
    }

    private void bindSearchField() {
        // קישור דו-כיווני בין שדה החיפוש למודל
        searchField.textProperty().bindBidirectional(searchModel.searchQueryProperty());
    }


    // קישור לייבל הסטטוס למאפיין הסטטוס במודל
    private void bindStatusLabel() {
        // קישור לסטטוס חיפוש רגיל כברירת מחדל
        statusLabel.textProperty().bind(searchModel.statusMessageProperty());
    }


    // קישור כפתור החיפוש לזמינות החיפוש
    private void bindSearchButton() {
        // כפתור החיפוש יהיה מושבת אם:
        // 1. אין טקסט בשדה החיפוש
        // 2. החיפוש לא זמין (אין אינדקס)
        // 3. אינדוקס פעיל כרגע
        searchButton.disableProperty().bind(
                searchField.textProperty().isEmpty()
                        .or(searchModel.searchEnabledProperty().not())
                        .or(indexController.getIndexEngineModel().indexingInProgressProperty())
        );
    }

    private void bindToIndexingStatus() {
        if (indexController.getIndexEngineModel() != null) {
            indexController.getIndexEngineModel().indexingInProgressProperty().addListener((obs, oldVal, newVal) -> {
                progressIndicator.setVisible(newVal);

                if (newVal) {
                    // במהלך אינדוקס - מציג את סטטוס האינדוקס
                    progressIndicator.setProgress(-1.0);
                    statusLabel.textProperty().unbind();
                    statusLabel.textProperty().bind(indexController.getIndexEngineModel().indexingStatusProperty());
                } else {
                    // כאשר האינדוקס מסתיים - מעדכן זמינות חיפוש וחוזר לסטטוס החיפוש הרגיל
                    searchModel.updateSearchAvailability();
                    statusLabel.textProperty().unbind();
                    statusLabel.textProperty().bind(searchModel.statusMessageProperty());
                }
            });
        }
    }

    // יצירת כפתור הגדרות
    private Button createSettingsButton() {
        // יש להחליף את הנתיב לאייקון או להשתמש באייקון מתוך המשאבים
        Image image = new Image(Objects.requireNonNull(getClass().getResource("/icons/setting.png")).toExternalForm());
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(25);
        imageView.setFitHeight(25);

        Button settingsButton = new Button();
        settingsButton.setGraphic(imageView);
        settingsButton.setPrefSize(45, 45);
        settingsButton.getStyleClass().add("settings-button");
        settingsButton.setOnAction(e -> openSettingsWindow());

        return settingsButton;
    }

    // פתיחת חלון הגדרות
    private void openSettingsWindow() {
        driveSelectionWindow.show();
    }

    // מחזיר את הפאנל הראשי
    public VBox getPane() {
        return searchPane;
    }
}