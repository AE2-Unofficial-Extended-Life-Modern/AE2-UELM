package appeng.api.crafting;

/**
 * Storage class to allow for easier future expansions of pattern information without having to change method
 * signatures. For maybe stuff like tag matching and whatnot.
 */
public record PatternInfo(String author) {
    public static final PatternInfo EMPTY = new PatternInfo("");

    public PatternInfo {
        author = author == null ? "" : author;
    }

    public static PatternInfo ofAuthor(String author) {
        return new PatternInfo(author);
    }
}
