'use client';

import { ArrowRight, Mic, MicOff, MoreVertical, Video } from 'lucide-react';
import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { useState } from 'react';
import { AppHeader } from '@/components/shared/app-header.tsx';
import { GREEN_ROOM_ATTENDEES } from '@/lib/mock-data/green-room.ts';

export function GreenRoomScreen() {
    const locale = useLocale();
    const t = useTranslations('greenRoom');
    const common = useTranslations('workspace.common');
    const [micEnabled, setMicEnabled] = useState(true);
    const [videoEnabled, setVideoEnabled] = useState(true);

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
                    <div className='relative aspect-[1.6] w-full overflow-hidden rounded-[1.7rem] bg-[linear-gradient(135deg,_#111827_0%,_#2b313b_32%,_#111827_100%)] shadow-[0_26px_70px_-38px_rgba(15,23,42,0.35)]'>
                        <div className='absolute inset-0 bg-[radial-gradient(circle_at_40%_50%,_rgba(255,213,128,0.12),_transparent_28%),linear-gradient(90deg,_rgba(0,0,0,0.52)_0%,_rgba(0,0,0,0.12)_45%,_rgba(0,0,0,0.62)_100%)]' />
                        <div className='absolute left-[43%] top-[20%] h-[46%] w-[12%] rounded-[0.9rem] border border-white/12 bg-[linear-gradient(180deg,_rgba(42,46,52,0.5),_rgba(24,28,33,0.68))] shadow-[0_20px_40px_-26px_rgba(0,0,0,0.8)]' />
                        <div className='absolute left-[47%] top-[40%] h-[10%] w-[7%] rotate-45 border-b-[6px] border-r-[6px] border-[#1f2937] opacity-80' />

                        <span className='absolute left-6 top-6 rounded-2xl bg-black/38 px-4 py-1.5 text-[0.95rem] font-medium text-white backdrop-blur'>
                            {t('preview')}
                        </span>

                        <div className='absolute bottom-6 left-1/2 flex -translate-x-1/2 items-center gap-4 rounded-[2rem] bg-white/88 px-6 py-4 shadow-[0_24px_60px_-36px_rgba(15,23,42,0.55)] backdrop-blur'>
                            <button
                                className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-text-primary transition-colors ${
                                    micEnabled
                                        ? 'bg-surface-input'
                                        : 'bg-error-subtle text-error-dark'
                                }`}
                                onClick={() => setMicEnabled((v) => !v)}
                                type='button'
                            >
                                <span className='flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm'>
                                    {micEnabled ? (
                                        <Mic className='h-7 w-7' />
                                    ) : (
                                        <MicOff className='h-7 w-7' />
                                    )}
                                </span>
                                <span className='text-[0.8rem] font-medium uppercase tracking-[0.12em]'>
                                    {t('mic')}
                                </span>
                            </button>

                            <button
                                className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-text-primary transition-colors ${
                                    videoEnabled
                                        ? 'bg-surface-input'
                                        : 'bg-error-subtle text-error-dark'
                                }`}
                                onClick={() => setVideoEnabled((v) => !v)}
                                type='button'
                            >
                                <span className='flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm'>
                                    <Video className='h-7 w-7' />
                                </span>
                                <span className='text-[0.8rem] font-medium uppercase tracking-[0.12em]'>
                                    {t('video')}
                                </span>
                            </button>

                            <button
                                className='flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl bg-surface-input px-2.5 py-2 text-text-primary transition-colors hover:bg-[#e9eef7]'
                                type='button'
                            >
                                <span className='flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm'>
                                    <MoreVertical className='h-7 w-7' />
                                </span>
                                <span className='text-[0.8rem] font-medium uppercase tracking-[0.12em]'>
                                    {t('check')}
                                </span>
                            </button>
                        </div>
                    </div>
                </section>

                <aside className='flex items-center'>
                    <div className='w-full max-w-[400px] lg:ml-auto'>
                        <p className='text-[1.7rem] leading-none text-text-secondary'>
                            {t('ready')}
                        </p>
                        <h1 className='mt-3 text-5xl font-semibold leading-[0.98] tracking-tight text-text-dark xl:text-[4.2rem]'>
                            {t('meetingTitle')}
                        </h1>

                        <p className='mt-9 text-[1.18rem] text-text-secondary'>
                            {t('alreadyInMeeting', {
                                count: GREEN_ROOM_ATTENDEES.length,
                            })}
                        </p>

                        <div className='mt-6 space-y-6'>
                            {GREEN_ROOM_ATTENDEES.map((attendee) => (
                                <div
                                    className='flex items-center gap-5'
                                    key={attendee.name}
                                >
                                    <div
                                        className={`flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br ${attendee.palette} text-sm font-semibold text-white shadow-[0_14px_28px_-18px_rgba(15,23,42,0.45)]`}
                                    >
                                        {attendee.initials}
                                    </div>
                                    <span className='text-[1.4rem] font-medium tracking-tight text-text-dark'>
                                        {attendee.name}
                                    </span>
                                </div>
                            ))}
                        </div>

                        <Link
                            className='mt-10 flex h-16 w-full items-center justify-center gap-3 rounded-[1.1rem] bg-primary px-8 text-[1.55rem] font-semibold text-white shadow-[0_24px_50px_-26px_rgba(26,115,232,0.95)] transition-all hover:-translate-y-0.5 hover:bg-primary-hover'
                            href={`/${locale}/workspace/meeting-room`}
                        >
                            {t('joinNow')}
                            <ArrowRight className='h-6 w-6' />
                        </Link>

                        <p className='mt-5 text-center text-base text-text-muted'>
                            {t('otherJoinOptions')}{' '}
                            <button
                                className='font-medium text-primary transition-colors hover:text-primary-hover'
                                type='button'
                            >
                                {t('phoneAudio')}
                            </button>
                        </p>
                    </div>
                </aside>
            </div>
        </main>
    );
}
