package project.Builder;


import project.Common.Config;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BPlusTreeLeafPage {
    private Config config;

    private final int pageId;
    private final List<IndexEntry> entries = new ArrayList<>();
    private int nextPageId = -1;

    BPlusTreeLeafPage(int pageId) {
        this.config = Config.getInstance();
        this.pageId = pageId;
    }

    boolean canFit(IndexEntry entry) {
        int currentSize = 8; // header
        for (IndexEntry e : entries) {
            currentSize += e.getSize();
        }
        return (currentSize + entry.getSize()) <= config.getInt("page.size");
    }

    void addEntry(IndexEntry entry) { entries.add(entry); }
    void setNextPageId(int nextPageId) { this.nextPageId = nextPageId; }
    boolean isEmpty() { return entries.isEmpty(); }

    byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeByte(2);               // PAGE_TYPE_LEAF (1 byte)
            dos.writeByte(0);               // reserved (1 byte)
            dos.writeShort(entries.size());   // num entries (2 bytes)
            dos.writeInt(nextPageId);         // next page ID (4 bytes)


            for (IndexEntry entry : entries) {
                dos.writeUTF(entry.term);
                dos.writeInt(entry.numDocs);
                dos.writeLong(entry.postingsOffset);
            }

            byte[] result = new byte[config.getInt("page.size")];
            byte[] data = baos.toByteArray();
            System.arraycopy(data, 0, result, 0, Math.min(data.length, result.length));
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
