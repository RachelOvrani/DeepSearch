package project.Builder;

import static project.Builder.UtfEncoder.getUTFLength;

public class IndexEntry {
    public final String term;
    public final int numDocs;
    public final long postingsOffset;

    public IndexEntry(String term, int numDocs, long postingsOffset) {
        this.term = term;
        this.numDocs = numDocs;
        this.postingsOffset = postingsOffset;
    }

    public int getSize() {
        return 2 + getUTFLength(term) + 4 + 8;
    }
}
