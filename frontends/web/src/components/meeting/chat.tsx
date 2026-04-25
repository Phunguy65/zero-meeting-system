'use client';

import { Send } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useRef, useState } from 'react';
import type { MeetingMessage } from '@/lib/mock-data/messages.ts';

type MeetingChatProps = {
    messages: MeetingMessage[];
    onSendMessage: (text: string) => void;
};

export function MeetingChat({ messages, onSendMessage }: MeetingChatProps) {
    const t = useTranslations('meetingRoom');
    const [inputValue, setInputValue] = useState('');
    const scrollRef = useRef<HTMLDivElement>(null);

    function handleSend() {
        const text = inputValue.trim();
        if (!text) return;
        onSendMessage(text);
        setInputValue('');
    }

    return (
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
                                        ? 'text-primary'
                                        : 'text-text-dark'
                                }`}
                            >
                                {msg.isYou ? t('you') : msg.sender}
                            </span>
                            <span className='shrink-0 text-[0.78rem] text-text-message-time'>
                                {msg.time}
                            </span>
                        </div>
                        <div
                            className={`rounded-[1rem] px-4 py-3 text-[0.97rem] leading-7 ${
                                msg.isYou
                                    ? 'bg-primary-muted text-text-message-own'
                                    : 'bg-surface-input text-text-primary'
                            }`}
                        >
                            {msg.text}
                        </div>
                    </div>
                ))}
            </div>

            <div className='shrink-0 border-t border-border p-4'>
                <div className='flex items-center gap-3 rounded-full bg-surface-input px-5 py-3 ring-1 ring-transparent transition focus-within:ring-2 focus-within:ring-primary'>
                    <input
                        className='flex-1 bg-transparent text-[0.97rem] text-text-dark outline-none placeholder:text-text-message-time'
                        onChange={(e) => setInputValue(e.target.value)}
                        onKeyDown={(e) => e.key === 'Enter' && handleSend()}
                        placeholder={t('messagePlaceholder')}
                        type='text'
                        value={inputValue}
                    />
                    <button
                        className='flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-white shadow-sm transition-all hover:bg-primary-hover disabled:opacity-40'
                        disabled={!inputValue.trim()}
                        onClick={handleSend}
                        type='button'
                    >
                        <Send className='h-5 w-5' />
                    </button>
                </div>
            </div>
        </>
    );
}
