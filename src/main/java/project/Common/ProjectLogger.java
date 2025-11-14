package project.Common;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ProjectLogger {
    private static final String LOG_FILE = "log.txt";
    private static BufferedWriter writer;
    private static final Object lock = new Object();

    static {
        try {
            writer = new BufferedWriter(new FileWriter(LOG_FILE, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public static void info(String message) {
        writeLog("INFO", message);
    }

    public static void error(String message) {
        writeLog("ERROR", message);
    }
    public static void warning(String message) {
        writeLog("WARNING", message);
    }

    private static void writeLog(String level, String message) {
        synchronized (lock) {
            try {
                writer.write("[" + getTimestamp() + "] [" + level + "] " + message);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                System.err.println("Logging failed: " + e.getMessage());
            }
        }
    }

    public static void close() {
        try {
            if (writer != null) writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
