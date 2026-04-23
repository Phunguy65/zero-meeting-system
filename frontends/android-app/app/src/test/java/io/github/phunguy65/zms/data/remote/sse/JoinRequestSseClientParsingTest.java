package io.github.phunguy65.zms.data.remote.sse;

import static org.junit.Assert.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link JoinRequestSseClient} JSON parsing.
 *
 * <p>Focuses on Jackson deserialization correctness for approval/denial event payloads,
 * including edge cases with special characters in tokens and malformed payloads.
 */
@RunWith(MockitoJUnitRunner.class)
public class JoinRequestSseClientParsingTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new ObjectMapper()
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    public void parseApprovedToken_extractsTokenFromValidPayload() throws Exception {
        String data = "{\"token\":\"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.abc123\","
                + "\"roomName\":\"room-123\"}";
        JoinRequestSseClient.ApprovedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.ApprovedEventData.class);

        assertEquals("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.abc123", parsed.token);
        assertEquals("room-123", parsed.roomName);
    }

    @Test
    public void parseApprovedToken_handlesJwtWithEmbeddedQuotesAndColons() throws Exception {
        String jwt = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9"
                + ".eyJpc3MiOiJ0ZXN0IiwiZXhwIjoxNjk5OTk5OTk5fQ"
                + ".sign_with:special\"chars";
        String data = "{\"token\":\"" + escapeJson(jwt) + "\",\"roomName\":\"r\"}";

        JoinRequestSseClient.ApprovedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.ApprovedEventData.class);

        assertEquals(jwt, parsed.token);
    }

    @Test
    public void parseApprovedToken_handlesExtraFieldsGracefully() throws Exception {
        String data = "{\"token\":\"abc\",\"roomName\":\"room\",\"extra\":\"value\"}";
        JoinRequestSseClient.ApprovedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.ApprovedEventData.class);

        assertEquals("abc", parsed.token);
        assertEquals("room", parsed.roomName);
    }

    @Test
    public void parseDeniedReason_extractsReasonFromValidPayload() throws Exception {
        String data = "{\"reason\":\"Meeting has ended\"}";
        JoinRequestSseClient.DeniedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.DeniedEventData.class);

        assertEquals("Meeting has ended", parsed.reason);
    }

    @Test
    public void parseDeniedReason_returnsNullReasonForMissingField() throws Exception {
        String data = "{\"otherField\":\"value\"}";
        JoinRequestSseClient.DeniedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.DeniedEventData.class);

        assertNull(parsed.reason);
    }

    @Test
    public void parseApprovedToken_returnsNullTokenForEmptyPayload() throws Exception {
        String data = "{}";
        JoinRequestSseClient.ApprovedEventData parsed =
                objectMapper.readValue(data, JoinRequestSseClient.ApprovedEventData.class);

        assertNull(parsed.token);
        assertNull(parsed.roomName);
    }

    @Test(expected = Exception.class)
    public void parseApprovedToken_throwsOnInvalidJson() throws Exception {
        String data = "not-json";
        objectMapper.readValue(data, JoinRequestSseClient.ApprovedEventData.class);
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
