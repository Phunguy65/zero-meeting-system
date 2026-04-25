'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Loader2 } from 'lucide-react';
import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { useForm } from 'react-hook-form';
import { Button } from '@/components/ui/button.tsx';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';
import { Form } from '@/components/ui/form.tsx';
import {
    type InstantMeetingValues,
    instantMeetingSchema,
    MEETING_SETTINGS_DEFAULTS,
} from '@/lib/schemas/meeting.ts';
import { MeetingSettingsForm } from './meeting-settings-form.tsx';
import { SuccessDialog } from './success-dialog.tsx';
import { useCreateMeeting } from './use-create-meeting.ts';

const MEETING_TOKEN_KEY = 'meeting_token';
const MEETING_ROOM_KEY = 'meeting_room_name';

type InstantMeetingDialogProps = {
    open: boolean;
    onClose: () => void;
};

export function InstantMeetingDialog({
    open,
    onClose,
}: InstantMeetingDialogProps) {
    const t = useTranslations('createMeeting');
    const locale = useLocale();
    const router = useRouter();
    const { state, create, retry, reset } = useCreateMeeting();

    const form = useForm<InstantMeetingValues>({
        resolver: zodResolver(instantMeetingSchema),
        defaultValues: {
            title: '',
            settings: MEETING_SETTINGS_DEFAULTS,
        },
    });

    async function handleSubmit(values: InstantMeetingValues) {
        await create(values);
    }

    function handleNavigate() {
        if (state.phase === 'READY') {
            sessionStorage.setItem(MEETING_TOKEN_KEY, state.token);
            sessionStorage.setItem(MEETING_ROOM_KEY, state.roomName);
        }
        router.push(`/${locale}/workspace/meeting-room`);
    }

    function handleClose() {
        reset();
        form.reset();
        onClose();
    }

    const isCreating = state.phase === 'CREATING' || state.phase === 'STARTING';
    const isReady = state.phase === 'READY';

    if (isReady) {
        return (
            <SuccessDialog
                onNavigate={handleNavigate}
                open
                shortCode={state.shortCode}
            />
        );
    }

    return (
        <Dialog onOpenChange={(v) => !v && handleClose()} open={open}>
            <DialogContent className='max-w-lg'>
                <DialogHeader>
                    <DialogTitle>{t('instantTitle')}</DialogTitle>
                    <DialogDescription>
                        {t('instantDescription')}
                    </DialogDescription>
                </DialogHeader>

                {state.phase === 'ERROR' && (
                    <div className='rounded-xl border border-error/40 bg-error-subtle px-4 py-3'>
                        <p className='text-sm text-error-dark'>
                            {state.message}
                        </p>
                        {state.retryable && (
                            <Button
                                className='mt-2'
                                onClick={retry}
                                size='sm'
                                type='button'
                                variant='outline'
                            >
                                {t('retry')}
                            </Button>
                        )}
                    </div>
                )}

                <Form {...form}>
                    <form
                        className='space-y-5'
                        onSubmit={form.handleSubmit(handleSubmit)}
                    >
                        <div className='flex flex-col gap-2'>
                            <label
                                className='text-sm font-medium text-text-dark'
                                htmlFor='instant-title'
                            >
                                {t('titleLabel')}
                            </label>
                            <input
                                className='h-11 w-full rounded-xl border border-border-input bg-surface-input px-4 text-sm text-text-primary outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                                id='instant-title'
                                placeholder={t('titlePlaceholder')}
                                type='text'
                                {...form.register('title')}
                            />
                        </div>

                        <MeetingSettingsForm form={form} />

                        <DialogFooter>
                            <Button
                                onClick={handleClose}
                                type='button'
                                variant='outline'
                            >
                                {t('cancel')}
                            </Button>
                            <Button disabled={isCreating} type='submit'>
                                {isCreating && (
                                    <Loader2 className='h-4 w-4 animate-spin' />
                                )}
                                {isCreating ? t('creating') : t('startMeeting')}
                            </Button>
                        </DialogFooter>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
