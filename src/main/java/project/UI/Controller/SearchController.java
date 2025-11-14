package project.UI.Controller;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import project.UI.Model.SearchEngineModel;
import project.UI.Model.SearchModel;
import project.UI.Model.SearchResult;
import project.UI.Model.FileType;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// בקר לניהול החיפוש והאינטראקציה עם המשתמש
public class SearchController {
    public final SearchModel searchModel;
    private final SearchEngineModel searchEngineModel;
    private Task<Void> currentSearchTask;

    // שמירת התוצאות הלא מסוננות לצורך סינון בזמן אמת
    private List<SearchResult> lastUnfilteredResults;

    public SearchController() {
        this.searchModel = SearchModel.getInstance();
        this.searchEngineModel = SearchEngineModel.getInstance();

        // הוספת listener לסינון בזמן אמת
        setupRealTimeFiltering();
    }

    private void setupRealTimeFiltering() {
        // listener לשינוי סוג קובץ
        searchModel.fileTypeProperty().addListener((obs, oldVal, newVal) -> {
            applyCurrentFilters();
        });


//        searchModel.exactMatchProperty().addListener((obs, oldVal, newVal) -> {
//            searchModel.setFileType(FileType.ALL);
//        });
    }

    private void applyCurrentFilters() {
        if (lastUnfilteredResults != null && !lastUnfilteredResults.isEmpty()) {
            List<SearchResult> filteredResults = filterByFileType(lastUnfilteredResults, searchModel.getFileType());

            Platform.runLater(() -> {
                searchModel.clearAndSetSearchResults(filteredResults);
                searchModel.setResultsCount(filteredResults.size());

                String statusMessage = filteredResults.isEmpty() ?
                        "לא נמצאו תוצאות אחרי סינון" :
                        "מוצגות " + filteredResults.size() + " תוצאות מסוננות מתוך " + lastUnfilteredResults.size();

                searchModel.setStatusMessage(statusMessage);
            });
        }
    }


    public void performSearch() {
        // עדכון מודל החיפוש
        searchModel.setStatusMessage("מחפש...");
        searchModel.clearAndSetSearchResults(new ArrayList<>());
        searchModel.setResultsCount(0);

        // יצירת משימת חיפוש ברקע
        currentSearchTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                try {

                    performActualSearch();
                    // סינון לפי סוג קובץ אם נדרש
                    List<SearchResult> filteredResults = lastUnfilteredResults;
                    if (searchModel.getFileType() != FileType.ALL) {
                        filteredResults = filterByFileType(lastUnfilteredResults, searchModel.getFileType());
                    }

                    final List<SearchResult> finalDocResults = new ArrayList<>(filteredResults);
                    Platform.runLater(() -> {
                        searchModel.clearAndSetSearchResults(finalDocResults);
                        searchModel.setResultsCount(finalDocResults.size());

                        String statusMessage = finalDocResults.isEmpty() ?
                                "לא נמצאו תוצאות עבור: " + searchModel.getSearchQuery() :
                                lastUnfilteredResults.size() == finalDocResults.size() ?
                                        "נמצאו " + finalDocResults.size() + " תוצאות עבור: " + searchModel.getSearchQuery() :
                                        "מוצגות " + finalDocResults.size() + " תוצאות מסוננות מתוך " + lastUnfilteredResults.size() + " עבור: " + searchModel.getSearchQuery();
                        searchModel.setStatusMessage(statusMessage);
                    });



                } catch (Exception e) {
                    Platform.runLater(() -> {
                        searchModel.setStatusMessage("שגיאה בחיפוש: " + e.getMessage());
                    });
                    throw e;
                }
                return null;
            }
        };

        // הרצת המשימה בתהליך נפרד
        Thread searchThread = new Thread(currentSearchTask);
        searchThread.setDaemon(true);
        searchThread.start();
    }


    private void performActualSearch() {
        System.out.println("Starting search for: " + searchModel.getSearchQuery());
        List<SearchResult> results = new ArrayList<>();
        try {
            if(searchModel.isSearchInContent() && searchModel.isExactMatch())
                results = searchEngineModel.searchInContentExactMatch(searchModel.getSearchQuery());

            else if(searchModel.isSearchInContent() && (!searchModel.isExactMatch()))
                results = searchEngineModel.searchInContentNOExactMatch(searchModel.getSearchQuery());

            else if((!searchModel.isSearchInContent()) && searchModel.isExactMatch())
                results = searchEngineModel.searchInNameExactMatch(searchModel.getSearchQuery());

            else if((!searchModel.isSearchInContent()) && (!searchModel.isExactMatch()))
                results = searchEngineModel.searchInNameNOExactMatch(searchModel.getSearchQuery());


            lastUnfilteredResults = new ArrayList<>(results);
            System.out.println("Search completed. Found " + results.size() + " results.\n");
        } catch (IOException e) {
            Platform.runLater(() -> {
                searchModel.setStatusMessage("לא קיים אינדקס. יש לבנות אינדקס לפני חיפוש.");
            });
        }
    }

    private List<SearchResult> filterByFileType(List<SearchResult> results, FileType fileType) {
        return results.stream()
                .filter(result -> matchesFileType(result, fileType))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }


    private boolean matchesFileType(SearchResult result, FileType fileType) {
        FileType resultType = FileType.getFileType(new File(result.getFullPath()));

        switch (fileType) {
            case TEXT:
                return resultType == FileType.TEXT;
            case IMAGE:
                return resultType == FileType.IMAGE;
            case DOCUMENT:
                return resultType == FileType.DOCUMENT || resultType == FileType.TEXT;
            case VIDEO:
                return resultType == FileType.VIDEO;
            case AUDIO:
                return resultType == FileType.AUDIO;
            case FOLDER:
                return resultType == FileType.FOLDER;
            case ALL:
            default:
                return true;
        }
    }

    public void openFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                } else {
                    showAlert("שגיאה", "פתיחת קבצים אינה נתמכת במערכת זו");
                }
            } else {
                showAlert("שגיאה", "הקובץ לא נמצא: " + filePath);
            }
        } catch (IOException e) {
            showAlert("שגיאה", "לא ניתן לפתוח את הקובץ: " + e.getMessage());
        }
    }

    public void openFileLocation(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                File parent = file.getParentFile();
                if (parent != null && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(parent);
                } else {
                    showAlert("שגיאה", "פתיחת תיקיות אינה נתמכת במערכת זו");
                }
            } else {
                showAlert("שגיאה", "הקובץ לא נמצא: " + filePath);
            }
        } catch (IOException e) {
            showAlert("שגיאה", "לא ניתן לפתוח את מיקום הקובץ: " + e.getMessage());
        }
    }



    public void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    public ObservableList<SearchResult> getResults() {
        return searchModel.getSearchResults();
    }

    public void stopCurrentSearch() {
        if (currentSearchTask != null && !currentSearchTask.isDone()) {
            currentSearchTask.cancel(true);
            searchModel.setStatusMessage("החיפוש הופסק.");
        }
    }

    public void reset() {
        stopCurrentSearch();
        searchModel.setSearchQuery("");
        searchModel.setResultsCount(0);
        searchModel.setStatusMessage("מוכן לחיפוש.");
    }


    public void shutdown() {
        searchEngineModel.shutdown();
    }

}