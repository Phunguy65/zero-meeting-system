'use client';

import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { NewMeetingDropdown } from '@/components/create-meeting/new-meeting-dropdown.tsx';
import { MeetingSettingsDialog } from '@/components/meeting/meeting-settings-dialog.tsx';
import { WorkspaceShell } from '@/components/workspace-shell.tsx';
import { listHostMeetings } from '@/generated/sdk.gen.ts';
import type { MeetingManagementMeetingResponse } from '@/generated/types.gen.ts';

function NewMeetingIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M14 7a2 2 0 0 1 2 2v1.56l3.2-2.4A1 1 0 0 1 21 8.96v6.08a1 1 0 0 1-1.8.8L16 13.44V15a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h9Z' />
            <path d='M11 9h2v6h-2zM8 12h8v2H8z' fill='#fff' />
        </svg>
    );
}

function JoinIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M6 3h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2Zm5 4h2v10h-2V7Zm-4 4h10v2H7v-2Z' />
        </svg>
    );
}

function CalendarIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='none'
            stroke='currentColor'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <rect x='4' y='5' width='16' height='15' rx='2' />
            <path d='M8 3v4M16 3v4M4 10h16' />
        </svg>
    );
}

function ArrowIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-8 w-8'
            fill='none'
            stroke='currentColor'
            strokeLinecap='round'
            strokeWidth='2.2'
            viewBox='0 0 24 24'
        >
            <path d='M5 12h14M13 5l7 7-7 7' />
        </svg>
    );
}

function SettingsIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-4 w-4'
            fill='none'
            stroke='currentColor'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <path d='M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z' />
            <path d='M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1Z' />
        </svg>
    );
}

type HostMeetingListState =
    | { phase: 'LOADING' }
    | { phase: 'SUCCESS'; meetings: MeetingManagementMeetingResponse[] }
    | { phase: 'EMPTY' }
    | { phase: 'ERROR' };

function formatMeetingStartTime(startTime: string | undefined): string {
    if (!startTime) return '';
    const date = new Date(startTime);
    return date.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
    });
}

export function WorkspaceHomeScreen() {
    const t = useTranslations('workspace.home');
    const common = useTranslations('workspace.common');
    const locale = useLocale();
    const [meetingCode, setMeetingCode] = useState('');
    const [hostMeetings, setHostMeetings] = useState<HostMeetingListState>({
        phase: 'LOADING',
    });
    const [settingsMeetingId, setSettingsMeetingId] = useState<string | null>(
        null,
    );

    useEffect(() => {
        listHostMeetings()
            .then(({ data }) => {
                const meetings = data?.content ?? [];
                if (meetings.length === 0) {
                    setHostMeetings({ phase: 'EMPTY' });
                } else {
                    setHostMeetings({ phase: 'SUCCESS', meetings });
                }
            })
            .catch(() => {
                setHostMeetings({ phase: 'ERROR' });
            });
    }, []);

    return (
        <WorkspaceShell activeTab='home'>
            <section>
                <div className='max-w-[980px]'>
                    <h1 className='text-6xl font-semibold leading-[0.98] tracking-tight text-[#15191f] sm:text-7xl lg:text-[5.4rem]'>
                        {t('headline')}
                    </h1>
                    <p className='mt-6 max-w-[900px] text-2xl leading-[1.45] text-[#344054] sm:text-[1.18rem] sm:leading-10'>
                        {t('description')}
                    </p>
                </div>

                <div className='mt-14 grid gap-6 xl:grid-cols-3'>
                    <article className='flex min-h-[370px] flex-col rounded-[2rem] bg-white p-10 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.22)]'>
                        <span className='flex h-18 w-18 items-center justify-center rounded-full bg-[#1a73e8] text-white shadow-[0_18px_40px_-24px_rgba(26,115,232,0.9)]'>
                            <NewMeetingIcon />
                        </span>
                        <h2 className='mt-10 text-[2.15rem] font-semibold tracking-tight text-[#15191f]'>
                            {t('cards.newMeeting.title')}
                        </h2>
                        <p className='mt-4 text-xl leading-8 text-[#475467] sm:text-[1.1rem]'>
                            {t('cards.newMeeting.description')}
                        </p>
                        <NewMeetingDropdown>
                            <button
                                aria-label={t('cards.newMeeting.title')}
                                className='mt-auto self-end text-[#1a73e8] transition-transform hover:translate-x-1'
                                type='button'
                            >
                                <ArrowIcon />
                            </button>
                        </NewMeetingDropdown>
                    </article>

                    <article className='flex min-h-[370px] flex-col rounded-[2rem] bg-[#f3f4f6] p-10 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.18)]'>
                        <span className='flex h-18 w-18 items-center justify-center rounded-full bg-[#dde1e7] text-[#36538a]'>
                            <JoinIcon />
                        </span>
                        <h2 className='mt-10 text-[2.15rem] font-semibold tracking-tight text-[#15191f]'>
                            {t('cards.joinMeeting.title')}
                        </h2>
                        <p className='mt-4 text-xl leading-8 text-[#475467] sm:text-[1.1rem]'>
                            {t('cards.joinMeeting.description')}
                        </p>
                        <div className='mt-auto flex flex-col gap-4 sm:flex-row'>
                            <input
                                className='h-14 flex-1 rounded-full bg-white px-5 text-lg text-[#111827] outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-[#1a73e8]'
                                onChange={(event) =>
                                    setMeetingCode(event.target.value)
                                }
                                placeholder={t('cards.joinMeeting.placeholder')}
                                type='text'
                                value={meetingCode}
                            />
                            <Link
                                aria-disabled={!meetingCode.trim()}
                                tabIndex={meetingCode.trim() ? 0 : -1}
                                className={`flex h-14 items-center justify-center rounded-full px-8 text-xl font-semibold transition-all ${
                                    meetingCode.trim()
                                        ? 'bg-[#1a73e8] text-white shadow-[0_18px_34px_-22px_rgba(26,115,232,0.95)] hover:bg-[#1765cc]'
                                        : 'pointer-events-none bg-[#d6dbe4] text-[#8a94a6]'
                                }`}
                                href={`/${locale}/workspace/green-room?code=${meetingCode.trim()}`}
                            >
                                {common('join')}
                            </Link>
                        </div>
                    </article>

                    <Link
                        className='flex min-h-[370px] flex-col rounded-[2rem] bg-white p-10 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.22)]'
                        href={`/${locale}/workspace/schedule`}
                    >
                        <span className='flex h-18 w-18 items-center justify-center rounded-full bg-[#cddcff] text-[#38518f]'>
                            <CalendarIcon />
                        </span>
                        <h2 className='mt-10 text-[2.15rem] font-semibold tracking-tight text-[#15191f]'>
                            {t('cards.schedule.title')}
                        </h2>
                        <p className='mt-4 text-xl leading-8 text-[#475467] sm:text-[1.1rem]'>
                            {t('cards.schedule.description')}
                        </p>
                        <span className='mt-auto self-end text-[#36538a] transition-transform hover:translate-x-1'>
                            <ArrowIcon />
                        </span>
                    </Link>
                </div>
            </section>

            <section className='mt-16'>
                <div className='flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between'>
                    <h2 className='text-4xl font-semibold tracking-tight text-[#15191f]'>
                        {t('upcomingTitle')}
                    </h2>
                    <Link
                        className='text-xl font-medium text-[#1a73e8] transition-colors hover:text-[#1765cc]'
                        href={`/${locale}/workspace/schedule`}
                    >
                        {t('viewCalendar')}
                    </Link>
                </div>

                <div className='mt-8 space-y-4'>
                    {hostMeetings.phase === 'LOADING' && (
                        <p className='text-lg text-[#475467]'>
                            {t('meetingsLoading')}
                        </p>
                    )}

                    {hostMeetings.phase === 'EMPTY' && (
                        <p className='text-lg text-[#475467]'>
                            {t('meetingsEmpty')}
                        </p>
                    )}

                    {hostMeetings.phase === 'ERROR' && (
                        <p className='text-lg text-[#d93025]'>
                            {t('meetingsError')}
                        </p>
                    )}

                    {hostMeetings.phase === 'SUCCESS'
                        && hostMeetings.meetings.map((meeting) => (
                            <article
                                className='flex flex-col gap-6 rounded-[1.8rem] bg-white px-7 py-7 shadow-[0_22px_60px_-38px_rgba(15,23,42,0.2)] sm:flex-row sm:items-center sm:gap-8'
                                key={meeting.id}
                            >
                                <div className='min-w-0 flex-1'>
                                    <h3 className='text-[2rem] font-semibold tracking-tight text-[#15191f]'>
                                        {meeting.title ?? ''}
                                    </h3>
                                    <p className='mt-2 text-xl text-[#475467] sm:text-[1.08rem]'>
                                        {formatMeetingStartTime(
                                            meeting.startTime,
                                        )}{' '}
                                        {meeting.status && (
                                            <span className='ml-2 rounded-full bg-[#e8f0fe] px-3 py-0.5 text-sm font-medium text-[#1a73e8]'>
                                                {meeting.status}
                                            </span>
                                        )}
                                    </p>
                                </div>

                                {meeting.id && (
                                    <button
                                        aria-label={t('meetingSettings')}
                                        className='flex items-center gap-2 rounded-xl border border-[#e4e9f2] bg-white px-4 py-2 text-sm font-medium text-[#475467] transition-colors hover:bg-[#f3f4f6]'
                                        onClick={() =>
                                            setSettingsMeetingId(
                                                meeting.id ?? null,
                                            )
                                        }
                                        type='button'
                                    >
                                        <SettingsIcon />
                                        {t('meetingSettings')}
                                    </button>
                                )}
                            </article>
                        ))}
                </div>

                <NewMeetingDropdown>
                    <button
                        aria-label={t('createMeeting')}
                        className='fixed bottom-8 right-8 inline-flex h-20 w-20 items-center justify-center rounded-full bg-[#1a73e8] text-white shadow-[0_26px_60px_-22px_rgba(26,115,232,0.9)] transition-transform hover:-translate-y-1'
                        type='button'
                    >
                        <svg
                            aria-hidden='true'
                            className='h-9 w-9'
                            fill='none'
                            stroke='currentColor'
                            strokeLinecap='round'
                            strokeWidth='2.5'
                            viewBox='0 0 24 24'
                        >
                            <path d='M12 5v14M5 12h14' />
                        </svg>
                    </button>
                </NewMeetingDropdown>
            </section>

            <MeetingSettingsDialog
                meetingId={settingsMeetingId}
                onClose={() => setSettingsMeetingId(null)}
                open={Boolean(settingsMeetingId)}
            />
        </WorkspaceShell>
    );
}
