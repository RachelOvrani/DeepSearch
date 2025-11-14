package project.Common;

import java.util.Arrays;
import java.util.List;

public class TextAnalyzer {

    public static List<String> tokenize(String text) {
        // פיצול לפי רווחים, תווי שורה, וסימני פיסוק עיקריים
        String[] tokens = text.split("[\\s\\p{Punct}]+");
        return Arrays.asList(tokens);
    }

    public static String normalize(String token) {
        return token
                .replaceAll("\\p{Cf}", "") // הסרת תווים בלתי נראים (כמו RLM, LRM)
                .toLowerCase()
                .replaceAll("^[\\p{Punct}\\s]+", "")   // הסרת תווים מההתחלה
                .replaceAll("[\\p{Punct}\\s]+$", "");  // הסרת תווים מהסוף
    }

}
