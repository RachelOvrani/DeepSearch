package project.UI.Controller;

import project.UI.Model.IndexEngineModel;
import project.UI.Model.SearchModel;
import project.Common.ProjectLogger;

import java.util.List;

// בקר לניהול תהליכי האינדוקס
public class IndexController {
    private final IndexEngineModel indexEngineModel;
    private final SearchModel searchModel;

    public IndexController() {
        this.indexEngineModel = IndexEngineModel.getInstance();
        this.searchModel = SearchModel.getInstance();
        System.out.println("IndexController initialized");
    }


    // התחלת תהליך אינדוקס
    public void startIndexing(List<String> paths) {
        if (paths == null || paths.isEmpty()) {
            ProjectLogger.warning("No paths provided for indexing");
            return;
        }

        if (indexEngineModel.isIndexingInProgress()) {
            ProjectLogger.warning("Indexing already in progress, ignoring request");
            return;
        }

        System.out.println("Starting indexing for " + paths.size() + " drives: " + paths);

        // בדיקה האם יש כוננים חדשים
        boolean hasNewDrives = indexEngineModel.hasNewDrives(paths);

        if (hasNewDrives) {
            System.out.println("New drives detected, starting full indexing");
            indexEngineModel.startIndexing(paths);

            // הוספת listener לעדכון SearchModel כשהאינדקס מסתיים
            indexEngineModel.indexingInProgressProperty().addListener((obs, wasIndexing, isIndexing) -> {
                if (wasIndexing && !isIndexing) {
                    // האינדקס הסתיים - עדכון זמינות החיפוש
                    searchModel.updateSearchAvailability();
                }
            });
        } else {
            System.out.println("No new drives detected, indexing skipped");
        }
    }

    // קבלת רשימת כל הכוננים הזמינים במערכת
    public List<String> getDrivers() {
        return indexEngineModel.getAvailableDrives();
    }

     // קבלת רשימת הכוננים המאינדקסים כרגע
     public List<String> getIndexedPaths() {
        return indexEngineModel.getIndexedDrives();
    }

    // בדיקה האם תהליך אינדוקס פעיל כרגע
    public boolean isIndexingInProgress() {
        return indexEngineModel.isIndexingInProgress();
    }

    // קבלת מודל האינדוקס לצורך binding
    public IndexEngineModel getIndexEngineModel() {
        return indexEngineModel;
    }

    // בדיקה האם יש צורך באינדוקס
    public boolean needsIndexing(List<String> selectedDrives) {
        return indexEngineModel.hasNewDrives(selectedDrives);
    }


    // ניקוי משאבים
    public void shutdown() {
        indexEngineModel.shutdown();
    }
}