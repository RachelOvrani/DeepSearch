package project.UI.View;

import javafx.scene.control.*;
import project.Common.TextExtractor;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import project.UI.Model.FileType;
import project.UI.Model.SearchModel;
import project.UI.Model.SearchResult;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class PreviewPane {
    private final VBox previewPane;
    private final EnhancedTextFlow fileContentPreview;
    private final ProgressIndicator spinner;
    private static final int MAX_PREVIEW_SIZE = 10 * 1024;
    private final double DEFAULT_HEIGHT = 150.0;

    private boolean useEnhancedTextFlow = false;
    private VBox currentContentPane;
    private StackPane contentContainer; // מכולה עם StackPane לספינר מעל התוכן

    // כפתורי ניווט
    private Button firstBtn;
    private Button prevBtn;
    private Button nextBtn;
    private Button lastBtn;

    private SearchModel searchModel;

    // enum לסוגי חיפוש
    public enum SearchType {
        CONTENT_SEARCH,  // חיפוש תוכן
        NAME_SEARCH      // חיפוש שם
    }

    public PreviewPane() {
        this.searchModel = SearchModel.getInstance();

        previewPane = new VBox(10);
        previewPane.setPadding(new Insets(15));
        previewPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        previewPane.getStyleClass().add("preview-pane");

        // יצירת כפתורי הניווט
        createNavigationButtons();

        // כותרת
        Text previewTitle = new Text("תצוגה מקדימה");
        previewTitle.setFont(Font.font("System", FontWeight.BOLD, 16));
        previewTitle.getStyleClass().add("preview-title");

        // תיבת כפתורי הניווט
        HBox navigationBox = new HBox();
        navigationBox.getStyleClass().add("navigation-box");
        navigationBox.getChildren().addAll(firstBtn, prevBtn, nextBtn, lastBtn);

        // הוספת הכותרת מימין והכפתורים משמאל עם מרווח ביניהם
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER);
        topRow.setSpacing(10);
        topRow.getChildren().addAll(previewTitle, spacer, navigationBox);



        // יצירת EnhancedTextFlow
        fileContentPreview = new EnhancedTextFlow();
        fileContentPreview.getStyleClass().add("enhanced-text-flow");

        // רכיב טעינה
        spinner = new ProgressIndicator();
        spinner.setMaxSize(30, 30);
        spinner.setVisible(false);

        // יצירת StackPane שמכיל את תיבת הטקסט והספינר
        contentContainer = new StackPane();
        contentContainer.getChildren().addAll(fileContentPreview, spinner);

        // מרכז את הספינר בתוך ה-StackPane
        StackPane.setAlignment(spinner, Pos.CENTER);

        // הגדרת גדילה ל-StackPane במקום ל-fileContentPreview
        VBox.setVgrow(contentContainer, Priority.ALWAYS);

        currentContentPane = new VBox(contentContainer);
        VBox.setVgrow(currentContentPane, Priority.ALWAYS);

        previewPane.getChildren().addAll(topRow, currentContentPane);
    }

    /**
     * יצירת כפתורי הניווט עם קלאסי CSS
     */
    private void createNavigationButtons() {
        // יצירת הכפתורים
        firstBtn = new Button("⏮");
        firstBtn.setOnAction(e -> fileContentPreview.moveFirst());
        firstBtn.setTooltip(new Tooltip("מעבר להדגשה הראשונה"));
        firstBtn.setDisable(true);

        prevBtn = new Button("⏪");
        prevBtn.setOnAction(e -> fileContentPreview.movePrev());
        prevBtn.setTooltip(new Tooltip("מעבר להדגשה הקודמת"));
        prevBtn.setDisable(true);

        nextBtn = new Button("⏩");
        nextBtn.setOnAction(e -> fileContentPreview.moveNext());
        nextBtn.setTooltip(new Tooltip("מעבר להדגשה הבאה"));
        nextBtn.setDisable(true);

        lastBtn = new Button("⏭");
        lastBtn.setOnAction(e -> fileContentPreview.moveLast());
        lastBtn.setTooltip(new Tooltip("מעבר להדגשה האחרונה"));
        lastBtn.setDisable(true);

        // הוספת קלאסי CSS לכפתורים
        firstBtn.getStyleClass().addAll("navigation-button", "first-button");
        prevBtn.getStyleClass().addAll("navigation-button", "prev-button");
        nextBtn.getStyleClass().addAll("navigation-button", "next-button");
        lastBtn.getStyleClass().addAll("navigation-button", "last-button");
    }

    /**
     * עדכון מצב כפתורי הניווט
     */
    private void updateNavigationButtons() {
        boolean hasHighlights = fileContentPreview.hasHighlights();
        boolean hasText = fileContentPreview.getText() != null && !fileContentPreview.getText().trim().isEmpty();

        firstBtn.setDisable(!hasHighlights || !hasText);
        prevBtn.setDisable(!hasHighlights || !hasText);
        nextBtn.setDisable(!hasHighlights || !hasText);
        lastBtn.setDisable(!hasHighlights || !hasText);
    }

    /**
     * הצגת תצוגה מקדימה של קובץ - עם זיהוי סוג החיפוש
     */
    public void showFilePreview(SearchResult result) {
        clearContent();

        if (result == null) return;

        // הצגת הספינר
        spinner.setVisible(true);

        // יצירת משימת רקע לטעינת הקובץ
        Task<String> loadPreviewTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                if (TextExtractor.isSupportedFile(Paths.get(result.getFullPath())) && searchModel.isSearchInContent()) {
                    return readTextFileContent(result.getFullPath());
                }
                return generateFileDetailsMessage(result);
            }
        };

        // הצגת התוצאה כשסיים
        loadPreviewTask.setOnSucceeded(event -> {
            spinner.setVisible(false);

            fileContentPreview.setText(loadPreviewTask.getValue());

            // highlighting the search query in the preview
            String textToHighlight = searchModel.getSearchQuery();
            if (textToHighlight != null && !textToHighlight.trim().isEmpty()) {
                EnhancedTextFlow.HighlightMode highlightMode = searchModel.isExactMatch()
                        ? EnhancedTextFlow.HighlightMode.EXACT
                        : EnhancedTextFlow.HighlightMode.ALL_SEQUENCES;
                fileContentPreview.highlightString(textToHighlight, highlightMode);
            }

            // עדכון מצב הכפתורים אחרי ההדגשה
            updateNavigationButtons();
        });

        // במקרה של שגיאה
        loadPreviewTask.setOnFailed(event -> {
            fileContentPreview.setText("שגיאה בטעינת התצוגה המקדימה.");
            spinner.setVisible(false);
            updateNavigationButtons();
        });

        // הרצת המשימה
        new Thread(loadPreviewTask).start();
    }

    /**
     * הצגת פרטי הקובץ (לחיפוש שם)
     */
    private void showFileDetails(SearchResult result) {
        String fileDetails = generateFileDetailsMessage(result);
        fileContentPreview.setText(fileDetails);

        // אין הדגשות בתצוגת פרטים
        updateNavigationButtons();
    }

    /**
     * יצירת הודעת פרטי קובץ מפורטת
     */
    private String generateFileDetailsMessage(SearchResult result) {
        File file = new File(result.getFullPath());
        StringBuilder details = new StringBuilder();

        details.append("--- פרטי הקובץ ---").append("\n\n");

        details.append("שם הקובץ: ").append(result.getName()).append("\n");
        // סוג הקובץ
        details.append("סוג קובץ: ").append(FileType.getFileType(file)).append("\n");
        // גודל הקובץ
        details.append("גודל: ").append(result.getSize()).append("\n");

        details.append("\nכדי להפעיל את הקובץ, לחץ לחיצה כפולה על התוצאה.");

        return details.toString();
    }

    private void clearContent() {
        fileContentPreview.setText("");
        fileContentPreview.clearHighlights();
        updateNavigationButtons();
    }

    /**
     * הדגשת מחרוזות
     */
    public void highlightStrings(List<String> strings) {
        if (strings != null && !strings.isEmpty()) {
            fileContentPreview.highlightStrings(strings);
            updateNavigationButtons();
        }
    }

    /**
     * ניקוי הדגשות
     */
    public void clearHighlights() {
        fileContentPreview.clearHighlights();
        updateNavigationButtons();
    }

    /**
     * קריאת תוכן קובץ טקסט
     */
    private String readTextFileContent(String filePath) {
        try {
            return TextExtractor.extractText(new File(filePath));
        } catch (IOException e) {
            return "שגיאה בקריאת הקובץ: " + e.getMessage();
        }
    }

    /**
     * קבלת הפאנל הראשי
     */
    public VBox getPane() {
        return previewPane;
    }
}