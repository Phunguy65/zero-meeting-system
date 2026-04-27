'use client';

import { CheckCheck, Loader2, RefreshCw, UserCheck, UserX } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Sheet,
    SheetContent,
    SheetHeader,
    SheetTitle,
} from '@/components/ui/sheet.tsx';
import type { MeetingManagementJoinRequestResponse } from '@/generated/types.gen.ts';

type WaitingRoomSheetProps = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
    requests: MeetingManagementJoinRequestResponse[];
    pendingCount: number;
    isLoading: boolean;
    error: string | null;
    onApprove: (requestId: string) => Promise<void>;
    onDeny: (requestId: string) => Promise<void>;
    onApproveAll: () => Promise<void>;
    onRefresh: () => void;
};

function RequestRow({
    request,
    onApprove,
    onDeny,
}: {
    request: MeetingManagementJoinRequestResponse;
    onApprove: (id: string) => Promise<void>;
    onDeny: (id: string) => Promise<void>;
}) {
    const t = useTranslations('meetingRoom');
    if (!request.id) return null;

    const name = request.displayName ?? request.id;
    const initials = name
        .split(' ')
        .map((p) => p[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();

    return (
        <div className='flex items-center gap-3 rounded-xl bg-surface-input px-4 py-3'>
            <div className='flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[var(--avatar-gradient-navy-start)] to-[var(--avatar-gradient-navy-end)] text-sm font-semibold text-white'>
                {initials}
            </div>
            <span className='min-w-0 flex-1 truncate text-sm font-medium text-text-dark'>
                {name}
            </span>
            <button
                aria-label={t('waitingRoomApprove', { name })}
                className='flex h-8 w-8 items-center justify-center rounded-full text-green-600 transition-colors hover:bg-green-50'
                onClick={() => onApprove(request.id ?? '')}
                type='button'
            >
                <UserCheck className='h-4 w-4' />
            </button>
            <button
                aria-label={t('waitingRoomDeny', { name })}
                className='flex h-8 w-8 items-center justify-center rounded-full text-red-500 transition-colors hover:bg-red-50'
                onClick={() => onDeny(request.id ?? '')}
                type='button'
            >
                <UserX className='h-4 w-4' />
            </button>
        </div>
    );
}

/**
 * Slide-out sheet for host waiting room management.
 * Renders loading, error, empty, and populated states.
 */
export function WaitingRoomSheet({
    open,
    onOpenChange,
    requests,
    pendingCount,
    isLoading,
    error,
    onApprove,
    onDeny,
    onApproveAll,
    onRefresh,
}: WaitingRoomSheetProps) {
    const t = useTranslations('meetingRoom');

    const pendingRequests = requests.filter((r) => r.status === 'PENDING');

    return (
        <Sheet onOpenChange={onOpenChange} open={open}>
            <SheetContent className='flex flex-col gap-0 p-0' side='right'>
                <SheetHeader className='border-b border-border px-6 py-5'>
                    <div className='flex items-center gap-3'>
                        <SheetTitle>{t('waitingRoomTitle')}</SheetTitle>
                        {pendingCount > 0 && (
                            <Badge variant='destructive'>{pendingCount}</Badge>
                        )}
                    </div>
                </SheetHeader>

                <div className='flex flex-1 flex-col gap-4 overflow-y-auto p-5'>
                    {isLoading && (
                        <div className='flex flex-1 items-center justify-center gap-2 py-10 text-text-muted'>
                            <Loader2 className='h-5 w-5 animate-spin' />
                            <span className='text-sm'>
                                {t('waitingRoomLoading')}
                            </span>
                        </div>
                    )}

                    {!isLoading && error && (
                        <div className='flex flex-col items-center gap-3 py-10'>
                            <p className='text-sm text-error'>
                                {t('waitingRoomError')}
                            </p>
                            <Button
                                onClick={onRefresh}
                                size='sm'
                                type='button'
                                variant='outline'
                            >
                                <RefreshCw className='mr-2 h-4 w-4' />
                                {t('waitingRoomRetry')}
                            </Button>
                        </div>
                    )}

                    {!isLoading && !error && pendingRequests.length === 0 && (
                        <div className='flex flex-1 items-center justify-center py-10'>
                            <p className='text-sm text-text-muted'>
                                {t('waitingRoomEmpty')}
                            </p>
                        </div>
                    )}

                    {!isLoading && !error && pendingRequests.length > 0 && (
                        <>
                            <div className='space-y-2'>
                                {pendingRequests.map((request) => (
                                    <RequestRow
                                        key={request.id}
                                        onApprove={onApprove}
                                        onDeny={onDeny}
                                        request={request}
                                    />
                                ))}
                            </div>

                            {pendingRequests.length > 1 && (
                                <Button
                                    className='w-full'
                                    onClick={onApproveAll}
                                    type='button'
                                    variant='outline'
                                >
                                    <CheckCheck className='mr-2 h-4 w-4' />
                                    {t('waitingRoomApproveAll')}
                                </Button>
                            )}
                        </>
                    )}
                </div>
            </SheetContent>
        </Sheet>
    );
}
