'use client';

import { LayoutGrid, Monitor, PanelRight, Sparkles } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Button } from '@/components/ui/button.tsx';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu.tsx';
import type { MeetingLayoutMode } from '@/hooks/use-meeting-layout.ts';

type LayoutPickerProps = {
    currentMode: MeetingLayoutMode;
    onSelect: (mode: MeetingLayoutMode) => void;
};

type LayoutOption = {
    mode: MeetingLayoutMode;
    icon: React.ReactNode;
};

const LAYOUT_OPTIONS: LayoutOption[] = [
    { mode: 'auto', icon: <Sparkles className='h-4 w-4' /> },
    { mode: 'tiled', icon: <LayoutGrid className='h-4 w-4' /> },
    { mode: 'spotlight', icon: <Monitor className='h-4 w-4' /> },
    { mode: 'sidebar', icon: <PanelRight className='h-4 w-4' /> },
];

const LAYOUT_LABEL_KEYS: Record<
    MeetingLayoutMode,
    'layoutAuto' | 'layoutTiled' | 'layoutSpotlight' | 'layoutSidebar'
> = {
    auto: 'layoutAuto',
    tiled: 'layoutTiled',
    spotlight: 'layoutSpotlight',
    sidebar: 'layoutSidebar',
};

/**
 * Dropdown that lets the user select a meeting layout mode.
 * Intended to be embedded in the meeting toolbar.
 */
export function LayoutPicker({ currentMode, onSelect }: LayoutPickerProps) {
    const t = useTranslations('meetingRoom');
    const currentLabelKey = LAYOUT_LABEL_KEYS[currentMode];

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button
                    aria-label={t('layoutPickerLabel')}
                    className='flex h-11 w-11 items-center justify-center rounded-full bg-surface-input text-text-secondary hover:bg-surface-input hover:text-primary'
                    size='icon'
                    title={t(currentLabelKey)}
                    variant='ghost'
                >
                    <LayoutGrid className='h-5 w-5' />
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align='center' side='top'>
                {LAYOUT_OPTIONS.map(({ mode, icon }) => {
                    const labelKey = LAYOUT_LABEL_KEYS[mode];
                    return (
                        <DropdownMenuItem
                            className={
                                mode === currentMode ? 'text-primary' : ''
                            }
                            key={mode}
                            onClick={() => onSelect(mode)}
                        >
                            {icon}
                            <span className='ml-2'>{t(labelKey)}</span>
                        </DropdownMenuItem>
                    );
                })}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
