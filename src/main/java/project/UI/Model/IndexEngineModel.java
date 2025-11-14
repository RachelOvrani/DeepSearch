package project.UI.Model;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.concurrent.Task;
import project.Common.Config;
import project.Common.ProjectLogger;
import project.Builder.IndexBuilderService;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class IndexEngineModel {

    private IndexBuilderService indexBuilderService;
    private Config config;
    private ExecutorService executorService;

    // Properties for UI binding
    private final ReadOnlyBooleanWrapper indexingInProgress;
    private final ReadOnlyStringWrapper indexingStatus;
    private final ReadOnlyDoubleWrapper indexingProgress;

    // Singleton instance
    private static IndexEngineModel instance;

    public static IndexEngineModel getInstance() {
        if (instance == null) {
            instance = new IndexEngineModel();
        }
        return instance;
    }

    private IndexEngineModel(){
        this.config = Config.getInstance();

        // אתחול properties
        this.indexingInProgress = new ReadOnlyBooleanWrapper(false);
        this.indexingStatus = new ReadOnlyStringWrapper("מוכן לאינדוקס");
        this.indexingProgress = new ReadOnlyDoubleWrapper(0.0);

        // אתחול executor service
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("IndexingThread");
            return t;
        });

        try {
            indexBuilderService = new IndexBuilderService();
            config = Config.getInstance();
            ProjectLogger.info("IndexEngineModel initialized successfully");
        } catch (Exception e) {
            ProjectLogger.error("שגיאה באתחול שירות האינדוקס: " + e.getMessage());
            updateStatus("שגיאה באתחול השירות");
        }
    }

    public void startIndexing(List<String> drivePaths) {
        if (indexingInProgress.get()) {
            ProjectLogger.warning("Indexing already in progress");
            return;
        }

        // יצירת Task לאינדוקס
        Task<Void> indexingTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {


                    // המרת paths ל-Path objects
                    List<Path> pathsToIndex = new ArrayList<>();
                    for (String drivePath : drivePaths) {
                        pathsToIndex.add(Paths.get(drivePath));
                    }

                    updateMessage("מתחיל אינדוקס עבור " + drivePaths.size() + " כוננים");

                    // קריאה לשירות האינדוקס
                    indexBuilderService.start(pathsToIndex);

                    // עדכון קונפיגורציה
                    Platform.runLater(() -> {
                        config.updateIndexedDrives(drivePaths);
                    });


                    ProjectLogger.info("Indexing completed successfully for " + drivePaths.size() + " drives");

                } catch (Exception e) {
                    ProjectLogger.error("Indexing failed: " + e.getMessage());
                    updateMessage("שגיאה באינדוקס: " + e.getMessage());
                    throw e;
                }
                return null;
            }

            @Override
            protected void updateMessage(String message) {
                super.updateMessage(message);
                Platform.runLater(() -> updateStatus(message));
            }

            @Override
            protected void updateProgress(double workDone, double max) {
                super.updateProgress(workDone, max);
                Platform.runLater(() -> indexingProgress.set(workDone / max));
            }
        };

        // הגדרת event handlers
        indexingTask.setOnRunning(e -> {
            indexingInProgress.set(true);
            updateStatus("מתחיל אינדוקס...");
        });

        indexingTask.setOnSucceeded(e -> {
            indexingInProgress.set(false);
            updateStatus("אינדוקס הושלם בהצלחה");
            indexingProgress.set(0.0);
        });

        indexingTask.setOnFailed(e -> {
            indexingInProgress.set(false);
            Throwable exception = indexingTask.getException();
            String errorMsg = "שגיאה באינדוקס" + (exception != null ? ": " + exception.getMessage() : "");
            updateStatus(errorMsg);
            indexingProgress.set(0.0);
        });

        indexingTask.setOnCancelled(e -> {
            indexingInProgress.set(false);
            updateStatus("אינדוקס בוטל");
            indexingProgress.set(0.0);
        });

        // הרצת הTask
        executorService.submit(indexingTask);
    }


    private void updateStatus(String status) {
        Platform.runLater(() -> indexingStatus.set(status));
    }


    public List<String> getAvailableDrives() {
        List<String> drivers = new ArrayList<>();

        // קבלת כל הכוננים הזמינים
        File[] roots = File.listRoots();

        for (File root : roots) {
            if (root.exists() && root.canRead()) {
                drivers.add(root.getAbsolutePath());
            }
        }

        // אם לא נמצאו כוננים (לינוקס/מק), נוסיף נתיבים נפוצים
        if (drivers.isEmpty()) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                drivers.add(userHome);
            }
            drivers.add("/");
        }

        return drivers;
    }

    public List<String> getIndexedDrives() {
        return config.getIndexedDrives();
    }

    public boolean hasNewDrives(List<String> selectedDrives) {
        return config.hasNewDrives(selectedDrives);
    }

    public boolean isIndexingInProgress() {
        return indexingInProgress.get();
    }

    // Properties getters for UI binding
    public ReadOnlyBooleanProperty indexingInProgressProperty() {
        return indexingInProgress.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty indexingStatusProperty() {
        return indexingStatus.getReadOnlyProperty();
    }


    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}