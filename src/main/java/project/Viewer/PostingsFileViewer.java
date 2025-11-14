package project.Viewer;

import project.Common.Config;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class PostingsFileViewer {
    public static void printPostingsFile(String filePath) {
        System.out.println("=== Postings File: " + filePath + " ===");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            long currentOffset = 0;
            int postingListCount = 0;

            System.out.println("פורמט: PostingList# (Start Offset, Length) -> [DocID: freq, positions...]");
            System.out.println("-".repeat(100));

            while (dis.available() > 0) {
                long startOffset = currentOffset;
                List<PostingEntry> entries = new ArrayList<>();

                // קריאת רשימת postings עד שמזהה דפוס חדש או סיום הקובץ
                while (dis.available() >= 8) { // לפחות docId + termFreq
                    try {
                        int docId = dis.readInt();
                        int termFreq = dis.readInt();
                        currentOffset += 8;

                        List<Integer> positions = new ArrayList<>();
                        for (int i = 0; i < termFreq && dis.available() >= 4; i++) {
                            int position = dis.readInt();
                            positions.add(position);
                            currentOffset += 4;
                        }

                        entries.add(new PostingEntry(docId, termFreq, positions));

                        // בדיקה אם זה סוף הרשימה
                        if (dis.available() == 0) break;

                        // לוגיקה לזיהוי סיום רשימה - אם ה-docId הבא קטן מהנוכחי
                        if (dis.available() >= 4) {
                            dis.mark(4);
                            int nextDocId = dis.readInt();
                            dis.reset();

                            // אם ה-docId הבא קטן או שווה לנוכחי ויש כבר כמה entries, זו רשימה חדשה
                            if (nextDocId <= docId && entries.size() > 1) {
                                break;
                            }
                        }

                    } catch (IOException e) {
                        break;
                    }
                }

                if (entries.isEmpty()) break;

                long totalLength = currentOffset - startOffset;

                // הדפסת הרשימה
                System.out.printf("PostingList #%04d (Offset: %8d, Length: %4d) -> ",
                        postingListCount, startOffset, totalLength);

                boolean first = true;
                for (PostingEntry entry : entries) {
                    if (!first) System.out.print(", ");
                    System.out.printf("[DocID:%d freq:%d pos:%s]",
                            entry.docId, entry.termFreq, entry.positions);
                    first = false;
                }
                System.out.println();

                postingListCount++;
            }

            System.out.println("-".repeat(100));
            System.out.println("סה\"כ Posting Lists: " + postingListCount);
            System.out.println("גודל קובץ: " + currentOffset + " bytes");

        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת Postings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void printPostingsSample(String filePath, int maxLists) {
        System.out.println("=== דוגמה של " + maxLists + " Posting Lists ראשונות מקובץ: " + filePath + " ===");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            long currentOffset = 0;
            int postingListCount = 0;

            System.out.println("פורמט: PostingList# (Start Offset, Length) -> [DocID: freq, positions...]");
            System.out.println("-".repeat(100));

            while (dis.available() > 0 && postingListCount < maxLists) {
                long startOffset = currentOffset;
                List<PostingEntry> entries = new ArrayList<>();

                while (dis.available() >= 8) {
                    try {
                        int docId = dis.readInt();
                        int termFreq = dis.readInt();
                        currentOffset += 8;

                        List<Integer> positions = new ArrayList<>();
                        for (int i = 0; i < termFreq && dis.available() >= 4; i++) {
                            int position = dis.readInt();
                            positions.add(position);
                            currentOffset += 4;
                        }

                        entries.add(new PostingEntry(docId, termFreq, positions));

                        if (dis.available() == 0) break;

                        if (dis.available() >= 4) {
                            dis.mark(4);
                            int nextDocId = dis.readInt();
                            dis.reset();

                            if (nextDocId <= docId && entries.size() > 1) {
                                break;
                            }
                        }

                    } catch (IOException e) {
                        break;
                    }
                }

                if (entries.isEmpty()) break;

                long totalLength = currentOffset - startOffset;

                // פורמט תואם לפונקציה הראשית
                System.out.printf("PostingList #%04d (Offset: %8d, Length: %4d) -> ",
                        postingListCount, startOffset, totalLength);

                boolean first = true;
                for (PostingEntry entry : entries) {
                    if (!first) System.out.print(", ");
                    System.out.printf("[DocID:%d freq:%d pos:%s]",
                            entry.docId, entry.termFreq, entry.positions);
                    first = false;
                }
                System.out.println();

                postingListCount++;
            }

            System.out.println("-".repeat(100));
            System.out.println("סה\"כ שהוצגו: " + postingListCount + " Posting Lists");

        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת Postings: " + e.getMessage());
        }
    }

    static class PostingEntry {
        int docId;
        int termFreq;
        List<Integer> positions;

        PostingEntry(int docId, int termFreq, List<Integer> positions) {
            this.docId = docId;
            this.termFreq = termFreq;
            this.positions = positions;
        }

        @Override
        public String toString() {
            return String.format("DocID:%d,Freq:%d,Pos:%s", docId, termFreq, positions);
        }
    }

    public static void main(String[] args) {
        printMenu();
        Config config = Config.getInstance();
        
        if (args.length > 0) {
            if (args[0].equals("--name")) {
                printPostingsFile(config.getPostingsFile("name"));
            } else if (args[0].equals("--content")) {
                printPostingsFile(config.getPostingsFile("content"));
            } else if (args[0].equals("--sample") && args.length > 2) {
                String type = args[1];
                try {
                    int maxLists = Integer.parseInt(args[2]);
                    String filePath = type.equals("name") ?
                            config.getPostingsFile("name") :
                            type.equals("content") ?
                                    config.getPostingsFile("content") : type;
                    printPostingsSample(filePath, maxLists);
                } catch (NumberFormatException e) {
                    System.err.println("מספר הרשימות חייב להיות מספר: " + args[2]);
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("=== PostingsFileViewer Menu ===");
        System.out.println("  --name");
        System.out.println("  --content");
        System.out.println("  --sample <type> <n>");
        System.out.println();

    }
}