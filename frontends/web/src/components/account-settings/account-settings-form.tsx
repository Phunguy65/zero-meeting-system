'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import type { UserManagementUserResponse } from '@/generated/types.gen.ts';
import type { AccountProfileFormValues } from './schema.ts';
import {
    AVATAR_ALLOWED_TYPES,
    AVATAR_MAX_SIZE_BYTES,
    accountProfileSchema,
} from './schema.ts';
import type { SavePhase } from './types.ts';

type AccountSettingsFormProps = {
    profile: UserManagementUserResponse;
    saveState: SavePhase;
    saveErrorMessage: string | null;
    onSave: (
        payload: {
            fullName: string;
            username: string;
            avatarUrl?: string;
        },
        errorFallback: string,
    ) => Promise<void>;
};

function getInitials(name: string | undefined): string {
    if (!name) return '?';
    return name
        .split(' ')
        .map((part) => part[0] ?? '')
        .join('')
        .toUpperCase()
        .slice(0, 2);
}

function AvatarPreview({
    profile,
    previewUrl,
}: {
    profile: UserManagementUserResponse;
    previewUrl: string | null;
}) {
    const t = useTranslations('workspace.accountSettings');

    if (previewUrl) {
        return (
            // biome-ignore lint/performance/noImgElement: Avatar preview requires direct object URL
            <img
                alt={t('avatarPreviewAlt')}
                className='h-36 w-36 rounded-full object-cover'
                src={previewUrl}
            />
        );
    }

    if (profile.avatarUrl) {
        return (
            // biome-ignore lint/performance/noImgElement: Profile avatar from backend URL
            <img
                alt={profile.fullName ?? t('avatarPreviewAlt')}
                className='h-36 w-36 rounded-full object-cover'
                src={profile.avatarUrl}
            />
        );
    }

    return (
        <div className='flex h-36 w-36 items-center justify-center rounded-full bg-[linear-gradient(135deg,#183b72_0%,#3c7fe8_100%)] text-5xl font-semibold text-white'>
            {getInitials(profile.fullName)}
        </div>
    );
}

export function AccountSettingsForm({
    profile,
    saveState,
    saveErrorMessage,
    onSave,
}: AccountSettingsFormProps) {
    const t = useTranslations('workspace.accountSettings');
    const errors = useTranslations('errors');
    const [avatarPreviewUrl, setAvatarPreviewUrl] = useState<string | null>(
        null,
    );
    const [avatarFileError, setAvatarFileError] = useState<string | null>(null);
    const avatarInputRef = useRef<HTMLInputElement | null>(null);
    const previewUrlRef = useRef<string | null>(null);

    const {
        register,
        handleSubmit,
        reset,
        setValue,
        formState: { isDirty, errors: formErrors },
    } = useForm<AccountProfileFormValues>({
        resolver: zodResolver(accountProfileSchema),
        defaultValues: {
            fullName: profile.fullName ?? '',
            username: profile.username ?? '',
            avatarUrl: profile.avatarUrl ?? '',
        },
    });

    const handleAvatarChange = useCallback(
        (event: React.ChangeEvent<HTMLInputElement>) => {
            setAvatarFileError(null);
            const file = event.target.files?.[0];
            if (!file) return;

            if (!AVATAR_ALLOWED_TYPES.includes(file.type)) {
                setAvatarFileError(t('avatarInvalidType'));
                if (avatarInputRef.current) {
                    avatarInputRef.current.value = '';
                }
                return;
            }

            if (file.size > AVATAR_MAX_SIZE_BYTES) {
                setAvatarFileError(t('avatarFileTooLarge'));
                if (avatarInputRef.current) {
                    avatarInputRef.current.value = '';
                }
                return;
            }

            if (previewUrlRef.current) {
                URL.revokeObjectURL(previewUrlRef.current);
            }
            const objectUrl = URL.createObjectURL(file);
            previewUrlRef.current = objectUrl;
            setAvatarPreviewUrl(objectUrl);
            setValue('avatarUrl', objectUrl, { shouldDirty: true });
        },
        [t, setValue],
    );

    const clearAvatarPreview = useCallback(() => {
        if (previewUrlRef.current) {
            URL.revokeObjectURL(previewUrlRef.current);
            previewUrlRef.current = null;
        }
        setAvatarPreviewUrl(null);
        setAvatarFileError(null);
        if (avatarInputRef.current) {
            avatarInputRef.current.value = '';
        }
        setValue('avatarUrl', profile.avatarUrl ?? '', { shouldDirty: true });
    }, [profile.avatarUrl, setValue]);

    useEffect(() => {
        return () => {
            if (previewUrlRef.current) {
                URL.revokeObjectURL(previewUrlRef.current);
            }
        };
    }, []);

    const onFormSubmit = handleSubmit((values) => {
        const payload = {
            fullName: values.fullName.trim(),
            username: values.username.trim(),
            avatarUrl: values.avatarUrl?.trim() || undefined,
        };
        void onSave(payload, t('saveError'));
    });

    const handleCancel = useCallback(() => {
        clearAvatarPreview();
        reset({
            fullName: profile.fullName ?? '',
            username: profile.username ?? '',
            avatarUrl: profile.avatarUrl ?? '',
        });
    }, [clearAvatarPreview, reset, profile]);

    const isSaving = saveState === 'SAVING';
    const showSuccess = saveState === 'SUCCESS';
    const showFormError = saveState === 'ERROR' && saveErrorMessage;

    return (
        <form className='space-y-8' onSubmit={onFormSubmit} noValidate>
            <div className='flex flex-col items-center text-center'>
                <AvatarPreview
                    previewUrl={avatarPreviewUrl}
                    profile={profile}
                />

                <div className='mt-4 flex flex-col items-center gap-2'>
                    <button
                        className='rounded-full border border-[#e4e7ec] bg-white px-5 py-2.5 text-sm font-medium text-[#344054] shadow-sm transition-colors hover:bg-[#f5f6fa]'
                        onClick={() => avatarInputRef.current?.click()}
                        type='button'
                    >
                        {t('changeAvatar')}
                    </button>
                    <input
                        ref={avatarInputRef}
                        accept={AVATAR_ALLOWED_TYPES.join(',')}
                        className='hidden'
                        type='file'
                        onChange={handleAvatarChange}
                    />
                    {(avatarPreviewUrl || profile.avatarUrl) && (
                        <button
                            className='text-sm text-[#dc2626] underline'
                            onClick={clearAvatarPreview}
                            type='button'
                        >
                            {t('removeAvatar')}
                        </button>
                    )}
                </div>

                {avatarFileError && (
                    <p className='mt-2 text-sm text-[#dc2626]'>
                        {avatarFileError}
                    </p>
                )}

                <p className='mt-2 text-xs text-[#98a2b3]'>{t('avatarHint')}</p>
            </div>

            <div className='space-y-5'>
                <div>
                    <label
                        className='mb-2 block text-sm font-medium text-[#344054]'
                        htmlFor='fullName'
                    >
                        {t('fullNameLabel')}
                    </label>
                    <input
                        {...register('fullName')}
                        aria-describedby={
                            formErrors.fullName ? 'fullName-error' : undefined
                        }
                        aria-invalid={formErrors.fullName ? 'true' : undefined}
                        className='w-full rounded-xl border border-[#e4e7ec] bg-white px-4 py-3 text-[#15191f] outline-none ring-1 ring-transparent transition focus:border-[#1a73e8] focus:ring-2 focus:ring-[#1a73e8]'
                        id='fullName'
                        placeholder={t('fullNamePlaceholder')}
                        type='text'
                    />
                    {formErrors.fullName && (
                        <p
                            className='mt-1.5 text-sm text-[#dc2626]'
                            id='fullName-error'
                        >
                            {formErrors.fullName.message
                            === 'validation_fullName_too_long'
                                ? errors('validation_too_long')
                                : errors('validation_required')}
                        </p>
                    )}
                </div>

                <div>
                    <label
                        className='mb-2 block text-sm font-medium text-[#344054]'
                        htmlFor='username'
                    >
                        {t('usernameLabel')}
                    </label>
                    <input
                        {...register('username')}
                        aria-describedby={
                            formErrors.username ? 'username-error' : undefined
                        }
                        aria-invalid={formErrors.username ? 'true' : undefined}
                        className='w-full rounded-xl border border-[#e4e7ec] bg-white px-4 py-3 text-[#15191f] outline-none ring-1 ring-transparent transition focus:border-[#1a73e8] focus:ring-2 focus:ring-[#1a73e8]'
                        id='username'
                        placeholder={t('usernamePlaceholder')}
                        type='text'
                    />
                    {formErrors.username && (
                        <p
                            className='mt-1.5 text-sm text-[#dc2626]'
                            id='username-error'
                        >
                            {formErrors.username.message
                            === 'validation_username_too_short'
                                ? errors('validation_too_short')
                                : formErrors.username.message
                                    === 'validation_username_too_long'
                                  ? errors('validation_too_long')
                                  : formErrors.username.message
                                      === 'validation_username_invalid_chars'
                                    ? errors('validation_invalid_format')
                                    : errors('validation_required')}
                        </p>
                    )}
                </div>

                <div>
                    <label
                        className='mb-2 block text-sm font-medium text-[#344054]'
                        htmlFor='email'
                    >
                        {t('emailLabel')}
                    </label>
                    <input
                        className='w-full cursor-not-allowed rounded-xl border border-[#e4e7ec] bg-[#f5f6fa] px-4 py-3 text-[#98a2b3] outline-none'
                        defaultValue={profile.email ?? ''}
                        disabled
                        id='email'
                        type='email'
                    />
                    <p className='mt-1.5 text-xs text-[#98a2b3]'>
                        {t('emailReadOnly')}
                    </p>
                </div>
            </div>

            {showFormError && (
                <div className='rounded-xl border border-[#fca5a5] bg-[#fef2f2] px-4 py-3'>
                    <p className='text-sm text-[#dc2626]'>{saveErrorMessage}</p>
                </div>
            )}

            {showSuccess && (
                <div className='rounded-xl border border-[#6ee7b7] bg-[#f0fdf4] px-4 py-3'>
                    <p className='text-sm text-[#15803d]'>{t('saveSuccess')}</p>
                </div>
            )}

            <div className='flex justify-end gap-3'>
                {isDirty && !isSaving && (
                    <button
                        className='rounded-xl border border-[#e4e7ec] bg-white px-6 py-3 text-sm font-medium text-[#344054] shadow-sm transition-colors hover:bg-[#f5f6fa]'
                        onClick={handleCancel}
                        type='button'
                    >
                        {t('cancel')}
                    </button>
                )}
                <button
                    className='rounded-xl bg-[#1a73e8] px-6 py-3 text-sm font-medium text-white shadow-sm transition-colors hover:bg-[#1765cc] disabled:cursor-not-allowed disabled:opacity-60'
                    disabled={isSaving || (!isDirty && !showFormError)}
                    type='submit'
                >
                    {isSaving ? t('saving') : t('saveChanges')}
                </button>
            </div>
        </form>
    );
}
