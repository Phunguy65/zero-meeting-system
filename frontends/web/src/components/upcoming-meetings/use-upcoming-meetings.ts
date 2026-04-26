'use client';

import { useCallback, useEffect, useState } from 'react';
import { cancelMeeting, listHostMeetings } from '@/generated/sdk.gen.ts';
import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';
import { ApiError, ApiFailError } from '@/lib/api/types.ts';
import type {
    UpcomingMeetingsState,
    UseUpcomingMeetingsResult,
} from './types.ts';

function isUpcoming(
    meeting: MeetingManagementMeetingResponse,
    now: Date,
): boolean {
    if (meeting.status !== 'SCHEDULED') return false;
    if (!meeting.startTime) return false;
    return new Date(meeting.startTime) > now;
}

function sortAscendingByStartTime(
    a: MeetingManagementMeetingResponse,
    b: MeetingManagementMeetingResponse,
): number {
    const aTime = a.startTime ? new Date(a.startTime).getTime() : 0;
    const bTime = b.startTime ? new Date(b.startTime).getTime() : 0;
    return aTime - bTime;
}

/**
 * Fetches host meetings, filters to scheduled future meetings sorted by
 * ascending start time, and exposes centralised action handlers for
 * meeting selection, copy-link feedback, settings, and cancellation.
 */
export function useUpcomingMeetings(): UseUpcomingMeetingsResult {
    const [listState, setListState] = useState<UpcomingMeetingsState>({
        phase: 'LOADING',
    });
    const [selectedMeeting, setSelectedMeeting] =
        useState<MeetingManagementMeetingResponse | null>(null);
    const [cancelTarget, setCancelTarget] =
        useState<MeetingManagementMeetingResponse | null>(null);
    const [isCancelling, setIsCancelling] = useState(false);
    const [cancelError, setCancelError] = useState<string | null>(null);
    const [cancelFeedback, setCancelFeedback] = useState(false);
    const [settingsMeetingId, setSettingsMeetingId] = useState<string | null>(
        null,
    );
    const [copyFeedback, setCopyFeedback] = useState(false);

    const loadMeetings = useCallback(() => {
        const now = new Date();
        setListState({ phase: 'LOADING' });

        listHostMeetings()
            .then(({ data }) => {
                const all = data?.content ?? [];
                const upcoming = all
                    .filter((m) => isUpcoming(m, now))
                    .sort(sortAscendingByStartTime);

                if (upcoming.length === 0) {
                    setListState({ phase: 'EMPTY' });
                } else {
                    setListState({ phase: 'SUCCESS', meetings: upcoming });
                }
            })
            .catch(() => {
                setListState({ phase: 'ERROR' });
            });
    }, []);

    useEffect(() => {
        loadMeetings();
    }, [loadMeetings]);

    const retry = useCallback(() => {
        loadMeetings();
    }, [loadMeetings]);

    const selectMeeting = useCallback(
        (meeting: MeetingManagementMeetingResponse) => {
            setSelectedMeeting(meeting);
        },
        [],
    );

    const clearSelectedMeeting = useCallback(() => {
        setSelectedMeeting(null);
    }, []);

    const openSettings = useCallback((meetingId: string) => {
        setSettingsMeetingId(meetingId);
    }, []);

    const closeSettings = useCallback(() => {
        setSettingsMeetingId(null);
    }, []);

    const requestCancel = useCallback(
        (meeting: MeetingManagementMeetingResponse) => {
            setCancelError(null);
            setCancelTarget(meeting);
        },
        [],
    );

    const dismissCancel = useCallback(() => {
        setCancelTarget(null);
        setCancelError(null);
    }, []);

    const confirmCancel = useCallback(
        async (errorFallback: string) => {
            if (!cancelTarget?.id) return;

            setIsCancelling(true);
            setCancelError(null);

            try {
                await cancelMeeting({
                    path: { id: cancelTarget.id },
                    throwOnError: true,
                });

                const cancelledId = cancelTarget.id;
                setCancelTarget(null);
                setSelectedMeeting(null);
                setCancelFeedback(true);
                setTimeout(() => setCancelFeedback(false), 3000);
                setListState((prev) => {
                    if (prev.phase !== 'SUCCESS') return prev;
                    const remaining = prev.meetings.filter(
                        (m) => m.id !== cancelledId,
                    );
                    return remaining.length === 0
                        ? { phase: 'EMPTY' }
                        : { phase: 'SUCCESS', meetings: remaining };
                });
            } catch (error) {
                if (
                    error instanceof ApiFailError
                    || error instanceof ApiError
                ) {
                    setCancelError(error.message);
                } else {
                    setCancelError(errorFallback);
                }
            } finally {
                setIsCancelling(false);
            }
        },
        [cancelTarget],
    );

    const copyShortCode = useCallback(async (shortCode: string) => {
        try {
            await navigator.clipboard.writeText(shortCode);
            setCopyFeedback(true);
            setTimeout(() => setCopyFeedback(false), 2000);
        } catch {
            return;
        }
    }, []);

    return {
        listState,
        selectedMeeting,
        cancelTarget,
        isCancelling,
        cancelError,
        cancelFeedback,
        settingsMeetingId,
        copyFeedback,
        actions: {
            selectMeeting,
            clearSelectedMeeting,
            openSettings,
            closeSettings,
            requestCancel,
            dismissCancel,
            confirmCancel,
            copyShortCode,
            retry,
        },
    };
}
