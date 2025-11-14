package project.Viewer;

import project.Common.Config;

import java.io.*;

public class LengthFilesViewer {

    public static void printLengthFile(String filePath, String type) {
        System.out.println("===  קובץ אורכי " + type + " קבצים  ===");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            long totalFileCount = dis.readLong();
            long totalLength = dis.readLong();

            System.out.println("מספר קבצים כולל: " + totalFileCount);
            System.out.println("אורך כולל: " + totalLength);
            System.out.println("ממוצע אורך " + ": " +
                    (totalFileCount > 0 ? (double) totalLength / totalFileCount : 0));
            System.out.println("-".repeat(70));

            int recordCount = 0;
            int minLength = Integer.MAX_VALUE;
            int maxLength = 0;

            System.out.println("פורמט: Record# -> Document ID |  Length");

            while (dis.available() > 0) {
                int docId = dis.readInt();
                int length = dis.readInt();

                minLength = Math.min(minLength, length);
                maxLength = Math.max(maxLength, length);

                System.out.printf("Record #%06d -> DocID: %8d | Length: %6d%n",
                        ++recordCount, docId, length);
            }

            System.out.println("-".repeat(70));
            System.out.println("סה\"כ רשומות שנקראו: " + recordCount);
            System.out.println("אורך מינימלי: " + (minLength == Integer.MAX_VALUE ? 0 : minLength));
            System.out.println("אורך מקסימלי: " + maxLength);

        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת קובץ " + filePath + ": " + e.getMessage());
        }
    }

    public static void printLengthSample(String filePath, String type, int maxRecords) {
        System.out.println("=== דוגמה של עד " + maxRecords + " רשומות מתוך קובץ אורכי " + type + " ===");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            long totalFileCount = dis.readLong();
            long totalLength = dis.readLong();

            System.out.println("מספר קבצים כולל: " + totalFileCount);
            System.out.println("אורך כולל: " + totalLength);
            System.out.println("ממוצע אורך " + ": " +
                    (totalFileCount > 0 ? (double) totalLength / totalFileCount : 0));
            System.out.println("-".repeat(70));

            System.out.println("פורמט: Record# -> Document ID |  Length");

            int recordCount = 0;

            while (dis.available() > 0 && recordCount < maxRecords) {
                int docId = dis.readInt();
                int length = dis.readInt();

                System.out.printf("Record #%06d -> DocID: %8d | Length: %6d%n",
                        ++recordCount, docId, length);
            }

            System.out.println("-".repeat(70));
            System.out.println("סה\"כ רשומות שהוצגו: " + recordCount);

        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת קובץ " + filePath + ": " + e.getMessage());
        }
    }
    
    // =============== Main ===============
    public static void main(String[] args) {
        Config config = Config.getInstance();
//        printLengthFile(config.getLenFile("name"),  "שמות");
        printLengthSample(config.getLenFile("name"), "שמות", 10);


        if (args.length == 0) {
            System.out.println("יש לספק פרמטר: --name או --content");
            return;
        }

        String option = args[0];

        switch (option) {
            case "--name":
                printLengthFile(config.getLenFile("name"),  "שמות");
                break;

            case "--content":
                printLengthFile(config.getLenFile("content"), "תוכן");
                break;

            case "--sample":
                if (args.length < 3) {
                    System.err.println("יש לספק גם סוג (name/content) וגם מספר רשומות להציג");
                    break;
                }
                String type = args[1];
                int maxRecords;
                try {
                    maxRecords = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    System.err.println("המספר שהוזן אינו חוקי: " + args[2]);
                    break;
                }
                String filePath = type.equals("name") ? config.getLenFile("name") :
                        type.equals("content") ? config.getLenFile("content") : type;
                printLengthSample(filePath, type, maxRecords);
                break;

            default:
                System.out.println("פרמטר לא חוקי. השתמש ב־--name או --content בלבד.");
                break;
        }
    }

    private static void printMenu() {
        System.out.println("=== LengthFilesViewer Menu ===");
        System.out.println("  --name");
        System.out.println("  --content");
        System.out.println("  --sample <type> <n>");
        System.out.println();

    }
}