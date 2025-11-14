package project.UI.Util;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.filechooser.FileSystemView;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * כלי עזר לקבלת אייקונים של קבצים
 */
public class FileIconUtil {

    /**
     * מחזיר אייקון של קובץ מהמערכת
     */
    public static ImageView getSystemIcon(File file) {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        Icon swingIcon = fileSystemView.getSystemIcon(file);

        if (swingIcon instanceof ImageIcon) {
            ImageIcon imageIcon = (ImageIcon) swingIcon;
            BufferedImage bufferedImage = toBufferedImage(imageIcon.getImage());
            Image fxImage = SwingFXUtils.toFXImage(bufferedImage, null);
            ImageView iconView = new ImageView(fxImage);
            iconView.setFitWidth(16);
            iconView.setFitHeight(16);
            return iconView;
        }
        return new ImageView(); // מחזיר אייקון ריק אם לא נמצא אייקון מתאים
    }

    /**
     * ממיר java.awt.Image ל-BufferedImage
     */
    private static BufferedImage toBufferedImage(java.awt.Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        BufferedImage bImage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = bImage.createGraphics();
        graphics.drawImage(img, 0, 0, null);
        graphics.dispose();
        return bImage;
    }
}