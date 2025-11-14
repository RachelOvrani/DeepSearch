package project.Searcher;

import java.util.*;


// מנוע דירוג תוצאות חיפוש
public class RankingEngine {

    private final Map<Integer, Integer> docsLen;
    private final double avgDocLength;
    private final int docsCount;
    private final long totalLength;
    private final double k1 = 1.5;
    private final double b = 0.75;

    public RankingEngine(Integer documentCount, Long totalLength, Map<Integer, Integer> documentLengths) {
        this.docsCount = documentCount;
        this.totalLength = totalLength;
        this.avgDocLength = (double) this.totalLength / this.docsCount;
        this.docsLen = documentLengths;
        System.out.println("RankingEngine initialized with " + docsCount + " documents, total length: " + totalLength + ", avg length: " + avgDocLength);
    }


    // חישוב ציון BM25 למסמך - הפונקציה הראשית
    public double computeBM25ScoreForDocument(int docId,
                                              List<String> queryTerms,
                                              Map<String, Integer> termFrequenciesInDoc,
                                              Map<String, Integer> documentFrequencies) {
        double score = 0.0;

        for (String term : queryTerms) {
            int tf = termFrequenciesInDoc.getOrDefault(term, 0);
            int df = documentFrequencies.getOrDefault(term, 0);

            if (tf == 0 || df == 0) continue;

            double idf = Math.log(1 + (docsCount - df + 0.5) / (df + 0.5));
            double norm = tf * (k1 + 1) / (tf + k1 * (1 - b + b * (docsLen.get(docId) / avgDocLength)));

            score += idf * norm;
            //System.out.println("docId: " + docId + ", tf: " + tf + ", docLen: " + docsLen.get(docId) + ", score: " + score);
        }

        return score;
    }


    // חישוב בונוס קרבה בין מונחים
    public double calculateProximityBonus(Map<String, List<Integer>> termPositions) {
        if (termPositions.size() < 2) {
            return 0.0;
        }

        double proximityScore = 0.0;
        int pairCount = 0;

        // בדיקת כל זוגות מונחים
        String[] terms = termPositions.keySet().toArray(new String[0]);

        for (int i = 0; i < terms.length; i++) {
            for (int j = i + 1; j < terms.length; j++) {
                String term1 = terms[i];
                String term2 = terms[j];

                List<Integer> positions1 = termPositions.get(term1);
                List<Integer> positions2 = termPositions.get(term2);

                // מציאת המרחק המינימלי בין המונחים
                int minDistance = Integer.MAX_VALUE;
                for (int pos1 : positions1) {
                    for (int pos2 : positions2) {
                        int distance = Math.abs(pos1 - pos2);
                        minDistance = Math.min(minDistance, distance);
                    }
                }

                // ציון קרבה: ככל שהמרחק קטן יותר, הציון גבוה יותר
                if (minDistance < 10) { // בונוס רק למונחים קרובים
                    proximityScore += 1.0 / (1.0 + minDistance);
                    pairCount++;
                }
            }
        }

        // ממוצע הציון עבור כל הזוגות
        return pairCount > 0 ? proximityScore / pairCount : 0.0;
    }

}