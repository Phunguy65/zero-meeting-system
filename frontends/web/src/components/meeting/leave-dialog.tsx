'use client';

import { useRoomContext } from '@livekit/components-react';
import { useLocale, useTranslations } from 'next-intl';
import { useCallback } from 'react';
import { Button } from '@/components/ui/button.tsx';
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from '@/components/ui/dialog.tsx';

type LeaveDialogProps = {
    open: boolean;
    onOpenChange: (open: boolean) => void;
};

/**
 * Confirmation dialog that disconnects the LiveKit room before navigating
 * the user back to the workspace.
 */
export function LeaveDialog({ open, onOpenChange }: LeaveDialogProps) {
    const t = useTranslations('meetingRoom');
    const locale = useLocale();
    const room = useRoomContext();

    const handleConfirmLeave = useCallback(async () => {
        try {
            await room.disconnect();
        } finally {
            window.location.href = `/${locale}/workspace`;
        }
    }, [room, locale]);

    return (
        <Dialog onOpenChange={onOpenChange} open={open}>
            <DialogContent className='max-w-sm'>
                <DialogHeader>
                    <DialogTitle>{t('leaveDialogTitle')}</DialogTitle>
                </DialogHeader>

                <p className='text-sm text-text-secondary'>
                    {t('leaveDialogMessage')}
                </p>

                <DialogFooter>
                    <Button
                        onClick={() => onOpenChange(false)}
                        type='button'
                        variant='outline'
                    >
                        {t('leaveDialogCancel')}
                    </Button>
                    <Button
                        onClick={handleConfirmLeave}
                        type='button'
                        variant='destructive'
                    >
                        {t('leaveDialogConfirm')}
                    </Button>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    );
}
