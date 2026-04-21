package io.github.phunguy65.zms.data.mapper;

import static org.junit.Assert.*;

import io.github.phunguy65.zms.data.remote.dto.MeetingManagementMeetingSettingsResponse;
import io.github.phunguy65.zms.domain.model.MeetingSettings;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link MeetingMapper}.
 * Focuses on settings mapping including requirePassword field.
 */
public class MeetingMapperTest {

    private MeetingMapper mapper;

    @Before
    public void setup() {
        mapper = new MeetingMapper();
    }

    @Test
    public void toMeetingSettings_nullSource_returnsDefaults() {
        MeetingSettings result = mapper.toMeetingSettings(null);

        assertNotNull(result);
        assertFalse(result.isRequirePassword());
        assertTrue(result.isWaitingRoomEnabled());
        assertTrue(result.isAllowGuest());
    }

    @Test
    public void toMeetingSettings_requirePasswordTrue_mapsCorrectly() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setRequirePassword(true);
        source.setAllowGuest(true);
        source.setMaxParticipants(50);
        source.setAllowScreenShare(true);
        source.setChatEnabled(true);
        source.setAllowMicrophone(true);
        source.setAllowVideo(true);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertTrue(result.isRequirePassword());
    }

    @Test
    public void toMeetingSettings_requirePasswordFalse_mapsCorrectly() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setRequirePassword(false);
        source.setAllowGuest(true);
        source.setMaxParticipants(100);
        source.setAllowScreenShare(true);
        source.setChatEnabled(true);
        source.setAllowMicrophone(true);
        source.setAllowVideo(true);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertFalse(result.isRequirePassword());
    }

    @Test
    public void toMeetingSettings_requirePasswordNull_defaultsToFalse() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setRequirePassword(null);
        source.setAllowGuest(true);
        source.setMaxParticipants(100);
        source.setAllowScreenShare(true);
        source.setChatEnabled(true);
        source.setAllowMicrophone(true);
        source.setAllowVideo(true);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertFalse(result.isRequirePassword());
    }

    @Test
    public void toMeetingSettings_waitingRoomFromAdmissionPolicy_mapsCorrectly() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setAdmissionPolicy("MANUAL_APPROVAL");
        source.setRequirePassword(false);
        source.setAllowGuest(true);
        source.setMaxParticipants(100);
        source.setAllowScreenShare(true);
        source.setChatEnabled(true);
        source.setAllowMicrophone(true);
        source.setAllowVideo(true);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertTrue(result.isWaitingRoomEnabled());
    }

    @Test
    public void toMeetingSettings_noWaitingRoom_whenDifferentPolicy() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setAdmissionPolicy("AUTO_ADMIT");
        source.setRequirePassword(false);
        source.setAllowGuest(true);
        source.setMaxParticipants(100);
        source.setAllowScreenShare(true);
        source.setChatEnabled(true);
        source.setAllowMicrophone(true);
        source.setAllowVideo(true);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertFalse(result.isWaitingRoomEnabled());
    }

    @Test
    public void toMeetingSettings_allFieldsMapped() {
        MeetingManagementMeetingSettingsResponse source = new MeetingManagementMeetingSettingsResponse();
        source.setAdmissionPolicy("MANUAL_APPROVAL");
        source.setRequirePassword(true);
        source.setAllowGuest(false);
        source.setMaxParticipants(25);
        source.setAllowScreenShare(false);
        source.setChatEnabled(false);
        source.setAllowMicrophone(false);
        source.setAllowVideo(false);

        MeetingSettings result = mapper.toMeetingSettings(source);

        assertTrue(result.isWaitingRoomEnabled());
        assertTrue(result.isRequirePassword());
        assertFalse(result.isAllowGuest());
        assertEquals(25, result.getMaxParticipants());
        assertFalse(result.isAllowScreenShare());
        assertFalse(result.isChatEnabled());
        assertFalse(result.isAllowMicrophone());
        assertFalse(result.isAllowVideo());
    }
}
