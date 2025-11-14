package project.UI.Model;


/**
 * מחלקה לייצוג תוצאת חיפוש
 */
public class SearchResult {
    private final String name;
    private final String path;
    private final String size;
    private final String modified;
    private final String created;
    private final String locationResult;

    public SearchResult(String name, String path, String size, String modified, String created, String locationResult) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.modified = modified;
        this.created = created;
        this.locationResult = locationResult;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getSize() { return size; }
    public String getModified() { return modified; }
    public String getCreated() { return created; }
    public String getLocationResult() { return locationResult; }



    /**
     * מחזיר את הנתיב המלא של הקובץ
     */
    public String getFullPath() {
        return path + "\\" + name;
    }
}