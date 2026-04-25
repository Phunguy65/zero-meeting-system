'use client';

import { Lock, MessageSquare, Mic, Monitor, Users, Video } from 'lucide-react';
import { useTranslations } from 'next-intl';
import type { UseFormReturn } from 'react-hook-form';
import {
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/components/ui/form.tsx';
import { Switch } from '@/components/ui/switch.tsx';

type MeetingSettingsFormProps = {
    // biome-ignore lint/suspicious/noExplicitAny: form types are co-/contra-variant; callers with superset field types need this cast
    form: UseFormReturn<any>;
};

export function MeetingSettingsForm({ form }: MeetingSettingsFormProps) {
    const t = useTranslations('meetingSettings');

    return (
        <div className='space-y-4'>
            <h3 className='text-base font-semibold text-text-dark'>
                {t('title')}
            </h3>

            <div className='space-y-3'>
                <FormField
                    control={form.control}
                    name='settings.waitingRoom'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Users
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('waitingRoom')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('waitingRoomDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.allowGuest'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Users
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('allowGuest')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('allowGuestDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.allowScreenShare'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Monitor
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('screenShare')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('screenShareDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.chatEnabled'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <MessageSquare
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('chat')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('chatDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.allowMicrophone'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Mic
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('microphone')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('microphoneDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.allowVideo'
                    render={({ field }) => (
                        <FormItem>
                            <div className='flex items-center justify-between rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Video
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <div>
                                        <FormLabel className='text-sm font-semibold text-text-dark'>
                                            {t('video')}
                                        </FormLabel>
                                        <p className='text-xs text-text-subtle'>
                                            {t('videoDescription')}
                                        </p>
                                    </div>
                                </div>
                                <FormControl>
                                    <Switch
                                        checked={field.value}
                                        onCheckedChange={field.onChange}
                                    />
                                </FormControl>
                            </div>
                            <FormMessage />
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.maxParticipants'
                    render={({ field }) => (
                        <FormItem>
                            <div className='rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Users
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <FormLabel className='text-sm font-semibold text-text-dark'>
                                        {t('maxParticipants')}
                                    </FormLabel>
                                </div>
                                <FormControl>
                                    <input
                                        className='mt-3 h-10 w-full rounded-lg border border-border-input bg-surface px-3 text-sm text-text-primary outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                                        max={500}
                                        min={2}
                                        onChange={(e) =>
                                            field.onChange(
                                                Number(e.target.value),
                                            )
                                        }
                                        type='number'
                                        value={field.value}
                                    />
                                </FormControl>
                                <FormMessage />
                            </div>
                        </FormItem>
                    )}
                />

                <FormField
                    control={form.control}
                    name='settings.password'
                    render={({ field }) => (
                        <FormItem>
                            <div className='rounded-xl bg-surface-input p-4'>
                                <div className='flex items-center gap-3'>
                                    <Lock
                                        aria-hidden='true'
                                        className='h-5 w-5 text-primary'
                                    />
                                    <FormLabel className='text-sm font-semibold text-text-dark'>
                                        {t('password')}
                                    </FormLabel>
                                </div>
                                <FormControl>
                                    <input
                                        className='mt-3 h-10 w-full rounded-lg border border-border-input bg-surface px-3 text-sm text-text-primary outline-none ring-transparent transition focus:ring-2 focus:ring-primary'
                                        onChange={field.onChange}
                                        placeholder={t('passwordPlaceholder')}
                                        type='password'
                                        value={field.value ?? ''}
                                    />
                                </FormControl>
                                <FormMessage />
                            </div>
                        </FormItem>
                    )}
                />
            </div>
        </div>
    );
}
