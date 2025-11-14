package project.Builder;

import project.Common.Config;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static project.Builder.UtfEncoder.getUTFLength;

public class BPlusTreeInternalPage {
    private Config config;

    private final int pageId;
    private int firstPointer = -1;
    private final List<String> keys = new ArrayList<>();
    private final List<Integer> childPointers = new ArrayList<>();

    BPlusTreeInternalPage(int pageId) {
        this.config = Config.getInstance();
        this.pageId = pageId;
    }

    void addFirstChild(int childPageId) { this.firstPointer = childPageId; }

    void addChild(String separatorKey, int childPageId) {
        keys.add(separatorKey);
        childPointers.add(childPageId);
    }

    boolean isEmpty() {
        return firstPointer == -1 && keys.isEmpty();
    }

    //בדיקת מקום - בודק אם יש מקום להוסיף מפתח נוסף
    boolean canFit(String newKey) {
        int currentSize = 4 + 4;  // header + firstPointer

        // חיבור גודל כל המפתחות הקיימים
        for (String key : keys) {
            currentSize += 2 + getUTFLength(key) + 4; // UTF prefix + key + pointer
        }

        // גודל המפתח החדש
        int newEntrySize = 2 + getUTFLength(newKey) + 4;

        return (currentSize + newEntrySize) <= config.getInt("page.size");
    }

    byte[] serialize() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);

            dos.writeByte(1);               // PAGE_TYPE_INTERNAL (1 byte)
            dos.writeByte(0);               // reserved (1 byte)
            dos.writeShort(keys.size());       // num keys (2 bytes)

            dos.writeInt(firstPointer);     // first child pointer (4 bytes)
            for (int i = 0; i < keys.size(); i++) {
                dos.writeUTF(keys.get(i));
                dos.writeInt(childPointers.get(i));
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
