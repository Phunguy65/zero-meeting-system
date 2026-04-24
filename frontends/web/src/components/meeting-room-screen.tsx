'use client';

import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { useEffect, useRef, useState } from 'react';

/* ── Icons ──────────────────────────────────────────────── */

function QuestionIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.06 15.5a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5ZM14 10.3c-.56.46-1.14.86-1.14 1.7v.5h-1.75v-.7c0-1.08.72-1.74 1.36-2.27.55-.46 1.03-.84 1.03-1.45 0-.77-.58-1.3-1.48-1.3-.9 0-1.56.48-2.12 1.2L8.5 6.86C9.39 5.67 10.63 5 12.18 5c2.06 0 3.56 1.2 3.56 3.06 0 1.09-.68 1.76-1.74 2.24Z' />
        </svg>
    );
}

function SettingsIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='m19.14 12.94.05-.94-.05-.94 1.64-1.28a.8.8 0 0 0 .19-1.03l-1.55-2.68a.8.8 0 0 0-.98-.35l-1.94.78a7.4 7.4 0 0 0-1.63-.94l-.3-2.06a.82.82 0 0 0-.8-.68h-3.1a.82.82 0 0 0-.8.68l-.3 2.06c-.58.22-1.13.53-1.63.94l-1.94-.78a.8.8 0 0 0-.98.35L3.03 8.75a.8.8 0 0 0 .19 1.03l1.64 1.28-.05.94.05.94-1.64 1.28a.8.8 0 0 0-.19 1.03l1.55 2.68c.2.35.62.5.98.35l1.94-.78c.5.4 1.05.72 1.63.94l.3 2.06c.07.4.4.68.8.68h3.1c.4 0 .73-.29.8-.68l.3-2.06c.58-.22 1.13-.53 1.63-.94l1.94.78c.36.15.78 0 .98-.35l1.55-2.68a.8.8 0 0 0-.19-1.03l-1.64-1.28ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z' />
        </svg>
    );
}

function UserAvatarIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm0 2c-4.4 0-8 2.69-8 6v1h16v-1c0-3.31-3.6-6-8-6Z' />
        </svg>
    );
}

function MicOnIcon({ small = false }: { small?: boolean }) {
    const cls = small ? 'h-4 w-4' : 'h-6 w-6';
    return (
        <svg
            aria-hidden='true'
            className={cls}
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M12 15a3 3 0 0 0 3-3V7a3 3 0 1 0-6 0v5a3 3 0 0 0 3 3Zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.92V22h2v-3.08A7 7 0 0 0 19 12h-2Z' />
        </svg>
    );
}

function MicOffIcon({ small = false }: { small?: boolean }) {
    const cls = small ? 'h-4 w-4' : 'h-6 w-6';
    return (
        <svg
            aria-hidden='true'
            className={cls}
            fill='none'
            stroke='currentColor'
            strokeLinecap='round'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <line x1='1' x2='23' y1='1' y2='23' />
            <path d='M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V7a3 3 0 0 0-5.5-2.75' />
            <path d='M17 16.95A7 7 0 0 1 5 12m7 7v3' />
        </svg>
    );
}

function VideoOnIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M14 7a2 2 0 0 1 2 2v1.56l3.2-2.4A1 1 0 0 1 21 8.96v6.08a1 1 0 0 1-1.8.8L16 13.44V15a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h9Z' />
        </svg>
    );
}

function VideoOffIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='none'
            stroke='currentColor'
            strokeLinecap='round'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <path d='m1 1 22 22' />
            <path d='M17 17H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h3m3.5-3H14a2 2 0 0 1 2 2v1.5' />
            <path d='M16 11.37V9a1 1 0 0 1 1.8-.6L21 10.96v6.08' />
        </svg>
    );
}

function ScreenShareIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='none'
            stroke='currentColor'
            strokeLinecap='round'
            strokeLinejoin='round'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <path d='M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8' />
            <polyline points='16 6 12 2 8 6' />
            <line x1='12' x2='12' y1='2' y2='15' />
        </svg>
    );
}

function ChatBubbleIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M5 4a3 3 0 0 0-3 3v12l4.5-3H19a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3H5Zm2 4h10v2H7V8Zm0 4h6v2H7v-2Z' />
        </svg>
    );
}

function PeopleIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm6-1a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5ZM3 18a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v1H3v-1Zm11 1v-1c0-1.13-.37-2.18-.99-3.03.31-.06.64-.1.99-.1h2a4 4 0 0 1 4 4v.13A1.87 1.87 0 0 1 19.87 19H14Z' />
        </svg>
    );
}

function SendIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-5 w-5'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M2.01 21 23 12 2.01 3 2 10l15 2-15 2z' />
        </svg>
    );
}

function SoundWaveIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-4 w-4'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <rect height='8' rx='1' width='3' x='1' y='8' />
            <rect height='16' rx='1' width='3' x='9' y='4' />
            <rect height='12' rx='1' width='3' x='17' y='6' />
        </svg>
    );
}

function LeaveCallIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-5 w-5 rotate-[135deg]'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.46.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z' />
        </svg>
    );
}

/* ── Types & Data ─────────────────────────────────────────── */

type SidebarTab = 'chat' | 'people';

type Participant = {
    id: string;
    name: string;
    initials: string;
    isHost: boolean;
    micOn: boolean;
    isSpeaking: boolean;
    tileBg: string;
    avatarGradient: string;
};

type MessageItem = {
    id: number;
    sender: string;
    time: string;
    text: string;
    isYou: boolean;
};

const PARTICIPANTS: Participant[] = [
    {
        id: 'sarah',
        name: 'Sarah Jenkins',
        initials: 'SJ',
        isHost: true,
        micOn: true,
        isSpeaking: false,
        tileBg: 'bg-[linear-gradient(160deg,_#1a2c4a_0%,_#2b4068_50%,_#1e3558_100%)]',
        avatarGradient: 'from-[#243b67] to-[#4e7fd4]',
    },
    {
        id: 'david',
        name: 'David Chen',
        initials: 'DC',
        isHost: false,
        micOn: false,
        isSpeaking: false,
        tileBg: 'bg-[linear-gradient(160deg,_#1c2433_0%,_#2a3547_50%,_#1a2233_100%)]',
        avatarGradient: 'from-[#374151] to-[#6b7280]',
    },
    {
        id: 'elena',
        name: 'Elena Rodriguez',
        initials: 'ER',
        isHost: false,
        micOn: true,
        isSpeaking: false,
        tileBg: 'bg-[linear-gradient(160deg,_#1a1f2e_0%,_#252d42_50%,_#1a1f2e_100%)]',
        avatarGradient: 'from-[#1d4ed8] to-[#60a5fa]',
    },
    {
        id: 'marcus',
        name: 'Marcus Thorne',
        initials: 'MT',
        isHost: false,
        micOn: true,
        isSpeaking: true,
        tileBg: 'bg-[linear-gradient(160deg,_#1e2d3a_0%,_#2d4455_50%,_#1e2d3a_100%)]',
        avatarGradient: 'from-[#065f46] to-[#10b981]',
    },
];

const INITIAL_MESSAGES: MessageItem[] = [
    {
        id: 1,
        sender: 'Sarah Jenkins',
        time: '10:42 AM',
        text: "Welcome everyone! Let's start with the Q3 review.",
        isYou: false,
    },
    {
        id: 2,
        sender: 'Elena Rodriguez',
        time: '10:43 AM',
        text: "I've updated the Figma file with the latest prototypes.",
        isYou: false,
    },
    {
        id: 3,
        sender: 'You',
        time: '10:45 AM',
        text: 'The new video grid logic looks great. Checking performance now.',
        isYou: true,
    },
    {
        id: 4,
        sender: 'Marcus Thorne',
        time: '10:48 AM',
        text: 'Agreed. @Elena, can you share the link?',
        isYou: false,
    },
];

/* ── Component ────────────────────────────────────────────── */

export function MeetingRoomScreen() {
    const locale = useLocale();
    const t = useTranslations('meetingRoom');
    const common = useTranslations('workspace.common');

    const [micOn, setMicOn] = useState(true);
    const [videoOn, setVideoOn] = useState(true);
    const [activeTab, setActiveTab] = useState<SidebarTab>('chat');
    const [messages, setMessages] = useState<MessageItem[]>(INITIAL_MESSAGES);
    const [inputValue, setInputValue] = useState('');
    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
        }
    }, []);

    const handleSend = () => {
        const text = inputValue.trim();
        if (!text) return;

        const time = new Date().toLocaleTimeString('en-US', {
            hour: 'numeric',
            minute: '2-digit',
            hour12: true,
        });

        setMessages((prev) => [
            ...prev,
            { id: Date.now(), sender: 'You', time, text, isYou: true },
        ]);
        setInputValue('');
    };

    return (
        <main className='flex h-screen flex-col overflow-hidden bg-[#111827]'>
            {/* ── Header ─────────────────────────────────────────── */}
            <header className='flex shrink-0 items-center justify-between border-b border-[#e5eaf2] bg-white px-6 py-4'>
                <div className='flex items-center gap-5'>
                    <Link
                        className='text-[1.65rem] font-semibold tracking-tight text-[#1a73e8]'
                        href={`/${locale}/workspace`}
                    >
                        {common('brand')}
                    </Link>
                    <span className='hidden text-[1.02rem] text-[#344054] sm:block'>
                        {t('meetingName')}
                    </span>
                </div>

                <div className='flex items-center gap-3 sm:gap-4'>
                    <button
                        aria-label={common('help')}
                        className='inline-flex h-10 w-10 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]'
                        type='button'
                    >
                        <QuestionIcon />
                    </button>
                    <button
                        aria-label={common('settings')}
                        className='inline-flex h-10 w-10 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]'
                        type='button'
                    >
                        <SettingsIcon />
                    </button>
                    <button
                        aria-label={common('profile')}
                        className='inline-flex h-11 w-11 items-center justify-center rounded-full bg-[linear-gradient(135deg,_#243b67_0%,_#4e7fd4_100%)] text-white shadow-[0_14px_28px_-18px_rgba(26,115,232,0.85)]'
                        type='button'
                    >
                        <UserAvatarIcon />
                    </button>
                </div>
            </header>

            {/* ── Body ───────────────────────────────────────────── */}
            <div className='flex flex-1 overflow-hidden'>
                {/* Video Grid */}
                <section className='flex-1 overflow-hidden p-4'>
                    <div className='grid h-full grid-cols-2 grid-rows-2 gap-3'>
                        {PARTICIPANTS.map((p) => (
                            <div
                                className={`relative overflow-hidden rounded-[1.4rem] ${p.tileBg} ${
                                    p.isSpeaking
                                        ? 'ring-[3px] ring-[#1a73e8] ring-offset-2 ring-offset-[#111827]'
                                        : ''
                                }`}
                                key={p.id}
                            >
                                {/* Avatar placeholder */}
                                <div className='absolute inset-0 flex items-center justify-center'>
                                    <div
                                        className={`flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br ${p.avatarGradient} text-2xl font-semibold text-white shadow-[0_14px_32px_-18px_rgba(0,0,0,0.6)] md:h-[7rem] md:w-[7rem] md:text-3xl`}
                                    >
                                        {p.initials}
                                    </div>
                                </div>

                                {/* Active-speaker indicator */}
                                {p.isSpeaking && (
                                    <div className='absolute right-4 top-4 flex h-9 w-9 items-center justify-center rounded-full bg-[#1a73e8] shadow-[0_8px_20px_-8px_rgba(26,115,232,0.8)]'>
                                        <SoundWaveIcon />
                                    </div>
                                )}

                                {/* Name tag */}
                                <div className='absolute bottom-4 left-4 flex items-center gap-2 rounded-xl bg-black/55 px-3 py-1.5 backdrop-blur-sm'>
                                    <span
                                        className={
                                            p.micOn
                                                ? 'text-white/90'
                                                : 'text-red-400'
                                        }
                                    >
                                        {p.micOn ? (
                                            <MicOnIcon small />
                                        ) : (
                                            <MicOffIcon small />
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

                {/* Sidebar */}
                <aside className='flex w-[300px] shrink-0 flex-col border-l border-[#e5eaf2] bg-white xl:w-[340px]'>
                    {/* Sidebar header */}
                    <div className='shrink-0 border-b border-[#e5eaf2] px-6 py-5'>
                        <h1 className='text-[1.5rem] font-semibold tracking-tight text-[#15191f]'>
                            {t('meetingDetails')}
                        </h1>
                        <p className='mt-1 text-[0.95rem] text-[#475467]'>
                            {t('activeSession', { count: PARTICIPANTS.length })}
                        </p>
                    </div>

                    {/* Tabs */}
                    <div className='flex shrink-0 border-b border-[#e5eaf2]'>
                        <button
                            className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                                activeTab === 'chat'
                                    ? 'border-[#1a73e8] text-[#1a73e8]'
                                    : 'border-transparent text-[#667085] hover:text-[#344054]'
                            }`}
                            onClick={() => setActiveTab('chat')}
                            type='button'
                        >
                            <ChatBubbleIcon />
                            {t('tabChat')}
                        </button>
                        <button
                            className={`flex flex-1 items-center justify-center gap-2 border-b-2 px-4 py-4 text-[1.02rem] font-medium transition-colors ${
                                activeTab === 'people'
                                    ? 'border-[#1a73e8] text-[#1a73e8]'
                                    : 'border-transparent text-[#667085] hover:text-[#344054]'
                            }`}
                            onClick={() => setActiveTab('people')}
                            type='button'
                        >
                            <PeopleIcon />
                            {t('tabPeople')}
                        </button>
                    </div>

                    {/* ── Chat tab ── */}
                    {activeTab === 'chat' ? (
                        <>
                            <div
                                className='flex-1 space-y-5 overflow-y-auto px-5 py-5'
                                ref={scrollRef}
                            >
                                {messages.map((msg) => (
                                    <div key={msg.id}>
                                        <div className='mb-2 flex items-center justify-between gap-3'>
                                            <span
                                                className={`text-[1.02rem] font-semibold ${
                                                    msg.isYou
                                                        ? 'text-[#1a73e8]'
                                                        : 'text-[#15191f]'
                                                }`}
                                            >
                                                {msg.isYou
                                                    ? t('you')
                                                    : msg.sender}
                                            </span>
                                            <span className='shrink-0 text-[0.78rem] text-[#98a2b3]'>
                                                {msg.time}
                                            </span>
                                        </div>
                                        <div
                                            className={`rounded-[1rem] px-4 py-3 text-[0.97rem] leading-7 ${
                                                msg.isYou
                                                    ? 'bg-[#e8f0fe] text-[#1a3760]'
                                                    : 'bg-[#f2f4f7] text-[#1f2937]'
                                            }`}
                                        >
                                            {msg.text}
                                        </div>
                                    </div>
                                ))}
                            </div>

                            {/* Message input */}
                            <div className='shrink-0 border-t border-[#e5eaf2] p-4'>
                                <div className='flex items-center gap-3 rounded-full bg-[#f2f4f7] px-5 py-3 ring-1 ring-transparent transition focus-within:ring-2 focus-within:ring-[#1a73e8]'>
                                    <input
                                        className='flex-1 bg-transparent text-[0.97rem] text-[#15191f] outline-none placeholder:text-[#98a2b3]'
                                        onChange={(e) =>
                                            setInputValue(e.target.value)
                                        }
                                        onKeyDown={(e) =>
                                            e.key === 'Enter' && handleSend()
                                        }
                                        placeholder={t('messagePlaceholder')}
                                        type='text'
                                        value={inputValue}
                                    />
                                    <button
                                        className='flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-[#1a73e8] text-white shadow-sm transition-all hover:bg-[#1765cc] disabled:opacity-40'
                                        disabled={!inputValue.trim()}
                                        onClick={handleSend}
                                        type='button'
                                    >
                                        <SendIcon />
                                    </button>
                                </div>
                            </div>
                        </>
                    ) : (
                        /* ── People tab ── */
                        <div className='flex-1 space-y-3 overflow-y-auto p-5'>
                            {PARTICIPANTS.map((p) => (
                                <div
                                    className='flex items-center gap-4 rounded-[1.2rem] bg-[#f7f9fc] px-5 py-4'
                                    key={p.id}
                                >
                                    <div
                                        className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-gradient-to-br ${p.avatarGradient} text-sm font-semibold text-white shadow-sm`}
                                    >
                                        {p.initials}
                                    </div>
                                    <div className='min-w-0 flex-1'>
                                        <p className='truncate text-[1.05rem] font-semibold text-[#15191f]'>
                                            {p.name}
                                        </p>
                                        {p.isHost && (
                                            <p className='text-[0.85rem] text-[#1a73e8]'>
                                                {t('host')}
                                            </p>
                                        )}
                                    </div>
                                    <span
                                        className={
                                            p.micOn
                                                ? 'text-[#1a73e8]'
                                                : 'text-[#d1d5db]'
                                        }
                                    >
                                        {p.micOn ? (
                                            <MicOnIcon small />
                                        ) : (
                                            <MicOffIcon small />
                                        )}
                                    </span>
                                </div>
                            ))}
                        </div>
                    )}
                </aside>
            </div>

            {/* ── Control Bar ────────────────────────────────────── */}
            <footer className='shrink-0 border-t border-[#e5eaf2] bg-white px-6 py-5'>
                <div className='flex items-center justify-center gap-3 sm:gap-5'>
                    {/* Mic */}
                    <button
                        className={`flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 transition-colors ${
                            micOn
                                ? 'text-[#344054] hover:bg-[#f2f4f7]'
                                : 'text-[#dc2626] hover:bg-[#fff0f0]'
                        }`}
                        onClick={() => setMicOn((v) => !v)}
                        type='button'
                    >
                        <span
                            className={`flex h-11 w-11 items-center justify-center rounded-full ${
                                micOn ? 'bg-[#f2f4f7]' : 'bg-[#fee2e2]'
                            }`}
                        >
                            {micOn ? <MicOnIcon /> : <MicOffIcon />}
                        </span>
                        <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                            {micOn ? t('controlMic') : t('controlMicOff')}
                        </span>
                    </button>

                    {/* Video */}
                    <button
                        className={`flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 transition-colors ${
                            videoOn
                                ? 'text-[#344054] hover:bg-[#f2f4f7]'
                                : 'text-[#dc2626] hover:bg-[#fff0f0]'
                        }`}
                        onClick={() => setVideoOn((v) => !v)}
                        type='button'
                    >
                        <span
                            className={`flex h-11 w-11 items-center justify-center rounded-full ${
                                videoOn ? 'bg-[#f2f4f7]' : 'bg-[#fee2e2]'
                            }`}
                        >
                            {videoOn ? <VideoOnIcon /> : <VideoOffIcon />}
                        </span>
                        <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                            {videoOn ? t('controlVideo') : t('controlVideoOff')}
                        </span>
                    </button>

                    {/* Share */}
                    <button
                        className='flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 text-[#344054] transition-colors hover:bg-[#f2f4f7]'
                        type='button'
                    >
                        <span className='flex h-11 w-11 items-center justify-center rounded-full bg-[#f2f4f7]'>
                            <ScreenShareIcon />
                        </span>
                        <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                            {t('controlShare')}
                        </span>
                    </button>

                    {/* Chat */}
                    <button
                        className='flex flex-col items-center gap-1.5 rounded-2xl px-4 py-2.5 text-[#1a73e8] transition-colors hover:bg-[#eef3fd]'
                        onClick={() => setActiveTab('chat')}
                        type='button'
                    >
                        <span className='flex h-11 w-11 items-center justify-center rounded-full bg-[#e8f0fe]'>
                            <ChatBubbleIcon />
                        </span>
                        <span className='text-[0.76rem] font-medium uppercase tracking-[0.1em]'>
                            {t('controlChat')}
                        </span>
                    </button>

                    {/* Divider */}
                    <div className='mx-2 h-12 w-px bg-[#e5eaf2]' />

                    {/* Leave */}
                    <Link
                        className='flex h-14 items-center gap-3 rounded-full bg-[#dc2626] px-7 text-[1.08rem] font-semibold text-white shadow-[0_18px_40px_-20px_rgba(220,38,38,0.85)] transition-all hover:-translate-y-0.5 hover:bg-[#b91c1c] hover:shadow-[0_24px_46px_-20px_rgba(220,38,38,0.9)]'
                        href={`/${locale}/workspace`}
                    >
                        <LeaveCallIcon />
                        {t('leave')}
                    </Link>
                </div>
            </footer>
        </main>
    );
}
