// 1. עדכון FileType.java - הוסף תיקיות כסוג חדש

package project.UI.Model;

import java.io.File;

/**
 * enum לסוגי קבצים שונים לסינון חיפוש
 */
public enum FileType {
    ALL("כל הקבצים"),
    FOLDER("תיקיות"),
    TEXT("קבצי טקסט"),
    IMAGE("תמונות"),
    DOCUMENT("מסמכים"),
    VIDEO("וידאו"),
    AUDIO("אודיו");


    private final String displayName;

    FileType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * מציאת FileType לפי שם התצוגה
     * @param displayName שם התצוגה
     * @return FileType המתאים או null אם לא נמצא
     */
    public static FileType fromDisplayName(String displayName) {
        for (FileType type : values()) {
            if (type.getDisplayName().equals(displayName)) {
                return type;
            }
        }
        return null;
    }

    private static final String TEXT_EXTENSIONS = "txt|csv|rtf|log|md|json|xml|html|css|js|py|java|c|cpp|h|doc|docx|xlsx|xls|odt";
    private static final String IMAGE_EXTENSIONS = "jpg|jpeg|png|gif|bmp|svg|webp|tiff|ico|raw";
    private static final String DOCUMENT_EXTENSIONS = "pdf|doc|docx|ppt|pptx|xls|xlsx|odt|ods|odp";
    private static final String VIDEO_EXTENSIONS = "mp4|avi|mov|wmv|flv|mkv|webm|m4v|3gp|mpg|mpeg";
    private static final String AUDIO_EXTENSIONS = "mp3|wav|flac|aac|ogg|wma|m4a|opus";

    /**
     * בדיקה אם קובץ שייך לסוג זה לפי הסיומת
     * @param fileName שם הקובץ
     * @return true אם הקובץ מתאים לסוג זה
     */
    public boolean matches(String fileName) {
        if (this == ALL) {
            return true;
        }

        String extension = getFileExtension(fileName).toLowerCase();

        switch (this) {
            case TEXT:
                return extension.matches("txt|csv|doc|docx|xlsx|xls|rtf|odt");
            case IMAGE:
                return extension.matches("jpg|jpeg|png|gif|bmp|svg|webp|tiff|ico");
            case DOCUMENT:
                return extension.matches("pdf|doc|docx|ppt|pptx|xls|xlsx|odt|ods|odp");
            case VIDEO:
                return extension.matches("mp4|avi|mov|wmv|flv|mkv|webm|m4v|3gp");
            case AUDIO:
                return extension.matches("mp3|wav|flac|aac|ogg|wma|m4a");
            case FOLDER:
                return false;
            default:
                return false;
        }
    }


    public static FileType getFileType(File file) {
        // אם זו תיקייה, מחזיר FOLDER
        if (file.isDirectory()) {
            return FOLDER;
        }

        // בדיקה לפי סיומת הקובץ
        String extension = getFileExtension(file.getName()).toLowerCase();

        if (extension.matches(TEXT_EXTENSIONS)) {
            return TEXT;
        } else if (extension.matches(IMAGE_EXTENSIONS)) {
            return IMAGE;
        } else if (extension.matches(DOCUMENT_EXTENSIONS)) {
            return DOCUMENT;
        } else if (extension.matches(VIDEO_EXTENSIONS)) {
            return VIDEO;
        } else if (extension.matches(AUDIO_EXTENSIONS)) {
            return AUDIO;
        }

        // אם לא נמצא התאמה, מחזיר TEXT כברירת מחדל
        return TEXT;
    }


    /**
     * קבלת סיומת קובץ
     * @param fileName שם הקובץ
     * @return סיומת הקובץ (ללא נקודה)
     */
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
}


