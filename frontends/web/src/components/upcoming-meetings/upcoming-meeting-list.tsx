'use client';

import { useTranslations } from 'next-intl';
import { MeetingSettingsDialog } from '@/components/meeting/meeting-settings-dialog.tsx';
import { Button } from '@/components/ui/button.tsx';
import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';
import { CancelMeetingDialog } from './cancel-meeting-dialog.tsx';
import { MeetingDetailDialog } from './meeting-detail-dialog.tsx';
import { UpcomingMeetingCard } from './upcoming-meeting-card.tsx';
import { useUpcomingMeetings } from './use-upcoming-meetings.ts';

/**
 * Renders the upcoming host meetings list with loading, empty, error, and
 * success states, and wires all meeting-level dialogs through the shared hook.
 */
export function UpcomingMeetingList() {
    const t = useTranslations('workspace.home');
    const {
        listState,
        selectedMeeting,
        cancelTarget,
        isCancelling,
        cancelError,
        cancelFeedback,
        settingsMeetingId,
        copyFeedback,
        actions,
    } = useUpcomingMeetings();

    return (
        <>
            <div className='mt-8 space-y-4'>
                {listState.phase === 'LOADING' && (
                    <p className='text-lg text-[#475467]'>
                        {t('meetingsLoading')}
                    </p>
                )}

                {listState.phase === 'EMPTY' && (
                    <p className='text-lg text-[#475467]'>
                        {t('meetingsEmpty')}
                    </p>
                )}

                {listState.phase === 'ERROR' && (
                    <div className='space-y-2'>
                        <p className='text-lg text-[#d93025]'>
                            {t('meetingsError')}
                        </p>
                        <Button
                            onClick={actions.retry}
                            type='button'
                            variant='outline'
                        >
                            {t('retryLoadMeetings')}
                        </Button>
                    </div>
                )}

                {cancelFeedback && (
                    <p className='text-sm text-[#475467]'>
                        {t('cancelSuccess')}
                    </p>
                )}

                {listState.phase === 'SUCCESS'
                    && listState.meetings.map(
                        (meeting: MeetingManagementMeetingResponse) => (
                            <UpcomingMeetingCard
                                copyFeedback={copyFeedback}
                                key={meeting.id}
                                meeting={meeting}
                                onCancel={actions.requestCancel}
                                onCardClick={actions.selectMeeting}
                                onCopyLink={actions.copyShortCode}
                                onOpenSettings={actions.openSettings}
                            />
                        ),
                    )}
            </div>

            <MeetingDetailDialog
                copyFeedback={copyFeedback}
                meeting={selectedMeeting}
                onCancel={actions.requestCancel}
                onClose={actions.clearSelectedMeeting}
                onCopyLink={actions.copyShortCode}
                onOpenSettings={actions.openSettings}
                open={Boolean(selectedMeeting)}
            />

            <CancelMeetingDialog
                cancelError={cancelError}
                isCancelling={isCancelling}
                meeting={cancelTarget}
                onClose={actions.dismissCancel}
                onConfirm={actions.confirmCancel}
                open={Boolean(cancelTarget)}
            />

            <MeetingSettingsDialog
                meetingId={settingsMeetingId}
                onClose={actions.closeSettings}
                open={Boolean(settingsMeetingId)}
            />
        </>
    );
}
