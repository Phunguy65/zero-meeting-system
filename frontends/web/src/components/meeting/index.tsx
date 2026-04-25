'use client';

import { useLocale, useTranslations } from 'next-intl';
import { useEffect, useRef, useState } from 'react';
import { AppHeader } from '@/components/shared/app-header.tsx';
import { MEETING_PARTICIPANTS } from '@/lib/mock-data/meeting.ts';
import {
    INITIAL_MEETING_MESSAGES,
    type MeetingMessage,
} from '@/lib/mock-data/messages.ts';
import { ParticipantGrid } from './participant-grid.tsx';
import { MeetingSidebar } from './sidebar.tsx';
import { MeetingToolbar } from './toolbar.tsx';

export function MeetingContainer() {
    const locale = useLocale();
    const t = useTranslations('meetingRoom');
    const common = useTranslations('workspace.common');

    const [micOn, setMicOn] = useState(true);
    const [videoOn, setVideoOn] = useState(true);
    const [messages, setMessages] = useState<MeetingMessage[]>(
        INITIAL_MEETING_MESSAGES,
    );
    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, []);

    function handleSendMessage(text: string) {
        const time = new Date().toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true,
        });
        setMessages((prev) => [
            ...prev,
            { id: Date.now(), sender: 'You', time, text, isYou: true },
        ]);
    }

    return (
        <main className='flex h-screen flex-col overflow-hidden bg-meeting-bg'>
            <AppHeader
                brand={common('brand')}
                brandHref={`/${locale}/workspace`}
                helpLabel={common('help')}
                meetingName={t('meetingName')}
                profileLabel={common('profile')}
                settingsLabel={common('settings')}
                variant='meeting'
            />

            <div className='flex flex-1 overflow-hidden'>
                <ParticipantGrid participants={MEETING_PARTICIPANTS} />
                <MeetingSidebar
                    messages={messages}
                    onSendMessage={handleSendMessage}
                    participants={MEETING_PARTICIPANTS}
                />
            </div>

            <MeetingToolbar
                locale={locale}
                micOn={micOn}
                onOpenChat={() => {}}
                onToggleMic={() => setMicOn((v) => !v)}
                onToggleVideo={() => setVideoOn((v) => !v)}
                videoOn={videoOn}
            />
        </main>
    );
}
