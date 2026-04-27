'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import {
    approveAllJoinRequests,
    approveJoinRequest,
    denyJoinRequest,
    listJoinRequests,
} from '@/generated/sdk.gen.ts';
import type { MeetingManagementJoinRequestResponse } from '@/generated/types.gen.ts';

type WaitingRoomState = {
    requests: MeetingManagementJoinRequestResponse[];
    isLoading: boolean;
    error: string | null;
};

type UseWaitingRoomResult = {
    requests: MeetingManagementJoinRequestResponse[];
    pendingCount: number;
    isLoading: boolean;
    error: string | null;
    approve: (requestId: string) => Promise<void>;
    deny: (requestId: string) => Promise<void>;
    approveAll: () => Promise<void>;
    refresh: () => void;
};

const JOIN_REQUEST_EVENTS = [
    'join_request_created',
    'join_request_expired',
] as const;
const SSE_RETRY_DELAY_MS = 3000;

/**
 * Manages host-facing waiting room state including pending join requests,
 * approve/deny/approve-all mutations, and SSE-driven live updates.
 *
 * The list endpoint is the authoritative source of truth; SSE events trigger
 * targeted refetches rather than purely event-sourced local state.
 */
export function useWaitingRoom(meetingId: string | null): UseWaitingRoomResult {
    const [state, setState] = useState<WaitingRoomState>({
        requests: [],
        isLoading: false,
        error: null,
    });

    const sseRef = useRef<EventSource | null>(null);
    const retryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const isMountedRef = useRef(true);

    const loadRequests = useCallback(() => {
        if (!meetingId) return;

        setState((prev) => ({ ...prev, isLoading: true, error: null }));

        listJoinRequests({ path: { id: meetingId } })
            .then(({ data }) => {
                if (!isMountedRef.current) return;
                setState({
                    requests: data?.content ?? [],
                    isLoading: false,
                    error: null,
                });
            })
            .catch(() => {
                if (!isMountedRef.current) return;
                setState((prev) => ({
                    ...prev,
                    isLoading: false,
                    error: 'waitingRoomLoadError',
                }));
            });
    }, [meetingId]);

    const closeSse = useCallback(() => {
        if (retryTimerRef.current !== null) {
            clearTimeout(retryTimerRef.current);
            retryTimerRef.current = null;
        }
        if (sseRef.current) {
            sseRef.current.close();
            sseRef.current = null;
        }
    }, []);

    const openSse = useCallback(() => {
        if (!meetingId) return;
        closeSse();

        const es = new EventSource(`/api/v1/meetings/${meetingId}/events`);
        sseRef.current = es;

        const handleJoinRequestEvent = () => {
            if (isMountedRef.current) {
                loadRequests();
            }
        };

        JOIN_REQUEST_EVENTS.forEach((eventType) => {
            es.addEventListener(eventType, handleJoinRequestEvent);
        });

        es.onerror = () => {
            closeSse();
            if (isMountedRef.current) {
                retryTimerRef.current = setTimeout(() => {
                    if (isMountedRef.current) {
                        openSse();
                        loadRequests();
                    }
                }, SSE_RETRY_DELAY_MS);
            }
        };
    }, [meetingId, closeSse, loadRequests]);

    useEffect(() => {
        isMountedRef.current = true;
        if (meetingId) {
            loadRequests();
            openSse();
        }
        return () => {
            isMountedRef.current = false;
            closeSse();
        };
    }, [meetingId, loadRequests, openSse, closeSse]);

    const remove = useCallback((requestId: string) => {
        setState((prev) => ({
            ...prev,
            requests: prev.requests.filter((r) => r.id !== requestId),
        }));
    }, []);

    const approve = useCallback(
        async (requestId: string) => {
            if (!meetingId) return;
            remove(requestId);
            try {
                await approveJoinRequest({
                    path: { id: meetingId, requestId },
                    throwOnError: true,
                });
                loadRequests();
            } catch {
                loadRequests();
            }
        },
        [meetingId, remove, loadRequests],
    );

    const deny = useCallback(
        async (requestId: string) => {
            if (!meetingId) return;
            remove(requestId);
            try {
                await denyJoinRequest({
                    path: { id: meetingId, requestId },
                    throwOnError: true,
                });
                loadRequests();
            } catch {
                loadRequests();
            }
        },
        [meetingId, remove, loadRequests],
    );

    const approveAll = useCallback(async () => {
        if (!meetingId) return;
        setState((prev) => ({ ...prev, requests: [] }));
        try {
            await approveAllJoinRequests({
                path: { id: meetingId },
                throwOnError: true,
            });
            loadRequests();
        } catch {
            loadRequests();
        }
    }, [meetingId, loadRequests]);

    const pendingCount = state.requests.filter(
        (r) => r.status === 'PENDING',
    ).length;

    return {
        requests: state.requests,
        pendingCount,
        isLoading: state.isLoading,
        error: state.error,
        approve,
        deny,
        approveAll,
        refresh: loadRequests,
    };
}
