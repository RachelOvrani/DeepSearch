package project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import project.UI.View.*;

import java.util.Objects;

// מחלקה ראשית של האפליקציה
public class App extends Application {
    @Override
    public void start(Stage primaryStage) {
        //--module-path "C:\Program Files\Java\javafx-sdk-23.0.2\lib" --add-modules javafx.controls,javafx.fxml

        try {
            // יצירת התצוגה הראשית
            MainView mainView = new MainView();

            // יצירת הסצנה
            Scene scene = new Scene(mainView.getMainLayout(), 1300, 850);

            // טעינת קבצי CSS בשיטה בטוחה
            loadStylesheets(scene);

            // הגדרות החלון
            primaryStage.setTitle("DeepSearch");
            primaryStage.getIcons().add(
                    new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/search-icon.png")))
            );
            primaryStage.setScene(scene);
            primaryStage.show();

            primaryStage.setOnCloseRequest(event -> {
                mainView.cleanupBeforeExit();
            });

        } catch (Exception e) {
            e.printStackTrace();
            showError("שגיאה באתחול האפליקציה: " + e.getMessage());
        }
    }

    // טעינת קבצי סגנון בצורה בטוחה
    private void loadStylesheets(Scene scene) {
        try {
            // שיטה בטוחה לטעינת קבצי CSS
            String[] styleSheets = {
                    "/css/main-style-css.css",
                    "/css/search-pane-css.css",
                    "/css/filter-pane-css.css",
                    "/css/preview-pane-css.css",
                    "/css/results-pane-css.css",
                    "/css/setting-window-css.css"
            };

            for (String stylePath : styleSheets) {
                String styleResource = Objects.requireNonNull(
                        getClass().getResource(stylePath),
                        "קובץ סגנון חסר: " + stylePath
                ).toExternalForm();
                scene.getStylesheets().add(styleResource);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("שגיאה בטעינת קבצי CSS: " + e.getMessage());

            // במקרה של שגיאה, נוכל להמשיך ללא סגנונות
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("אזהרה");
            alert.setHeaderText("בעיה בטעינת קבצי סגנון");
            alert.setContentText("האפליקציה תפעל עם סגנון בסיסי. " + e.getMessage());
            alert.showAndWait();
        }
    }

    // הצגת הודעת שגיאה
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("שגיאה");
        alert.setHeaderText("התרחשה שגיאה");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}