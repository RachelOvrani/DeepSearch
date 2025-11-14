package project.Searcher;

import project.Builder.IndexEntry;

import java.io.*;
import java.util.*;

// חיפוש בעץ B+ מבוסס דיסק - טוען רק את הצמתים הנדרשים
// עם שורש בזיכרון וcache משופר
public class BPlusTreeSearcher implements AutoCloseable {
    private final RandomAccessFile treeFile;
    private final RandomAccessFile postingsFile;

    // מידע מה-Header
    private final int pageSize;
    private final int rootPageId;
    private final int treeHeight;

    // שורש טעון בזיכרון!
    private final InternalPageData rootPage;

    // מספר המסמכים ואורך כולל
    private long docsCount;
    private long totalLength;

    // Cache מורחב - כולל השורש + עמודים נוספים
    private final Map<Integer, byte[]> pageCache = new LinkedHashMap<Integer, byte[]>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
            return size() > 10; // מגביל ל-10 עמודים בcache (לא כולל השורש)
        }
    };

    public BPlusTreeSearcher(String treeFilePath, String postingsFilePath) throws IOException {
        this.treeFile = new RandomAccessFile(treeFilePath, "r");
        this.postingsFile = new RandomAccessFile(postingsFilePath, "r");

        // קריאת Header
        TreeHeader header = readTreeHeader();
        this.pageSize = header.pageSize;
        this.rootPageId = header.rootPageId;
        int totalPages = header.totalPages;
        this.treeHeight = header.treeHeight;

        // טעינת השורש לזיכרון!
        this.rootPage = loadRootPage();

        System.out.println("B+ Tree נטען: Root=" + rootPageId + " (בזיכרון), Pages=" + totalPages + ", Height=" + treeHeight);
    }

    // טעינת השורש פעם אחת בלבד
    private InternalPageData loadRootPage() throws IOException {
        // אם השורש הוא leaf (עץ בגובה 1)
        if (treeHeight == 1) {
            return null; // נטפל בזה בחיפוש
        }

        byte[] rootData = loadPageFromDisk(rootPageId);

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(rootData))) {
            byte pageType = dis.readByte();
            if (pageType != 1) {
                throw new IOException("Root must be internal page, got type: " + pageType);
            }

            dis.readByte(); // reserved
            short numKeys = dis.readShort();
            int firstPointer = dis.readInt();

            List<String> keys = new ArrayList<>();
            List<Integer> childPointers = new ArrayList<>();

            for (int i = 0; i < numKeys; i++) {
                String key = dis.readUTF();
                int childPointer = dis.readInt();
                keys.add(key);
                childPointers.add(childPointer);
            }

            return new InternalPageData(firstPointer, keys, childPointers);
        }
    }

    public DocumentLengthsStats loadFileLengths(String filePath) throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {

            this.docsCount = dis.readLong(); // מספר המסמכים
            this.totalLength = dis.readLong(); // אורך כולל במילים

            Map<Integer, Integer> contentLengths = new HashMap<>();
            while(dis.available() >= 8){
                int docId = dis.readInt();
                int docLen = dis.readInt();
                contentLengths.put(docId, docLen);
            }

            return new DocumentLengthsStats(docsCount, totalLength, contentLengths);

        } catch (FileNotFoundException e) {
            System.err.println("שגיאה: קובץ אורכי תוכן לא נמצא: " + filePath);
            return new DocumentLengthsStats(0, 0, new HashMap<>());
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת אורכי תוכן: " + e.getMessage());
            return new DocumentLengthsStats(0, 0, new HashMap<>());
        }
    }

    // חיפוש מונח בעץ - עכשיו מתחיל מהשורש בזיכרון
    public SearchResult searchTerm(String term) throws IOException {
        if (term == null || term.isEmpty()) {
            return null;
        }

        int currentPageId;

        // התחלה מהשורש
        if (treeHeight == 1) {
            // השורש הוא leaf
            currentPageId = rootPageId;
        } else {
            // השורש הוא internal - חיפוש בזיכרון!
            currentPageId = findChildPageId(rootPage, term);
            if (currentPageId == -1) {
                return null;
            }
        }

        // חיפוש דרך שאר ה-Internal Pages (מהדיסק עם cache)
        for (int level = treeHeight - 1; level > 1; level--) {
            InternalPageData internalPage = loadInternalPage(currentPageId);
            currentPageId = findChildPageId(internalPage, term);

            if (currentPageId == -1) {
                return null;
            }
        }

        // חיפוש ב-Leaf Page
        LeafPageData leafPage = loadLeafPage(currentPageId);
        return findTermInLeaf(leafPage, term);
    }

    public TermInfo getTermInfo(String term) throws IOException {
        if (term == null || term.isEmpty()) {
            return null;
        }

        int currentPageId;

        // התחלה מהשורש
        if (treeHeight == 1) {
            // השורש הוא leaf
            currentPageId = rootPageId;
        } else {
            // השורש הוא internal - חיפוש בזיכרון!
            currentPageId = findChildPageId(rootPage, term);
            if (currentPageId == -1) {
                return null;
            }
        }

        // חיפוש דרך שאר ה-Internal Pages
        for (int level = treeHeight - 1; level > 1; level--) {
            InternalPageData internalPage = loadInternalPage(currentPageId);
            currentPageId = findChildPageId(internalPage, term);

            if (currentPageId == -1) {
                return null; // לא נמצא
            }
        }

        // חיפוש ב-Leaf Page
        LeafPageData leafPage = loadLeafPage(currentPageId);

        // יחזיר את הרשומה מקובץ העץ (ללא טעינה של רשימת המסמכים)
        return findTermInfoInLeaf(leafPage, term);
    }

    // קריאת Header של העץ
    private TreeHeader readTreeHeader() throws IOException {
        treeFile.seek(0);

        // Magic number (4 bytes)
        byte[] magic = new byte[4];
        treeFile.read(magic);
        String magicStr = new String(magic);
        if (!"BPT1".equals(magicStr)) {
            throw new IOException("Invalid tree file format: " + magicStr);
        }

        // Version (2 bytes)
        short version = treeFile.readShort();

        // Page size (4 bytes)
        int pageSize = treeFile.readInt();

        // Root page ID (4 bytes)
        int rootPageId = treeFile.readInt();

        // Total pages (4 bytes)
        int totalPages = treeFile.readInt();

        // Tree height (2 bytes)
        short treeHeight = treeFile.readShort();

        return new TreeHeader(version, pageSize, rootPageId, totalPages, treeHeight);
    }

    // טעינת Internal Page מהדיסק (כעת עם cache)
    private InternalPageData loadInternalPage(int pageId) throws IOException {
        byte[] pageData = loadPage(pageId);

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pageData))) {
            byte pageType = dis.readByte();
            if (pageType != 1) {
                throw new IOException("Expected internal page, got type: " + pageType);
            }

            dis.readByte(); // reserved
            short numKeys = dis.readShort();
            int firstPointer = dis.readInt();

            List<String> keys = new ArrayList<>();
            List<Integer> childPointers = new ArrayList<>();

            for (int i = 0; i < numKeys; i++) {
                String key = dis.readUTF();
                int childPointer = dis.readInt();
                keys.add(key);
                childPointers.add(childPointer);
            }

            return new InternalPageData(firstPointer, keys, childPointers);
        }
    }

    // טעינת Leaf Page מהדיסק (כעת עם cache)
    private LeafPageData loadLeafPage(int pageId) throws IOException {
        byte[] pageData = loadPage(pageId);

        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(pageData))) {
            byte pageType = dis.readByte();
            if (pageType != 2) {
                throw new IOException("Expected leaf page, got type: " + pageType);
            }

            dis.readByte(); // reserved
            short numEntries = dis.readShort();
            int nextPageId = dis.readInt();

            List<IndexEntry> entries = new ArrayList<>();

            for (int i = 0; i < numEntries; i++) {
                String term = dis.readUTF();
                int numDocs = dis.readInt();
                long postingsOffset = dis.readLong();
                entries.add(new IndexEntry(term, numDocs, postingsOffset));
            }

            return new LeafPageData(nextPageId, entries);
        }
    }

    // טעינת עמוד עם cache משופר
    private byte[] loadPage(int pageId) throws IOException {
        // בדיקה ב-cache
        byte[] cachedData = pageCache.get(pageId);
        if (cachedData != null) {
            return cachedData;
        }

        // טעינה מהדיסק
        byte[] pageData = loadPageFromDisk(pageId);

        // הוספה ל-cache (השורש לא נכנס כי הוא כבר בזיכרון)
        if (pageId != rootPageId) {
            pageCache.put(pageId, pageData.clone());
        }

        return pageData;
    }

    // טעינה ישירה מהדיסק (ללא cache)
    private byte[] loadPageFromDisk(int pageId) throws IOException {
        // חישוב offset: Header (32 bytes) + pageId * pageSize
        long offset = 32L + (long) pageId * pageSize;
        treeFile.seek(offset);

        byte[] pageData = new byte[pageSize];
        int bytesRead = treeFile.read(pageData);

        if (bytesRead != pageSize) {
            throw new IOException("Failed to read complete page " + pageId +
                    ", read " + bytesRead + " bytes instead of " + pageSize);
        }

        return pageData;
    }

    // מציאת ה-Child Page ID המתאים במעמד Internal
    private int findChildPageId(InternalPageData page, String term) {
        // בדיקה אם המונח קטן מהמפתח הראשון
        if (page.keys.isEmpty() || term.compareTo(page.keys.get(0)) < 0) {
            return page.firstPointer;
        }

        // חיפוש במפתחות
        for (int i = 0; i < page.keys.size(); i++) {
            String key = page.keys.get(i);

            if (term.compareTo(key) < 0) {
                // המונח קטן מהמפתח הנוכחי, לך לילד הקודם
                return i == 0 ? page.firstPointer : page.childPointers.get(i - 1);
            } else if (term.equals(key)) {
                // מפתח זהה, לך לילד הבא
                return page.childPointers.get(i);
            }
        }

        // המונח גדול מכל המפתחות, לך לילד האחרון
        return page.childPointers.get(page.childPointers.size() - 1);
    }

    // חיפוש מונח ב-Leaf Page
    private SearchResult findTermInLeaf(LeafPageData page, String term) throws IOException {
        for (IndexEntry entry : page.entries) {
            if (term.equals(entry.term)) {
                // נמצא! טען את ה-postings
                List<PostingEntry> postings = loadPostings(entry.postingsOffset, entry.numDocs);
                return new SearchResult(entry.term, entry.numDocs, postings);
            }
        }

        return null; // לא נמצא
    }

    // חיפוש מידע על מונח ב-Leaf Page (ללא טעינת postings)
    private TermInfo findTermInfoInLeaf(LeafPageData page, String term) {
        for (IndexEntry entry : page.entries) {
            if (term.equals(entry.term)) {
                return new TermInfo(entry.term, entry.numDocs, entry.postingsOffset);
            }
        }

        return null; // לא נמצא
    }

    // טעינת Postings מקובץ ה-Postings
    private List<PostingEntry> loadPostings(long offset, int numDocs) throws IOException {
        postingsFile.seek(offset);

        List<PostingEntry> postings = new ArrayList<>();

        // קריאת כל ה-documents
        for (int i = 0; i < numDocs; i++) {
            int docId = postingsFile.readInt();
            int numPositions = postingsFile.readInt();

            List<Integer> positions = new ArrayList<>();
            for (int j = 0; j < numPositions; j++) {
                positions.add(postingsFile.readInt());
            }

            postings.add(new PostingEntry(docId, positions));
        }

        return postings;
    }

    // סטטיסטיקות על השימוש ב-cache
    public void printCacheStats() {
        System.out.println("Cache size: " + pageCache.size() + "/10");
        System.out.println("Root in memory: " + (rootPage != null ? "Yes" : "No"));
    }

    @Override
    public void close() throws IOException {
        if (treeFile != null) {
            treeFile.close();
        }
        if (postingsFile != null) {
            postingsFile.close();
        }
    }

    // Data Classes

    private static class TreeHeader {
        final short version;
        final int pageSize;
        final int rootPageId;
        final int totalPages;
        final short treeHeight;

        TreeHeader(short version, int pageSize, int rootPageId, int totalPages, short treeHeight) {
            this.version = version;
            this.pageSize = pageSize;
            this.rootPageId = rootPageId;
            this.totalPages = totalPages;
            this.treeHeight = treeHeight;
        }
    }

    private static class InternalPageData {
        final int firstPointer;
        final List<String> keys;
        final List<Integer> childPointers;

        InternalPageData(int firstPointer, List<String> keys, List<Integer> childPointers) {
            this.firstPointer = firstPointer;
            this.keys = keys;
            this.childPointers = childPointers;
        }
    }

    private static class LeafPageData {
        final int nextPageId;
        final List<IndexEntry> entries;

        LeafPageData(int nextPageId, List<IndexEntry> entries) {
            this.nextPageId = nextPageId;
            this.entries = entries;
        }
    }

    public static class SearchResult {
        public final String term;
        public final int numDocs;
        public final List<PostingEntry> postings;

        SearchResult(String term, int numDocs, List<PostingEntry> postings) {
            this.term = term;
            this.numDocs = numDocs;
            this.postings = postings;
        }

        @Override
        public String toString() {
            return "SearchResult{term='" + term + "', docs=" + numDocs + ", postings=" + postings.size() + "}";
        }
    }

    public static class TermInfo {
        public final String term;
        public final int numDocs;
        public final long postingsOffset;

        TermInfo(String term, int numDocs, long postingsOffset) {
            this.term = term;
            this.numDocs = numDocs;
            this.postingsOffset = postingsOffset;
        }

        @Override
        public String toString() {
            return "TermInfo{term='" + term + "', docs=" + numDocs + ", offset=" + postingsOffset + "}";
        }
    }

    public static class PostingEntry {
        public final int docId;
        public final List<Integer> positions;

        PostingEntry(int docId, List<Integer> positions) {
            this.docId = docId;
            this.positions = positions;
        }

        @Override
        public String toString() {
            return "Doc(" + docId + "):" + positions;
        }
    }

    public class DocumentLengthsStats {
        public final int documentCount;
        public final long totalLength;
        public final Map<Integer, Integer> documentLengths;

        public DocumentLengthsStats(long documentCount, long totalLength, Map<Integer, Integer> documentLengths) {
            this.documentCount = (int) documentCount;
            this.totalLength = totalLength;
            this.documentLengths = documentLengths;
        }
    }
}