'use client';

import { Mic, MicOff, Volume2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import type { MeetingParticipant } from '@/lib/mock-data/meeting.ts';

type ParticipantGridProps = {
    participants: MeetingParticipant[];
};

export function ParticipantGrid({ participants }: ParticipantGridProps) {
    const t = useTranslations('meetingRoom');

    return (
        <section className='flex-1 overflow-hidden p-4'>
            <div className='grid h-full grid-cols-2 grid-rows-2 gap-3'>
                {participants.map((p) => (
                    <div
                        className={`relative overflow-hidden rounded-[1.4rem] ${p.tileBg} ${
                            p.isSpeaking
                                ? 'ring-[3px] ring-primary ring-offset-2 ring-offset-meeting-bg'
                                : ''
                        }`}
                        key={p.id}
                    >
                        <div className='absolute inset-0 flex items-center justify-center'>
                            <div
                                className={`flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br ${p.avatarGradient} text-2xl font-semibold text-white shadow-[0_14px_32px_-18px_rgba(0,0,0,0.6)] md:h-[7rem] md:w-[7rem] md:text-3xl`}
                            >
                                {p.initials}
                            </div>
                        </div>

                        {p.isSpeaking && (
                            <div className='absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full bg-primary shadow-[0_8px_20px_-8px_rgba(26,115,232,0.8)]'>
                                <Volume2 className='h-4 w-4 text-white' />
                            </div>
                        )}

                        <div className='absolute bottom-4 left-4 flex items-center gap-2 rounded-xl bg-black/55 px-3 py-1.5 backdrop-blur-sm'>
                            <span
                                className={
                                    p.micOn ? 'text-white/90' : 'text-red-400'
                                }
                            >
                                {p.micOn ? (
                                    <Mic className='h-4 w-4' />
                                ) : (
                                    <MicOff className='h-4 w-4' />
                                )}
                            </span>
                            <span className='text-sm font-medium text-white'>
                                {p.name}
                                {p.isHost ? ` (${t('host')})` : ''}
                            </span>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}
