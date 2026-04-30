'use client';

import {
    Camera,
    CameraOff,
    Loader2,
    MessageSquare,
    Mic,
    MicOff,
    Users,
    X,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/utils.ts';
import type { ChatMessage } from '@/types/chat.ts';
import { MeetingChat } from './chat.tsx';
import type { ParticipantViewModel } from './types.ts';

type SidebarTab = 'chat' | 'people';

type MuteAllState = 'idle' | 'loading' | 'success';

type MeetingSidebarProps = {
    participants: ParticipantViewModel[];
    messages: ChatMessage[];
    loading: boolean;
    error: boolean;
    sendError: boolean;
    isOpen: boolean;
    isHost: boolean;
    meetingId: string | null;
    userId: string;
    onClose: () => void;
    onSendMessage: (content: string) => void;
    onRetry: () => void;
    onLoadHistory: () => void;
    onMuteAll: () => Promise<void>;
    onMuteMic: (identity: string) => Promise<void>;
    onMuteCamera: (identity: string) => Promise<void>;
};

type LoadingTrack = 'mic' | 'camera';

type ParticipantRowProps = {
    participant: ParticipantViewModel;
    isHost: boolean;
    onMuteMic: (identity: string) => Promise<void>;
    onMuteCamera: (identity: string) => Promise<void>;
    onError: (message: string) => void;
};

function participantInitials(displayName: string): string {
    return displayName
        .split(' ')
        .map((part) => part[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();
}

function isModerableRow(
    participant: ParticipantViewModel,
    isHost: boolean,
): boolean {
    return isHost && !participant.isLocal && participant.role !== 'HOST';
}

function ParticipantRow({
    participant,
    isHost,
    onMuteMic,
    onMuteCamera,
    onError,
}: ParticipantRowProps) {
    const t = useTranslations('meetingRoom');
    const [loadingTrack, setLoadingTrack] = useState<LoadingTrack | null>(null);
    const moderable = isModerableRow(participant, isHost);

    async function handleMuteMic() {
        setLoadingTrack('mic');
        try {
            await onMuteMic(participant.identity);
        } catch {
            onError(t('moderationMuteError'));
        } finally {
            setLoadingTrack(null);
        }
    }

    async function handleMuteCamera() {
        setLoadingTrack('camera');
        try {
            await onMuteCamera(participant.identity);
        } catch {
            onError(t('moderationMuteError'));
        } finally {
            setLoadingTrack(null);
        }
    }

    return (
        <div className='flex items-center gap-4 rounded-[1.2rem] bg-surface-person-item px-5 py-4'>
            <div className='flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[var(--avatar-gradient-navy-start)] to-[var(--avatar-gradient-navy-end)] text-sm font-semibold text-white shadow-sm'>
                {participantInitials(participant.displayName)}
            </div>
            <div className='min-w-0 flex-1'>
                <p className='truncate text-[1.05rem] font-semibold text-text-dark'>
                    {participant.displayName}
                </p>
                {participant.isLocal && (
                    <p className='text-[0.85rem] text-primary'>{t('you')}</p>
                )}
                {participant.role === 'HOST' && !participant.isLocal && (
                    <p className='text-[0.85rem] text-text-muted'>
                        {t('host')}
                    </p>
                )}
            </div>

            {moderable ? (
                <div className='flex items-center gap-1'>
                    <button
                        aria-label={t('moderationMuteMic', {
                            name: participant.displayName,
                        })}
                        className='flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition-colors hover:bg-surface-input hover:text-text-dark disabled:cursor-not-allowed disabled:opacity-50'
                        disabled={loadingTrack !== null}
                        onClick={() => void handleMuteMic()}
                        title={t('moderationMuteMicTooltip')}
                        type='button'
                    >
                        {loadingTrack === 'mic' ? (
                            <Loader2 className='h-4 w-4 animate-spin' />
                        ) : participant.isMicEnabled ? (
                            <Mic className='h-4 w-4' />
                        ) : (
                            <MicOff className='h-4 w-4' />
                        )}
                    </button>
                    <button
                        aria-label={t('moderationMuteCamera', {
                            name: participant.displayName,
                        })}
                        className='flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition-colors hover:bg-surface-input hover:text-text-dark disabled:cursor-not-allowed disabled:opacity-50'
                        disabled={loadingTrack !== null}
                        onClick={() => void handleMuteCamera()}
                        title={t('moderationMuteCameraTooltip')}
                        type='button'
                    >
                        {loadingTrack === 'camera' ? (
                            <Loader2 className='h-4 w-4 animate-spin' />
                        ) : participant.isCameraEnabled ? (
                            <Camera className='h-4 w-4' />
                        ) : (
                            <CameraOff className='h-4 w-4' />
                        )}
                    </button>
                </div>
            ) : (
                <span
                    className={
                        participant.isMicEnabled
                            ? 'text-primary'
                            : 'text-border-muted-disabled'
                    }
                >
                    {participant.isMicEnabled ? (
                        <Mic className='h-4 w-4' />
                    ) : (
                        <MicOff className='h-4 w-4' />
                    )}
                </span>
            )}
        </div>
    );
}

type MuteAllBannerProps = {
    onMuteAll: () => Promise<void>;
    onError: (message: string) => void;
};

function MuteAllBanner({ onMuteAll, onError }: MuteAllBannerProps) {
    const t = useTranslations('meetingRoom');
    const [state, setState] = useState<MuteAllState>('idle');

    async function handleMuteAll() {
        setState('loading');
        try {
            await onMuteAll();
            setState('success');
            setTimeout(() => setState('idle'), 2000);
        } catch {
            setState('idle');
            onError(t('moderationMuteAllError'));
        }
    }

    return (
        <div className='sticky top-0 z-10 shrink-0 border-b border-border bg-surface px-5 py-3'>
            <button
                className='flex w-full items-center justify-center gap-2 rounded-lg bg-surface-input px-4 py-2.5 text-sm font-medium text-text-dark transition-colors hover:bg-surface-person-item disabled:cursor-not-allowed disabled:opacity-60'
                disabled={state === 'loading'}
                onClick={() => void handleMuteAll()}
                type='button'
            >
                {state === 'loading' ? (
                    <>
                        <Loader2 className='h-4 w-4 animate-spin' />
                        {t('moderationMuteAllLoading')}
                    </>
                ) : state === 'success' ? (
                    <>
                        <MicOff className='h-4 w-4' />
                        {t('moderationMuteAllSuccess')}
                    </>
                ) : (
                    <>
                        <MicOff className='h-4 w-4' />
                        {t('moderationMuteAll')}
                    </>
                )}
            </button>
        </div>
    );
}

/**
 * Meeting sidebar with chat and people tabs. On desktop it renders inline;
 * on smaller screens it renders as a dismissible overlay drawer.
 *
 * When `isHost` is true and `meetingId` is set, the People tab exposes
 * per-participant mute controls and a sticky mute-all banner for moderable
 * rows. Non-host users see a read-only participant list.
 *
 * History load is triggered once when the chat tab becomes active for the
 * first time.
 */
export function MeetingSidebar({
    participants,
    messages,
    loading,
    error,
    sendError,
    isOpen,
    isHost,
    meetingId,
    userId,
    onClose,
    onSendMessage,
    onRetry,
    onLoadHistory,
    onMuteAll,
    onMuteMic,
    onMuteCamera,
}: MeetingSidebarProps) {
    const t = useTranslations('meetingRoom');
    const [activeTab, setActiveTab] = useState<SidebarTab>('chat');
    const [moderationError, setModerationError] = useState<string | null>(null);
    const chatHistoryTriggeredRef = useRef(false);

    const hostCanModerate = isHost && Boolean(meetingId);

    function clearError() {
        setModerationError(null);
    }

    function handleChatTabActivate() {
        setActiveTab('chat');
        if (!chatHistoryTriggeredRef.current) {
            chatHistoryTriggeredRef.current = true;
            onLoadHistory();
        }
    }

    useEffect(() => {
        if (
            isOpen
            && activeTab === 'chat'
            && !chatHistoryTriggeredRef.current
        ) {
            chatHistoryTriggeredRef.current = true;
            onLoadHistory();
        }
    }, [isOpen, activeTab, onLoadHistory]);

    return (
        <>
            {isOpen && (
                <div
                    aria-hidden='true'
                    className='fixed inset-0 z-30 bg-black/50 md:hidden'
                    onClick={onClose}
                />
            )}

            <aside
                className={cn(
                    'flex flex-col border-l border-border bg-surface transition-all duration-300',
                    'fixed inset-y-0 right-0 z-40 w-[300px] md:relative md:z-auto md:inset-auto xl:w-[340px]',
                    isOpen
                        ? 'translate-x-0'
                        : 'translate-x-full md:hidden md:translate-x-0',
                )}
            >
                <div className='shrink-0 border-b border-border px-6 py-5'>
                    <div className='flex items-center justify-between'>
                        <h2 className='text-[1.5rem] font-semibold tracking-tight text-text-dark'>
                            {t('meetingDetails')}
                        </h2>
                        <button
                            aria-label={t('closeSidebar')}
                            className='flex h-8 w-8 items-center justify-center rounded-full text-text-muted transition-colors hover:bg-surface-input hover:text-text-dark md:hidden'
                            onClick={onClose}
                            type='button'
                        >
                            <X className='h-4 w-4' />
                        </button>
                    </div>
                    <p className='mt-1 text-[0.95rem] text-text-muted'>
                        {t('activeSession', { count: participants.length })}
                    </p>
                </div>

                <div className='flex shrink-0 border-b border-border'>
                    <button
                        className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                            activeTab === 'chat'
                                ? 'border-primary text-primary'
                                : 'border-transparent text-text-subtle hover:text-text-secondary'
                        }`}
                        onClick={handleChatTabActivate}
                        type='button'
                    >
                        <MessageSquare className='h-6 w-6' />
                        {t('tabChat')}
                    </button>
                    <button
                        className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                            activeTab === 'people'
                                ? 'border-primary text-primary'
                                : 'border-transparent text-text-subtle hover:text-text-secondary'
                        }`}
                        onClick={() => {
                            setActiveTab('people');
                            clearError();
                        }}
                        type='button'
                    >
                        <Users className='h-6 w-6' />
                        {t('tabPeople')}
                    </button>
                </div>

                {activeTab === 'chat' ? (
                    <MeetingChat
                        error={error}
                        loading={loading}
                        messages={messages}
                        onRetry={onRetry}
                        onSend={onSendMessage}
                        sendError={sendError}
                        userId={userId}
                    />
                ) : (
                    <div className='flex flex-1 flex-col overflow-hidden'>
                        {hostCanModerate && (
                            <MuteAllBanner
                                onError={setModerationError}
                                onMuteAll={onMuteAll}
                            />
                        )}

                        {moderationError && (
                            <div
                                aria-live='polite'
                                className='shrink-0 px-5 py-2 text-sm text-error'
                                role='alert'
                            >
                                {moderationError}
                            </div>
                        )}

                        <div className='flex-1 space-y-3 overflow-y-auto p-5'>
                            {participants.map((p) => (
                                <ParticipantRow
                                    isHost={isHost}
                                    key={p.identity}
                                    onError={setModerationError}
                                    onMuteCamera={onMuteCamera}
                                    onMuteMic={onMuteMic}
                                    participant={p}
                                />
                            ))}
                        </div>
                    </div>
                )}
            </aside>
        </>
    );
}
