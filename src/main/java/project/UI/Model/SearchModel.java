package project.UI.Model;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import project.Common.Config;

import java.util.ArrayList;
import java.util.List;

public class SearchModel {
    // search term
    private final StringProperty searchQuery = new SimpleStringProperty();

    // search results
    private final ListProperty<SearchResult> searchResults = new SimpleListProperty<>(FXCollections.observableArrayList());

    // search status message
    private final StringProperty statusMessage = new SimpleStringProperty();

    // results count
    private final IntegerProperty resultsCount = new SimpleIntegerProperty();

    // file type filter
    private final ObjectProperty<FileType> fileType = new SimpleObjectProperty<>();

    // search in content or name files
    private final BooleanProperty searchInContent = new SimpleBooleanProperty();

    // search exact match
    private final BooleanProperty exactMatch = new SimpleBooleanProperty();

    // search enabled (based on index availability)
    private final BooleanProperty searchEnabled = new SimpleBooleanProperty();

    // Config instance for checking indexed drives
    private final Config config;

    // singleton instance
    private static SearchModel instance;

    public static SearchModel getInstance() {
        if (instance == null) {
            instance = new SearchModel();
        }
        return instance;
    }

    public SearchModel() {
        this.config = Config.getInstance();
        resetToDefaults();
        updateSearchAvailability();
    }

    // update search availability based on indexed drives
    public void updateSearchAvailability() {
        List<String> indexedDrives = config.getIndexedDrives();
        boolean hasIndex = indexedDrives != null && !indexedDrives.isEmpty();

        setSearchEnabled(hasIndex);

        if (!hasIndex) {
            setStatusMessage("יש ליצור אינדקס תחילה.");
        } else if (getSearchQuery().isEmpty()) {
            setStatusMessage("התחל חיפוש...");
        }
    }

    // getters and setters

    // SearchTerm
    public String getSearchQuery() { return searchQuery.get(); }
    public void setSearchQuery(String searchTerm) { this.searchQuery.set(searchTerm); }
    public StringProperty searchQueryProperty() { return searchQuery; }

    // search results
    public ObservableList<SearchResult> getSearchResults() { return searchResults.get(); }
    public void clearAndSetSearchResults(List<SearchResult> newResults) {
        this.searchResults.clear();
        this.searchResults.addAll(newResults);
    }
    public ListProperty<SearchResult> searchResultsProperty() { return searchResults; }

    // search status message
    public String getStatusMessage() { return statusMessage.get(); }
    public void setStatusMessage(String statusMessage) { this.statusMessage.set(statusMessage); }
    public StringProperty statusMessageProperty() { return statusMessage; }

    // results count
    public int getResultsCount() { return resultsCount.get(); }
    public void setResultsCount(int resultsCount) { this.resultsCount.set(resultsCount); }
    public IntegerProperty resultsCountProperty() { return resultsCount; }

    // FileType
    public FileType getFileType() { return fileType.get();}
    public void setFileType(FileType fileType) { this.fileType.set(fileType); }
    public ObjectProperty<FileType> fileTypeProperty() { return fileType; }

    // SearchInContent
    public boolean isSearchInContent() { return searchInContent.get(); }
    public void setSearchInContent(boolean searchInContent) { this.searchInContent.set(searchInContent); }
    public BooleanProperty searchInContentProperty() { return searchInContent; }

    // ExactMatch
    public boolean isExactMatch() { return exactMatch.get(); }
    public void setExactMatch(boolean exactMatch) { this.exactMatch.set(exactMatch); }
    public BooleanProperty exactMatchProperty() { return exactMatch; }

    // SearchEnabled
    public boolean isSearchEnabled() { return searchEnabled.get(); }
    public void setSearchEnabled(boolean searchEnabled) { this.searchEnabled.set(searchEnabled); }
    public BooleanProperty searchEnabledProperty() { return searchEnabled; }

    // Reset
    public void resetToDefaults() {
        setSearchQuery("");
        clearAndSetSearchResults(new ArrayList<>());
        setResultsCount(0);
        setFileType(FileType.ALL);
        setSearchInContent(false);
        setExactMatch(true);
        updateSearchAvailability();
    }
}