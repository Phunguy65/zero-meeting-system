'use client';

import { useLocalParticipant } from '@livekit/components-react';
import {
    Circle,
    Loader2,
    Mic,
    MicOff,
    MoreHorizontal,
    Phone,
    Settings,
    Users,
    Video,
    VideoOff,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Button } from '@/components/ui/button.tsx';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu.tsx';
import {
    Tooltip,
    TooltipContent,
    TooltipProvider,
    TooltipTrigger,
} from '@/components/ui/tooltip.tsx';
import type { MeetingLayoutMode } from '@/hooks/use-meeting-layout.ts';
import type { RecordingState } from '@/hooks/use-recording-state.ts';
import { LayoutPicker } from './layout-picker.tsx';

type MeetingToolbarProps = {
    isHost: boolean;
    recordingState: RecordingState;
    hasWaitingRoom: boolean;
    pendingWaitingCount: number;
    unreadCount: number;
    currentLayout: MeetingLayoutMode;
    canOpenSettings: boolean;
    onToggleMic: () => void;
    onToggleVideo: () => void;
    onOpenSettings?: () => void;
    onOpenChat: () => void;
    onOpenWaitingRoom: () => void;
    onStartRecording: () => void;
    onStopRecording: () => void;
    onLayoutChange: (mode: MeetingLayoutMode) => void;
    onRequestLeave: () => void;
};

type ToolbarButtonProps = {
    label: string;
    onClick?: () => void;
    active?: boolean;
    destructive?: boolean;
    disabled?: boolean;
    children: React.ReactNode;
    className?: string;
};

function ToolbarIconButton({
    label,
    onClick,
    active = false,
    disabled = false,
    children,
    className,
}: ToolbarButtonProps) {
    return (
        <Tooltip>
            <TooltipTrigger asChild>
                <button
                    aria-label={label}
                    className={
                        className
                        ?? `flex h-11 w-11 items-center justify-center rounded-full transition-colors ${
                            active
                                ? 'bg-error-subtle text-error hover:bg-error-subtle/80'
                                : 'bg-surface-input text-text-secondary hover:bg-surface-input/80 hover:text-primary'
                        } disabled:cursor-not-allowed disabled:opacity-60`
                    }
                    disabled={disabled}
                    onClick={onClick}
                    type='button'
                >
                    {children}
                </button>
            </TooltipTrigger>
            <TooltipContent side='top'>
                <p>{label}</p>
            </TooltipContent>
        </Tooltip>
    );
}

function RecordingButton({
    recordingState,
    onStart,
    onStop,
    labelIdle,
    labelStarting,
    labelRecording,
    labelStopping,
}: {
    recordingState: RecordingState;
    onStart: () => void;
    onStop: () => void;
    labelIdle: string;
    labelStarting: string;
    labelRecording: string;
    labelStopping: string;
}) {
    const isDisabled =
        recordingState === 'starting' || recordingState === 'stopping';
    const isRecording = recordingState === 'recording';
    const isStopping = recordingState === 'stopping';

    const label = {
        idle: labelIdle,
        starting: labelStarting,
        recording: labelRecording,
        stopping: labelStopping,
    }[recordingState];

    const buttonClassName =
        isRecording || isStopping
            ? `flex h-11 w-11 items-center justify-center rounded-full transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${
                  isStopping
                      ? 'bg-error/70 text-white'
                      : 'bg-error text-white shadow-[0_0_12px_rgba(220,38,38,0.5)]'
              }`
            : 'flex h-11 w-11 items-center justify-center rounded-full bg-surface-input text-text-secondary transition-colors hover:bg-surface-input/80 hover:text-primary disabled:cursor-not-allowed disabled:opacity-60';

    function handleClick() {
        if (isRecording) {
            onStop();
        } else if (recordingState === 'idle') {
            onStart();
        }
    }

    return (
        <ToolbarIconButton
            className={buttonClassName}
            disabled={isDisabled}
            label={label}
            onClick={handleClick}
        >
            {isDisabled ? (
                <Loader2 className='h-5 w-5 animate-spin' />
            ) : isRecording ? (
                <Circle className='h-5 w-5 fill-white text-white' />
            ) : (
                <Circle className='h-5 w-5' />
            )}
        </ToolbarIconButton>
    );
}

/**
 * Floating pill toolbar centered at the bottom of the meeting room.
 * Exposes mic, video, layout, more-actions, and leave controls.
 * Host-only actions (waiting room, recording button) are conditionally rendered.
 * Shows an unread badge on the chat menu item when unreadCount > 0.
 */
export function MeetingToolbar({
    isHost,
    recordingState,
    hasWaitingRoom,
    pendingWaitingCount,
    unreadCount,
    currentLayout,
    canOpenSettings,
    onToggleMic,
    onToggleVideo,
    onOpenSettings,
    onOpenChat,
    onOpenWaitingRoom,
    onStartRecording,
    onStopRecording,
    onLayoutChange,
    onRequestLeave,
}: MeetingToolbarProps) {
    const t = useTranslations('meetingRoom');
    const { isMicrophoneEnabled, isCameraEnabled } = useLocalParticipant();

    const showHostSeparator = isHost && hasWaitingRoom;

    return (
        <TooltipProvider>
            <footer className='pointer-events-none absolute inset-x-0 bottom-6 z-20 flex justify-center'>
                <div className='pointer-events-auto flex items-center gap-2 rounded-full border border-border/60 bg-surface px-4 py-3 shadow-[0_8px_32px_-8px_rgba(0,0,0,0.25)] backdrop-blur-sm'>
                    <ToolbarIconButton
                        active={!isMicrophoneEnabled}
                        label={
                            isMicrophoneEnabled
                                ? t('controlMic')
                                : t('controlMicOff')
                        }
                        onClick={onToggleMic}
                    >
                        {isMicrophoneEnabled ? (
                            <Mic className='h-5 w-5' />
                        ) : (
                            <MicOff className='h-5 w-5' />
                        )}
                    </ToolbarIconButton>

                    <ToolbarIconButton
                        active={!isCameraEnabled}
                        label={
                            isCameraEnabled
                                ? t('controlVideo')
                                : t('controlVideoOff')
                        }
                        onClick={onToggleVideo}
                    >
                        {isCameraEnabled ? (
                            <Video className='h-5 w-5' />
                        ) : (
                            <VideoOff className='h-5 w-5' />
                        )}
                    </ToolbarIconButton>

                    <LayoutPicker
                        currentMode={currentLayout}
                        onSelect={onLayoutChange}
                    />

                    {isHost && (
                        <RecordingButton
                            labelIdle={t('controlStartRecording')}
                            labelRecording={t('controlStopRecording')}
                            labelStarting={t('recordingStarting')}
                            labelStopping={t('recordingStopping')}
                            onStart={onStartRecording}
                            onStop={onStopRecording}
                            recordingState={recordingState}
                        />
                    )}

                    <DropdownMenu>
                        <Tooltip>
                            <TooltipTrigger asChild>
                                <DropdownMenuTrigger asChild>
                                    <button
                                        aria-label={t('controlMore')}
                                        className='flex h-11 w-11 items-center justify-center rounded-full bg-surface-input text-text-secondary transition-colors hover:bg-surface-input/80 hover:text-primary'
                                        type='button'
                                    >
                                        <MoreHorizontal className='h-5 w-5' />
                                    </button>
                                </DropdownMenuTrigger>
                            </TooltipTrigger>
                            <TooltipContent side='top'>
                                <p>{t('controlMore')}</p>
                            </TooltipContent>
                        </Tooltip>
                        <DropdownMenuContent align='center' side='top'>
                            <DropdownMenuItem onClick={onOpenChat}>
                                <span className='flex items-center gap-2'>
                                    {t('controlChat')}
                                    {unreadCount > 0 && (
                                        <span className='flex h-5 min-w-5 items-center justify-center rounded-full bg-primary px-1 text-[0.7rem] font-semibold text-white'>
                                            {unreadCount}
                                        </span>
                                    )}
                                </span>
                            </DropdownMenuItem>
                            {canOpenSettings && onOpenSettings && (
                                <DropdownMenuItem onClick={onOpenSettings}>
                                    <Settings className='mr-2 h-4 w-4' />
                                    {t('controlSettings')}
                                </DropdownMenuItem>
                            )}
                            {showHostSeparator && <DropdownMenuSeparator />}
                            {isHost && hasWaitingRoom && (
                                <DropdownMenuItem onClick={onOpenWaitingRoom}>
                                    <Users className='mr-2 h-4 w-4' />
                                    {pendingWaitingCount > 0
                                        ? t('waitingRoomWithCount', {
                                              count: pendingWaitingCount,
                                          })
                                        : t('waitingRoomManage')}
                                </DropdownMenuItem>
                            )}
                        </DropdownMenuContent>
                    </DropdownMenu>

                    <div className='mx-1 h-8 w-px bg-border' />

                    <Tooltip>
                        <TooltipTrigger asChild>
                            <Button
                                className='h-11 rounded-full bg-error px-5 text-white shadow-[0_8px_24px_-8px_rgba(220,38,38,0.7)] hover:bg-error/90'
                                onClick={onRequestLeave}
                                type='button'
                                variant='destructive'
                            >
                                <Phone className='h-4 w-4 rotate-[135deg]' />
                                <span className='ml-1.5 text-sm font-medium'>
                                    {t('leave')}
                                </span>
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent side='top'>
                            <p>{t('leaveDialogTitle')}</p>
                        </TooltipContent>
                    </Tooltip>
                </div>
            </footer>
        </TooltipProvider>
    );
}
