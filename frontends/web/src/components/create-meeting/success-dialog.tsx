'use client';

import { Check, Copy } from 'lucide-react';
import { useLocale, useTranslations } from 'next-intl';
import { useEffect, useState } from 'react';
import { Button } from '@/components/ui/button.tsx';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';

const SUCCESS_REDIRECT_DELAY_MS = 3000;

type SuccessDialogProps = {
    open: boolean;
    shortCode: string;
    onNavigate: () => void;
};

export function SuccessDialog({
    open,
    shortCode,
    onNavigate,
}: SuccessDialogProps) {
    const t = useTranslations('createMeeting');
    const locale = useLocale();
    const [copied, setCopied] = useState(false);

    useEffect(() => {
        if (!open) return;
        const timer = setTimeout(onNavigate, SUCCESS_REDIRECT_DELAY_MS);
        return () => clearTimeout(timer);
    }, [open, onNavigate]);

    async function handleCopy() {
        const link = `${window.location.origin}/${locale}/join/${shortCode}`;
        await navigator.clipboard.writeText(link);
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
    }

    return (
        <Dialog open={open}>
            <DialogContent className='max-w-sm text-center'>
                <DialogHeader className='items-center'>
                    <div className='mb-3 flex h-14 w-14 items-center justify-center rounded-full bg-primary/10'>
                        <Check className='h-7 w-7 text-primary' />
                    </div>
                    <DialogTitle>{t('successTitle')}</DialogTitle>
                    <DialogDescription>
                        {t('successDescription')}
                    </DialogDescription>
                </DialogHeader>

                <div className='rounded-xl bg-surface-input px-5 py-4 text-center'>
                    <p className='text-xs font-medium uppercase tracking-widest text-text-subtle'>
                        {t('meetingCode')}
                    </p>
                    <p className='mt-1 text-2xl font-semibold tracking-wider text-text-dark'>
                        {shortCode}
                    </p>
                </div>

                <Button
                    className='mt-2 w-full gap-2'
                    onClick={() => void handleCopy()}
                    type='button'
                    variant='outline'
                >
                    {copied ? (
                        <Check className='h-4 w-4' />
                    ) : (
                        <Copy className='h-4 w-4' />
                    )}
                    {copied ? t('linkCopied') : t('copyLink')}
                </Button>

                <p className='text-xs text-text-subtle'>
                    {t('redirectingMessage')}
                </p>
            </DialogContent>
        </Dialog>
    );
}
