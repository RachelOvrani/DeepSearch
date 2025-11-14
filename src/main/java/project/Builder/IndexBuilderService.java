package project.Builder;

import project.Common.Config;
import project.Common.ProjectLogger;
import project.Common.SecureIndexFolder;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.List;

public class IndexBuilderService {
    private final SPIMIInvertedIndex nameInvertedIndex;
    private final SPIMIInvertedIndex contentInvertedIndex;

    public IndexBuilderService() {
        this.nameInvertedIndex = new SPIMIInvertedIndex("name");
        this.contentInvertedIndex = new SPIMIInvertedIndex("content");
    }

    public void start(List<Path> pathsToIndex) throws IOException {
        System.out.println(Config.getInstance().getIndexPath());
        SecureIndexFolder.unlockFolder(Config.getInstance().getIndexPath());

        long start = System.nanoTime();
        EnumSet<FileVisitOption> options = EnumSet.noneOf(FileVisitOption.class);
        try (FileProcessor fileProcessor = new FileProcessor(nameInvertedIndex, contentInvertedIndex)) {
            for (Path rootPath : pathsToIndex) {
                try {
                    Files.walkFileTree(rootPath, options, Integer.MAX_VALUE, new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            safeProcess(file, attrs);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            safeProcess(dir, attrs);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) {
                            ProjectLogger.warning("Failed to access: " + file);
                            return FileVisitResult.CONTINUE;
                        }

                        private void safeProcess(Path path, BasicFileAttributes attrs) throws IOException {
                            try {
                                fileProcessor.processFileOrDirectory(path, attrs);
                            } catch (NullPointerException e) {
                                ProjectLogger.error("Null path component at: " + path);
                            }
                        }
                    });
                } catch (IOException e) {
                    ProjectLogger.error("Failed to walk file tree for: " + rootPath);
                }
            }
        }
        long end = System.nanoTime();
        System.out.println("\nזמן סריקת הקבצים ועיבודם : " + (end-start)/1000000 + " מילישניות.");



        start = System.nanoTime();

        nameInvertedIndex.mergeBlocks();
        contentInvertedIndex.mergeBlocks();
        end = System.nanoTime();

        SecureIndexFolder.lockFolder(Config.getInstance().getIndexPath());
        System.out.println("\nזמן מיזוג ובניית עצי B+ (בשלב אחד) : " + (end-start)/1000000 + " מילישניות.");
    }
}