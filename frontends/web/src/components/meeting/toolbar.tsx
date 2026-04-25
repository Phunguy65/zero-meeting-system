'use client';

import {
    MessageSquare,
    Mic,
    MicOff,
    Monitor,
    Phone,
    Video,
    VideoOff,
} from 'lucide-react';
import Link from 'next/link';
import { useTranslations } from 'next-intl';

type MeetingToolbarProps = {
    locale: string;
    micOn: boolean;
    videoOn: boolean;
    onToggleMic: () => void;
    onToggleVideo: () => void;
    onOpenChat: () => void;
};

export function MeetingToolbar({
    locale,
    micOn,
    videoOn,
    onToggleMic,
    onToggleVideo,
    onOpenChat,
}: MeetingToolbarProps) {
    const t = useTranslations('meetingRoom');

    return (
        <footer className='shrink-0 border-t border-border bg-surface px-6 py-5'>
            <div className='flex items-center justify-center gap-3 sm:gap-5'>
                <button
                    className={`flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 transition-colors ${
                        micOn
                            ? 'text-text-secondary hover:bg-surface-input'
                            : 'text-error hover:bg-error-subtle'
                    }`}
                    onClick={onToggleMic}
                    type='button'
                >
                    <span
                        className={`flex h-11 w-11 items-center justify-center rounded-full ${
                            micOn ? 'bg-surface-input' : 'bg-error-subtle'
                        }`}
                    >
                        {micOn ? (
                            <Mic className='h-6 w-6' />
                        ) : (
                            <MicOff className='h-6 w-6' />
                        )}
                    </span>
                    <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                        {micOn ? t('controlMic') : t('controlMicOff')}
                    </span>
                </button>

                <button
                    className={`flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 transition-colors ${
                        videoOn
                            ? 'text-text-secondary hover:bg-surface-input'
                            : 'text-error hover:bg-error-subtle'
                    }`}
                    onClick={onToggleVideo}
                    type='button'
                >
                    <span
                        className={`flex h-11 w-11 items-center justify-center rounded-full ${
                            videoOn ? 'bg-surface-input' : 'bg-error-subtle'
                        }`}
                    >
                        {videoOn ? (
                            <Video className='h-6 w-6' />
                        ) : (
                            <VideoOff className='h-6 w-6' />
                        )}
                    </span>
                    <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                        {videoOn ? t('controlVideo') : t('controlVideoOff')}
                    </span>
                </button>

                <button
                    className='flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 text-text-secondary transition-colors hover:bg-surface-input'
                    type='button'
                >
                    <span className='flex h-11 w-11 items-center justify-center rounded-full bg-surface-input'>
                        <Monitor className='h-6 w-6' />
                    </span>
                    <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                        {t('controlShare')}
                    </span>
                </button>

                <button
                    className='flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 text-primary transition-colors hover:bg-primary-subtle'
                    onClick={onOpenChat}
                    type='button'
                >
                    <span className='flex h-11 w-11 items-center justify-center rounded-full bg-primary-muted'>
                        <MessageSquare className='h-6 w-6' />
                    </span>
                    <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                        {t('controlChat')}
                    </span>
                </button>

                <div className='mx-2 h-12 w-px bg-border' />

                <Link
                    className='flex h-14 items-center gap-3 rounded-full bg-error px-7 text-[1.08rem] font-semibold text-white shadow-[0_18px_40px_-20px_rgba(220,38,38,0.85)] transition-all hover:-translate-y-0.5 hover:bg-error-dark hover:shadow-[0_24px_46px_-20px_rgba(220,38,38,0.9)]'
                    href={`/${locale}/workspace`}
                >
                    <Phone className='h-5 w-5 rotate-[135deg]' />
                    {t('leave')}
                </Link>
            </div>
        </footer>
    );
}
