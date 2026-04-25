'use client';

import { CalendarPlus, Video } from 'lucide-react';
import Link from 'next/link';
import { useLocale, useTranslations } from 'next-intl';
import { useState } from 'react';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu.tsx';
import { InstantMeetingDialog } from './instant-meeting-dialog.tsx';

type NewMeetingDropdownProps = {
    children: React.ReactNode;
};

export function NewMeetingDropdown({ children }: NewMeetingDropdownProps) {
    const t = useTranslations('createMeeting');
    const locale = useLocale();
    const [instantOpen, setInstantOpen] = useState(false);

    return (
        <>
            <DropdownMenu>
                <DropdownMenuTrigger asChild>{children}</DropdownMenuTrigger>
                <DropdownMenuContent align='end' className='min-w-[220px]'>
                    <DropdownMenuItem
                        className='gap-3 py-3'
                        onSelect={() => setInstantOpen(true)}
                    >
                        <Video className='h-4 w-4 text-primary' />
                        <div>
                            <p className='font-medium'>
                                {t('instantMeetingAction')}
                            </p>
                            <p className='text-xs text-text-subtle'>
                                {t('instantMeetingActionDescription')}
                            </p>
                        </div>
                    </DropdownMenuItem>
                    <DropdownMenuItem asChild className='gap-3 py-3'>
                        <Link href={`/${locale}/workspace/schedule`}>
                            <CalendarPlus className='h-4 w-4 text-primary' />
                            <div>
                                <p className='font-medium'>
                                    {t('scheduleMeetingAction')}
                                </p>
                                <p className='text-xs text-text-subtle'>
                                    {t('scheduleMeetingActionDescription')}
                                </p>
                            </div>
                        </Link>
                    </DropdownMenuItem>
                </DropdownMenuContent>
            </DropdownMenu>

            <InstantMeetingDialog
                onClose={() => setInstantOpen(false)}
                open={instantOpen}
            />
        </>
    );
}
