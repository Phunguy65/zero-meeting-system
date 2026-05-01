'use client';

import { useTranslations } from 'next-intl';
import { useState } from 'react';
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';
import type { AccountSettingsDialogState } from './types.ts';

type DeleteAccountDialogProps = {
    dialogState: AccountSettingsDialogState;
    onClose: () => void;
    onConfirm: (errorFallback: string) => Promise<void>;
};

const DELETE_CONFIRMATION_TEXT = 'DELETE';

export function DeleteAccountDialog({
    dialogState,
    onClose,
    onConfirm,
}: DeleteAccountDialogProps) {
    const t = useTranslations('workspace.accountSettings');
    const [confirmText, setConfirmText] = useState('');

    const isOpen =
        dialogState.deletePhase === 'CONFIRMING'
        || dialogState.deletePhase === 'DELETING'
        || dialogState.deletePhase === 'ERROR';

    const isDeleting = dialogState.deletePhase === 'DELETING';
    const isConfirmEnabled =
        confirmText === DELETE_CONFIRMATION_TEXT && !isDeleting;

    const handleClose = () => {
        if (isDeleting) return;
        setConfirmText('');
        onClose();
    };

    const handleConfirm = () => {
        if (!isConfirmEnabled) return;
        void onConfirm(t('deleteError'));
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
            <DialogContent className='max-w-md'>
                <DialogHeader>
                    <DialogTitle className='text-2xl font-semibold text-[#b42318]'>
                        {t('deleteAccountTitle')}
                    </DialogTitle>
                    <DialogDescription className='text-base leading-7 text-[#475467]'>
                        {t('deleteAccountWarning')}
                    </DialogDescription>
                </DialogHeader>

                <div>
                    <label
                        className='mb-2 block text-sm font-medium text-[#344054]'
                        htmlFor='delete-confirm'
                    >
                        {t('deleteAccountConfirmPrompt', {
                            text: DELETE_CONFIRMATION_TEXT,
                        })}
                    </label>
                    <input
                        aria-label={t('deleteAccountConfirmInputLabel')}
                        autoComplete='off'
                        className='mt-3 w-full rounded-xl border border-[#e4e7ec] bg-white px-4 py-3 text-[#15191f] outline-none ring-1 ring-transparent transition focus:border-[#dc2626] focus:ring-2 focus:ring-[#dc2626]'
                        disabled={isDeleting}
                        id='delete-confirm'
                        placeholder={DELETE_CONFIRMATION_TEXT}
                        type='text'
                        value={confirmText}
                        onChange={(e) => setConfirmText(e.target.value)}
                    />
                </div>

                {dialogState.deleteErrorMessage && (
                    <div className='rounded-xl border border-[#fca5a5] bg-[#fef2f2] px-4 py-3'>
                        <p className='text-sm text-[#dc2626]'>
                            {dialogState.deleteErrorMessage}
                        </p>
                    </div>
                )}

                <DialogFooter className='mt-7 gap-3 sm:mt-0 sm:justify-end'>
                    <button
                        className='rounded-xl border border-[#e4e7ec] bg-white px-6 py-3 text-sm font-medium text-[#344054] shadow-sm transition-colors hover:bg-[#f5f6fa]'
                        disabled={isDeleting}
                        type='button'
                        onClick={handleClose}
                    >
                        {t('cancel')}
                    </button>
                    <button
                        className='rounded-xl bg-[#dc2626] px-6 py-3 text-sm font-medium text-white shadow-sm transition-colors hover:bg-[#b91c1c] disabled:cursor-not-allowed disabled:opacity-60'
                        disabled={!isConfirmEnabled}
                        type='button'
                        onClick={handleConfirm}
                    >
                        {isDeleting ? t('deleting') : t('deleteAccountAction')}
                    </button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
