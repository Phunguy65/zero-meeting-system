'use client';

import {
    useLocalParticipant,
    useParticipantTracks,
    VideoTrack,
} from '@livekit/components-react';
import { Track } from 'livekit-client';
import { useTranslations } from 'next-intl';

function SelfInitialsAvatar({ name }: { name: string }) {
    const initials = name
        .split(' ')
        .map((part) => part[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();

    return (
        <div className='flex h-full w-full items-center justify-center bg-[linear-gradient(160deg,var(--tile-bg-navy-start)_0%,var(--tile-bg-navy-mid)_50%,var(--tile-bg-navy-end)_100%)]'>
            <div className='flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br from-[var(--avatar-gradient-navy-start)] to-[var(--avatar-gradient-navy-end)] text-base font-semibold text-white'>
                {initials}
            </div>
        </div>
    );
}

/**
 * Floating self-view preview showing local camera video when enabled
 * or an initials fallback when the camera is off.
 */
export function SelfView() {
    const t = useTranslations('meetingRoom');
    const { localParticipant, isCameraEnabled } = useLocalParticipant();
    const cameraTracks = useParticipantTracks(
        [Track.Source.Camera],
        localParticipant.identity,
    );
    const cameraTrack = cameraTracks[0];

    const displayName = localParticipant.name ?? localParticipant.identity;

    return (
        <section
            aria-label={t('selfViewLabel')}
            className='absolute bottom-5 right-5 z-10 h-28 w-48 overflow-hidden rounded-2xl border border-border/40 bg-meeting-bg shadow-[0_8px_24px_-8px_rgba(0,0,0,0.5)]'
        >
            {isCameraEnabled && cameraTrack ? (
                <VideoTrack
                    className='h-full w-full object-cover'
                    playsInline
                    trackRef={cameraTrack}
                />
            ) : (
                <SelfInitialsAvatar name={displayName} />
            )}

            <div className='absolute bottom-1.5 left-2 rounded bg-black/50 px-1.5 py-0.5'>
                <span className='text-xs font-medium text-white'>
                    {t('you')}
                </span>
            </div>
        </section>
    );
}
