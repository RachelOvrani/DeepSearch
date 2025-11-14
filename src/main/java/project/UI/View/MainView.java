package project.UI.View;

import javafx.geometry.Orientation;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import project.UI.Controller.IndexController;
import project.UI.Controller.SearchController;



// תצוגה ראשית של האפליקציה
public class MainView {
    private final BorderPane mainLayout;
    private final SearchController searchController;
    private final IndexController indexController;

    private final SearchPane searchPane;
    private final ResultsPane resultsPane;
    private final PreviewPane previewPane;
    private final FilterPane filterPane;

    // פאנלים נוספים לסידור הממשק
    private final SplitPane mainSplitPane;
    private final SplitPane contentSplitPane;

    /**
     * קונסטרקטור - יוצר את התצוגה הראשית
     */
    public MainView() {
        mainLayout = new BorderPane();

        searchController = new SearchController();
        indexController = new IndexController();

        // יצירת הקומפוננטות
        searchPane = new SearchPane(searchController, indexController);
        resultsPane = new ResultsPane(searchController);
        previewPane = new PreviewPane();
        filterPane = new FilterPane();

        // הגדרת אירועים
        setupEventHandlers();

        // יצירת פאנל תוכן אופקי - תוצאות וסינון
        contentSplitPane = new SplitPane();
        contentSplitPane.setOrientation(Orientation.HORIZONTAL);
        contentSplitPane.getItems().addAll(resultsPane.getPane(), filterPane.getPane());
        contentSplitPane.setDividerPositions(0.80);
        contentSplitPane.getStyleClass().add("horizontal-split-pane");

        // הוספת פאנל החיפוש למעלה
        VBox topSection = new VBox();
        topSection.getChildren().add(searchPane.getPane());
        topSection.getStyleClass().add("top-section");

        // חלוקת המסך לשלושה חלקים (עליון, אמצעי, תחתון)
        mainSplitPane = new SplitPane();
        mainSplitPane.setOrientation(Orientation.VERTICAL);
        mainSplitPane.getItems().addAll(
                topSection,
                contentSplitPane,
                previewPane.getPane()
        );

        // הגדרת יחסי גודל התחלתיים (חלק עליון, חלק אמצעי, חלק תחתון)
        mainSplitPane.setDividerPositions(0.15, 0.70);
        mainSplitPane.getStyleClass().add("main-split-pane");

        // הוספת ה-SplitPane הראשי למסך הראשי
        mainLayout.setCenter(mainSplitPane);

        // הוספת מחלקות סגנון לשורש
        mainLayout.getStyleClass().add("root-layout");
    }


    //הגדרת מגיבי אירועים בממשק
    private void setupEventHandlers() {
        // הגדרת אירוע לבחירת שורה בטבלת התוצאות - לתצוגה מקדימה
        resultsPane.getResultsTable().getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        previewPane.showFilePreview(newValue);
                    }
                });
    }

    public void cleanupBeforeExit() {
        indexController.shutdown();
        searchController.shutdown();
    }

    // מחזיר את הפאנל הראשי של האפליקציה
    public BorderPane getMainLayout() {
        return mainLayout;
    }
}