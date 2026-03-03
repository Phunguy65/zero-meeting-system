package domain;

/**
 * Sort direction for paginated domain queries.
 *
 * <p>Pure Java enum with zero framework dependencies, suitable for use in domain repository port
 * signatures while maintaining ArchUnit-enforced domain purity.
 */
public enum SortDirection {
    ASC,
    DESC
}
