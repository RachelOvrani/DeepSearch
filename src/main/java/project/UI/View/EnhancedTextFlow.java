package project.UI.View;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnhancedTextFlow extends VBox {

    private TextFlow textFlow;
    private ScrollPane scrollPane;
    private String originalText = "";
    private List<VBox> highlightedNodes = new ArrayList<>();
    private int currentHighlightIndex = -1;
    private Color currentHighlightColor = Color.YELLOW;
    private Color focusHighlightColor = Color.web("#9bc3e0");

    public EnhancedTextFlow() {
        super();
        initializeTextFlow();
        setupScrollPane();
        setupLayout();
    }

    public EnhancedTextFlow(String text) {
        super();
        this.originalText = text;
        initializeTextFlow();
        setupScrollPane();
        setupLayout();
        setText(text);
    }

    /**
     * אתחול TextFlow ללא עיצוב פנימי
     */
    private void initializeTextFlow() {
        textFlow = new TextFlow();
        textFlow.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
    }

    /**
     * הגדרת ScrollPane בסיסי ללא עיצוב
     */
    private void setupScrollPane() {
        scrollPane = new ScrollPane(textFlow);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    }

    /**
     * הגדרת המבנה הכללי של הפקד
     */
    private void setupLayout() {
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        this.getChildren().add(scrollPane);
        this.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
    }

    /**
     * הגדרת טקסט לתצוגה
     */
    public void setText(String text) {
        this.originalText = text;
        textFlow.getChildren().clear();

        if (text != null && !text.isEmpty()) {
            Text textNode = new Text(text);
            textFlow.getChildren().add(textNode);
        }
    }

    /**
     * קבלת הטקסט הנוכחי
     */
    public String getText() {
        return originalText;
    }

    /**
     * הדגשת רשימת מחרוזות ברקע צהוב
     */
    public void highlightStrings(List<String> stringsToHighlight) {
        highlightStrings(stringsToHighlight, Color.YELLOW);
    }

    /**
     * הדגשת רשימת מחרוזות בצבע רקע מותאם
     */
    public void highlightStrings(List<String> stringsToHighlight, Color backgroundColor) {
        if (originalText == null || originalText.isEmpty() || stringsToHighlight == null || stringsToHighlight.isEmpty()) {
            return;
        }

        textFlow.getChildren().clear();
        highlightedNodes.clear();
        currentHighlightIndex = -1;
        this.currentHighlightColor = backgroundColor;

        String text = originalText;
        boolean[] highlighted = new boolean[text.length()];

        // סימון כל התווים שצריכים להיות מודגשים
        for (String stringToHighlight : stringsToHighlight) {
            int startIndex = 0;
            while (true) {
                int index = text.indexOf(stringToHighlight, startIndex);
                if (index == -1) break;

                if (isWholeWord(text, index, stringToHighlight)) {
                    for (int i = index; i < index + stringToHighlight.length(); i++) {
                        highlighted[i] = true;
                    }
                }

                startIndex = index + 1;
            }
        }

        // בניית הטקסט לפי הסימון
        int currentIndex = 0;
        while (currentIndex < text.length()) {
            if (highlighted[currentIndex]) {
                int startHighlight = currentIndex;
                while (currentIndex < text.length() && highlighted[currentIndex]) {
                    currentIndex++;
                }
                String highlightedText = text.substring(startHighlight, currentIndex);
                addHighlightedText(highlightedText, backgroundColor);
            } else {
                int startNormal = currentIndex;
                while (currentIndex < text.length() && !highlighted[currentIndex]) {
                    currentIndex++;
                }
                String normalText = text.substring(startNormal, currentIndex);
                addNormalText(normalText);
            }
        }
    }

    /**
     * בדיקה האם המחרוזת היא מילה שלמה ולא חלק ממילה אחרת
     */
    private boolean isWholeWord(String text, int startIndex, String word) {
        int endIndex = startIndex + word.length();

        if (startIndex > 0) {
            char charBefore = text.charAt(startIndex - 1);
            if (Character.isLetterOrDigit(charBefore)) {
                return false;
            }
        }

        if (endIndex < text.length()) {
            char charAfter = text.charAt(endIndex);
            if (Character.isLetterOrDigit(charAfter)) {
                return false;
            }
        }

        return true;
    }

    /**
     * הוספת טקסט רגיל ל-TextFlow
     */
    private void addNormalText(String text) {
        Text textNode = new Text(text);
        textFlow.getChildren().add(textNode);
    }

    /**
     * הוספת טקסט מודגש עם רקע צבעוני
     */
    private void addHighlightedText(String text, Color backgroundColor) {
        Text highlightedText = new Text(text);
        highlightedText.setFill(Color.BLACK);

        Background coloredBackground = new Background(
                new BackgroundFill(backgroundColor, CornerRadii.EMPTY, Insets.EMPTY)
        );

        VBox textContainer = new VBox();
        textContainer.setBackground(coloredBackground);
        textContainer.getChildren().add(highlightedText);

        textFlow.getChildren().add(textContainer);
        highlightedNodes.add(textContainer);
    }

    /**
     * ניקוי כל ההדגשות והחזרה לטקסט רגיל
     */
    public void clearHighlights() {
        highlightedNodes.clear();
        currentHighlightIndex = -1;
        setText(originalText);
    }

    /**
     * הוספת טקסט לטקסט הקיים
     */
    public void appendText(String text) {
        if (text != null) {
            this.originalText += text;
            setText(this.originalText);
        }
    }

    /**
     * הדגשת מחרוזת יחידה בצהוב
     */
    public void highlightString(String stringToHighlight) {
        highlightStrings(Arrays.asList(stringToHighlight), Color.YELLOW);
    }

    /**
     * אפשרויות הדגשה
     */
    public enum HighlightMode {
        EXACT,
        INDIVIDUAL,
        ALL_SEQUENCES
    }

    /**
     * הדגשת מחרוזת עם מצב מותאם
     */
    public void highlightString(String stringToHighlight, HighlightMode mode) {
        if (stringToHighlight == null || stringToHighlight.trim().isEmpty()) {
            return;
        }

        List<String> stringsToHighlight;

        switch (mode) {
            case EXACT:
                stringsToHighlight = Arrays.asList(stringToHighlight);
                break;
            case INDIVIDUAL:
                stringsToHighlight = getIndividualWords(stringToHighlight);
                break;
            case ALL_SEQUENCES:
                stringsToHighlight = getAllWordSequences(stringToHighlight);
                break;
            default:
                stringsToHighlight = Arrays.asList(stringToHighlight);
        }

        highlightStrings(stringsToHighlight, Color.YELLOW);
    }

    /**
     * יצירת רשימה של מילים בודדות
     */
    private List<String> getIndividualWords(String text) {
        String[] words = text.trim().split("\\s+");
        return Arrays.asList(words);
    }

    /**
     * יצירת רשימה של כל רצפי המילים האפשריים
     */
    private List<String> getAllWordSequences(String text) {
        String[] words = text.trim().split("\\s+");
        List<String> sequences = new ArrayList<>();

        for (String word : words) {
            sequences.add(word);
        }

        for (int length = 2; length <= words.length; length++) {
            for (int start = 0; start <= words.length - length; start++) {
                StringBuilder sequence = new StringBuilder();
                for (int i = start; i < start + length; i++) {
                    if (i > start) {
                        sequence.append(" ");
                    }
                    sequence.append(words[i]);
                }
                sequences.add(sequence.toString());
            }
        }

        return sequences;
    }

    public void highlightInYellow(List<String> strings) {
        highlightStrings(strings, Color.YELLOW);
    }

    public void highlightInRed(List<String> strings) {
        highlightStrings(strings, Color.LIGHTCORAL);
    }

    public void highlightInGreen(List<String> strings) {
        highlightStrings(strings, Color.LIGHTGREEN);
    }

    /**
     * מעבר להדגשה הראשונה
     */
    public void moveFirst() {
        if (!highlightedNodes.isEmpty()) {
            setCurrentHighlight(0);
        }
    }

    /**
     * מעבר להדגשה הבאה
     */
    public void moveNext() {
        if (!highlightedNodes.isEmpty()) {
            int nextIndex = (currentHighlightIndex + 1) % highlightedNodes.size();
            setCurrentHighlight(nextIndex);
        }
    }

    /**
     * מעבר להדגשה הקודמת
     */
    public void movePrev() {
        if (!highlightedNodes.isEmpty()) {
            int prevIndex = currentHighlightIndex - 1;
            if (prevIndex < 0) {
                prevIndex = highlightedNodes.size() - 1;
            }
            setCurrentHighlight(prevIndex);
        }
    }

    /**
     * מעבר להדגשה האחרונה
     */
    public void moveLast() {
        if (!highlightedNodes.isEmpty()) {
            setCurrentHighlight(highlightedNodes.size() - 1);
        }
    }

    /**
     * הגדרת ההדגשה הנוכחית והזזת הגלילה אליה
     */
    private void setCurrentHighlight(int index) {
        if (index < 0 || index >= highlightedNodes.size()) {
            return;
        }

        if (currentHighlightIndex >= 0 && currentHighlightIndex < highlightedNodes.size()) {
            VBox previousNode = highlightedNodes.get(currentHighlightIndex);
            Background normalBackground = new Background(
                    new BackgroundFill(currentHighlightColor, CornerRadii.EMPTY, Insets.EMPTY)
            );
            previousNode.setBackground(normalBackground);
        }

        currentHighlightIndex = index;

        VBox currentNode = highlightedNodes.get(currentHighlightIndex);
        Background focusBackground = new Background(
                new BackgroundFill(focusHighlightColor, CornerRadii.EMPTY, Insets.EMPTY)
        );
        currentNode.setBackground(focusBackground);

        scrollToNode(currentNode);
    }

    /**
     * גלילה אל צומת מסוים
     */
    private void scrollToNode(Node node) {
        Platform.runLater(() -> {
            Bounds nodeBounds = node.getBoundsInParent();
            Bounds textFlowBounds = textFlow.getBoundsInLocal();

            double nodeY = nodeBounds.getMinY();
            double textFlowHeight = textFlowBounds.getHeight();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            double scrollValue = 0.0;
            if (textFlowHeight > viewportHeight) {
                scrollValue = nodeY / (textFlowHeight - viewportHeight);
                scrollValue = Math.max(0.0, Math.min(1.0, scrollValue));
            }

            scrollPane.setVvalue(scrollValue);
        });
    }

    /**
     * קבלת מספר ההדגשות הכולל
     */
    public int getHighlightCount() {
        return highlightedNodes.size();
    }

    /**
     * קבלת האינדקס הנוכחי (מתחיל מ-1)
     */
    public int getCurrentHighlightNumber() {
        return currentHighlightIndex >= 0 ? currentHighlightIndex + 1 : 0;
    }

    /**
     * בדיקה האם יש הדגשות
     */
    public boolean hasHighlights() {
        return !highlightedNodes.isEmpty();
    }
}