'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Calendar, Clock, Loader2 } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { InviteeInput } from '@/components/create-meeting/invitee-input.tsx';
import { MeetingSettingsForm } from '@/components/create-meeting/meeting-settings-form.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';
import { Form } from '@/components/ui/form.tsx';
import { WorkspaceShell } from '@/components/workspace-shell.tsx';
import { scheduleMeeting } from '@/generated/sdk.gen.ts';
import { ApiError, ApiFailError } from '@/lib/api/types.ts';
import {
    MEETING_SETTINGS_DEFAULTS,
    mapSettingsToRequest,
    type ScheduleMeetingValues,
    scheduleMeetingSchema,
} from '@/lib/schemas/meeting.ts';

const DURATION_OPTIONS = [15, 30, 45, 60, 90, 120];

type ScheduleSuccessState = {
    shortCode: string;
    title: string;
    startTime: string;
};

export function WorkspaceScheduleScreen() {
    const t = useTranslations('workspace.schedule');
    const [successState, setSuccessState] =
        useState<ScheduleSuccessState | null>(null);
    const [serverError, setServerError] = useState<string | null>(null);

    const form = useForm<ScheduleMeetingValues>({
        resolver: zodResolver(scheduleMeetingSchema),
        defaultValues: {
            title: '',
            description: '',
            date: '',
            time: '',
            durationMinutes: 60,
            invitees: [],
            settings: MEETING_SETTINGS_DEFAULTS,
        },
    });

    async function handleSubmit(values: ScheduleMeetingValues) {
        setServerError(null);

        const startTime = new Date(`${values.date}T${values.time}`);
        const endTime = new Date(
            startTime.getTime() + values.durationMinutes * 60_000,
        );

        try {
            const { data } = await scheduleMeeting({
                body: {
                    title: values.title ?? undefined,
                    description: values.description ?? undefined,
                    startTime: startTime.toISOString(),
                    endTime: endTime.toISOString(),
                    settings: mapSettingsToRequest(values.settings),
                    invitees:
                        values.invitees.length > 0
                            ? values.invitees.map((email) => ({ email }))
                            : undefined,
                },
                throwOnError: true,
            });

            setSuccessState({
                shortCode: data?.shortCode ?? '',
                title: data?.title ?? values.title ?? '',
                startTime: startTime.toLocaleString(),
            });
            form.reset();
        } catch (error) {
            if (error instanceof ApiFailError) {
                setServerError(error.message);
            } else if (error instanceof ApiError) {
                setServerError(t('errorServer'));
            } else {
                setServerError(t('errorNetwork'));
            }
        }
    }

    const isSubmitting = form.formState.isSubmitting;

    return (
        <WorkspaceShell activeTab='schedule' rightMode='search'>
            <section className='mx-auto max-w-[1280px]'>
                <div className='max-w-[720px]'>
                    <h1 className='text-5xl font-semibold tracking-tight text-text-dark'>
                        {t('headline')}
                    </h1>
                    <p className='mt-4 text-2xl leading-9 text-text-secondary sm:text-[1.1rem]'>
                        {t('description')}
                    </p>
                </div>

                <div className='mt-12 grid gap-8 xl:grid-cols-[1.12fr_0.78fr]'>
                    <article className='rounded-[2rem] bg-surface p-8 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.2)] sm:p-10'>
                        {serverError && (
                            <div className='mb-6 rounded-xl border border-error/40 bg-error-subtle px-5 py-4'>
                                <p
                                    className='text-base text-error-dark'
                                    role='alert'
                                >
                                    {serverError}
                                </p>
                            </div>
                        )}

                        <Form {...form}>
                            <form
                                className='space-y-8'
                                onSubmit={form.handleSubmit(handleSubmit)}
                            >
                                <div className='flex flex-col gap-3'>
                                    <label
                                        className='text-[1.6rem] font-semibold tracking-tight text-text-dark'
                                        htmlFor='schedule-title'
                                    >
                                        {t('topicLabel')}
                                    </label>
                                    <input
                                        className='h-16 w-full rounded-[1.2rem] bg-surface-input px-6 text-xl text-text-primary outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-primary'
                                        id='schedule-title'
                                        placeholder={t('topicPlaceholder')}
                                        type='text'
                                        {...form.register('title')}
                                    />
                                </div>

                                <div className='grid gap-6 sm:grid-cols-2'>
                                    <div className='flex flex-col gap-3'>
                                        <label
                                            className='text-[1.45rem] font-semibold tracking-tight text-text-dark'
                                            htmlFor='schedule-date'
                                        >
                                            {t('dateLabel')}
                                        </label>
                                        <div className='flex h-16 items-center justify-between rounded-[1.2rem] bg-surface-input px-6'>
                                            <input
                                                className='w-full bg-transparent text-xl text-text-primary outline-none'
                                                id='schedule-date'
                                                type='date'
                                                {...form.register('date')}
                                            />
                                            <span className='text-text-muted'>
                                                <Calendar className='h-6 w-6' />
                                            </span>
                                        </div>
                                        {form.formState.errors.date && (
                                            <p
                                                className='text-sm text-error-dark'
                                                role='alert'
                                            >
                                                {form.formState.errors.date
                                                    .message
                                                === 'startTimeMustBeFuture'
                                                    ? t(
                                                          'validation.startTimeMustBeFuture',
                                                      )
                                                    : form.formState.errors.date
                                                          .message}
                                            </p>
                                        )}
                                    </div>

                                    <div className='flex flex-col gap-3'>
                                        <label
                                            className='text-[1.45rem] font-semibold tracking-tight text-text-dark'
                                            htmlFor='schedule-time'
                                        >
                                            {t('timeLabel')}
                                        </label>
                                        <div className='flex h-16 items-center justify-between rounded-[1.2rem] bg-surface-input px-6'>
                                            <input
                                                className='w-full bg-transparent text-xl text-text-primary outline-none'
                                                id='schedule-time'
                                                type='time'
                                                {...form.register('time')}
                                            />
                                            <span className='text-text-muted'>
                                                <Clock className='h-6 w-6' />
                                            </span>
                                        </div>
                                        {form.formState.errors.time && (
                                            <p
                                                className='text-sm text-error-dark'
                                                role='alert'
                                            >
                                                {
                                                    form.formState.errors.time
                                                        .message
                                                }
                                            </p>
                                        )}
                                    </div>
                                </div>

                                <div className='flex flex-col gap-3'>
                                    <label
                                        className='text-[1.45rem] font-semibold tracking-tight text-text-dark'
                                        htmlFor='schedule-duration'
                                    >
                                        {t('durationLabel')}
                                    </label>
                                    <div className='flex h-16 items-center rounded-[1.2rem] bg-surface-input px-6'>
                                        <select
                                            className='w-full appearance-none bg-transparent text-xl text-text-primary outline-none'
                                            id='schedule-duration'
                                            {...form.register(
                                                'durationMinutes',
                                                {
                                                    valueAsNumber: true,
                                                },
                                            )}
                                        >
                                            {DURATION_OPTIONS.map((minutes) => (
                                                <option
                                                    key={minutes}
                                                    value={minutes}
                                                >
                                                    {t(
                                                        `duration${minutes}` as Parameters<
                                                            typeof t
                                                        >[0],
                                                    )}
                                                </option>
                                            ))}
                                        </select>
                                    </div>
                                </div>

                                <div className='flex flex-col gap-3'>
                                    {/* biome-ignore lint/a11y/noLabelWithoutControl: InviteeInput is a composite widget; its inner input is focused via the container click handler */}
                                    <label className='text-[1.45rem] font-semibold tracking-tight text-text-dark'>
                                        {t('inviteesLabel')}
                                    </label>
                                    <InviteeInput
                                        onChange={(emails) =>
                                            form.setValue('invitees', emails)
                                        }
                                        value={form.watch('invitees')}
                                    />
                                </div>

                                <MeetingSettingsForm form={form} />

                                <Button
                                    className='h-18 w-full rounded-[1.2rem] text-[1.9rem] font-semibold'
                                    disabled={isSubmitting}
                                    type='submit'
                                >
                                    {isSubmitting && (
                                        <Loader2 className='h-5 w-5 animate-spin' />
                                    )}
                                    {t('submit')}
                                </Button>
                            </form>
                        </Form>
                    </article>

                    <div className='space-y-8'>
                        <article className='overflow-hidden rounded-[2rem] bg-[linear-gradient(145deg,_#20444d_0%,_#18353e_45%,_#132b33_100%)] shadow-[0_26px_70px_-38px_rgba(15,23,42,0.2)]'>
                            <div className='relative min-h-[320px] p-8'>
                                <div className='absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/40 to-transparent' />
                                <div className='absolute left-10 top-16 h-40 w-1 bg-[#8597a1]/40' />
                                <div className='absolute left-10 top-16 h-20 w-20 rounded-full border-4 border-[#96a8b2]/40 border-l-transparent border-b-transparent' />
                                <div className='absolute left-10 top-52 h-6 w-20 rounded-full bg-[#8e7558]' />
                                <div className='absolute bottom-16 left-10 h-4 w-32 rounded-full bg-[#6b553d]' />
                                <div className='absolute right-10 top-12 h-32 w-28 rounded-[1.2rem] border-4 border-[#5c5042] bg-[linear-gradient(180deg,_#243b44,_#132b33)]' />
                                <div className='absolute right-20 top-24 h-12 w-12 rounded-full border-[6px] border-[#a9aca4]' />
                                <div className='absolute right-24 top-38 h-4 w-8 rounded-full bg-[#a9aca4]' />
                                <div className='absolute right-16 bottom-12 h-34 w-46 rounded-[1.4rem] bg-[linear-gradient(160deg,_#f0f1f4_0%,_#c7ccd5_100%)] shadow-lg' />
                                <div className='absolute bottom-10 right-8 h-5 w-20 rounded-full bg-[#525b63]' />
                                <p className='absolute bottom-8 left-8 max-w-[260px] text-xl font-medium text-white'>
                                    {t('visualCaption')}
                                </p>
                            </div>
                        </article>

                        <article className='rounded-[2rem] bg-surface-input p-8 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.16)]'>
                            <p className='text-lg leading-8 text-text-secondary'>
                                {t('note')}
                            </p>
                        </article>
                    </div>
                </div>

                <footer className='mt-12 text-center text-[1.2rem] text-text-subtle'>
                    {t('footer')}
                </footer>
            </section>

            {successState && (
                <Dialog
                    onOpenChange={(open) => !open && setSuccessState(null)}
                    open
                >
                    <DialogContent className='max-w-sm text-center'>
                        <DialogHeader className='items-center'>
                            <DialogTitle>{t('successTitle')}</DialogTitle>
                            <DialogDescription>
                                {t('successDescription', {
                                    title: successState.title,
                                    startTime: successState.startTime,
                                })}
                            </DialogDescription>
                        </DialogHeader>
                        <div className='rounded-xl bg-surface-input px-5 py-4'>
                            <p className='text-xs font-medium uppercase tracking-widest text-text-subtle'>
                                {t('meetingCode')}
                            </p>
                            <p className='mt-1 text-2xl font-semibold tracking-wider text-text-dark'>
                                {successState.shortCode}
                            </p>
                        </div>
                        <Button
                            className='mt-2 w-full'
                            onClick={() => setSuccessState(null)}
                            type='button'
                        >
                            {t('successDone')}
                        </Button>
                    </DialogContent>
                </Dialog>
            )}
        </WorkspaceShell>
    );
}
