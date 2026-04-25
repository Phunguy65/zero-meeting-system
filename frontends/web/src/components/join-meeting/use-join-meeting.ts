'use client';

import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useReducer, useRef } from 'react';
import { getMeetingByShortCode, requestJoin } from '@/generated/sdk.gen.ts';
import { ApiError, ApiFailError } from '@/lib/api/types.ts';

export type JoinMode = 'guest' | 'authenticated';

export type DenialReason =
    | 'INVALID_PASSWORD'
    | 'GUEST_NOT_ALLOWED'
    | 'MEETING_FULL'
    | 'MEETING_NOT_LIVE'
    | 'UNKNOWN';

export type JoinState =
    | { phase: 'IDLE' }
    | { phase: 'LOOKING_UP' }
    | {
          phase: 'NEEDS_PASSWORD';
          meetingId: string;
          title: string;
          error?: string;
      }
    | { phase: 'REQUESTING'; meetingId: string; title: string }
    | {
          phase: 'WAITING_APPROVAL';
          meetingId: string;
          requestId: string;
          title: string;
      }
    | { phase: 'APPROVED'; token: string; roomName: string }
    | { phase: 'DENIED'; reason: DenialReason }
    | { phase: 'EXPIRED' }
    | { phase: 'ERROR'; message: string; retryable: boolean };

type JoinAction =
    | { type: 'LOOKUP_STARTED' }
    | { type: 'LOOKUP_NEEDS_PASSWORD'; meetingId: string; title: string }
    | { type: 'LOOKUP_READY'; meetingId: string; title: string }
    | { type: 'LOOKUP_FAILED'; message: string; retryable: boolean }
    | { type: 'REQUEST_STARTED' }
    | { type: 'REQUEST_APPROVED'; token: string; roomName: string }
    | { type: 'REQUEST_PENDING'; requestId: string }
    | { type: 'REQUEST_DENIED'; reason: DenialReason }
    | { type: 'REQUEST_FAILED'; message: string; retryable: boolean }
    | { type: 'SSE_APPROVED'; token: string; roomName: string }
    | { type: 'SSE_DENIED'; reason: DenialReason }
    | { type: 'SSE_EXPIRED' }
    | { type: 'SSE_FAILED'; message: string; retryable: boolean }
    | { type: 'RETRY' };

const INITIAL_STATE: JoinState = { phase: 'IDLE' };

export const SSE_BACKOFF_DELAYS_MS = [1000, 2000, 4000];
const DEVICE_ID_SESSION_KEY = 'join_device_id';

function getOrCreateDeviceId(): string {
    const stored = sessionStorage.getItem(DEVICE_ID_SESSION_KEY);
    if (stored) return stored;
    const id = crypto.randomUUID();
    sessionStorage.setItem(DEVICE_ID_SESSION_KEY, id);
    return id;
}

function normalizeDenialReason(code: string): DenialReason {
    switch (code) {
        case 'INVALID_PASSWORD':
            return 'INVALID_PASSWORD';
        case 'GUEST_NOT_ALLOWED':
            return 'GUEST_NOT_ALLOWED';
        case 'MEETING_FULL':
            return 'MEETING_FULL';
        case 'MEETING_NOT_LIVE':
            return 'MEETING_NOT_LIVE';
        default:
            return 'UNKNOWN';
    }
}

export function joinReducer(state: JoinState, action: JoinAction): JoinState {
    switch (action.type) {
        case 'LOOKUP_STARTED':
            return { phase: 'LOOKING_UP' };

        case 'LOOKUP_NEEDS_PASSWORD':
            return {
                phase: 'NEEDS_PASSWORD',
                meetingId: action.meetingId,
                title: action.title,
            };

        case 'LOOKUP_READY':
            return {
                phase: 'REQUESTING',
                meetingId: action.meetingId,
                title: action.title,
            };

        case 'LOOKUP_FAILED':
            return {
                phase: 'ERROR',
                message: action.message,
                retryable: action.retryable,
            };

        case 'REQUEST_STARTED': {
            if (state.phase === 'NEEDS_PASSWORD') {
                return {
                    phase: 'REQUESTING',
                    meetingId: state.meetingId,
                    title: state.title,
                };
            }
            return state;
        }

        case 'REQUEST_APPROVED':
            return {
                phase: 'APPROVED',
                token: action.token,
                roomName: action.roomName,
            };

        case 'REQUEST_PENDING': {
            if (state.phase === 'REQUESTING') {
                return {
                    phase: 'WAITING_APPROVAL',
                    meetingId: state.meetingId,
                    requestId: action.requestId,
                    title: state.title,
                };
            }
            return state;
        }

        case 'REQUEST_DENIED':
            if (
                action.reason === 'INVALID_PASSWORD'
                && state.phase === 'REQUESTING'
            ) {
                return {
                    phase: 'NEEDS_PASSWORD',
                    meetingId: state.meetingId,
                    title: state.title,
                    error: 'INVALID_PASSWORD',
                };
            }
            return { phase: 'DENIED', reason: action.reason };

        case 'REQUEST_FAILED':
            return {
                phase: 'ERROR',
                message: action.message,
                retryable: action.retryable,
            };

        case 'SSE_APPROVED':
            return {
                phase: 'APPROVED',
                token: action.token,
                roomName: action.roomName,
            };

        case 'SSE_DENIED':
            if (
                action.reason === 'INVALID_PASSWORD'
                && state.phase === 'WAITING_APPROVAL'
            ) {
                return {
                    phase: 'NEEDS_PASSWORD',
                    meetingId: state.meetingId,
                    title: state.title,
                    error: 'INVALID_PASSWORD',
                };
            }
            return { phase: 'DENIED', reason: action.reason };

        case 'SSE_EXPIRED':
            return { phase: 'EXPIRED' };

        case 'SSE_FAILED':
            return {
                phase: 'ERROR',
                message: action.message,
                retryable: action.retryable,
            };

        case 'RETRY':
            return INITIAL_STATE;

        default:
            return state;
    }
}

type UseJoinMeetingOptions = {
    mode: JoinMode;
    authenticatedDisplayName?: string;
};

type LookupAndJoinParams = {
    code: string;
    displayName: string;
    password?: string;
};

type SubmitPasswordParams = {
    displayName: string;
    password: string;
};

export type UseJoinMeetingReturn = {
    state: JoinState;
    lookupAndJoin: (params: LookupAndJoinParams) => Promise<void>;
    submitPassword: (params: SubmitPasswordParams) => Promise<void>;
    retry: () => void;
};

export function useJoinMeeting({
    mode,
    authenticatedDisplayName,
}: UseJoinMeetingOptions): UseJoinMeetingReturn {
    const t = useTranslations('joinMeeting');
    const [state, dispatch] = useReducer(joinReducer, INITIAL_STATE);
    const sseRef = useRef<EventSource | null>(null);
    const sseRequestIdRef = useRef<string | null>(null);
    const sseRetryTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

    const closeSse = useCallback(() => {
        if (sseRetryTimerRef.current !== null) {
            clearTimeout(sseRetryTimerRef.current);
            sseRetryTimerRef.current = null;
        }
        if (sseRef.current) {
            sseRef.current.close();
            sseRef.current = null;
        }
    }, []);

    const openSse = useCallback(
        (requestId: string, retryCount: number) => {
            closeSse();
            const es = new EventSource(
                `/api/v1/joinRequests/${requestId}/events`,
            );
            sseRef.current = es;

            es.addEventListener(
                'join_request_approved',
                (event: MessageEvent) => {
                    closeSse();
                    try {
                        const data = JSON.parse(event.data) as {
                            token: string;
                            roomName: string;
                        };
                        dispatch({
                            type: 'SSE_APPROVED',
                            token: data.token,
                            roomName: data.roomName,
                        });
                    } catch {
                        dispatch({
                            type: 'SSE_FAILED',
                            message: t('errors.approvalDataUnreadable'),
                            retryable: false,
                        });
                    }
                },
            );

            es.addEventListener(
                'join_request_denied',
                (event: MessageEvent) => {
                    closeSse();
                    try {
                        const data = JSON.parse(event.data) as {
                            reason?: string;
                        };
                        dispatch({
                            type: 'SSE_DENIED',
                            reason: normalizeDenialReason(data.reason ?? ''),
                        });
                    } catch {
                        dispatch({ type: 'SSE_DENIED', reason: 'UNKNOWN' });
                    }
                },
            );

            es.addEventListener('join_request_expired', () => {
                closeSse();
                dispatch({ type: 'SSE_EXPIRED' });
            });

            es.onerror = () => {
                closeSse();
                if (retryCount < SSE_BACKOFF_DELAYS_MS.length) {
                    const delay = SSE_BACKOFF_DELAYS_MS[retryCount];
                    sseRetryTimerRef.current = setTimeout(() => {
                        sseRetryTimerRef.current = null;
                        if (sseRequestIdRef.current === requestId) {
                            openSse(requestId, retryCount + 1);
                        }
                    }, delay);
                } else {
                    dispatch({
                        type: 'SSE_FAILED',
                        message: t('errors.connectionLost'),
                        retryable: true,
                    });
                }
            };
        },
        [closeSse, t],
    );

    const activeRequestId =
        state.phase === 'WAITING_APPROVAL' ? state.requestId : null;

    useEffect(() => {
        if (activeRequestId) {
            sseRequestIdRef.current = activeRequestId;
            openSse(activeRequestId, 0);
        } else {
            sseRequestIdRef.current = null;
            closeSse();
        }

        return () => {
            closeSse();
        };
    }, [activeRequestId, openSse, closeSse]);

    const submitJoinRequest = useCallback(
        async (meetingId: string, displayName: string, password?: string) => {
            dispatch({ type: 'REQUEST_STARTED' });
            const deviceId = getOrCreateDeviceId();

            try {
                const { data } = await requestJoin({
                    path: { id: meetingId },
                    body: {
                        displayName,
                        deviceId,
                        ...(password !== undefined ? { password } : {}),
                    },
                    throwOnError: true,
                });

                const status = data?.status;

                if (status === 'APPROVED' && data?.token && data?.roomName) {
                    dispatch({
                        type: 'REQUEST_APPROVED',
                        token: data.token,
                        roomName: data.roomName,
                    });
                } else if (status === 'PENDING' && data?.requestId) {
                    dispatch({
                        type: 'REQUEST_PENDING',
                        requestId: data.requestId,
                    });
                } else if (status === 'DENIED') {
                    const reason = normalizeDenialReason(
                        (data as { reason?: string }).reason ?? '',
                    );
                    dispatch({ type: 'REQUEST_DENIED', reason });
                } else {
                    dispatch({
                        type: 'REQUEST_FAILED',
                        message: t('errors.unexpectedResponse'),
                        retryable: true,
                    });
                }
            } catch (error) {
                if (error instanceof ApiFailError) {
                    const reason = normalizeDenialReason(error.code);
                    if (reason !== 'UNKNOWN') {
                        dispatch({ type: 'REQUEST_DENIED', reason });
                    } else {
                        dispatch({
                            type: 'REQUEST_FAILED',
                            message: error.message,
                            retryable: false,
                        });
                    }
                } else if (error instanceof ApiError) {
                    dispatch({
                        type: 'REQUEST_FAILED',
                        message: error.message,
                        retryable: true,
                    });
                } else {
                    dispatch({
                        type: 'REQUEST_FAILED',
                        message: t('errors.networkError'),
                        retryable: true,
                    });
                }
            }
        },
        [t],
    );

    const lookupAndJoin = useCallback(
        async ({ code, displayName, password }: LookupAndJoinParams) => {
            dispatch({ type: 'LOOKUP_STARTED' });

            try {
                const { data: meeting } = await getMeetingByShortCode({
                    query: { code },
                    throwOnError: true,
                });

                const meetingId = meeting?.id;
                if (!meetingId) {
                    dispatch({
                        type: 'LOOKUP_FAILED',
                        message: t('errors.notFound'),
                        retryable: false,
                    });
                    return;
                }

                const title = meeting?.title ?? '';
                const requiresPassword =
                    meeting?.settings?.requirePassword ?? false;

                if (requiresPassword && !password) {
                    dispatch({
                        type: 'LOOKUP_NEEDS_PASSWORD',
                        meetingId,
                        title,
                    });
                    return;
                }

                dispatch({ type: 'LOOKUP_READY', meetingId, title });

                const resolvedDisplayName =
                    mode === 'authenticated' && authenticatedDisplayName
                        ? authenticatedDisplayName
                        : displayName;

                await submitJoinRequest(
                    meetingId,
                    resolvedDisplayName,
                    password,
                );
            } catch (error) {
                if (error instanceof ApiFailError) {
                    dispatch({
                        type: 'LOOKUP_FAILED',
                        message: error.message,
                        retryable: false,
                    });
                } else if (error instanceof ApiError) {
                    dispatch({
                        type: 'LOOKUP_FAILED',
                        message: error.message,
                        retryable: true,
                    });
                } else {
                    dispatch({
                        type: 'LOOKUP_FAILED',
                        message: t('errors.serverUnreachable'),
                        retryable: true,
                    });
                }
            }
        },
        [mode, authenticatedDisplayName, submitJoinRequest, t],
    );

    const submitPassword = useCallback(
        async ({ displayName, password }: SubmitPasswordParams) => {
            if (state.phase !== 'NEEDS_PASSWORD') return;

            dispatch({ type: 'REQUEST_STARTED' });

            const meetingId = state.meetingId;
            const resolvedDisplayName =
                mode === 'authenticated' && authenticatedDisplayName
                    ? authenticatedDisplayName
                    : displayName;

            await submitJoinRequest(meetingId, resolvedDisplayName, password);
        },
        [state, mode, authenticatedDisplayName, submitJoinRequest],
    );

    const retry = useCallback(() => {
        dispatch({ type: 'RETRY' });
    }, []);

    return { state, lookupAndJoin, submitPassword, retry };
}
