import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';

/** Discriminated-union state for the upcoming meetings list. */
export type UpcomingMeetingsState =
    | { phase: 'LOADING' }
    | { phase: 'SUCCESS'; meetings: MeetingManagementMeetingResponse[] }
    | { phase: 'EMPTY' }
    | { phase: 'ERROR' };

/** Actions exposed by the upcoming meetings hook to child components. */
export type UpcomingMeetingActions = {
    selectMeeting: (meeting: MeetingManagementMeetingResponse) => void;
    clearSelectedMeeting: () => void;
    openSettings: (meetingId: string) => void;
    closeSettings: () => void;
    requestCancel: (meeting: MeetingManagementMeetingResponse) => void;
    dismissCancel: () => void;
    confirmCancel: (errorFallback: string) => Promise<void>;
    copyShortCode: (shortCode: string) => Promise<void>;
    retry: () => void;
};

/** Full state bag returned from the upcoming meetings hook. */
export type UseUpcomingMeetingsResult = {
    listState: UpcomingMeetingsState;
    selectedMeeting: MeetingManagementMeetingResponse | null;
    cancelTarget: MeetingManagementMeetingResponse | null;
    isCancelling: boolean;
    cancelError: string | null;
    cancelFeedback: boolean;
    settingsMeetingId: string | null;
    copyFeedback: boolean;
    actions: UpcomingMeetingActions;
};
