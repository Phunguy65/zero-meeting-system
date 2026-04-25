'use client';

import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { AppHeader } from '@/components/shared/app-header.tsx';
import { Button } from '@/components/ui/button.tsx';
import { getMe } from '@/generated/sdk.gen.ts';
import { JoinMeetingForm } from './join-form.tsx';
import { type JoinMode, useJoinMeeting } from './use-join-meeting.ts';
import { WaitingDialog } from './waiting-dialog.tsx';

const MEETING_TOKEN_KEY = 'meeting_token';
const MEETING_ROOM_KEY = 'meeting_room_name';

type JoinMeetingContainerProps = {
    mode: JoinMode;
    initialCode?: string;
    authenticatedDisplayName?: string;
};

export function JoinMeetingContainer({
    mode,
    initialCode,
    authenticatedDisplayName,
}: JoinMeetingContainerProps) {
    const t = useTranslations('joinMeeting');
    const common = useTranslations('workspace.common');
    const locale = useLocale();
    const router = useRouter();

    const [resolvedDisplayName, setResolvedDisplayName] = useState<
        string | undefined
    >(authenticatedDisplayName);

    useEffect(() => {
        if (mode !== 'authenticated') return;
        if (resolvedDisplayName) return;

        getMe()
            .then(({ data }) => {
                const name = data?.fullName ?? data?.username;
                if (name) {
                    setResolvedDisplayName(name);
                }
            })
            .catch(() => {
                // Silently fall back — user can type their name manually
            });
    }, [mode, resolvedDisplayName]);

    const { state, lookupAndJoin, submitPassword, retry } = useJoinMeeting({
        mode,
        authenticatedDisplayName: resolvedDisplayName,
    });

    useEffect(() => {
        if (state.phase === 'APPROVED') {
            sessionStorage.setItem(MEETING_TOKEN_KEY, state.token);
            sessionStorage.setItem(MEETING_ROOM_KEY, state.roomName);
            router.push(`/${locale}/workspace/meeting-room`);
        }
    }, [state, locale, router]);

    const isWaiting = state.phase === 'WAITING_APPROVAL';
    const meetingTitle =
        state.phase === 'WAITING_APPROVAL' || state.phase === 'NEEDS_PASSWORD'
            ? state.title
            : '';

    const denialMessage = (() => {
        if (state.phase !== 'DENIED') return null;
        switch (state.reason) {
            case 'INVALID_PASSWORD':
                return null;
            case 'GUEST_NOT_ALLOWED':
                return t('errors.guestNotAllowed');
            case 'MEETING_FULL':
                return t('errors.meetingFull');
            case 'MEETING_NOT_LIVE':
                return t('errors.meetingNotLive');
            default:
                return t('errors.joinDenied');
        }
    })();

    return (
        <main className='min-h-screen bg-surface-pale text-text-dark'>
            <AppHeader
                brand={common('brand')}
                brandHref={`/${locale}/workspace`}
                helpLabel={common('help')}
                profileLabel={common('profile')}
                settingsLabel={common('settings')}
                variant='green-room'
            />

            <div className='mx-auto grid min-h-[calc(100vh-80px)] max-w-[1600px] gap-8 px-6 py-7 sm:px-8 lg:grid-cols-[1.12fr_0.82fr] lg:px-10 lg:py-8'>
                <section className='flex items-center'>
                    <div className='w-full'>
                        <JoinMeetingForm
                            initialCode={initialCode}
                            initialDisplayName={resolvedDisplayName}
                            mode={mode}
                            onSubmit={lookupAndJoin}
                            onSubmitPassword={submitPassword}
                            state={state}
                        />
                    </div>
                </section>

                <aside className='flex items-center'>
                    <div className='w-full max-w-[400px] lg:ml-auto'>
                        <p className='text-[1.7rem] leading-none text-text-secondary'>
                            {t('eyebrow')}
                        </p>
                        <h1 className='mt-3 text-5xl font-semibold leading-[0.98] tracking-tight text-text-dark xl:text-[4.2rem]'>
                            {meetingTitle || t('title')}
                        </h1>

                        {state.phase === 'ERROR' && (
                            <div className='mt-8 rounded-xl border border-error/40 bg-error-subtle p-5'>
                                <p className='text-base text-error-dark'>
                                    {state.message}
                                </p>
                                {state.retryable && (
                                    <Button
                                        className='mt-3'
                                        onClick={retry}
                                        size='sm'
                                        type='button'
                                        variant='outline'
                                    >
                                        {t('retry')}
                                    </Button>
                                )}
                            </div>
                        )}

                        {denialMessage && (
                            <div className='mt-8 rounded-xl border border-error/40 bg-error-subtle p-5'>
                                <p className='text-base text-error-dark'>
                                    {denialMessage}
                                </p>
                                <Button
                                    className='mt-3'
                                    onClick={retry}
                                    size='sm'
                                    type='button'
                                    variant='outline'
                                >
                                    {t('tryAgain')}
                                </Button>
                            </div>
                        )}

                        {state.phase === 'EXPIRED' && (
                            <div className='mt-8 rounded-xl border border-border bg-surface p-5'>
                                <p className='text-base text-text-secondary'>
                                    {t('expired')}
                                </p>
                                <Button
                                    className='mt-3'
                                    onClick={retry}
                                    size='sm'
                                    type='button'
                                >
                                    {t('startOver')}
                                </Button>
                            </div>
                        )}
                    </div>
                </aside>
            </div>

            <WaitingDialog
                meetingTitle={meetingTitle}
                onCancel={retry}
                open={isWaiting}
            />
        </main>
    );
}
