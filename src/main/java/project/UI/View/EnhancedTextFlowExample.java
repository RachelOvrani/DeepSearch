package project.UI.View;

import javafx.application.Application;
import javafx.geometry.NodeOrientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

public class EnhancedTextFlowExample extends Application {

    @Override
    public void start(Stage primaryStage) {
        // יצירת הפקד המורחב - כל ההגדרות כבר בבנאי
        String sampleText = "אבג ילד בבב הולך בבב לגן ככככ ילד הולך גככככ הולך לגן גגגילד הולך לגןדדדד כגכגכג ילד הולך לגן עעעע";

        EnhancedTextFlow textFlow = new EnhancedTextFlow(sampleText);

        // שני כפתורים להשוואה בין המצבים
        Button exactBtn = new Button("צבע מחרוזת מדוייקת: 'ילד הולך לגן'");
        exactBtn.setOnAction(e -> {
            textFlow.highlightString("ילד הולך לגן", EnhancedTextFlow.HighlightMode.EXACT);
        });

        Button allSequencesBtn = new Button("צבע כל רצף אפשרי: 'ילד הולך לגן'");
        allSequencesBtn.setOnAction(e -> {
            textFlow.highlightString("ילד הולך לגן", EnhancedTextFlow.HighlightMode.ALL_SEQUENCES);
        });

        Button clearBtn = new Button("נקה הדגשות");
        clearBtn.setOnAction(e -> textFlow.clearHighlights());

        // כפתורי ניווט
        Button firstBtn = new Button("⏮ ראשון");
        firstBtn.setOnAction(e -> textFlow.moveFirst());

        Button prevBtn = new Button("⏪ קודם");
        prevBtn.setOnAction(e -> textFlow.movePrev());

        Button nextBtn = new Button("⏩ הבא");
        nextBtn.setOnAction(e -> textFlow.moveNext());

        Button lastBtn = new Button("⏭ אחרון");
        lastBtn.setOnAction(e -> textFlow.moveLast());

        // ארגון כפתורי הניווט
        HBox navigationBox = new HBox(5);
        navigationBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        navigationBox.getChildren().addAll(firstBtn, prevBtn, nextBtn, lastBtn);

        // ארגון הממשק
        VBox buttonsBox = new VBox(5);
        buttonsBox.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        buttonsBox.getChildren().addAll(
                exactBtn,
                allSequencesBtn,
                clearBtn,
                navigationBox
        );

        VBox root = new VBox(10);
        root.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        root.getChildren().addAll(textFlow, buttonsBox);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setTitle("Enhanced TextFlow Example");
        primaryStage.setScene(scene);
        primaryStage.show();

        // דוגמה לשימוש ישיר בקוד
        demonstrateUsage(textFlow);
    }

    private void demonstrateUsage(EnhancedTextFlow textFlow) {
        // הדגמה של כל האופציות עבור "ילד הולך לגן"
        // יצבע את: "ילד", "הולך", "לגן", "ילד הולך", "הולך לגן", "ילד הולך לגן"
        textFlow.highlightString("ילד הולך לגן", EnhancedTextFlow.HighlightMode.ALL_SEQUENCES);
    }

    public static void main(String[] args) {
        launch(args);
    }
}