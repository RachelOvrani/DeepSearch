//package project.UI.Util;
//
//import project.UI.Model.FileType;
//
///**
// * כלי עזר לזיהוי סוגי קבצים
// */
//public class FileTypeUtil {
//
//    /**
//     * מחלץ את סיומת הקובץ משם הקובץ
//     */
//    public static String getFileExtension(String fileName) {
//        if (fileName == null || fileName.isEmpty()) {
//            return "";
//        }
//
//        int dotIndex = fileName.lastIndexOf('.');
//        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
//            return fileName.substring(dotIndex + 1).toLowerCase();
//        }
//        return "";
//    }
//
//    /**
//     * מחזיר את סוג הקובץ לפי שם הקובץ
//     */
//    public static FileType getFileType(String fileName) {
//        String extension = getFileExtension(fileName);
//        return FileType.getTypeFile(extension);
//    }
//
//    /**
//     * בודק האם קובץ הוא קובץ טקסט שניתן להציג בתצוגה מקדימה
//     */
//    public static boolean isPreviewableTextFile(String fileName) {
//        String extension = getFileExtension(fileName);
//        String[] previewableExtensions = {
//                "txt", "log", "ini", "cfg", "csv", "json", "xml",
//                "html", "htm", "css", "js", "java", "py", "c", "cpp", "h"
//        };
//
//        for (String ext : previewableExtensions) {
//            if (ext.equals(extension)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * בודק האם קובץ הוא קובץ תמונה שניתן להציג בתצוגה מקדימה
//     */
//    public static boolean isPreviewableImageFile(String fileName) {
//        String extension = getFileExtension(fileName);
//        String[] previewableExtensions = {
//                "jpg", "jpeg", "png", "gif", "bmp"
//        };
//
//        for (String ext : previewableExtensions) {
//            if (ext.equals(extension)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    /**
//     * מחזיר תיאור ידידותי למשתמש של סוג הקובץ
//     */
//    public static String getFileTypeDescription(String fileName) {
//        FileType type = getFileType(fileName);
//        String extension = getFileExtension(fileName);
//
//        switch (type) {
//            case TEXT:
//                if (extension.equals("txt")) {
//                    return "קובץ טקסט";
//                } else if (extension.equals("html") || extension.equals("htm")) {
//                    return "דף אינטרנט";
//                } else if (extension.equals("csv")) {
//                    return "קובץ ערכים מופרדים בפסיקים";
//                } else if (extension.equals("json")) {
//                    return "קובץ JSON";
//                } else if (extension.equals("xml")) {
//                    return "קובץ XML";
//                } else {
//                    return "קובץ קוד מקור";
//                }
//            case IMAGE:
//                return "קובץ תמונה";
//            case DOCUMENT:
//                if (extension.equals("pdf")) {
//                    return "מסמך PDF";
//                } else if (extension.equals("doc") || extension.equals("docx")) {
//                    return "מסמך Word";
//                } else if (extension.equals("xls") || extension.equals("xlsx")) {
//                    return "גיליון אלקטרוני Excel";
//                } else if (extension.equals("ppt") || extension.equals("pptx")) {
//                    return "מצגת PowerPoint";
//                } else {
//                    return "מסמך";
//                }
//            case VIDEO:
//                return "קובץ וידאו";
//            case AUDIO:
//                return "קובץ שמע";
//            case OTHER:
//                if (extension.equals("exe") || extension.equals("msi")) {
//                    return "קובץ הפעלה";
//                } else if (extension.equals("zip") || extension.equals("rar") || extension.equals("7z")) {
//                    return "קובץ ארכיון דחוס";
//                } else {
//                    return "קובץ " + (extension.isEmpty() ? "לא מזוהה" : extension);
//                }
//            default:
//                return "קובץ";
//        }
//    }
//}