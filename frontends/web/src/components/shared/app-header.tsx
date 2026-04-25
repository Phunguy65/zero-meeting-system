'use client';

import { Bell, HelpCircle, Search, Settings, User } from 'lucide-react';
import Link from 'next/link';
import type { ReactNode } from 'react';

type NavItem = {
    id: string;
    label: string;
    href: string;
};

type WorkspaceHeaderProps = {
    variant: 'workspace';
    brand: string;
    brandHref: string;
    navItems: NavItem[];
    activeNavId: string;
    rightMode?: 'compact' | 'search';
    helpLabel: string;
    notificationsLabel: string;
    searchPlaceholder: string;
    profileLabel: string;
};

type MeetingHeaderProps = {
    variant: 'meeting';
    brand: string;
    brandHref: string;
    meetingName?: string;
    helpLabel: string;
    settingsLabel: string;
    profileLabel: string;
    actions?: ReactNode;
};

type GreenRoomHeaderProps = {
    variant: 'green-room';
    brand: string;
    brandHref: string;
    helpLabel: string;
    settingsLabel: string;
    profileLabel: string;
};

type AppHeaderProps =
    | WorkspaceHeaderProps
    | MeetingHeaderProps
    | GreenRoomHeaderProps;

function ProfileAvatar({ label }: { label: string }) {
    return (
        <button
            aria-label={label}
            className='inline-flex h-12 w-12 items-center justify-center rounded-full bg-[linear-gradient(135deg,_var(--avatar-gradient-navy-start)_0%,_var(--avatar-gradient-navy-end)_100%)] text-white shadow-[0_14px_28px_-18px_rgba(26,115,232,0.85)]'
            type='button'
        >
            <User className='h-6 w-6' />
        </button>
    );
}

function IconButton({
    label,
    size = 'md',
    children,
}: {
    label: string;
    size?: 'sm' | 'md';
    children: ReactNode;
}) {
    const sizeClass = size === 'sm' ? 'h-10 w-10' : 'h-11 w-11';
    return (
        <button
            aria-label={label}
            className={`inline-flex ${sizeClass} items-center justify-center rounded-full text-text-muted transition-colors hover:bg-primary-subtle hover:text-primary`}
            type='button'
        >
            {children}
        </button>
    );
}

function WorkspaceHeader(props: WorkspaceHeaderProps) {
    const {
        brand,
        brandHref,
        navItems,
        activeNavId,
        rightMode = 'compact',
    } = props;

    return (
        <header className='sticky top-0 z-40 border-b border-border bg-white/92 backdrop-blur'>
            <div className='mx-auto flex max-w-[1600px] items-center justify-between px-6 py-5 sm:px-8 lg:px-10'>
                <div className='flex items-center gap-8 lg:gap-14'>
                    <Link
                        className='text-[2.05rem] font-semibold tracking-tight text-primary'
                        href={brandHref}
                    >
                        {brand}
                    </Link>

                    <nav className='hidden items-center gap-8 text-[1.08rem] sm:flex'>
                        {navItems.map((tab) => {
                            const isActive = tab.id === activeNavId;
                            return (
                                <Link
                                    aria-current={isActive ? 'page' : undefined}
                                    className={`border-b-[3px] pb-2 transition-colors ${
                                        isActive
                                            ? 'border-primary font-medium text-primary'
                                            : 'border-transparent text-text-secondary hover:text-primary'
                                    }`}
                                    href={tab.href}
                                    key={tab.id}
                                >
                                    {tab.label}
                                </Link>
                            );
                        })}
                    </nav>
                </div>

                <div className='flex items-center gap-4 sm:gap-6'>
                    {rightMode === 'search' ? (
                        <>
                            <div className='hidden items-center gap-3 rounded-full bg-surface-input px-5 py-3 text-text-subtle shadow-inner sm:flex sm:min-w-[290px]'>
                                <Search className='h-6 w-6' />
                                <span className='text-[1.05rem]'>
                                    {props.searchPlaceholder}
                                </span>
                            </div>
                            <IconButton label={props.notificationsLabel}>
                                <Bell className='h-6 w-6' />
                            </IconButton>
                        </>
                    ) : (
                        <IconButton label={props.helpLabel}>
                            <HelpCircle className='h-6 w-6' />
                        </IconButton>
                    )}

                    <ProfileAvatar label={props.profileLabel} />
                </div>
            </div>
        </header>
    );
}

function MeetingHeader(props: MeetingHeaderProps) {
    const {
        brand,
        brandHref,
        meetingName,
        helpLabel,
        settingsLabel,
        profileLabel,
        actions,
    } = props;

    return (
        <header className='flex shrink-0 items-center justify-between border-b border-border bg-surface px-6 py-4'>
            <div className='flex items-center gap-5'>
                <Link
                    className='text-[1.65rem] font-semibold tracking-tight text-primary'
                    href={brandHref}
                >
                    {brand}
                </Link>
                {meetingName ? (
                    <span className='hidden text-[1.02rem] text-text-secondary sm:block'>
                        {meetingName}
                    </span>
                ) : null}
            </div>

            <div className='flex items-center gap-3 sm:gap-4'>
                {actions}
                <IconButton label={helpLabel} size='sm'>
                    <HelpCircle className='h-6 w-6' />
                </IconButton>
                <IconButton label={settingsLabel} size='sm'>
                    <Settings className='h-6 w-6' />
                </IconButton>
                <button
                    aria-label={profileLabel}
                    className='inline-flex h-11 w-11 items-center justify-center rounded-full bg-[linear-gradient(135deg,_var(--avatar-gradient-navy-start)_0%,_var(--avatar-gradient-navy-end)_100%)] text-white shadow-[0_14px_28px_-18px_rgba(26,115,232,0.85)]'
                    type='button'
                >
                    <User className='h-6 w-6' />
                </button>
            </div>
        </header>
    );
}

function GreenRoomHeader(props: GreenRoomHeaderProps) {
    const { brand, brandHref, helpLabel, settingsLabel, profileLabel } = props;

    return (
        <header className='sticky top-0 z-40 border-b border-border bg-white/94 backdrop-blur'>
            <div className='mx-auto flex max-w-[1600px] items-center justify-between px-6 py-4 sm:px-8 lg:px-10'>
                <Link
                    className='text-[1.85rem] font-semibold tracking-tight text-primary'
                    href={brandHref}
                >
                    {brand}
                </Link>

                <div className='flex items-center gap-4 sm:gap-5'>
                    <IconButton label={helpLabel}>
                        <HelpCircle className='h-6 w-6' />
                    </IconButton>
                    <IconButton label={settingsLabel}>
                        <Settings className='h-6 w-6' />
                    </IconButton>
                    <ProfileAvatar label={profileLabel} />
                </div>
            </div>
        </header>
    );
}

export function AppHeader(props: AppHeaderProps) {
    if (props.variant === 'workspace') {
        return <WorkspaceHeader {...props} />;
    }
    if (props.variant === 'meeting') {
        return <MeetingHeader {...props} />;
    }
    return <GreenRoomHeader {...props} />;
}
