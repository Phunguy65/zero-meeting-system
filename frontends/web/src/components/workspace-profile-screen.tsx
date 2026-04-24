'use client';

import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { WorkspaceShell } from '@/components/workspace-shell.tsx';

function SectionIcon({
    children,
    tint = 'blue',
}: {
    children: React.ReactNode;
    tint?: 'blue' | 'slate' | 'indigo' | 'red';
}) {
    const classes = {
        blue: 'bg-[#dce9ff] text-[#1a73e8]',
        slate: 'bg-[#eef2f7] text-[#475467]',
        indigo: 'bg-[#e6e7ff] text-[#4f46e5]',
        red: 'bg-[#ffe2e2] text-[#dc2626]',
    };

    return (
        <span
            className={`flex h-14 w-14 items-center justify-center rounded-2xl ${classes[tint]}`}
        >
            {children}
        </span>
    );
}

function SettingsIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='m19.14 12.94.05-.94-.05-.94 1.64-1.28a.8.8 0 0 0 .19-1.03l-1.55-2.68a.8.8 0 0 0-.98-.35l-1.94.78a7.4 7.4 0 0 0-1.63-.94l-.3-2.06a.82.82 0 0 0-.8-.68h-3.1a.82.82 0 0 0-.8.68l-.3 2.06c-.58.22-1.13.53-1.63.94l-1.94-.78a.8.8 0 0 0-.98.35L3.03 8.75a.8.8 0 0 0 .19 1.03l1.64 1.28-.05.94.05.94-1.64 1.28a.8.8 0 0 0-.19 1.03l1.55 2.68c.2.35.62.5.98.35l1.94-.78c.5.4 1.05.72 1.63.94l.3 2.06c.07.4.4.68.8.68h3.1c.4 0 .73-.29.8-.68l.3-2.06c.58-.22 1.13-.53 1.63-.94l1.94.78c.36.15.78 0 .98-.35l1.55-2.68a.8.8 0 0 0-.19-1.03l-1.64-1.28ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z' />
        </svg>
    );
}

function HistoryIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='none'
            stroke='currentColor'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <path d='M3 12a9 9 0 1 0 3-6.71' />
            <path d='M3 4v5h5' />
            <path d='M12 7v5l3 2' />
        </svg>
    );
}

function HelpIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='currentColor'
            viewBox='0 0 24 24'
        >
            <path d='M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.06 15.5a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5ZM14 10.3c-.56.46-1.14.86-1.14 1.7v.5h-1.75v-.7c0-1.08.72-1.74 1.36-2.27.55-.46 1.03-.84 1.03-1.45 0-.77-.58-1.3-1.48-1.3-.9 0-1.56.48-2.12 1.2L8.5 6.86C9.39 5.67 10.63 5 12.18 5c2.06 0 3.56 1.2 3.56 3.06 0 1.09-.68 1.76-1.74 2.24Z' />
        </svg>
    );
}

function LogoutIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-7 w-7'
            fill='none'
            stroke='currentColor'
            strokeWidth='2'
            viewBox='0 0 24 24'
        >
            <path d='M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4' />
            <path d='M10 17l5-5-5-5' />
            <path d='M15 12H3' />
        </svg>
    );
}

function ChevronIcon() {
    return (
        <svg
            aria-hidden='true'
            className='h-6 w-6'
            fill='none'
            stroke='currentColor'
            strokeWidth='2.2'
            viewBox='0 0 24 24'
        >
            <path d='m9 6 6 6-6 6' />
        </svg>
    );
}

type ProfileCardProps = {
    title: string;
    description: string;
    tint?: 'blue' | 'slate' | 'indigo' | 'red';
    icon: React.ReactNode;
    destructive?: boolean;
};

function ProfileCard({
    title,
    description,
    tint,
    icon,
    destructive = false,
}: ProfileCardProps) {
    return (
        <button
            className={`flex w-full items-center gap-5 rounded-[1.7rem] bg-white px-7 py-6 text-left shadow-[0_22px_60px_-38px_rgba(15,23,42,0.2)] transition-all hover:-translate-y-0.5 ${
                destructive ? 'hover:bg-[#fff7f7]' : 'hover:bg-[#fbfcff]'
            }`}
            type='button'
        >
            <SectionIcon tint={tint}>{icon}</SectionIcon>
            <div className='min-w-0 flex-1'>
                <h3
                    className={`text-[1.55rem] font-semibold tracking-tight ${
                        destructive ? 'text-[#b42318]' : 'text-[#15191f]'
                    }`}
                >
                    {title}
                </h3>
                <p className='mt-2 text-lg leading-8 text-[#475467]'>
                    {description}
                </p>
            </div>
            <span className={destructive ? 'text-[#dc2626]' : 'text-[#98a2b3]'}>
                <ChevronIcon />
            </span>
        </button>
    );
}

export function WorkspaceProfileScreen() {
    const t = useTranslations('workspace.profile');
    const locale = useLocale();

    const displayName = t('name');
    const initials = displayName
        .split(' ')
        .map((part) => part[0])
        .join('')
        .toUpperCase()
        .slice(0, 2);

    return (
        <WorkspaceShell activeTab='profile' rightMode='search'>
            <section className='mx-auto max-w-[1320px]'>
                <div className='max-w-[760px]'>
                    <h1 className='text-5xl font-semibold tracking-tight text-[#15191f] sm:text-6xl'>
                        {t('headline')}
                    </h1>
                    <p className='mt-5 text-2xl leading-9 text-[#344054] sm:text-[1.1rem]'>
                        {t('description')}
                    </p>
                </div>

                <div className='mt-12 grid gap-8 xl:grid-cols-[0.82fr_1.18fr]'>
                    <article className='rounded-[2rem] bg-white p-8 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.2)] sm:p-10'>
                        <div className='flex flex-col items-center text-center'>
                            <div className='flex h-36 w-36 items-center justify-center rounded-full bg-[linear-gradient(135deg,_#183b72_0%,_#3c7fe8_100%)] text-5xl font-semibold text-white shadow-[0_24px_50px_-28px_rgba(26,115,232,0.95)]'>
                                {initials}
                            </div>
                            <h2 className='mt-8 text-[2.35rem] font-semibold tracking-tight text-[#15191f]'>
                                {t('name')}
                            </h2>
                            <p className='mt-2 text-xl text-[#475467]'>
                                {t('email')}
                            </p>
                        </div>

                        <div className='mt-10 rounded-[1.6rem] bg-[#f5f7fb] p-6'>
                            <p className='text-sm font-semibold uppercase tracking-[0.18em] text-[#1a73e8]'>
                                {t('statusLabel')}
                            </p>
                            <p className='mt-4 text-[1.7rem] font-semibold text-[#15191f]'>
                                {t('statusTitle')}
                            </p>
                            <p className='mt-2 text-lg leading-8 text-[#475467]'>
                                {t('statusDescription')}
                            </p>
                        </div>
                    </article>

                    <div className='space-y-5'>
                        <Link
                            className='flex w-full items-center gap-5 rounded-[1.7rem] bg-white px-7 py-6 text-left shadow-[0_22px_60px_-38px_rgba(15,23,42,0.2)] transition-all hover:-translate-y-0.5 hover:bg-[#fbfcff]'
                            href={`/${locale}/workspace/profile`}
                        >
                            <SectionIcon tint='blue'>
                                <SettingsIcon />
                            </SectionIcon>
                            <div className='min-w-0 flex-1'>
                                <h3 className='text-[1.55rem] font-semibold tracking-tight text-[#15191f]'>
                                    {t('accountSettingsTitle')}
                                </h3>
                                <p className='mt-2 text-lg leading-8 text-[#475467]'>
                                    {t('accountSettingsDescription')}
                                </p>
                            </div>
                            <span className='text-[#98a2b3]'>
                                <ChevronIcon />
                            </span>
                        </Link>
                        <Link
                            className='flex w-full items-center gap-5 rounded-[1.7rem] bg-white px-7 py-6 text-left shadow-[0_22px_60px_-38px_rgba(15,23,42,0.2)] transition-all hover:-translate-y-0.5 hover:bg-[#fbfcff]'
                            href={`/${locale}/workspace/profile`}
                        >
                            <SectionIcon tint='slate'>
                                <HistoryIcon />
                            </SectionIcon>
                            <div className='min-w-0 flex-1'>
                                <h3 className='text-[1.55rem] font-semibold tracking-tight text-[#15191f]'>
                                    {t('meetingHistoryTitle')}
                                </h3>
                                <p className='mt-2 text-lg leading-8 text-[#475467]'>
                                    {t('meetingHistoryDescription')}
                                </p>
                            </div>
                            <span className='text-[#98a2b3]'>
                                <ChevronIcon />
                            </span>
                        </Link>
                        <ProfileCard
                            description={t('helpSupportDescription')}
                            icon={<HelpIcon />}
                            tint='indigo'
                            title={t('helpSupportTitle')}
                        />
                        <ProfileCard
                            description={t('logoutDescription')}
                            destructive
                            icon={<LogoutIcon />}
                            tint='red'
                            title={t('logoutTitle')}
                        />
                    </div>
                </div>
            </section>
        </WorkspaceShell>
    );
}
