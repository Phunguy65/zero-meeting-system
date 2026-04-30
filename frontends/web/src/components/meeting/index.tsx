'use client';

import {
    LiveKitRoom,
    useConnectionState,
    useLocalParticipant,
    useRemoteParticipants,
} from '@livekit/components-react';
import { ConnectionState } from 'livekit-client';
import { useLocale, useTranslations } from 'next-intl';
import { useCallback, useEffect, useState } from 'react';
import { AppHeader } from '@/components/shared/app-header.tsx';
import {
    getMeeting,
    muteAllParticipants,
    muteParticipantTrack,
    startRecording,
    stopRecording,
} from '@/generated/sdk.gen.ts';
import { useCallTimer } from '@/hooks/use-call-timer.ts';
import { useMeetingChat } from '@/hooks/use-meeting-chat.ts';
import { useMeetingLayout } from '@/hooks/use-meeting-layout.ts';
import { useWaitingRoom } from '@/hooks/use-waiting-room.ts';
import {
    MEETING_ID_KEY,
    MEETING_ROOM_KEY,
    MEETING_TOKEN_KEY,
} from '@/lib/meeting-room-handoff.ts';
import { ADMISSION_POLICY_WAITING_ROOM } from '@/lib/schemas/meeting.ts';
import { ConnectionIndicator } from './connection-indicator.tsx';
import { LeaveDialog } from './leave-dialog.tsx';
import { MeetingSettingsDialog } from './meeting-settings-dialog.tsx';
import { ParticipantGrid } from './participant-grid.tsx';
import { MeetingSidebar } from './sidebar.tsx';
import { MeetingToolbar } from './toolbar.tsx';
import type { ParticipantRole, ParticipantViewModel } from './types.ts';
import { WaitingRoomSheet } from './waiting-room-sheet.tsx';

type SessionCredentials = {
    token: string;
    roomName: string;
    meetingId: string | null;
} | null;

function consumeSessionCredentials(): SessionCredentials {
    const token = sessionStorage.getItem(MEETING_TOKEN_KEY);
    const roomName = sessionStorage.getItem(MEETING_ROOM_KEY);
    const meetingId = sessionStorage.getItem(MEETING_ID_KEY);
    sessionStorage.removeItem(MEETING_TOKEN_KEY);
    sessionStorage.removeItem(MEETING_ROOM_KEY);
    sessionStorage.removeItem(MEETING_ID_KEY);
    if (token && roomName) {
        return { token, roomName, meetingId };
    }
    return null;
}

function liveKitServerUrl(): string {
    return process.env.NEXT_PUBLIC_LIVEKIT_URL ?? 'wss://localhost:7880';
}

function mapConnectionStatus(
    state: ConnectionState,
): 'connected' | 'reconnecting' | 'disconnected' {
    if (
        state === ConnectionState.Reconnecting
        || state === ConnectionState.SignalReconnecting
    ) {
        return 'reconnecting';
    }
    if (state === ConnectionState.Connected) {
        return 'connected';
    }
    return 'disconnected';
}

type MeetingRoomContentProps = {
    meetingId: string | null;
    userId: string | null;
    hostId: string | null;
    hasWaitingRoom: boolean;
};

function MeetingRoomContent({
    meetingId,
    userId,
    hostId,
    hasWaitingRoom,
}: MeetingRoomContentProps) {
    const t = useTranslations('meetingRoom');
    const common = useTranslations('workspace.common');
    const locale = useLocale();

    const connectionState = useConnectionState();
    const { localParticipant } = useLocalParticipant();
    const remoteParticipants = useRemoteParticipants();

    const {
        mode: layoutMode,
        setMode: setLayoutMode,
        pinnedIdentity,
    } = useMeetingLayout();
    const { formattedDuration } = useCallTimer();

    const [sidebarOpen, setSidebarOpen] = useState(false);
    const [activeTab, setActiveTab] = useState<'chat' | 'people'>('chat');
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [leaveDialogOpen, setLeaveDialogOpen] = useState(false);
    const [waitingRoomOpen, setWaitingRoomOpen] = useState(false);
    const [isRecording, setIsRecording] = useState(false);

    const isHost = Boolean(userId && hostId && userId === hostId);
    const connectionStatus = mapConnectionStatus(connectionState);
    const isReconnecting = connectionStatus === 'reconnecting';

    const isChatVisible = sidebarOpen && activeTab === 'chat';

    const chat = useMeetingChat(meetingId ?? '', userId ?? '', isChatVisible);

    const waitingRoom = useWaitingRoom(
        isHost && hasWaitingRoom ? meetingId : null,
    );

    function resolveRole(identity: string): ParticipantRole {
        return identity === hostId ? 'HOST' : 'PARTICIPANT';
    }

    const sidebarParticipants: ParticipantViewModel[] = [
        {
            identity: localParticipant.identity,
            displayName: localParticipant.name ?? localParticipant.identity,
            isMicEnabled: localParticipant.isMicrophoneEnabled,
            isCameraEnabled: localParticipant.isCameraEnabled,
            isLocal: true,
            role: resolveRole(localParticipant.identity),
            livekitParticipant: localParticipant,
        },
        ...remoteParticipants.map((p) => ({
            identity: p.identity,
            displayName: p.name ?? p.identity,
            isMicEnabled: p.isMicrophoneEnabled,
            isCameraEnabled: p.isCameraEnabled,
            isLocal: false,
            role: resolveRole(p.identity),
            livekitParticipant: p,
        })),
    ];

    async function handleToggleMic() {
        await localParticipant.setMicrophoneEnabled(
            !localParticipant.isMicrophoneEnabled,
        );
    }

    async function handleToggleVideo() {
        await localParticipant.setCameraEnabled(
            !localParticipant.isCameraEnabled,
        );
    }

    const handleToggleRecording = useCallback(async () => {
        if (!meetingId) return;
        if (isRecording) {
            await stopRecording({ path: { id: meetingId } });
            setIsRecording(false);
        } else {
            await startRecording({ path: { id: meetingId } });
            setIsRecording(true);
        }
    }, [meetingId, isRecording]);

    const handleMuteAll = useCallback(async () => {
        if (!meetingId) return;
        await muteAllParticipants({ path: { id: meetingId } });
    }, [meetingId]);

    const handleMuteMic = useCallback(
        async (identity: string) => {
            if (!meetingId) return;
            await muteParticipantTrack({
                path: { id: meetingId, identity },
                query: { source: 'microphone' },
            });
        },
        [meetingId],
    );

    const handleMuteCamera = useCallback(
        async (identity: string) => {
            if (!meetingId) return;
            await muteParticipantTrack({
                path: { id: meetingId, identity },
                query: { source: 'camera' },
            });
        },
        [meetingId],
    );

    const handleSend = useCallback(
        (content: string) => {
            const senderName =
                localParticipant.name ?? localParticipant.identity;
            void chat.send(content, senderName);
        },
        [chat, localParticipant],
    );

    function handleOpenChat() {
        setSidebarOpen(true);
        setActiveTab('chat');
    }

    const canOpenSettings = Boolean(meetingId);

    const headerActions = (
        <div className='flex items-center gap-3'>
            <ConnectionIndicator
                connectedLabel={t('statusConnected')}
                disconnectedLabel={t('statusDisconnected')}
                reconnectingLabel={t('statusReconnecting')}
                status={connectionStatus}
            />
            <span className='tabular-nums text-sm font-medium text-text-secondary'>
                {formattedDuration}
            </span>
        </div>
    );

    return (
        <main className='relative flex h-screen flex-col overflow-hidden bg-meeting-bg'>
            <AppHeader
                actions={headerActions}
                brand={common('brand')}
                brandHref={`/${locale}/workspace`}
                helpLabel={common('help')}
                meetingName={t('meetingName')}
                profileLabel={common('profile')}
                settingsLabel={common('settings')}
                variant='meeting'
            />

            {isReconnecting && (
                <div
                    aria-live='polite'
                    className='shrink-0 bg-yellow-500/10 px-6 py-2 text-center text-sm font-medium text-yellow-600'
                    role='alert'
                >
                    {t('statusReconnecting')}
                </div>
            )}

            <div className='flex flex-1 overflow-hidden'>
                <ParticipantGrid
                    layoutMode={layoutMode}
                    pinnedIdentity={pinnedIdentity}
                />
                <MeetingSidebar
                    error={chat.error}
                    isHost={isHost}
                    isOpen={sidebarOpen}
                    loading={chat.loading}
                    meetingId={meetingId}
                    messages={chat.messages}
                    onClose={() => setSidebarOpen(false)}
                    onLoadHistory={chat.loadHistory}
                    onMuteAll={handleMuteAll}
                    onMuteCamera={handleMuteCamera}
                    onMuteMic={handleMuteMic}
                    onRetry={chat.loadHistory}
                    onSendMessage={handleSend}
                    participants={sidebarParticipants}
                    sendError={chat.sendError}
                    userId={userId ?? ''}
                />
            </div>

            <MeetingToolbar
                canOpenSettings={canOpenSettings}
                currentLayout={layoutMode}
                hasWaitingRoom={hasWaitingRoom}
                isHost={isHost}
                isRecording={isRecording}
                onLayoutChange={setLayoutMode}
                onOpenChat={handleOpenChat}
                onOpenSettings={
                    canOpenSettings ? () => setSettingsOpen(true) : undefined
                }
                onOpenWaitingRoom={() => setWaitingRoomOpen(true)}
                onRequestLeave={() => setLeaveDialogOpen(true)}
                onToggleMic={() => void handleToggleMic()}
                onToggleRecording={() => void handleToggleRecording()}
                onToggleVideo={() => void handleToggleVideo()}
                pendingWaitingCount={waitingRoom.pendingCount}
                unreadCount={chat.unreadCount}
            />

            <LeaveDialog
                isHost={isHost}
                meetingId={meetingId}
                onOpenChange={setLeaveDialogOpen}
                open={leaveDialogOpen}
            />

            <MeetingSettingsDialog
                meetingId={meetingId}
                onClose={() => setSettingsOpen(false)}
                open={settingsOpen}
            />

            {isHost && hasWaitingRoom && (
                <WaitingRoomSheet
                    error={waitingRoom.error}
                    isLoading={waitingRoom.isLoading}
                    onApprove={waitingRoom.approve}
                    onApproveAll={waitingRoom.approveAll}
                    onDeny={waitingRoom.deny}
                    onOpenChange={setWaitingRoomOpen}
                    onRefresh={waitingRoom.refresh}
                    open={waitingRoomOpen}
                    pendingCount={waitingRoom.pendingCount}
                    requests={waitingRoom.requests}
                />
            )}
        </main>
    );
}

type MeetingBootstrapProps = {
    credentials: NonNullable<SessionCredentials>;
};

function MeetingBootstrap({ credentials }: MeetingBootstrapProps) {
    const [userId, setUserId] = useState<string | null>(null);
    const [hostId, setHostId] = useState<string | null>(null);
    const [hasWaitingRoom, setHasWaitingRoom] = useState(false);

    useEffect(() => {
        if (!credentials.meetingId) return;

        Promise.all([
            import('@/generated/sdk.gen.ts').then((sdk) =>
                sdk.getMe({}).then(({ data }) => data?.id ?? null),
            ),
            getMeeting({ path: { id: credentials.meetingId } }).then(
                ({ data }) => ({
                    hostId: data?.hostId ?? null,
                    hasWaitingRoom:
                        data?.settings?.admissionPolicy
                        === ADMISSION_POLICY_WAITING_ROOM,
                }),
            ),
        ])
            .then(([resolvedUserId, meetingInfo]) => {
                setUserId(resolvedUserId);
                setHostId(meetingInfo.hostId);
                setHasWaitingRoom(meetingInfo.hasWaitingRoom);
            })
            .catch(() => {
                setUserId(null);
                setHostId(null);
                setHasWaitingRoom(false);
            });
    }, [credentials.meetingId]);

    return (
        <LiveKitRoom
            audio
            data-lk-theme='default'
            serverUrl={liveKitServerUrl()}
            token={credentials.token}
            video
        >
            <MeetingRoomContent
                hasWaitingRoom={hasWaitingRoom}
                hostId={hostId}
                meetingId={credentials.meetingId}
                userId={userId}
            />
        </LiveKitRoom>
    );
}

export function MeetingContainer() {
    const locale = useLocale();
    const t = useTranslations('meetingRoom');

    const [credentials] = useState<SessionCredentials>(() =>
        consumeSessionCredentials(),
    );

    if (!credentials) {
        return (
            <main className='flex h-screen flex-col items-center justify-center gap-4 bg-meeting-bg text-white'>
                <p className='text-lg'>{t('noActiveSession')}</p>
                <a
                    className='underline opacity-70 hover:opacity-100'
                    href={`/${locale}/workspace`}
                >
                    {t('backToWorkspace')}
                </a>
            </main>
        );
    }

    return <MeetingBootstrap credentials={credentials} />;
}
