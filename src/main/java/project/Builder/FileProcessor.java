package project.Builder;

import project.Common.Config;
import project.Common.ProjectLogger;
import project.Common.TextAnalyzer;
import project.Common.TextExtractor;
import org.apache.commons.io.output.CountingOutputStream;


import java.io.*;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class FileProcessor implements AutoCloseable {
    private Config config;

    public static final AtomicLong filenameLength  = new AtomicLong(0);
    public static final AtomicLong fileCount  = new AtomicLong(0);
    public static final AtomicLong contentLength  = new AtomicLong(0);
    public static final AtomicLong textFileCount  = new AtomicLong(0);


    private final CountingOutputStream countingOut;
    private final DataOutputStream pathDataWriter;

    private final DataOutputStream fileNameLenDataWriter;
    private final DataOutputStream textFileLenDataWriter;

    private final SPIMIInvertedIndex nameInvertedIndex;
    private final SPIMIInvertedIndex contentInvertedIndex;

    public FileProcessor(SPIMIInvertedIndex nameInvertedIndex,
                         SPIMIInvertedIndex contentInvertedIndex) throws FileNotFoundException {

        this.config = Config.getInstance();

        this.countingOut = new CountingOutputStream(
                new BufferedOutputStream(new FileOutputStream(config.getPathsFile()),
                        config.getBufferSize()));
        this.pathDataWriter = new DataOutputStream(countingOut);

        this.fileNameLenDataWriter = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(config.getLenFile("name")),
                config.getBufferSize()));

        this.textFileLenDataWriter = new DataOutputStream(new BufferedOutputStream(
                new FileOutputStream(config.getLenFile("content")),
                config.getBufferSize()));

        try {
            this.fileNameLenDataWriter.writeLong(0); // שמירה של מספר הקבצים
            this.fileNameLenDataWriter.writeLong(0); // שמירה לאורך שמות הקבצים הכולל

            this.textFileLenDataWriter.writeLong(0); // שמירה של מספר קבצי הטקסט
            this.textFileLenDataWriter.writeLong(0); // שמירה לאורך הקבצים הכולל
        } catch (IOException e) {
            System.out.println("שגיאה בכתיבה לקובץ אורך קבצים." );
        }



        this.nameInvertedIndex = nameInvertedIndex;
        this.contentInvertedIndex = contentInvertedIndex;
    }


    public void processFileOrDirectory(Path path, BasicFileAttributes attrs) {
        int docId;

        try{
            docId = writePathToDisk(path.toString());
        } catch (IOException e) {
            ProjectLogger.error("Failed to write path to disk: " + path);
            return;
        }

        try {
            processNameFile(path, docId);
        } catch (IOException e) {
            ProjectLogger.error("Error while extracting file name: " + path.getFileName());
        }

        if(TextExtractor.isSupportedFile(path)){
            try {
                processTextFile(path, docId, attrs);
            } catch (IOException e) {
                ProjectLogger.error("Error while reading file content: " + path.getFileName());
            }
        }
    }

    private int writePathToDisk(String path) throws IOException {
        int offset = countingOut.getCount(); // get current position before writing
        pathDataWriter.writeUTF(path);
        pathDataWriter.flush();
        return offset;
    }

    private void processNameFile(Path path, int docId) throws IOException {
        String fileName = path.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        String text = (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);

        List<String> tokens = TextAnalyzer.tokenize(text);


        List<String> normalizedTokens = new ArrayList<>();
        for (String token : tokens) {
            String normalized = TextAnalyzer.normalize(token);
            if (!normalized.isEmpty()) {
                normalizedTokens.add(normalized);
            }
        }

        writeFileNameLen(docId, normalizedTokens.size());


        for (int i = 0; i < normalizedTokens.size(); i++) {
            String term = normalizedTokens.get(i);
            nameInvertedIndex.addToTempIndex(term, docId, i);
        }
    }

    private void processTextFile(Path path, int docId, BasicFileAttributes attrs) throws IOException {
        File file = path.toFile();
        String text = TextExtractor.extractText(file);

        List<String> tokens = TextAnalyzer.tokenize(text);

        List<String> normalizedTokens = new ArrayList<>();
        for (String token : tokens) {
            String normalized = TextAnalyzer.normalize(token);
            if (!normalized.isEmpty()) {
                normalizedTokens.add(normalized);
            }
        }


        writeFileContentLen(docId, normalizedTokens.size());

        for (int i = 0; i < normalizedTokens.size(); i++) {
            String term = normalizedTokens.get(i);
            contentInvertedIndex.addToTempIndex(term, docId, i);
        }
    }

    private void writeFileNameLen(int docId, int size) {
        fileCount.incrementAndGet();
        filenameLength.addAndGet(size);

        try {
            fileNameLenDataWriter.writeInt(docId);
            fileNameLenDataWriter.writeInt(size);
        } catch (IOException e) {
            System.out.println("שגיאה במירת אורך הקובץ : " + docId + ", אורך : " + size);
        }

    }

    private void writeFileContentLen(int docId, int size) {
        textFileCount.incrementAndGet();
        contentLength.addAndGet(size);

        try {
            textFileLenDataWriter.writeInt(docId);
            textFileLenDataWriter.writeInt(size);
        } catch (IOException e) {
            System.out.println("שגיאה בשמירת אורך הקובץ : " + docId + ", אורך : " + size);
        }

    }

    @Override
    public void close() {
        try {
            pathDataWriter.close();

            // סגור קודם את ה-DataOutputStream של השמות
            fileNameLenDataWriter.flush();
            fileNameLenDataWriter.close();

            // ואז עדכן
            try (RandomAccessFile raf = new RandomAccessFile(config.getLenFile("name"), "rw")) {
                raf.seek(0);
                raf.writeLong(fileCount.get());
                raf.writeLong(filenameLength.get());
            }

            // סגור קודם את ה-DataOutputStream של התוכן
            textFileLenDataWriter.flush();
            textFileLenDataWriter.close();

            // ואז עדכן
            try (RandomAccessFile raf = new RandomAccessFile(config.getLenFile("content"), "rw")) {
                raf.seek(0);
                raf.writeLong(textFileCount.get());
                raf.writeLong(contentLength.get());
            }

        } catch (IOException e) {
            ProjectLogger.error("Failed to close FileProcessor: " + e.getMessage());
        }
    }

}
