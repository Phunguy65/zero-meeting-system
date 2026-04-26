'use client';

import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';

type UpcomingMeetingCardProps = {
    meeting: MeetingManagementMeetingResponse;
    copyFeedback: boolean;
    onCardClick: (meeting: MeetingManagementMeetingResponse) => void;
    onCopyLink: (shortCode: string) => Promise<void>;
    onOpenSettings: (meetingId: string) => void;
    onCancel: (meeting: MeetingManagementMeetingResponse) => void;
};

function formatDateTimeRange(
    startTime: string | undefined,
    endTime: string | undefined,
): string {
    if (!startTime) return '';
    const start = new Date(startTime);
    const dateStr = start.toLocaleDateString(undefined, {
        month: 'short',
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

function StatusBadge({ status }: { status: string | undefined }) {
    if (!status) return null;
    if (status === 'LIVE') {
        return (
            <Badge className='border-transparent bg-green-100 text-green-800'>
                {status}
            </Badge>
        );
    }
    return <Badge variant='secondary'>{status}</Badge>;
}

/**
 * Renders a single upcoming host meeting card with summary metadata and
 * action buttons for start/join, copy link, settings, and cancel.
 * Card body click opens the detail dialog; nested buttons stop propagation.
 */
export function UpcomingMeetingCard({
    meeting,
    copyFeedback,
    onCardClick,
    onCopyLink,
    onOpenSettings,
    onCancel,
}: UpcomingMeetingCardProps) {
    const t = useTranslations('workspace.home');
    const locale = useLocale();
    const router = useRouter();

    const title = meeting.title || t('untitledMeeting');
    const dateTimeRange = formatDateTimeRange(
        meeting.startTime,
        meeting.endTime,
    );
    const primaryActionLabel =
        meeting.status === 'LIVE' ? t('joinMeeting') : t('startMeeting');

    function handleCardClick() {
        onCardClick(meeting);
    }

    function handlePrimaryAction(event: React.MouseEvent) {
        event.stopPropagation();
        if (!meeting.shortCode) return;
        router.push(
            `/${locale}/workspace/green-room?code=${meeting.shortCode}`,
        );
    }

    function handleCopyLink(event: React.MouseEvent) {
        event.stopPropagation();
        if (meeting.shortCode) {
            void onCopyLink(meeting.shortCode);
        }
    }

    function handleSettings(event: React.MouseEvent) {
        event.stopPropagation();
        if (meeting.id) {
            onOpenSettings(meeting.id);
        }
    }

    function handleCancel(event: React.MouseEvent) {
        event.stopPropagation();
        onCancel(meeting);
    }

    return (
        <button
            className='flex w-full cursor-pointer flex-col gap-4 rounded-[1.8rem] bg-white px-7 py-6 text-left shadow-[0_22px_60px_-38px_rgba(15,23,42,0.2)] transition-shadow hover:shadow-[0_26px_70px_-38px_rgba(15,23,42,0.28)]'
            onClick={handleCardClick}
            type='button'
        >
            <div className='flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between'>
                <div className='min-w-0 flex-1'>
                    <h3 className='text-[1.35rem] font-semibold tracking-tight text-[#15191f]'>
                        {title}
                    </h3>
                    <p className='mt-1 text-sm text-[#475467]'>
                        {dateTimeRange}
                    </p>
                </div>
                <StatusBadge status={meeting.status} />
            </div>

            {meeting.description && (
                <p className='line-clamp-2 text-sm text-[#6b7280]'>
                    {meeting.description}
                </p>
            )}

            {meeting.shortCode && (
                <p className='text-xs font-mono text-[#9ca3af]'>
                    {meeting.shortCode}
                </p>
            )}

            <div className='flex flex-wrap gap-2'>
                {meeting.shortCode && (
                    <Button
                        className='rounded-full'
                        onClick={handlePrimaryAction}
                        size='sm'
                        type='button'
                    >
                        {primaryActionLabel}
                    </Button>
                )}

                <Button
                    onClick={handleCopyLink}
                    size='sm'
                    type='button'
                    variant='outline'
                    className='rounded-full'
                    disabled={!meeting.shortCode}
                >
                    {copyFeedback ? t('linkCopied') : t('copyLink')}
                </Button>

                {meeting.id && (
                    <Button
                        className='rounded-full'
                        onClick={handleSettings}
                        size='sm'
                        type='button'
                        variant='outline'
                    >
                        {t('meetingSettings')}
                    </Button>
                )}

                <Button
                    className='rounded-full'
                    onClick={handleCancel}
                    size='sm'
                    type='button'
                    variant='outline'
                >
                    {t('cancelMeeting')}
                </Button>
            </div>
        </button>
    );
}
