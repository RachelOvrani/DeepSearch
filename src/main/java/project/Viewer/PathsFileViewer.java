package project.Viewer;

import project.Common.Config;

import java.io.*;


public class PathsFileViewer {

    public static void printPathsFile(String filePath) {
        System.out.println("=== קובץ נתיבים: " + filePath + " ===");
        
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(filePath)))) {
            
            int pathCount = 0;
            long currentOffset = 0;
            

            System.out.println("-".repeat(80));
            System.out.println("פורמט: Path# (Start Offset, Length) -> Path");

            while (dis.available() > 0) {
                long startOffset = currentOffset;
                
                try {
                    // קריאת אורך המחרוזת (2 bytes לפי פרוטוקול UTF)
                    int utfLength = dis.readUnsignedShort();
                    currentOffset += 2; // short עבור אורך
                    
                    // קריאת המחרוזת
                    byte[] pathBytes = new byte[utfLength];
                    dis.readFully(pathBytes);
                    String path = new String(pathBytes, "UTF-8");
                    currentOffset += utfLength;
                    
                    long totalLength = currentOffset - startOffset;
                    
                    System.out.printf("Path #%04d (Offset: %8d, Length: %3d) -> %s%n", 
                                    pathCount, startOffset, totalLength, path);
                    pathCount++;
                    
                } catch (IOException e) {
                    System.err.println("שגיאה בקריאת נתיב #" + pathCount + " ב-offset " + startOffset + ": " + e.getMessage());
                    break;
                }
            }
            
            System.out.println("-".repeat(80));
            System.out.println("סה\"כ נתיבים: " + pathCount);
            System.out.println("גודל קובץ: " + currentOffset + " bytes");
            
        } catch (FileNotFoundException e) {
            System.err.println("קובץ לא נמצא: " + filePath);
        } catch (IOException e) {
            System.err.println("שגיאה בקריאת קובץ נתיבים: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    

    // =============== Main ===============
    
    public static void main(String[] args) {
        Config config = Config.getInstance();
        printPathsFile(config.getPathsFile());
    }
}