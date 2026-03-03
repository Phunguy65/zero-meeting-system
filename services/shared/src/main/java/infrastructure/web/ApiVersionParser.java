package infrastructure.web;

import org.springframework.web.accept.SemanticApiVersionParser;

/**
 * Custom API version parser that accepts both "v1", "v1.0", "1", and "1.0" formats,
 * normalising all of them to semantic version objects that Spring can compare.
 *
 * <p>Examples of accepted input:
 * <ul>
 *   <li>{@code v1}   → parsed as {@code 1.0.0}</li>
 *   <li>{@code v1.0} → parsed as {@code 1.0.0}</li>
 *   <li>{@code 1}    → parsed as {@code 1.0.0}</li>
 *   <li>{@code 1.0}  → parsed as {@code 1.0.0}</li>
 * </ul>
 */
public class ApiVersionParser extends SemanticApiVersionParser {

    @Override
    public Version parseVersion(String version) {
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return super.parseVersion(version);
    }
}
