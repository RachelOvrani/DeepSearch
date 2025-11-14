package project.UI.Model;

import project.Common.ProjectLogger;
import project.Searcher.IndexSearcherService;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// מודל לניהול חיפוש
public class SearchEngineModel {

    // Singleton instance
    private static SearchEngineModel instance;

    // שירות החיפוש
    private IndexSearcherService searchService;

    private SearchEngineModel() {
        initializeSearchService();
    }

    public static SearchEngineModel getInstance() {
        if (instance == null) {
            instance = new SearchEngineModel();
        }
        return instance;
    }

    private void initializeSearchService() {
        try {
            searchService = new IndexSearcherService();
        } catch (Exception e) {
            ProjectLogger.error("שגיאה באתחול שירות החיפוש: " + e.getMessage());
        }
    }


    public List<SearchResult> searchInContentExactMatch(String query) throws IOException {
        List<IndexSearcherService.FileResult> serviceResults;
        serviceResults = searchService.searchInContent(query);

        return convertToUIResults(serviceResults);
    }

    public List<SearchResult> searchInNameExactMatch(String query) throws IOException {
        List<IndexSearcherService.FileResult> serviceResults;
        serviceResults = searchService.searchInFileNames(query);

        return convertToUIResults(serviceResults);
    }
    public List<SearchResult> searchInContentNOExactMatch(String query) throws IOException {
        List<IndexSearcherService.FileResult> serviceResults;
        serviceResults = searchService.searchInContentWithRanking(query);

        return convertToUIResults(serviceResults);
    }
    public List<SearchResult> searchInNameNOExactMatch(String query) throws IOException {
        List<IndexSearcherService.FileResult> serviceResults;
        serviceResults = searchService.searchInFileNamesWithRanking(query);

        return convertToUIResults(serviceResults);
    }


    private List<SearchResult> convertToUIResults(List<IndexSearcherService.FileResult> serviceResults) {
        List<SearchResult> searchResults = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (IndexSearcherService.FileResult fileResult : serviceResults) {
            try {
                File file = new File(fileResult.path);

                // בדיקה שהקובץ עדיין קיים
                if (!file.exists()) {
                    continue;
                }

                String name = file.getName();
                String parentPath = file.getParent() != null ? file.getParent() : "";

                // חישוב גודל הקובץ
                String size = formatFileSize(file.length());

                // קבלת תאריכים
                String modified = dateFormat.format(new Date(file.lastModified()));
                String created = modified;

                // מיקום התוצאה
                String locationResult = "";
                if (!fileResult.positions.isEmpty()) {
                    locationResult = "מיקום: " + fileResult.positions.get(0);
                }

                SearchResult searchResult = new SearchResult(
                        name, parentPath, size, modified, created, locationResult
                );

                searchResults.add(searchResult);

            } catch (Exception e) {
                System.err.println("שגיאה בעיבוד קובץ: " + fileResult.path);
            }
        }

        return searchResults;
    }


    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }



    public void shutdown() {
        if (searchService != null) {
            try {
                searchService.close();
            } catch (Exception e) {
                ProjectLogger.error("שגיאה בסגירת שירות החיפוש: " + e.getMessage());
            }
        }
    }


}