'use client';

import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';
import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';
import { ADMISSION_POLICY_WAITING_ROOM } from '@/lib/schemas/meeting.ts';
import { InviteManagementSection } from './invite-management-section.tsx';
import { useInviteManagement } from './use-invite-management.ts';

type MeetingDetailDialogProps = {
    meeting: MeetingManagementMeetingResponse | null;
    open: boolean;
    copyFeedback: boolean;
    onClose: () => void;
    onCopyLink: (shortCode: string) => Promise<void>;
    onOpenSettings: (meetingId: string) => void;
    onCancel: (meeting: MeetingManagementMeetingResponse) => void;
};

function formatFullDateTimeRange(
    startTime: string | undefined,
    endTime: string | undefined,
): string {
    if (!startTime) return '';
    const start = new Date(startTime);
    const dateStr = start.toLocaleDateString(undefined, {
        weekday: 'long',
        month: 'long',
        day: 'numeric',
        year: 'numeric',
    });
    const startTimeStr = start.toLocaleTimeString(undefined, {
        hour: 'numeric',
        minute: '2-digit',
    });
    if (!endTime) return `${dateStr} · ${startTimeStr}`;
    const end = new Date(endTime);
    const endTimeStr = end.toLocaleTimeString(undefined, {
        hour: 'numeric',
        minute: '2-digit',
    });
    return `${dateStr} · ${startTimeStr} – ${endTimeStr}`;
}

function SettingRow({
    label,
    value,
    onLabel,
    offLabel,
}: {
    label: string;
    value: string | number | boolean | undefined;
    onLabel: string;
    offLabel: string;
}) {
    if (value === undefined || value === null) return null;
    const display =
        typeof value === 'boolean'
            ? value
                ? onLabel
                : offLabel
            : String(value);
    return (
        <div className='flex items-center justify-between py-1'>
            <span className='text-sm text-[#475467]'>{label}</span>
            <span className='text-sm font-medium text-[#15191f]'>
                {display}
            </span>
        </div>
    );
}

/**
 * Full-detail dialog for an upcoming host meeting, showing metadata, settings
 * summary, and the same host actions available on the card.
 */
export function MeetingDetailDialog({
    meeting,
    open,
    copyFeedback,
    onClose,
    onCopyLink,
    onOpenSettings,
    onCancel,
}: MeetingDetailDialogProps) {
    const t = useTranslations('workspace.home');
    const locale = useLocale();
    const router = useRouter();

    const inviteManagement = useInviteManagement(meeting?.id ?? '');

    if (!meeting) return null;

    const title = meeting.title || t('untitledMeeting');
    const dateTimeRange = formatFullDateTimeRange(
        meeting.startTime,
        meeting.endTime,
    );
    const primaryActionLabel =
        meeting.status === 'LIVE' ? t('joinMeeting') : t('startMeeting');

    function handlePrimaryAction() {
        if (!meeting?.shortCode) return;
        router.push(
            `/${locale}/workspace/green-room?code=${meeting.shortCode}`,
        );
    }

    function handleCopyLink() {
        if (meeting?.shortCode) {
            void onCopyLink(meeting.shortCode);
        }
    }

    function handleSettings() {
        if (meeting?.id) {
            onOpenSettings(meeting.id);
        }
    }

    function handleCancel() {
        if (meeting) {
            onCancel(meeting);
        }
    }

    const settings = meeting.settings;
    const hasSettings =
        settings
        && (settings.admissionPolicy !== undefined
            || settings.allowGuest !== undefined
            || settings.maxParticipants !== undefined
            || settings.allowScreenShare !== undefined
            || settings.chatEnabled !== undefined
            || settings.allowMicrophone !== undefined
            || settings.allowVideo !== undefined);

    return (
        <Dialog onOpenChange={(isOpen) => !isOpen && onClose()} open={open}>
            <DialogContent className='max-h-[90vh] max-w-xl overflow-y-auto'>
                <DialogHeader>
                    <div className='flex flex-wrap items-center gap-2'>
                        <DialogTitle className='text-xl font-semibold text-[#15191f]'>
                            {title}
                        </DialogTitle>
                        {meeting.status && (
                            <Badge
                                variant={
                                    meeting.status === 'LIVE'
                                        ? 'default'
                                        : 'secondary'
                                }
                            >
                                {meeting.status}
                            </Badge>
                        )}
                        {meeting.type && (
                            <Badge variant='outline'>{meeting.type}</Badge>
                        )}
                    </div>
                </DialogHeader>

                <div className='space-y-4 pt-2'>
                    {dateTimeRange && (
                        <p className='text-sm text-[#475467]'>
                            {dateTimeRange}
                        </p>
                    )}

                    {meeting.description && (
                        <div>
                            <p className='mb-1 text-xs font-medium uppercase tracking-wide text-[#9ca3af]'>
                                {t('meetingDescription')}
                            </p>
                            <p className='text-sm text-[#344054]'>
                                {meeting.description}
                            </p>
                        </div>
                    )}

                    {meeting.shortCode && (
                        <div>
                            <p className='mb-1 text-xs font-medium uppercase tracking-wide text-[#9ca3af]'>
                                {t('meetingShortCode')}
                            </p>
                            <button
                                className='font-mono text-sm text-[#1a73e8] hover:underline'
                                onClick={handleCopyLink}
                                type='button'
                            >
                                {meeting.shortCode}
                            </button>
                        </div>
                    )}

                    {hasSettings && (
                        <div>
                            <p className='mb-2 text-xs font-medium uppercase tracking-wide text-[#9ca3af]'>
                                {t('settingsSection')}
                            </p>
                            <div className='divide-y divide-[#f3f4f6] rounded-xl border border-[#e4e9f2] px-4'>
                                <SettingRow
                                    label={t('settingWaitingRoom')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={
                                        settings.admissionPolicy !== undefined
                                            ? settings.admissionPolicy
                                              === ADMISSION_POLICY_WAITING_ROOM
                                            : undefined
                                    }
                                />
                                <SettingRow
                                    label={t('settingAllowGuest')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.allowGuest}
                                />
                                <SettingRow
                                    label={t('settingMaxParticipants')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.maxParticipants}
                                />
                                <SettingRow
                                    label={t('settingScreenShare')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.allowScreenShare}
                                />
                                <SettingRow
                                    label={t('settingChat')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.chatEnabled}
                                />
                                <SettingRow
                                    label={t('settingMicrophone')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.allowMicrophone}
                                />
                                <SettingRow
                                    label={t('settingVideo')}
                                    offLabel={t('settingOff')}
                                    onLabel={t('settingOn')}
                                    value={settings.allowVideo}
                                />
                            </div>
                        </div>
                    )}

                    {meeting.id && (
                        <InviteManagementSection
                            addState={inviteManagement.addState}
                            listState={inviteManagement.listState}
                            onAddInvitee={inviteManagement.handleAddInvitee}
                            onResend={inviteManagement.handleResend}
                            onRevoke={inviteManagement.handleRevoke}
                            rowStates={inviteManagement.rowStates}
                        />
                    )}

                    <div className='flex flex-wrap gap-2 pt-2'>
                        {meeting.shortCode && (
                            <Button
                                className='rounded-full'
                                onClick={handlePrimaryAction}
                                type='button'
                            >
                                {primaryActionLabel}
                            </Button>
                        )}

                        <Button
                            className='rounded-full'
                            disabled={!meeting.shortCode}
                            onClick={handleCopyLink}
                            type='button'
                            variant='outline'
                        >
                            {copyFeedback ? t('linkCopied') : t('copyLink')}
                        </Button>

                        {meeting.id && (
                            <Button
                                className='rounded-full'
                                onClick={handleSettings}
                                type='button'
                                variant='outline'
                            >
                                {t('meetingSettings')}
                            </Button>
                        )}

                        <Button
                            className='rounded-full'
                            onClick={handleCancel}
                            type='button'
                            variant='outline'
                        >
                            {t('cancelMeeting')}
                        </Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
}
