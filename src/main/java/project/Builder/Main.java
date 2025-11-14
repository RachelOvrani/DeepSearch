package project.Builder;


import project.Common.Config;
import project.Common.ProjectLogger;

import java.io.*;
import java.nio.file.*;
import java.util.*;


public class Main {

    static Config config;
    public static void main(String[] args) {
        config = Config.getInstance();
        try {
            runIndexBuilder();

            // בדיקת התוצאות
            validateResults();

        } catch (Exception e) {
            System.out.println("שגיאה כללית: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private static void runIndexBuilder() throws IOException {
        ProjectLogger.info("מתחיל בניית אינדקס...");

        // יצירת service לבניית אינדקס
        IndexBuilderService indexBuilder = new IndexBuilderService();

        // רשימת התיקיות לאינדוקס
        List<Path> pathsToIndex = List.of(Paths.get("F:\\"));
        //List<Path> pathsToIndex = List.of(Paths.get("D:\\Users\\רחלי"));



        // הפעלת הבנייה
        long startTime = System.currentTimeMillis();
        indexBuilder.start(pathsToIndex);
        long endTime = System.currentTimeMillis();

        ProjectLogger.info("בניית האינדקס הושלמה בהצלחה.");
        System.out.println("בניית האינדקס הושלמה בהצלחה.");
        System.out.println("זמן בנייה: " + (endTime - startTime) + " מילישניות");
    }

    /**
     * בדיקת התוצאות שנוצרו
     */
    private static void validateResults() {
        System.out.println("בודק קבצי אינדקס שנוצרו...");

        String[] expectedFiles = {
                config.getPostingsFile("name"),
                config.getBPlusTreeFile("name"),
                config.getLenFile("name"),
                config.getPostingsFile("content"),
                config.getBPlusTreeFile("content"),
                config.getLenFile("content"),
                config.getPathsFile()
        };

        boolean allFilesExist = true;
        long totalSize = 0;

        for (String fileName : expectedFiles) {
            File file = new File(fileName);
            if (file.exists()) {
                long size = file.length();
                totalSize += size;
                System.out.println("✓ " + fileName + " (" + formatBytes(size) + ")");
            } else {
                System.out.println("✗ חסר קובץ: " + fileName);
                allFilesExist = false;
            }
        }

        if (allFilesExist) {
            System.out.println("כל קבצי האינדקס נוצרו בהצלחה.");
            System.out.println("גודל כולל: " + formatBytes(totalSize));

        } else {
            System.out.println("חלק מקבצי האינדקס לא נוצרו!");
        }
    }


    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}