'use client';

import { MessageSquare, Mic, MicOff, Users } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import type { MeetingParticipant } from '@/lib/mock-data/meeting.ts';
import type { MeetingMessage } from '@/lib/mock-data/messages.ts';
import { MeetingChat } from './chat.tsx';

type SidebarTab = 'chat' | 'people';

type MeetingSidebarProps = {
    participants: MeetingParticipant[];
    messages: MeetingMessage[];
    onSendMessage: (text: string) => void;
};

export function MeetingSidebar({
    participants,
    messages,
    onSendMessage,
}: MeetingSidebarProps) {
    const t = useTranslations('meetingRoom');
    const [activeTab, setActiveTab] = useState<SidebarTab>('chat');

    return (
        <aside className='flex w-[300px] shrink-0 flex-col border-l border-border bg-surface xl:w-[340px]'>
            <div className='shrink-0 border-b border-border px-6 py-5'>
                <h1 className='text-[1.5rem] font-semibold tracking-tight text-text-dark'>
                    {t('meetingDetails')}
                </h1>
                <p className='mt-1 text-[0.95rem] text-text-muted'>
                    {t('activeSession', { count: participants.length })}
                </p>
            </div>

            <div className='flex shrink-0 border-b border-border'>
                <button
                    className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                        activeTab === 'chat'
                            ? 'border-primary text-primary'
                            : 'border-transparent text-text-subtle hover:text-text-secondary'
                    }`}
                    onClick={() => setActiveTab('chat')}
                    type='button'
                >
                    <MessageSquare className='h-6 w-6' />
                    {t('tabChat')}
                </button>
                <button
                    className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                        activeTab === 'people'
                            ? 'border-primary text-primary'
                            : 'border-transparent text-text-subtle hover:text-text-secondary'
                    }`}
                    onClick={() => setActiveTab('people')}
                    type='button'
                >
                    <Users className='h-6 w-6' />
                    {t('tabPeople')}
                </button>
            </div>

            {activeTab === 'chat' ? (
                <MeetingChat
                    messages={messages}
                    onSendMessage={onSendMessage}
                />
            ) : (
                <div className='flex-1 space-y-3 overflow-y-auto p-5'>
                    {participants.map((p) => (
                        <div
                            className='flex items-center gap-4 rounded-[1.2rem] bg-surface-person-item px-5 py-4'
                            key={p.id}
                        >
                            <div
                                className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br ${p.avatarGradient} text-sm font-semibold text-white shadow-sm`}
                            >
                                {p.initials}
                            </div>
                            <div className='min-w-0 flex-1'>
                                <p className='truncate text-[1.05rem] font-semibold text-text-dark'>
                                    {p.name}
                                </p>
                                {p.isHost && (
                                    <p className='text-[0.85rem] text-primary'>
                                        {t('host')}
                                    </p>
                                )}
                            </div>
                            <span
                                className={
                                    p.micOn
                                        ? 'text-primary'
                                        : 'text-border-muted-disabled'
                                }
                            >
                                {p.micOn ? (
                                    <Mic className='h-4 w-4' />
                                ) : (
                                    <MicOff className='h-4 w-4' />
                                )}
                            </span>
                        </div>
                    ))}
                </div>
            )}
        </aside>
    );
}
