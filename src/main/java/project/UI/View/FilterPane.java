package project.UI.View;

import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import project.UI.Model.FileType;
import project.UI.Model.SearchModel;


 // קומפוננטת סינון ואפשרויות חיפוש
public class FilterPane {
    private final VBox filterPane;
    private final ComboBox<String> fileTypeFilter;
    private final RadioButton fileNameRadio;
    private final RadioButton contentRadio;
    private final CheckBox exactMatchCheck;
    private final SearchModel searchModel;


    public FilterPane() {
        this.searchModel = SearchModel.getInstance();

        filterPane = new VBox(10);
        filterPane.setPadding(new Insets(10));
        filterPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        filterPane.getStyleClass().add("filter-pane");

        VBox containerWithBorder = new VBox(10);
        containerWithBorder.setPadding(new Insets(10));
        containerWithBorder.getStyleClass().addAll("filter-container", "filter-container-height");

        Text filterTitle = new Text("הגדרות חיפוש");
        filterTitle.getStyleClass().add("filter-title");

        // בחירה בין חיפוש בשמות קבצים או בתוכן
        VBox searchTypeBox = new VBox(5);
        searchTypeBox.getStyleClass().add("filter-section");

        Label searchTypeLabel = new Label("חפש ב:");
        searchTypeLabel.getStyleClass().add("filter-label");

        ToggleGroup searchTypeGroup = new ToggleGroup();

        fileNameRadio = new RadioButton("שמות קבצים ותיקיות");
        fileNameRadio.setToggleGroup(searchTypeGroup);

        contentRadio = new RadioButton("תוכן קבצים");
        contentRadio.setToggleGroup(searchTypeGroup);

        searchTypeBox.getChildren().addAll(searchTypeLabel, fileNameRadio, contentRadio);

        // סינון לפי סוג קובץ
        VBox fileTypeBox = new VBox(5);
        Label fileTypeLabel = new Label("סוג קובץ:");
        fileTypeLabel.setStyle("-fx-font-weight: bold;");
        fileTypeFilter = new ComboBox<>();
        fileTypeFilter.getItems().addAll(
                FileType.ALL.getDisplayName(),
                FileType.TEXT.getDisplayName(),
                FileType.IMAGE.getDisplayName(),
                FileType.DOCUMENT.getDisplayName(),
                FileType.VIDEO.getDisplayName(),
                FileType.AUDIO.getDisplayName(),
                FileType.FOLDER.getDisplayName()  // הוספה חדשה
        );

        fileTypeBox.getChildren().addAll(fileTypeLabel, fileTypeFilter);

        // אפשרויות חיפוש נוספות
        VBox optionsBox = new VBox(5);
        Label optionsLabel = new Label("אפשרויות חיפוש:");
        optionsLabel.getStyleClass().add("filter-label");

        exactMatchCheck = new CheckBox("התאמה מדויקת");
        optionsBox.getChildren().addAll(optionsLabel, exactMatchCheck);

        // כפתור איפוס הגדרות
//        Button resetButton = new Button("שמירת הגדרות");
//        resetButton.getStyleClass().add("reset-button");
//        VBox.setMargin(resetButton, new Insets(15, 0, 0, 0));
//        resetButton.setOnAction(e -> resetOptions());

        // הוספת כל הרכיבים לפאנל
        containerWithBorder.getChildren().addAll(
                filterTitle,
                searchTypeBox,
                fileTypeBox,
                optionsBox
//                resetButton
        );

        // תווית מתחת
        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        Label belowLabel = new Label("");

        // הוספת הקומפוננטים לשורש
        filterPane.getChildren().addAll(containerWithBorder, belowLabel);

        // אחרי שהוספנו, קושרים את גובה הקונטיינר לגובה הפאנל פחות התווית
        filterPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            double totalHeight = newVal.doubleValue();
            double labelHeight = belowLabel.getHeight();
            double spacing = filterPane.getSpacing();
            double padding = filterPane.getPadding().getTop() + filterPane.getPadding().getBottom();
            double availableHeight = totalHeight - labelHeight - spacing - padding;
            if (availableHeight > 0) {
                containerWithBorder.setPrefHeight(availableHeight);
            }
        });


        bindToModel();
    }

     // קישור רכיבי הממשק למאפיינים במודל בצורה דו-כיוונית
    private void bindToModel() {
        // הגדרת ערכים ראשוניים לפני הקישור
        contentRadio.setSelected(searchModel.isSearchInContent());
        fileNameRadio.setSelected(!searchModel.isSearchInContent());

        // קישור ידני דו-כיווני לרדיו כפתורים
        setupRadioButtonBindings();

        bindFileTypeComboBox();

        exactMatchCheck.selectedProperty().bindBidirectional(searchModel.exactMatchProperty());
    }

     // הגדרת קישורים ידניים לרדיו כפתורים
    private void setupRadioButtonBindings() {
        // listener למודל -> UI
        searchModel.searchInContentProperty().addListener((obs, oldVal, newVal) -> {
            contentRadio.setSelected(newVal);
            fileNameRadio.setSelected(!newVal);
        });

        // listener לUI -> מודל (רק על contentRadio)
        contentRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                searchModel.setSearchInContent(newVal);
                // איפוס הסינון כשמשנים סוג חיפוש
                searchModel.setFileType(FileType.ALL);
            }
        });

        // listener לfileNameRadio (להפוך את contentRadio)
        fileNameRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal) {
                searchModel.setSearchInContent(false);
                // איפוס הסינון כשמשנים סוג חיפוש
                searchModel.setFileType(FileType.ALL);
            }
        });
    }

     // קישור מותאם אישית לComboBox של סוג הקובץ
    private void bindFileTypeComboBox() {
        // קישור מהמודל לממשק
        searchModel.fileTypeProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                fileTypeFilter.setValue(newVal.getDisplayName());
            }
        });

        // קישור מהממשק למודל
        fileTypeFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                FileType fileType = FileType.fromDisplayName(newVal);
                if (fileType != null) {
                    searchModel.setFileType(fileType);
                }
            }
        });

        // הגדרת הערך הראשוני
        fileTypeFilter.setValue(searchModel.getFileType().getDisplayName());
    }


     // איפוס כל ההגדרות לברירות המחדל
    private void resetOptions() {
        // הפעלת מתודת האיפוס במודל - הקישורים הדו-כיווניים יעדכנו את הממשק אוטומטית
        searchModel.resetToDefaults();
    }

     // מחזיר את הפאנל הראשי
    public VBox getPane() {
        return filterPane;
    }
}