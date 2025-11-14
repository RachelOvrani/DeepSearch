package project.Viewer;

import project.Common.Config;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * מחלקה לקריאה והדפסה של קבצי B+ Tree
 */
public class BPlusTreeViewer {

    // קבועים למבנה B+ Tree
    private static final int HEADER_SIZE = 32;
    private static final int PAGE_SIZE = 8 * 1024;

    // סוגי עמודים
    private static final byte PAGE_TYPE_INTERNAL = 1;
    private static final byte PAGE_TYPE_LEAF = 2;

    /**
     * מדפיס את תוכן B+ Tree
     */
    public static void printBPlusTree(String filePath) {
        System.out.println("=== B+ Tree: " + filePath + " ===");

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            // שלב 1: הדפסת Header של הקובץ
            BPlusTreeHeader header = readBPlusTreeHeader(dis);
            printBPlusTreeHeader(header);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("עמודי העץ:");
            System.out.println("=".repeat(60));

            int y = 0;
            // שלב 2: הדפסת כל העמודים
            for (int i = 0; i < header.totalPages; i++) {
                BPlusTreePage page = readBPlusTreePage(dis, i);
                printBPlusTreePage(page, i);
                if (page.pageType == PAGE_TYPE_INTERNAL) y++;

                // הפרדה בין עמודים
                if (i < header.totalPages - 1) {
                    System.out.println();
                }
            }

            System.out.println("\n" + "=".repeat(60));
            System.out.println("סיום - סה\"כ " + header.totalPages + " עמודים");

            System.out.println("סה\"כ עמודי Internal: " + y);

        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת B+ Tree: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * קריאת Header של B+ Tree - עודכן למבנה החדש של 32 bytes
     */
    private static BPlusTreeHeader readBPlusTreeHeader(DataInputStream dis) throws IOException {
        byte[] magic = new byte[4];
        dis.readFully(magic);

        short version = dis.readShort();
        int pageSize = dis.readInt();
        int rootPageId = dis.readInt();
        int totalPages = dis.readInt();
        short treeHeight = dis.readShort();

        // דילוג על Reserved space + Padding עד 32 bytes
        dis.skip(HEADER_SIZE - 4 - 2 - 4 - 4 - 4 - 2);

        return new BPlusTreeHeader(magic, version, pageSize, rootPageId, totalPages, treeHeight);
    }

    /**
     * קריאת עמוד B+ Tree - עודכן למבנה החדש
     */
    private static BPlusTreePage readBPlusTreePage(DataInputStream dis, int pageIndex) throws IOException {
        byte[] pageData = new byte[PAGE_SIZE];
        dis.readFully(pageData);

        ByteArrayInputStream bais = new ByteArrayInputStream(pageData);
        DataInputStream pageStream = new DataInputStream(bais);

        byte pageType = pageStream.readByte();
        byte reserved = pageStream.readByte(); // עודכן - קריאת reserved byte
        short numEntries = pageStream.readShort();

        BPlusTreePage page = new BPlusTreePage(pageIndex, pageType, numEntries, -1);

        if (pageType == PAGE_TYPE_LEAF) {
            // קריאת Leaf Page - עם nextPageId חדש
            int nextPageId = pageStream.readInt();
            page.nextPageId = nextPageId;

            for (int i = 0; i < numEntries; i++) {
                try {
                    String term = pageStream.readUTF();
                    int numDocs = pageStream.readInt();
                    long postingsOffset = pageStream.readLong();
                    page.addLeafEntry(new LeafEntry(term, numDocs, postingsOffset));
                } catch (IOException e) {
                    break; // סיום הנתונים בעמוד
                }
            }
        } else if (pageType == PAGE_TYPE_INTERNAL) {
            // קריאת Internal Page - מבנה חדש
            try {
                // המצביע הראשון
                int firstPointer = pageStream.readInt();
                page.setFirstPointer(firstPointer);

                // מפתחות + מצביעים
                for (int i = 0; i < numEntries; i++) {
                    String key = pageStream.readUTF();
                    int pointer = pageStream.readInt(); // עודכן מ-long ל-int
                    page.addInternalEntry(new InternalEntry(key, pointer));
                }
            } catch (IOException e) {
                // סיום הנתונים בעמוד
            }
        }

        return page;
    }

    /**
     * הדפסת Header של B+ Tree
     */
    private static void printBPlusTreeHeader(BPlusTreeHeader header) {
        System.out.println("\nB+ Tree Header:");
        System.out.println("  Magic Number: " + new String(header.magic));
        System.out.println("  Version: " + header.version);
        System.out.println("  Page Size: " + header.pageSize + " bytes");
        System.out.println("  Root Page ID: " + header.rootPageId);
        System.out.println("  Total Pages: " + header.totalPages);
        System.out.println("  Tree Height: " + header.treeHeight);
    }

    /**
     * הדפסת עמוד B+ Tree
     */
    private static void printBPlusTreePage(BPlusTreePage page, int pageIndex) {
        String pageTypeStr = (page.pageType == PAGE_TYPE_LEAF) ? "LEAF" : "INTERNAL";

        // כותרת העמוד
        System.out.println("\nPage #" + pageIndex + ":");
        System.out.println("  Page ID: " + page.pageId);
        System.out.println("  Type: " + pageTypeStr);
        System.out.println("  Entries: " + page.numEntries);
        if (page.nextPageId != -1) {
            System.out.println("  Next Page ID: " + page.nextPageId);
        }


        if (page.pageType == PAGE_TYPE_LEAF) {
            // עמוד Leaf - מונחים + postings offset
            System.out.println("  Leaf Entries:");
            for (int i = 0; i < page.leafEntries.size(); i++) {
                LeafEntry entry = page.leafEntries.get(i);
                System.out.printf("    Entry %d: Term='%s', Documents=%d, Postings Offset=%d%n",
                        i, entry.term, entry.numDocs, entry.postingsOffset);
            }

        } else {
            // עמוד Internal - מצביעים ומפתחות הפרדה
            System.out.println("  Internal Entries:");

            if (page.firstPointer != -1) {
                System.out.println("    First Pointer: " + page.firstPointer);
            }

            for (int i = 0; i < page.internalEntries.size(); i++) {
                InternalEntry entry = page.internalEntries.get(i);
                System.out.printf("    Entry %d: Key='%s', Page Pointer=%d%n",
                        i, entry.key, entry.pointer);
            }
        }
    }


    //  מחלקות נתונים
    static class BPlusTreeHeader {
        byte[] magic;
        short version; // עודכן מ-int ל-short
        int pageSize;
        int rootPageId; // עודכן מ-long ל-int
        int totalPages; // עודכן מ-long ל-int
        short treeHeight; // עודכן מ-int ל-short

        BPlusTreeHeader(byte[] magic, short version, int pageSize, int rootPageId, int totalPages, short treeHeight) {
            this.magic = magic;
            this.version = version;
            this.pageSize = pageSize;
            this.rootPageId = rootPageId;
            this.totalPages = totalPages;
            this.treeHeight = treeHeight;
        }
    }

    static class BPlusTreePage {
        int pageId;
        byte pageType;
        short numEntries;
        int nextPageId; // עודכן מ-long ל-int
        int firstPointer = -1; // עודכן מ-long ל-int
        List<LeafEntry> leafEntries = new ArrayList<>();
        List<InternalEntry> internalEntries = new ArrayList<>();

        BPlusTreePage(int pageId, byte pageType, short numEntries, int nextPageId) {
            this.pageId = pageId;
            this.pageType = pageType;
            this.numEntries = numEntries;
            this.nextPageId = nextPageId;
        }

        void addLeafEntry(LeafEntry entry) {
            leafEntries.add(entry);
        }

        void addInternalEntry(InternalEntry entry) {
            internalEntries.add(entry);
        }

        void setFirstPointer(int pointer) { // עודכן מ-long ל-int
            this.firstPointer = pointer;
        }
    }

    static class LeafEntry {
        String term;
        int numDocs;
        long postingsOffset;

        LeafEntry(String term, int numDocs, long postingsOffset) {
            this.term = term;
            this.numDocs = numDocs;
            this.postingsOffset = postingsOffset;
        }
    }

    static class InternalEntry {
        String key;
        int pointer; // עודכן מ-long ל-int

        InternalEntry(String key, int pointer) {
            this.key = key;
            this.pointer = pointer;
        }
    }

    public static void main(String[] args) {
        printMenu();
        Config config = Config.getInstance();

        if (args.length > 0) {
            if (args[0].equals("--name")) {
                printBPlusTree(config.getBPlusTreeFile("name"));
            } else if (args[0].equals("--content")) {
                printBPlusTree(config.getBPlusTreeFile("content"));
            }
        }
    }

    private static void printMenu() {
        System.out.println("=== PostingsFileViewer Menu ===");
        System.out.println("  --name");
        System.out.println("  --content");
        System.out.println();

    }

}