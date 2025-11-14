package project.Builder;

import project.Common.Config;

import java.io.*;
import java.util.*;

import static project.Builder.UtfEncoder.getUTFLength;

public class SPIMIInvertedIndex {
    private Config config;

    private Map<String, Map<Integer, List<Integer>>> tempIndex;
    private int blockCounter;
    private final long maxMemorySize;
    private long currentMemoryUsage;

    private File tempDir;
    private String postingsFile;
    private String bPlusTreeFile;

    private int currentPageId = 0;
    private int currentTreeHeight = 0;


    public SPIMIInvertedIndex(String type) {
        this.config = Config.getInstance();
        this.tempIndex = new TreeMap<>();
        this.blockCounter = 0;
        this.maxMemorySize = config.getMaxMemory(type);
        this.currentMemoryUsage = 0;

        // יצירת התיקייה הזמנית אם לא קיימת
        this.tempDir = new File(config.getTempDir(type));


        this.postingsFile = config.getPostingsFile(type);
        this.bPlusTreeFile = config.getBPlusTreeFile(type);
    }

     // מוסיף מונח לאינדקס הזמני
    public void addToTempIndex(String term, int docId, int position) {
        if (term == null || term.isEmpty() || term.length() > 255){
            return;
        }
        Map<Integer, List<Integer>> postings = tempIndex.get(term);

        if (postings == null) {
            postings = new HashMap<>();
            tempIndex.put(term, postings);
            currentMemoryUsage += 2 + getUTFLength(term);
        }

        List<Integer> positions = postings.get(docId);
        if (positions == null) {
            positions = new ArrayList<>();
            postings.put(docId, positions);
            currentMemoryUsage += 8;
        }

        positions.add(position);
        currentMemoryUsage += 4;

        if (currentMemoryUsage >= maxMemorySize) {
            writeBlockToDisk();
        }
    }

     //כותב את הבלוק הנוכחי לדיסק ומנקה את הזיכרון
    public void writeBlockToDisk() {

        if (tempIndex.isEmpty()) {
            return;
        }

        if (!this.tempDir.exists()) {
            this.tempDir.mkdirs();
        }

        String fileName = tempDir + File.separator + "block_" + blockCounter + ".bin";

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(fileName), config.getBufferSize()))) {

            for (Map.Entry<String, Map<Integer, List<Integer>>> termEntry : tempIndex.entrySet()) {
                String term = termEntry.getKey();
                Map<Integer, List<Integer>> postings = termEntry.getValue();

                dos.writeUTF(term);
                dos.writeInt(postings.size());

                for (Map.Entry<Integer, List<Integer>> docEntry : postings.entrySet()) {
                    int docId = docEntry.getKey();
                    List<Integer> positions = docEntry.getValue();

                    dos.writeInt(docId);
                    dos.writeInt(positions.size());

                    for (int position : positions) {
                        dos.writeInt(position);
                    }
                }
            }

            blockCounter++;

        } catch (IOException e) {
            System.err.println("שגיאה בכתיבת הבלוק לדיסק: " + e.getMessage());
            e.printStackTrace();
        }

        tempIndex.clear();
        currentMemoryUsage = 0;
    }


     // ממזג את כל הבלוקים ובונה B+ Tree
     public void mergeBlocks() {
        // כתיבת הבלוק האחרון אם יש תוכן
        writeBlockToDisk();


        // PriorityQueue לאופטימיזציה של המיזוג
        PriorityQueue<TermStream> termQueue = new PriorityQueue<>(
                (s1, s2) -> s1.currentTerm.compareTo(s2.currentTerm)
        );

        try {
            // פתיחת כל הבלוקים לקריאה והכנסה ל-PriorityQueue
            for (int i = 0; i < blockCounter; i++) {
                String fileName = tempDir + File.separator + "block_" + i + ".bin";
                DataInputStream dis = new DataInputStream(
                        new BufferedInputStream(new FileInputStream(fileName)));

                TermStream stream = readNextTerm(fileName, dis);
                if (stream != null) {
                    termQueue.offer(stream);
                }
            }

            // מיזוג ובניית B+ Tree ישירות - שני קבצים במקביל
            buildTreeAndPostingLists(termQueue);

            // מחיקת קבצי הבלוקים הזמניים
            for (int i = 0; i < blockCounter; i++) {
                File blockFile = new File(tempDir + File.separator + "block_" + i + ".bin");
                blockFile.delete();
            }

            if (tempDir.isDirectory() && Objects.requireNonNull(tempDir.list()).length == 0) {
                tempDir.delete();
            }

        } catch (IOException e) {
            System.err.println("שגיאה במיזוג הבלוקים: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //בניית B+ Tree ו-Postings במקביל
    private void buildTreeAndPostingLists(PriorityQueue<TermStream> termQueue) throws IOException {

        try (DataOutputStream postingsOutput = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(this.postingsFile), config.getBufferSize()));
             DataOutputStream treeOutput = new DataOutputStream(
                     new BufferedOutputStream(new FileOutputStream(this.bPlusTreeFile), config.getBufferSize()))) {

            // שלב 1: כתיבת Header זמני עם placeholder
            writeTemporaryTreeHeader(treeOutput);

            // שלב 2: מיזוג + כתיבת Leaf Pages ישירות עם PriorityQueue
            List<LeafPageInfo> leafInfos = mergeAndWriteLeafPages(termQueue, postingsOutput, treeOutput);

            // שלב 3: כתיבת Internal Pages עם buffer ובדיקת מקום מתוקנת
            int rootPageId = writeInternalLevels(treeOutput, leafInfos);

            // שלב 4: סגירת ה-buffer streams
            treeOutput.close();

            // שלב 5: עדכון Header עם RandomAccessFile
            updateTreeHeader(rootPageId, currentPageId, currentTreeHeight);

            System.out.println("B+ Tree ו-Postings נבנו במקביל עם PriorityQueue.");
        }
    }


     // מיזוג המונחים וכתיבת Leaf Pages
    private List<LeafPageInfo> mergeAndWriteLeafPages(PriorityQueue<TermStream> termQueue,
                                                               DataOutputStream postingsOutput,
                                                               DataOutputStream treeOutput) throws IOException {
        List<LeafPageInfo> leafInfos = new ArrayList<>();
        long currentPostingsOffset = 0;
        currentPageId = 0;
        currentTreeHeight = 1;

        // יצירת עמוד Leaf ראשון
        BPlusTreeLeafPage currentLeafPage = new BPlusTreeLeafPage(currentPageId);
        String firstKeyInPage = null;

        while (!termQueue.isEmpty()) {
            // הוצאת המונח הקטן ביותר
            TermStream currentStream = termQueue.poll();
            String currentTerm = currentStream.currentTerm;
            Map<Integer, List<Integer>> mergedPostings = new HashMap<>(currentStream.currentPostings);

            // איסוף כל הזרמים נוספים עם אותו מונח
            List<TermStream> sameTermStreams = new ArrayList<>();
            while (!termQueue.isEmpty() && termQueue.peek().currentTerm.equals(currentTerm)) {
                TermStream sameTermStream = termQueue.poll();
                mergePostings(mergedPostings, sameTermStream.currentPostings);
                sameTermStreams.add(sameTermStream);
            }

            // קריאת המונח הבא מכל הזרמים שעובדו והחזרה ל-queue
            TermStream nextTerm = readNextTerm(currentStream.fileName, currentStream.inputStream);
            if (nextTerm != null) {
                termQueue.offer(nextTerm);
            } else {
                // סיום הזרם
                currentStream.inputStream.close();
            }

            for (TermStream sameStream : sameTermStreams) {
                TermStream nextSameTerm = readNextTerm(sameStream.fileName, sameStream.inputStream);
                if (nextSameTerm != null) {
                    termQueue.offer(nextSameTerm);
                } else {
                    // סיום הזרם
                    sameStream.inputStream.close();
                }
            }

            // כתיבת ה-postings לקובץ
            long nextOffset = writePostingsToFile(postingsOutput, mergedPostings, currentPostingsOffset);

            // יצירת IndexEntry עבור B+ Tree
            IndexEntry entry = new IndexEntry(currentTerm, mergedPostings.size(), currentPostingsOffset);

            // בדיקה אם העמוד הנוכחי יכול להכיל את המונח
            if (!currentLeafPage.canFit(entry)) {
                if (currentPageId + 1 < Integer.MAX_VALUE) {
                    currentLeafPage.setNextPageId(currentPageId + 1);
                }

                treeOutput.write(currentLeafPage.serialize());
                leafInfos.add(new LeafPageInfo(currentPageId, firstKeyInPage));

                // יצירת עמוד חדש
                currentPageId++;
                currentLeafPage = new BPlusTreeLeafPage(currentPageId);
                firstKeyInPage = currentTerm;
            }

            if (firstKeyInPage == null) {
                firstKeyInPage = currentTerm;
            }

            // הוספת המונח לעמוד הנוכחי
            currentLeafPage.addEntry(entry);

            currentPostingsOffset = nextOffset;
        }

        // כתיבת העמוד האחרון
        if (!currentLeafPage.isEmpty()) {
            treeOutput.write(currentLeafPage.serialize());
            leafInfos.add(new LeafPageInfo(currentPageId, firstKeyInPage));
            currentPageId++;
        }

        return leafInfos;
    }


    //  ממזג postings מזרם מקור לזרם יעד
    private void mergePostings(Map<Integer, List<Integer>> target, Map<Integer, List<Integer>> source) {
        for (Map.Entry<Integer, List<Integer>> entry : source.entrySet()) {
            int docId = entry.getKey();

            List<Integer> sourcePositions = entry.getValue();

            List<Integer> targetPositions = target.get(docId);
            if (targetPositions == null) {
                targetPositions = new ArrayList<>(sourcePositions);
                target.put(docId, targetPositions);
            } else {
                targetPositions.addAll(sourcePositions);
                Collections.sort(targetPositions);
            }
        }
    }



    // קורא את המונח הבא מזרם נתון
    private TermStream readNextTerm(String fileName, DataInputStream dis) {
        try {
            if (dis.available() > 0) {
                String term = dis.readUTF();
                int numDocs = dis.readInt();

                Map<Integer, List<Integer>> postings = new HashMap<>();

                for (int i = 0; i < numDocs; i++) {
                    int docId = dis.readInt();
                    int numPositions = dis.readInt();

                    List<Integer> positions = new ArrayList<>();
                    for (int j = 0; j < numPositions; j++) {
                        positions.add(dis.readInt());
                    }

                    postings.put(docId, positions);
                }

                return new TermStream(fileName, dis, term, postings);
            }
        } catch (IOException e) {
            System.out.println("שגיאה בקריאת המונח הבא מזרם הנתונים: " + e.getMessage());
        }

        return null;
    }



    // כתיבת Internal Levels
    private int writeInternalLevels(DataOutputStream treeOutput, List<LeafPageInfo> leafInfos) throws IOException {
        List<LeafPageInfo> currentLevel = leafInfos;

        while (currentLevel.size() > 1) {
            List<LeafPageInfo> parentLevel = new ArrayList<>();
            currentTreeHeight++;

            // יצירת עמוד Internal ראשון
            BPlusTreeInternalPage currentInternalPage = new BPlusTreeInternalPage(currentPageId);
            String firstKeyInPage = null;

            for (int i = 0; i < currentLevel.size(); i++) {
                LeafPageInfo child = currentLevel.get(i);

                if (i == 0) {
                    // הילד הראשון - אין צורך במפתח מפריד
                    currentInternalPage.addFirstChild(child.pageId);
                    firstKeyInPage = child.firstKey;
                } else {
                    // בדיקה אם יש מקום להוסיף ילד נוסף
                    if (!currentInternalPage.canFit(child.firstKey)) {
                        // העמוד מלא - כתיבה לקובץ
                        treeOutput.write(currentInternalPage.serialize());
                        parentLevel.add(new LeafPageInfo(currentPageId, firstKeyInPage));

                        // יצירת עמוד חדש
                        currentPageId++;
                        currentInternalPage = new BPlusTreeInternalPage(currentPageId);
                        currentInternalPage.addFirstChild(child.pageId);
                        firstKeyInPage = child.firstKey;
                    } else {
                        // יש מקום - הוספת הילד
                        currentInternalPage.addChild(child.firstKey, child.pageId);
                    }
                }
            }

            // כתיבת העמוד האחרון
            if (!currentInternalPage.isEmpty()) {
                treeOutput.write(currentInternalPage.serialize());
                parentLevel.add(new LeafPageInfo(currentPageId, firstKeyInPage));
                currentPageId++;
            }

            currentLevel = parentLevel;
        }

        return currentLevel.get(0).pageId;
    }



    private void writeTemporaryTreeHeader(DataOutputStream treeOutput) throws IOException {
        // Magic number: BPT1 (4 bytes)
        treeOutput.write('B');
        treeOutput.write('P');
        treeOutput.write('T');
        treeOutput.write('1');

        // Version: 1 (2 byte)
        treeOutput.writeShort(1);

        // Page size: 8KB (4 bytes)
        treeOutput.writeInt(config.getPageSize());

        // Root page ID: placeholder (4 bytes)
        treeOutput.writeInt(-1);

        // Total pages: placeholder (4 bytes)
        treeOutput.writeInt(-1);

        // Tree height: placeholder (2 bytes)
        treeOutput.writeShort(-1);



        // Padding to reach 32 bytes total
        int remainingBytes = 32 - (4 + 2 + 4 + 4 + 4 + 2);
        for (int i = 0; i < remainingBytes; i++) {
            treeOutput.writeByte(0);
        }
    }

    // כותב postings לקובץ נפרד ומחזיר את ה-offset הבא
    private long writePostingsToFile(DataOutputStream postingsOutput,
                                     Map<Integer, List<Integer>> postings,
                                     long currentOffset) throws IOException {
        long bytesWritten = currentOffset;

        List<Integer> sortedDocIds = new ArrayList<>(postings.keySet());
        Collections.sort(sortedDocIds);

        for (int docId : sortedDocIds) {
            List<Integer> positions = postings.get(docId);

            postingsOutput.writeInt(docId);
            bytesWritten += 4;

            postingsOutput.writeInt(positions.size());
            bytesWritten += 4;

            Collections.sort(positions);
            for (int position : positions) {
                postingsOutput.writeInt(position);
                bytesWritten += 4;
            }
        }

        return bytesWritten;
    }

    // עדכון של הכותרת
     private void updateTreeHeader(int rootPageId, int totalPages, int treeHeight) throws IOException {
        try (RandomAccessFile treeFile = new RandomAccessFile(bPlusTreeFile, "rw")) {
            // Magic (4) + Version (2) + PageSize (4) = 12 bytes
            treeFile.seek(10);

            // Root page ID (4 bytes)
            treeFile.writeInt(rootPageId);

            // Total pages (4 bytes)
            treeFile.writeInt(totalPages);

            // Tree height (2 bytes)
            treeFile.writeShort(treeHeight);

            System.out.println("Header עודכן: Root=" + rootPageId + ", Pages=" + totalPages + ", Height=" + treeHeight);
        }
    }


    private static class TermStream implements Comparable<TermStream> {
        String fileName;
        DataInputStream inputStream;
        String currentTerm;
        Map<Integer, List<Integer>> currentPostings;

        public TermStream(String fileName, DataInputStream inputStream,
                          String currentTerm, Map<Integer, List<Integer>> currentPostings) {
            this.fileName = fileName;
            this.inputStream = inputStream;
            this.currentTerm = currentTerm;
            this.currentPostings = currentPostings;
        }

        @Override
        public int compareTo(TermStream other) {
            return this.currentTerm.compareTo(other.currentTerm);
        }
    }

    private record LeafPageInfo(int pageId, String firstKey) {
    }
}
