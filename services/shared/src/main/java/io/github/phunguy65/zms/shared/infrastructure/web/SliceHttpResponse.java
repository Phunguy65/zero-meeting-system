package io.github.phunguy65.zms.shared.infrastructure.web;

import java.util.List;

/**
 * Generic JSON response envelope for slice-based (no total-count) list endpoints.
 *
 * <p>All slice-based list endpoints in any module MUST wrap their item list in this record and then
 * pass it to {@link JsendResponse#success(Object)}.
 *
 * <p>Example JSON shape:
 * <pre>{@code
 * {
 *   "status": "success",
 *   "data": {
 *     "content": [...],
 *     "page": 0,
 *     "size": 20,
 *     "hasNext": true,
 *     "hasPrevious": false
 *   }
 * }
 * }</pre>
 *
 * @param <T> the item type for each element in {@code content}
 */
public record SliceHttpResponse<T>(
        List<T> content, int page, int size, boolean hasNext, boolean hasPrevious) {}
