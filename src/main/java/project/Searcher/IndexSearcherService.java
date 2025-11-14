package project.Searcher;

import project.Common.Config;
import project.Common.TextAnalyzer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

// מחלקה לשירות חיפוש באינדקס
public class IndexSearcherService implements AutoCloseable {
    private Config config;

    private final BPlusTreeSearcher nameSearcher;
    private final BPlusTreeSearcher contentSearcher;
    private final PathResolver pathResolver;
    private final RankingEngine nameRankingEngine;
    private final RankingEngine contentRankingEngine;

    public IndexSearcherService() throws IOException {
        this.config = Config.getInstance();
        this.nameSearcher = new BPlusTreeSearcher(
                config.getBPlusTreeFile("name"),
                config.getPostingsFile("name"));

        this.contentSearcher = new BPlusTreeSearcher(
                config.getBPlusTreeFile("content"),
                config.getPostingsFile("content"));

        this.pathResolver = new PathResolver(config.getPathsFile());

        // טעינת אורכי מסמכים לדירוג
        BPlusTreeSearcher.DocumentLengthsStats nameLengths = nameSearcher.loadFileLengths(
                config.getLenFile("name")
        );
        BPlusTreeSearcher.DocumentLengthsStats contentLengths = contentSearcher.loadFileLengths(
                config.getLenFile("content")
        );

        this.contentRankingEngine = new RankingEngine(contentLengths.documentCount, contentLengths.totalLength, contentLengths.documentLengths);
        this.nameRankingEngine = new RankingEngine(nameLengths.documentCount, nameLengths.totalLength, nameLengths.documentLengths);

    }

    // חיפוש בשמות קבצים
    public List<FileResult> searchInFileNames(String query) throws IOException {
        return performExactSearch(query, nameSearcher);
    }

    // חיפוש בתוכן קבצים
    public List<FileResult> searchInContent(String query) throws IOException {
        return performExactSearch(query, contentSearcher);
    }


    // חיפוש לא מדויק עם דירוג בשמות קבצים
    public List<FileResult> searchInFileNamesWithRanking(String query) throws IOException {
        return performFuzzySearchWithRanking(query, nameSearcher, nameRankingEngine);
    }

    // חיפוש לא מדויק עם דירוג בתוכן קבצים
    public List<FileResult> searchInContentWithRanking(String query) throws IOException {
        return performFuzzySearchWithRanking(query, contentSearcher, contentRankingEngine);
    }



    private List<FileResult> performFuzzySearchWithRanking(String query, BPlusTreeSearcher searcher, RankingEngine ranker) throws IOException {
        // נורמליזציה של החיפוש
        List<String> tokenizedQuery = TextAnalyzer.tokenize(query);
        List<String> normalizedQuery = new ArrayList<>();

        for (String token : tokenizedQuery) {
            normalizedQuery.add(TextAnalyzer.normalize(token));
        }

        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }


        // קבלת תוצאות לכל מונח
        Map<String, BPlusTreeSearcher.SearchResult> termResults = new HashMap<>();
        Map<String, Integer> documentFrequencies = new HashMap<>();

        for (String term : normalizedQuery) {
            BPlusTreeSearcher.SearchResult result = searcher.searchTerm(term);
            if (result != null) {
                termResults.put(term, result);
                documentFrequencies.put(term, result.numDocs);
            }
        }

        if (termResults.isEmpty()) {
            return new ArrayList<>();
        }

        // מציאת כל המסמכים שמכילים לפחות מונח אחד
        Map<Integer, DocumentMatch> documentMatches = new HashMap<>();

        for (Map.Entry<String, BPlusTreeSearcher.SearchResult> entry : termResults.entrySet()) {
            String term = entry.getKey();
            BPlusTreeSearcher.SearchResult result = entry.getValue();

            for (BPlusTreeSearcher.PostingEntry posting : result.postings) {
                DocumentMatch docMatch = documentMatches.computeIfAbsent(
                        posting.docId,
                        k -> new DocumentMatch(posting.docId)
                );
                docMatch.addTerm(term, posting.positions);
            }
        }

        List<FileResult> fuzzyMatches = new ArrayList<>();

        for (DocumentMatch docMatch : documentMatches.values()) {
            String filePath = pathResolver.getPath(docMatch.docId);
            if (filePath == null) continue;


            // התאמה לא מדויקת - חישוב ציון BM25
            double score = calculateBM25Score(docMatch, normalizedQuery, documentFrequencies, ranker);


            // איחוד כל המיקומים
            List<Integer> allPositions = docMatch.termPositions.values().stream()
                    .flatMap(List::stream)
                    .sorted()
                    .collect(Collectors.toList());

            fuzzyMatches.add(new FileResult(docMatch.docId, filePath, allPositions,
                    score, false));

        }

        // מיון התוצאות הלא מדויקות לפי ציון
        fuzzyMatches.sort((a, b) -> Double.compare(b.score, a.score));

        return new ArrayList<>(fuzzyMatches);
    }

    // חישוב ציון BM25 למסמך
    private double calculateBM25Score(DocumentMatch docMatch, List<String> queryTerms,
                                      Map<String, Integer> documentFrequencies, RankingEngine rankingEngine) {
        // יצירת מפה של תדירויות מונחים במסמך
        Map<String, Integer> termFrequencies = new HashMap<>();
        for (Map.Entry<String, List<Integer>> entry : docMatch.termPositions.entrySet()) {
            termFrequencies.put(entry.getKey(), entry.getValue().size());
        }

        return rankingEngine.computeBM25ScoreForDocument(
                docMatch.docId, queryTerms, termFrequencies, documentFrequencies
        );
    }

    private List<FileResult> performSearch(String query, BPlusTreeSearcher searcher) throws IOException {
        // נורמליזציה של החיפוש
        String normalizedQuery = TextAnalyzer.normalize(query);

        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        // חיפוש בעץ
        BPlusTreeSearcher.SearchResult result = searcher.searchTerm(normalizedQuery);

        if (result == null) {
            return new ArrayList<>();
        }

        // המרה לתוצאות עם נתיבי קבצים
        List<FileResult> fileResults = new ArrayList<>();

        for (BPlusTreeSearcher.PostingEntry posting : result.postings) {
            String filePath = pathResolver.getPath(posting.docId);
            if (filePath != null) {
                fileResults.add(new FileResult(posting.docId, filePath, posting.positions));
            }
        }

        return fileResults;
    }

    private List<FileResult> performExactSearch(String query, BPlusTreeSearcher searcher) throws IOException {
        // נורמליזציה של החיפוש
        List<String> tokenizeQuery = TextAnalyzer.tokenize(query);

        List<String> normalizedQuery = new ArrayList<>();
        for(String token : tokenizeQuery) {
            normalizedQuery.add(TextAnalyzer.normalize(token));
        }

        if (normalizedQuery.isEmpty()) {
            return new ArrayList<>();
        }

        // אם יש רק מילה אחת, נחזור לחיפוש רגיל
        if (normalizedQuery.size() == 1) {
            return performSearch(query, searcher);
        }

        // קבלת מידע על כל המונחים
        List<BPlusTreeSearcher.SearchResult> searchResults = new ArrayList<>();
        for(String term : normalizedQuery) {
            BPlusTreeSearcher.SearchResult searchResult = searcher.searchTerm(term);
            if (searchResult != null) {
                searchResults.add(searchResult);
            } else {
                // אם מונח לא נמצא, נחזיר תוצאות ריקות
                return new ArrayList<>();
            }
        }

        List<List<BPlusTreeSearcher.PostingEntry>> postings = searchResults.stream()
                .map(r -> r.postings)
                .collect(Collectors.toList());

        // מציאת מסמכים שמכילים את הביטוי המדויק באמצעות merge algorithm
        List<FileResult> fileResults = findExactMatchesWithMerge(postings, normalizedQuery.size());

        return fileResults;
    }


    // מציאת התאמות מדויקות באמצעות merge algorithm
    private List<FileResult> findExactMatchesWithMerge(List<List<BPlusTreeSearcher.PostingEntry>> allPostings, int numTerms) throws IOException {
        List<FileResult> results = new ArrayList<>();

        // אינדקסים עבור כל רשימת postings
        int[] indices = new int[numTerms];

        while (true) {
            // בדיקה אם אחת מהרשימות נגמרה
            boolean anyListFinished = false;
            for (int i = 0; i < numTerms; i++) {
                if (indices[i] >= allPostings.get(i).size()) {
                    anyListFinished = true;
                    break;
                }
            }

            if (anyListFinished) {
                break; // יציאה מהלולאה
            }

            // קבלת המסמכים הנוכחיים בכל רשימה
            int[] currentDocs = new int[numTerms];
            for (int i = 0; i < numTerms; i++) {
                currentDocs[i] = allPostings.get(i).get(indices[i]).docId;
            }

            // מציאת המסמך המקסימלי
            int maxDoc = currentDocs[0];
            for (int i = 1; i < numTerms; i++) {
                if (currentDocs[i] > maxDoc) {
                    maxDoc = currentDocs[i];
                }
            }

            // בדיקה אם כל המסמכים זהים
            boolean allEqual = true;
            for (int i = 0; i < numTerms; i++) {
                if (currentDocs[i] != maxDoc) {
                    allEqual = false;
                    break;
                }
            }

            if (allEqual) {
                // כל המונחים מופיעים באותו מסמך - בדיקת רצף מדויק
                List<List<Integer>> docPositions = new ArrayList<>();
                for (int i = 0; i < numTerms; i++) {
                    docPositions.add(allPostings.get(i).get(indices[i]).positions);
                }

                List<Integer> exactMatchPositions = findExactSequence(docPositions);

                if (!exactMatchPositions.isEmpty()) {
                    String filePath = pathResolver.getPath(maxDoc);
                    if (filePath != null) {
                        results.add(new FileResult(maxDoc, filePath, exactMatchPositions));
                    }
                }

                // התקדמות בכל הרשימות
                for (int i = 0; i < numTerms; i++) {
                    indices[i]++;
                }
            } else {
                // התקדמות ברשימות שהמסמך שלהן קטן מהמקסימלי
                for (int i = 0; i < numTerms; i++) {
                    if (currentDocs[i] < maxDoc) {
                        // מתקדם עד שנגיע למסמך >= maxDoc
                        while (indices[i] < allPostings.get(i).size() &&
                                allPostings.get(i).get(indices[i]).docId < maxDoc) {
                            indices[i]++;
                        }
                    }
                }
            }
        }

        return results;
    }


    // מציאת רצפים מדויקים במיקומי המילים
    private List<Integer> findExactSequence(List<List<Integer>> docPositions) {
        List<Integer> exactMatches = new ArrayList<>();

        if (docPositions.isEmpty()) {
            return exactMatches;
        }

        // אינדקסים עבור כל רשימת מיקומים
        int[] positionIndices = new int[docPositions.size()];

        while (true) {
            // בדיקה אם אחת מרשימות המיקומים נגמרה
            boolean anyPositionListFinished = false;
            for (int i = 0; i < docPositions.size(); i++) {
                if (positionIndices[i] >= docPositions.get(i).size()) {
                    anyPositionListFinished = true;
                    break;
                }
            }

            if (anyPositionListFinished) {
                break;
            }

            // קבלת המיקומים הנוכחיים
            int[] currentPositions = new int[docPositions.size()];
            for (int i = 0; i < docPositions.size(); i++) {
                currentPositions[i] = docPositions.get(i).get(positionIndices[i]);
            }

            // בדיקה אם המיקומים יוצרים רצף
            int startPosition = currentPositions[0];
            boolean isSequence = true;

            for (int i = 1; i < currentPositions.length; i++) {
                if (currentPositions[i] != startPosition + i) {
                    isSequence = false;
                    break;
                }
            }

            if (isSequence) {
                // נמצא רצף מדויק!
                exactMatches.add(startPosition);

                // התקדמות בכל רשימות המיקומים
                for (int i = 0; i < docPositions.size(); i++) {
                    positionIndices[i]++;
                }
            } else {
                // מציאת המיקום המינימלי והתקדמות ברשימות המתאימות
                int minPosition = currentPositions[0];
                for (int i = 1; i < currentPositions.length; i++) {
                    if (currentPositions[i] < minPosition) {
                        minPosition = currentPositions[i];
                    }
                }

                // התקדמות ברשימות שהמיקום שלהן הוא המינימלי
                for (int i = 0; i < docPositions.size(); i++) {
                    if (currentPositions[i] == minPosition) {
                        positionIndices[i]++;
                    }
                }
            }
        }

        return exactMatches;
    }

    @Override
    public void close() throws IOException {
        if (nameSearcher != null) nameSearcher.close();
        if (contentSearcher != null) contentSearcher.close();
        if (pathResolver != null) pathResolver.close();
    }


    private static class DocumentMatch {
        final int docId;
        final Map<String, List<Integer>> termPositions;

        DocumentMatch(int docId) {
            this.docId = docId;
            this.termPositions = new HashMap<>();
        }

        void addTerm(String term, List<Integer> positions) {
            termPositions.put(term, positions);
        }
    }
    public static class FileResult {
        public int docId;
        public String path;
        public List<Integer> positions;
        public double score;
        public boolean isExactMatch;

        FileResult(int docId, String path, List<Integer> positions, double score, boolean isExactMatch) {
            this.docId = docId;
            this.path = path;
            this.positions = positions;
            this.score = score;
            this.isExactMatch = isExactMatch;
        }

        FileResult(int docId, String filePath, List<Integer> positions) {
            this(docId, filePath, positions, 0.0, true);
        }
    }
    private static class PathResolver implements AutoCloseable {
        private final RandomAccessFile pathsFile;

        PathResolver(String pathsFilePath) throws IOException {
            this.pathsFile = new RandomAccessFile(pathsFilePath, "r");
        }

        String getPath(int docId) throws IOException {
            pathsFile.seek(docId);

            try {
                return pathsFile.readUTF();
            } catch (EOFException e) {
                return null; // DocId לא תקין
            }
        }

        @Override
        public void close() throws IOException {
            if (pathsFile != null) {
                pathsFile.close();
            }
        }
    }


    public static void main(String[] args) {
        try (IndexSearcherService searchService = new IndexSearcherService()) {

            // דוגמאות חיפוש
            System.out.println("מתחיל דוגמאות חיפוש...\n");

            // חיפוש מדויק בשמות קבצים
            System.out.println("===  חיפוש מדויק בשמות קבצים (\"MVI\") ===");
            List<FileResult> nameResults = searchService.searchInFileNames("MVI");
            printResults(nameResults);

            // חיפוש מדויק בתוכן
            System.out.println("\n=== חיפוש מדויק בתוכן (\"hello World\") ===");
            List<FileResult> contentResults = searchService.searchInContent("hello World");
            printResults(contentResults);

            // חיפוש לא מדויק עם דירוג בתוכן
            System.out.println("\n=== חיפוש לא מדויק עם דירוג בתוכן (\"כלב\") ===");
            List<FileResult> rankedResults = searchService.searchInContentWithRanking("כלב");
            printRankedResults(rankedResults);

            Scanner scanner = new Scanner(System.in);
            String query;
            while(true){
                System.out.println("\nחיפוש מדויק בשמות קבצים .1");
                System.out.println("חיפוש מדויק בתוכן קבצים .2");
                System.out.println("חיפוש לא מדויק עם דירוג בשמות קבצים .3");
                System.out.println("חיפוש לא מדויק עם דירוג בתוכן קבצים .4");

                System.out.print("נא לבחור אפשרות (1-4) או לצאת (q):");
                String choice = scanner.nextLine();

                switch (choice){
                    case "1":
                        System.out.print("נא להזין מונח לחיפוש בשמות קבצים: ");
                        query = scanner.nextLine();
                        printResults(searchService.searchInFileNames(query));
                        break;
                    case "2":
                        System.out.print("נא להזין מונח לחיפוש בתוכן קבצים: ");
                        query = scanner.nextLine();
                        printResults(searchService.searchInContent(query));
                        break;
                    case "3":
                        System.out.print("נא להזין מונח לחיפוש לא מדויק בשמות קבצים: ");
                        query = scanner.nextLine();
                        printResults(searchService.searchInFileNamesWithRanking(query));
                        break;
                    case "4":
                        System.out.print("נא להזין מונח לחיפוש לא מדויק בתוכן קבצים: ");
                        query = scanner.nextLine();
                        printResults(searchService.searchInContentWithRanking(query));
                        break;
                    default:
                        System.out.println("קלט לא חוקי.");
                        break;

                }
            }

        } catch (IOException e) {
            System.err.println("שגיאה בחיפוש: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void printResults(List<FileResult> results) {
        System.out.println("נמצאו " + results.size() + " קבצים:");

        for (int i = 0; i < Math.min(results.size(), 20); i++) { // הצגת עד 20 תוצאות
            FileResult file = results.get(i);
            System.out.println((i + 1) + ". " + file.path);
            System.out.println("   מיקומים: " + file.positions.subList(0, Math.min(file.positions.size(), 5)));
        }

        if (results.size() > 20) {
            System.out.println("... ועוד " + (results.size() - 20) + " תוצאות");
        }
    }

    public static void printRankedResults(List<FileResult> results) {
        System.out.println("נמצאו " + results.size() + " קבצים (מדורגים):");

        for (int i = 0; i < Math.min(results.size(), 20); i++) {
            FileResult file = results.get(i);
            String matchType = file.isExactMatch ? "[מדויק]" : "[לא מדויק]";
            String scoreStr = file.isExactMatch ? "" : String.format(" (ציון: %.3f)", file.score);

            System.out.println((i + 1) + ". " + matchType + " " + file.path + scoreStr);
            System.out.println("   מיקומים: " + file.positions.subList(0, Math.min(file.positions.size(), 5)));
        }

        if (results.size() > 20) {
            System.out.println("... ועוד " + (results.size() - 20) + " תוצאות");
        }
    }
}