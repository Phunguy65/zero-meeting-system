'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { Mic, MicOff, Video, VideoOff } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useEffect, useMemo, useState } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { Button } from '@/components/ui/button.tsx';
import type { JoinMode, JoinState } from './use-join-meeting.ts';

type InitialStepValues = {
    code: string;
    displayName: string;
};

type PasswordStepValues = {
    displayName: string;
    password: string;
};

export function buildInitialStepSchema(requiredMessage: string) {
    return z.object({
        code: z.string().min(1, requiredMessage),
        displayName: z.string().min(1, requiredMessage),
    });
}

export function buildPasswordStepSchema(requiredMessage: string) {
    return z.object({
        displayName: z.string().min(1, requiredMessage),
        password: z.string().min(1, requiredMessage),
    });
}

type JoinFormProps = {
    mode: JoinMode;
    initialCode?: string;
    initialDisplayName?: string;
    state: JoinState;
    onSubmit: (params: {
        code: string;
        displayName: string;
        password?: string;
    }) => void;
    onSubmitPassword: (params: {
        displayName: string;
        password: string;
    }) => void;
};

export function JoinMeetingForm({
    mode,
    initialCode = '',
    initialDisplayName = '',
    state,
    onSubmit,
    onSubmitPassword,
}: JoinFormProps) {
    const t = useTranslations('joinMeeting');
    const [micEnabled, setMicEnabled] = useState(true);
    const [videoEnabled, setVideoEnabled] = useState(true);

    const isNeedsPassword = state.phase === 'NEEDS_PASSWORD';
    const isLoading =
        state.phase === 'LOOKING_UP' || state.phase === 'REQUESTING';

    const initialStepSchema = useMemo(
        () => buildInitialStepSchema(t('validation.required')),
        [t],
    );

    const passwordStepSchema = useMemo(
        () => buildPasswordStepSchema(t('validation.required')),
        [t],
    );

    const initialForm = useForm<InitialStepValues>({
        resolver: zodResolver(initialStepSchema),
        defaultValues: {
            code: initialCode,
            displayName: initialDisplayName,
        },
    });

    const passwordForm = useForm<PasswordStepValues>({
        resolver: zodResolver(passwordStepSchema),
        defaultValues: {
            displayName: initialDisplayName,
            password: '',
        },
    });

    useEffect(() => {
        if (initialDisplayName) {
            initialForm.setValue('displayName', initialDisplayName);
            passwordForm.setValue('displayName', initialDisplayName);
        }
    }, [initialDisplayName, initialForm, passwordForm]);

    const passwordError =
        state.phase === 'NEEDS_PASSWORD' && state.error === 'INVALID_PASSWORD'
            ? t('errors.invalidPassword')
            : undefined;

    function handleInitialSubmit(values: InitialStepValues) {
        onSubmit({
            code: values.code.trim(),
            displayName: values.displayName.trim(),
            password: undefined,
        });
    }

    function handlePasswordSubmit(values: PasswordStepValues) {
        onSubmitPassword({
            displayName: values.displayName,
            password: values.password,
        });
    }

    return (
        <div className='flex flex-col gap-6'>
            <div className='relative aspect-[1.6] w-full overflow-hidden rounded-[1.7rem] bg-[linear-gradient(135deg,_#111827_0%,_#2b313b_32%,_#111827_100%)] shadow-[0_26px_70px_-38px_rgba(15,23,42,0.35)]'>
                <div className='absolute inset-0 bg-[radial-gradient(circle_at_40%_50%,_rgba(255,213,128,0.12),_transparent_28%),linear-gradient(90deg,_rgba(0,0,0,0.52)_0%,_rgba(0,0,0,0.12)_45%,_rgba(0,0,0,0.62)_100%)]' />
                <span className='absolute left-6 top-6 rounded-2xl bg-black/38 px-4 py-1.5 text-[0.95rem] font-medium text-white backdrop-blur'>
                    {t('preview')}
                </span>

                <div className='absolute bottom-6 left-1/2 flex -translate-x-1/2 items-center gap-4 rounded-[2rem] bg-white/88 px-6 py-4 shadow-[0_24px_60px_-36px_rgba(15,23,42,0.55)] backdrop-blur'>
                    <button
                        aria-label={micEnabled ? t('muteMic') : t('unmuteMic')}
                        className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-text-primary transition-colors ${
                            micEnabled
                                ? 'bg-surface-input'
                                : 'bg-error-subtle text-error-dark'
                        }`}
                        onClick={() => setMicEnabled((v) => !v)}
                        type='button'
                    >
                        <span className='flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm'>
                            {micEnabled ? (
                                <Mic className='h-7 w-7' />
                            ) : (
                                <MicOff className='h-7 w-7' />
                            )}
                        </span>
                        <span className='text-[0.8rem] font-medium uppercase tracking-[0.12em]'>
                            {t('mic')}
                        </span>
                    </button>

                    <button
                        aria-label={
                            videoEnabled ? t('stopVideo') : t('startVideo')
                        }
                        className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-text-primary transition-colors ${
                            videoEnabled
                                ? 'bg-surface-input'
                                : 'bg-error-subtle text-error-dark'
                        }`}
                        onClick={() => setVideoEnabled((v) => !v)}
                        type='button'
                    >
                        <span className='flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm'>
                            {videoEnabled ? (
                                <Video className='h-7 w-7' />
                            ) : (
                                <VideoOff className='h-7 w-7' />
                            )}
                        </span>
                        <span className='text-[0.8rem] font-medium uppercase tracking-[0.12em]'>
                            {t('video')}
                        </span>
                    </button>
                </div>
            </div>

            {isNeedsPassword ? (
                <form
                    className='flex flex-col gap-4'
                    onSubmit={passwordForm.handleSubmit(handlePasswordSubmit)}
                >
                    <p className='text-base text-text-secondary'>
                        {t('passwordRequired')}
                    </p>
                    <div className='flex flex-col gap-1'>
                        <label
                            className='text-sm font-medium text-text-dark'
                            htmlFor='join-password'
                        >
                            {t('passwordLabel')}
                        </label>
                        <input
                            autoComplete='current-password'
                            className='h-12 w-full rounded-xl border border-border-input bg-surface px-4 text-text-darkest outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                            id='join-password'
                            placeholder={t('passwordPlaceholder')}
                            type='password'
                            {...passwordForm.register('password')}
                        />
                        {passwordError && (
                            <p className='text-sm text-error-dark' role='alert'>
                                {passwordError}
                            </p>
                        )}
                        {passwordForm.formState.errors.password && (
                            <p className='text-sm text-error-dark' role='alert'>
                                {passwordForm.formState.errors.password.message}
                            </p>
                        )}
                    </div>
                    <Button
                        className='h-14 w-full rounded-xl text-base font-semibold'
                        disabled={isLoading}
                        type='submit'
                    >
                        {isLoading ? t('joining') : t('submitPassword')}
                    </Button>
                </form>
            ) : (
                <form
                    className='flex flex-col gap-4'
                    onSubmit={initialForm.handleSubmit(handleInitialSubmit)}
                >
                    <div className='flex flex-col gap-1'>
                        <label
                            className='text-sm font-medium text-text-dark'
                            htmlFor='join-code'
                        >
                            {t('codeLabel')}
                        </label>
                        <input
                            autoComplete='off'
                            className='h-12 w-full rounded-xl border border-border-input bg-surface px-4 text-text-darkest outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                            id='join-code'
                            placeholder={t('codePlaceholder')}
                            type='text'
                            {...initialForm.register('code')}
                        />
                        {initialForm.formState.errors.code && (
                            <p className='text-sm text-error-dark' role='alert'>
                                {initialForm.formState.errors.code.message}
                            </p>
                        )}
                    </div>

                    {(mode === 'guest' || !initialDisplayName) && (
                        <div className='flex flex-col gap-1'>
                            <label
                                className='text-sm font-medium text-text-dark'
                                htmlFor='join-display-name'
                            >
                                {t('displayNameLabel')}
                            </label>
                            <input
                                autoComplete='nickname'
                                className='h-12 w-full rounded-xl border border-border-input bg-surface px-4 text-text-darkest outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                                id='join-display-name'
                                placeholder={t('displayNamePlaceholder')}
                                type='text'
                                {...initialForm.register('displayName')}
                            />
                            {initialForm.formState.errors.displayName && (
                                <p
                                    className='text-sm text-error-dark'
                                    role='alert'
                                >
                                    {
                                        initialForm.formState.errors.displayName
                                            .message
                                    }
                                </p>
                            )}
                        </div>
                    )}

                    <Button
                        className='h-14 w-full rounded-xl text-base font-semibold'
                        disabled={isLoading}
                        type='submit'
                    >
                        {isLoading ? t('joining') : t('join')}
                    </Button>
                </form>
            )}
        </div>
    );
}
