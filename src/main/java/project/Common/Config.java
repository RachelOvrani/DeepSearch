package project.Common;

import java.io.*;
import java.util.*;

/**
 * ניהול קונפיגורציית התוכנית - גם ערכים כלליים וגם כוננים מאונדקסים
 */
public class Config {
    private static final String CONFIG_FILE = "config.properties";
    private static Config instance;

    private final Properties properties = new Properties();
    private Set<String> indexedDrives = new HashSet<>();

    // ברירות מחדל
    private static final Map<String, String> DEFAULTS = new HashMap<>();
    static {
        DEFAULTS.put("index.path", "./index");
        DEFAULTS.put("paths.file", "./index/paths.dat");
        DEFAULTS.put("name.temp.path", "./name-temp");
        DEFAULTS.put("content.temp.path", "./content-temp");



        DEFAULTS.put("name.postings.path", "./index/name_postings.dat");
        DEFAULTS.put("content.postings.path", "./index/content_postings.dat");
        DEFAULTS.put("name.tree.path", "./index/name_tree.idx");
        DEFAULTS.put("content.tree.path", "./index/content_tree.idx");
        DEFAULTS.put("name.len.path", "./index/name_len.dat");
        DEFAULTS.put("content.len.path", "./index/content_len.dat");

        DEFAULTS.put("content.max.memory", "1073741824");
        DEFAULTS.put("name.max.memory", "1073741824");
        DEFAULTS.put("page.size", "8192");
        DEFAULTS.put("buffer.size", "65536");

        DEFAULTS.put("index.types", "name,content");
        DEFAULTS.put("indexed.drives", "");
    }

    private Config() {
        loadConfig();
    }

    public static synchronized Config getInstance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    private void loadConfig() {
        boolean updated = false;
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    properties.load(fis);
                }
            }

            for (Map.Entry<String, String> entry : DEFAULTS.entrySet()) {
                if (!properties.containsKey(entry.getKey())) {
                    properties.setProperty(entry.getKey(), entry.getValue());
                    updated = true;
                }
            }

            String drives = properties.getProperty("indexed.drives", "");
            if (!drives.trim().isEmpty()) {
                indexedDrives = new HashSet<>(Arrays.asList(drives.split("\\s*,\\s*")));
            }

            if (updated || !configFile.exists()) {
                saveConfig();
            }

            ProjectLogger.info("Loaded configuration from " + CONFIG_FILE);
        } catch (Exception e) {
            ProjectLogger.warning("Could not load config: " + e.getMessage());
        }
    }

    private void saveConfig() {
        try {
            properties.setProperty("indexed.drives", String.join(",", indexedDrives));
            try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
                properties.store(fos, "Program Configuration");
            }
            ProjectLogger.info("Saved config to " + CONFIG_FILE);
        } catch (Exception e) {
            ProjectLogger.error("Could not save config: " + e.getMessage());
        }
    }

    // ========== קריאת פרמטרים ==========

    public String get(String key) {
        return properties.getProperty(key, DEFAULTS.getOrDefault(key, null));
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(get(key));
        } catch (Exception e) {
            return Integer.parseInt(DEFAULTS.getOrDefault(key, "0"));
        }
    }

    public List<String> getList(String key) {
        String raw = get(key);
        if (raw == null || raw.trim().isEmpty()) return Collections.emptyList();
        return Arrays.asList(raw.split("\\s*,\\s*"));
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
        saveConfig();
    }

    // ========== נתיבי קבצים ==========

    public int getBufferSize() {
        return getInt("buffer.size");
    }

    public int getPageSize() {
        return getInt("page.size");
    }
    public int getMaxMemory(String type) {
        return getInt(type + ".max.memory");
    }

    public String getIndexPath() {
        try {
            return new File(get("index.path")).getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getTempDir(String type) { return get(type + ".temp.path"); }


    public String getPathsFile() {
        return get("paths.file");
    }


    public String getLenFile(String type) {
        return get(type + ".len.path");
    }

    public String getPostingsFile(String type) {
        return get(type + ".postings.path");
    }

    public String getBPlusTreeFile(String type) {
        return get(type + ".tree.path");
    }

    // ========== כוננים מאונדקסים ==========

    public List<String> getIndexedDrives() {
        return new ArrayList<>(indexedDrives);
    }

    public void updateIndexedDrives(List<String> drives) {
        Set<String> newDrives = new HashSet<>(drives);
        if (!indexedDrives.equals(newDrives)) {
            indexedDrives = newDrives;
            saveConfig();
        }
    }

    public boolean hasNewDrives(List<String> selectedDrives) {
        return !indexedDrives.equals(new HashSet<>(selectedDrives));
    }
}
