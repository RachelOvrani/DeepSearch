package project.Common;

import java.io.File;
import java.io.IOException;

public class SecureIndexFolder {

    public static void lockFolder(String folderPath) throws IOException {
        try {
            // הסתרת התיקייה (הפיכתה למוסתרת וסיסטמית)
            String hideCommand = "attrib +h +s \"" + folderPath + "\"";

            Process hideProcess = Runtime.getRuntime().exec(hideCommand);
            hideProcess.waitFor();

            String command = "icacls \"" + folderPath + "\" /inheritance:r /grant:r Everyone:RX /T";
            Process p = null;

            p = Runtime.getRuntime().exec(command);
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

    }

    public static void unlockFolder(String folderPath) throws IOException {
        try {
            File folder = new File(folderPath);
            if (!folder.exists()) {
                if (!folder.mkdirs()) {
                    throw new IOException("לא ניתן ליצור את התיקייה: " + folderPath);
                }
            }


            // החזרת הרשאות ברירת מחדל (כולל ירושה)
            String command = "icacls \"" + folderPath + "\" /reset /T";
            Process p = Runtime.getRuntime().exec(command);
            p.waitFor();

            // ביטול הסתרה של התיקייה
            String unhideCommand = "attrib -h -s \"" + folderPath + "\"";
            Process unhideProcess = Runtime.getRuntime().exec(unhideCommand);
            unhideProcess.waitFor();

            System.out.println("ההרשאות הוחזרו לברירת מחדל.");

        } catch (Exception e) {
            System.err.println("שגיאה בשחרור הרשאות: " + e.getMessage());
        }
    }


    // דוגמה לשימוש
    public static void main(String[] args) throws IOException {

        lockFolder("D:\\Users\\רחלי\\Desktop\\test123");
        unlockFolder("D:\\Users\\רחלי\\Desktop\\test123");
    }
}