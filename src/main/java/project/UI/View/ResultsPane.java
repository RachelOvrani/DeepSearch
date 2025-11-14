package project.UI.View;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import project.UI.Controller.SearchController;
import project.UI.Model.SearchResult;
import project.UI.Util.FileIconUtil;

import java.io.File;

/**
 * קומפוננטת תוצאות החיפוש
 */
public class ResultsPane {
    private final BorderPane resultsPane;
    private final TableView<SearchResult> resultsTable;
    private final SearchController searchController;
    private final Label countLabel;

    /**
     * קונסטרקטור - יוצר את פאנל התוצאות
     * @param controller בקר החיפוש
     */
    public ResultsPane(SearchController controller) {
        this.searchController = controller;

        // יצירת פאנל ראשי והגדרת סגנון
        resultsPane = new BorderPane();
        resultsPane.setPadding(new Insets(10));
        resultsPane.getStyleClass().add("results-pane");

        // הגדרת טבלת תוצאות
        resultsTable = new TableView<>();
        resultsTable.getStyleClass().add("results-table");
        setupResultsTable();
        resultsPane.setCenter(resultsTable);

        // הוספת שורת סטטוס בתחתית
        countLabel = new Label("0 תוצאות");
        countLabel.getStyleClass().add("count-label");

        setupStatusBar();

        // קישור התצוגה לנתונים במודל
        bindToModel();
    }

    /**
     * מגדיר את שורת הסטטוס
     */
    private void setupStatusBar() {
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.getStyleClass().add("status-bar");
        statusBar.getChildren().add(countLabel);
        resultsPane.setBottom(statusBar);
    }

    /**
     * קישור התצוגה לנתונים במודל
     */
    private void bindToModel() {
        // קישור הטבלה לרשימת התוצאות במודל
        resultsTable.setItems(searchController.getResults());

        // עדכון מספר התוצאות כשהרשימה משתנה
        searchController.getResults().addListener((javafx.collections.ListChangeListener<SearchResult>) c -> {
            countLabel.setText(searchController.getResults().size() + " תוצאות");
        });
    }

    /**
     * מגדיר את טבלת התוצאות עם כל העמודות והסגנונות
     */
    private void setupResultsTable() {
        resultsTable.setEditable(false);

        // הגדרת כל העמודות בטבלה
        TableColumn<SearchResult, SearchResult> nameIconColumn = createNameIconColumn();
        TableColumn<SearchResult, String> pathColumn = createPathColumn();
        TableColumn<SearchResult, String> sizeColumn = createSizeColumn();
        TableColumn<SearchResult, String> modifiedColumn = createModifiedColumn();
        TableColumn<SearchResult, String> createdColumn = createCreatedColumn();

        // הגדרת אירוע לחיצה כפולה על שורה
        setupRowClickHandler();

        // הוספת העמודות לטבלה
        resultsTable.getColumns().addAll(nameIconColumn, pathColumn, sizeColumn, modifiedColumn, createdColumn);
    }

    /**
     * יוצר את עמודת שם הקובץ עם אייקון - עם תיקון לבעיית צבע בשורה נבחרת
     * @return עמודת שם קובץ מוגדרת
     */
    private TableColumn<SearchResult, SearchResult> createNameIconColumn() {
        TableColumn<SearchResult, SearchResult> nameIconColumn = new TableColumn<>("שם הקובץ");
        nameIconColumn.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue()));
        nameIconColumn.setPrefWidth(250);

        nameIconColumn.setCellFactory(column -> new TableCell<>() {
            private Label fileNameLabel;
            private HBox hbox;

            {
                // אתחול מראש של הרכיבים
                fileNameLabel = new Label();
                fileNameLabel.getStyleClass().add("file-name");
                hbox = new HBox(5);
                hbox.setAlignment(Pos.CENTER_LEFT);
                hbox.getStyleClass().add("name-icon-cell");
            }

            @Override
            protected void updateItem(SearchResult item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // הגדרת תוכן התא
                    File file = new File(item.getPath(), item.getName());
                    ImageView iconView = FileIconUtil.getSystemIcon(file);
                    fileNameLabel.setText(item.getName());

                    // ניקוי וסידור מחדש של המכולה
                    hbox.getChildren().clear();
                    hbox.getChildren().addAll(iconView, fileNameLabel);

                    // עדכון צבע הטקסט בהתאם למצב הבחירה - חשוב!
                    updateTextColor(isSelected());

                    setGraphic(hbox);
                }
            }

            // מתודה נפרדת לעדכון צבע הטקסט
            private void updateTextColor(boolean selected) {
                if (selected) {
                    fileNameLabel.setTextFill(Color.BLUE);
                } else {
                    fileNameLabel.setTextFill(Color.GREEN);
                }
            }

            // עדכון כשמצב הבחירה משתנה
            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);

                // וידוא שיש תכולה לתא לפני עדכון הצבע
                if (getGraphic() != null) {
                    updateTextColor(selected);
                }
            }
        });

        return nameIconColumn;
    }

    /**
     * יוצר את עמודת נתיב הקובץ
     * @return עמודת נתיב מוגדרת
     */
    private TableColumn<SearchResult, String> createPathColumn() {
        TableColumn<SearchResult, String> pathColumn = new TableColumn<>("נתיב");
        pathColumn.setCellValueFactory(new PropertyValueFactory<>("path"));
        pathColumn.setPrefWidth(340);
        return pathColumn;
    }

    /**
     * יוצר את עמודת גודל הקובץ
     * @return עמודת גודל מוגדרת
     */
    private TableColumn<SearchResult, String> createSizeColumn() {
        TableColumn<SearchResult, String> sizeColumn = new TableColumn<>("גודל");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeColumn.setPrefWidth(125);
        return sizeColumn;
    }

    /**
     * יוצר את עמודת תאריך שינוי
     * @return עמודת תאריך שינוי מוגדרת
     */
    private TableColumn<SearchResult, String> createModifiedColumn() {
        TableColumn<SearchResult, String> modifiedColumn = new TableColumn<>("תאריך שינוי");
        modifiedColumn.setCellValueFactory(new PropertyValueFactory<>("modified"));
        modifiedColumn.setPrefWidth(160);
        return modifiedColumn;
    }

    /**
     * יוצר את עמודת תאריך יצירה
     * @return עמודת תאריך יצירה מוגדרת
     */
    private TableColumn<SearchResult, String> createCreatedColumn() {
        TableColumn<SearchResult, String> createdColumn = new TableColumn<>("תאריך יצירה");
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("created"));
        createdColumn.setPrefWidth(160);
        return createdColumn;
    }

    private void setupRowClickHandler() {
        resultsTable.setRowFactory(tv -> {
            TableRow<SearchResult> row = new TableRow<>();

            // יצירת תפריט הקשר עם האפשרויות הרצויות
            ContextMenu contextMenu = new ContextMenu();
            contextMenu.getStyleClass().add("context-menu");  // הוספת class לעיצוב

            MenuItem openFileItem = new MenuItem("פתיחה");
            openFileItem.setOnAction(e -> {
                SearchResult item = row.getItem();
                if (item != null) {
                    searchController.openFile(item.getFullPath());
                }
            });
            openFileItem.getStyleClass().add("menu-item"); // הוספת class לעיצוב

            MenuItem openLocationItem = new MenuItem("פתיחת מיקום קובץ");
            openLocationItem.setOnAction(e -> {
                SearchResult item = row.getItem();
                if (item != null) {
                    searchController.openFileLocation(item.getFullPath());
                }
            });
            openLocationItem.getStyleClass().add("menu-item"); // הוספת class לעיצוב

            contextMenu.getItems().addAll(openFileItem, openLocationItem);

            // הקצאת התפריט לשורה בלחצן ימני
            row.setContextMenu(contextMenu);

            // טיפול בלחיצה כפולה לפתיחה (כבר קיים, נשאיר)
            row.setOnMouseClicked(event -> {
                if (!row.isEmpty() && event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2) {
                    searchController.openFile(row.getItem().getFullPath());
                }
            });

            return row;
        });
    }

    /**
     * מחזיר את הפאנל הראשי
     * @return פאנל התוצאות
     */
    public BorderPane getPane() {
        return resultsPane;
    }

    /**
     * מחזיר את טבלת התוצאות
     * @return טבלת התוצאות
     */
    public TableView<SearchResult> getResultsTable() {
        return resultsTable;
    }
}