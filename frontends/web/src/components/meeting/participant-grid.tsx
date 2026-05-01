'use client';

import {
    AudioTrack,
    useLocalParticipant,
    useRemoteParticipants,
    useSpeakingParticipants,
} from '@livekit/components-react';
import { Track } from 'livekit-client';
import type { MeetingLayoutMode } from '@/hooks/use-meeting-layout.ts';
import { useWindowSize } from '@/hooks/use-window-size.ts';
import { ParticipantTile } from './participant-tile.tsx';
import { SelfView } from './self-view.tsx';
import type { ParticipantViewModel } from './types.ts';

type ParticipantGridProps = {
    layoutMode: MeetingLayoutMode;
    pinnedIdentity: string | null;
};

function buildViewModel(
    participant:
        | ReturnType<typeof useLocalParticipant>['localParticipant']
        | ReturnType<typeof useRemoteParticipants>[number],
    isLocal: boolean,
): ParticipantViewModel {
    return {
        identity: participant.identity,
        displayName: participant.name ?? participant.identity,
        isMicEnabled: participant.isMicrophoneEnabled,
        isCameraEnabled: participant.isCameraEnabled,
        isLocal,
        livekitParticipant: participant,
    };
}

function columnClass(cols: number): string {
    const classes: Record<number, string> = {
        1: 'grid-cols-1',
        2: 'grid-cols-2',
        3: 'grid-cols-3',
        4: 'grid-cols-4',
    };
    return classes[cols] ?? 'grid-cols-2';
}

function autoColumns(count: number, width: number): number {
    if (width < 480) return 1;
    if (width < 768) return Math.min(2, count);
    if (count <= 1) return 1;
    if (count <= 4) return 2;
    if (count <= 9) return 3;
    return 4;
}

function RemoteAudio() {
    const remoteParticipants = useRemoteParticipants();
    return (
        <>
            {remoteParticipants.map((p) => {
                const audioTracks = p
                    .getTrackPublications()
                    .filter(
                        (pub) =>
                            pub.source === Track.Source.Microphone && pub.track,
                    );
                return audioTracks.map((pub) => (
                    <AudioTrack
                        key={`${p.identity}-audio`}
                        trackRef={{
                            participant: p,
                            source: Track.Source.Microphone,
                            publication: pub,
                        }}
                    />
                ));
            })}
        </>
    );
}

function TiledLayout({
    remoteViewModels,
}: {
    remoteViewModels: ParticipantViewModel[];
}) {
    const { width } = useWindowSize();
    const cols = Math.min(
        2,
        width < 480 ? 1 : Math.max(1, remoteViewModels.length),
    );

    return (
        <div className={`grid h-full ${columnClass(cols)} gap-3`}>
            {remoteViewModels.map((vm) => (
                <ParticipantTile key={vm.identity} participant={vm} />
            ))}
        </div>
    );
}

function SpotlightLayout({
    promotedViewModel,
    thumbnailViewModels,
}: {
    promotedViewModel: ParticipantViewModel;
    thumbnailViewModels: ParticipantViewModel[];
}) {
    return (
        <div className='flex h-full flex-col gap-3'>
            <div className='flex-1 overflow-hidden rounded-[1.4rem]'>
                <ParticipantTile isPromoted participant={promotedViewModel} />
            </div>
            {thumbnailViewModels.length > 0 && (
                <div className='flex h-28 shrink-0 gap-3 overflow-x-auto'>
                    {thumbnailViewModels.map((vm) => (
                        <div className='h-full w-48 shrink-0' key={vm.identity}>
                            <ParticipantTile participant={vm} />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

function SidebarLayout({
    promotedViewModel,
    sideViewModels,
    width,
}: {
    promotedViewModel: ParticipantViewModel;
    sideViewModels: ParticipantViewModel[];
    width: number;
}) {
    if (width < 768) {
        return (
            <SpotlightLayout
                promotedViewModel={promotedViewModel}
                thumbnailViewModels={sideViewModels}
            />
        );
    }

    return (
        <div className='flex h-full gap-3'>
            <div className='flex-[2] overflow-hidden rounded-[1.4rem]'>
                <ParticipantTile isPromoted participant={promotedViewModel} />
            </div>
            {sideViewModels.length > 0 && (
                <div className='flex w-48 shrink-0 flex-col gap-3 overflow-y-auto'>
                    {sideViewModels.map((vm) => (
                        <div className='h-36 shrink-0' key={vm.identity}>
                            <ParticipantTile participant={vm} />
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

function AutoLayout({
    remoteViewModels,
}: {
    remoteViewModels: ParticipantViewModel[];
}) {
    const { width } = useWindowSize();
    const cols = autoColumns(remoteViewModels.length, width);

    return (
        <div className={`grid h-full ${columnClass(cols)} gap-3`}>
            {remoteViewModels.map((vm) => (
                <ParticipantTile key={vm.identity} participant={vm} />
            ))}
        </div>
    );
}

function selectPromoted(
    viewModels: ParticipantViewModel[],
    pinnedIdentity: string | null,
    activeSpeakerIdentity: string | null,
): [ParticipantViewModel, ParticipantViewModel[]] {
    const pinned = pinnedIdentity
        ? viewModels.find((vm) => vm.identity === pinnedIdentity)
        : null;

    const activeSpeaker = activeSpeakerIdentity
        ? viewModels.find((vm) => vm.identity === activeSpeakerIdentity)
        : null;

    const promoted = pinned ?? activeSpeaker ?? viewModels[0];
    const rest = viewModels.filter((vm) => vm.identity !== promoted?.identity);

    return [promoted ?? viewModels[0], rest];
}

/**
 * Root participant grid that renders remote participants according to
 * the selected layout mode and responsive viewport constraints.
 * The local participant is always shown as a floating self-view overlay.
 */
export function ParticipantGrid({
    layoutMode,
    pinnedIdentity,
}: ParticipantGridProps) {
    const { localParticipant } = useLocalParticipant();
    const remoteParticipants = useRemoteParticipants();
    const speakingParticipants = useSpeakingParticipants();
    const { width } = useWindowSize();

    const remoteViewModels: ParticipantViewModel[] = remoteParticipants.map(
        (p) => buildViewModel(p, false),
    );

    const showEmptyState = remoteViewModels.length === 0;

    const activeSpeakerIdentity =
        speakingParticipants.find((p) => !p.isLocal)?.identity ?? null;

    const [promoted, rest] = selectPromoted(
        remoteViewModels,
        pinnedIdentity,
        activeSpeakerIdentity,
    );

    return (
        <section className='relative flex-1 overflow-hidden p-4'>
            <RemoteAudio />

            {showEmptyState ? (
                <div className='flex h-full items-center justify-center'>
                    <div className='flex h-48 w-48 items-center justify-center rounded-full bg-[linear-gradient(160deg,var(--tile-bg-navy-start)_0%,var(--tile-bg-navy-mid)_50%,var(--tile-bg-navy-end)_100%)]'>
                        <div className='text-xl font-semibold text-white/60'>
                            {(
                                localParticipant.name
                                ?? localParticipant.identity
                            )
                                .split(' ')
                                .map((p) => p[0])
                                .join('')
                                .slice(0, 2)
                                .toUpperCase()}
                        </div>
                    </div>
                </div>
            ) : layoutMode === 'tiled' ? (
                <TiledLayout remoteViewModels={remoteViewModels} />
            ) : layoutMode === 'spotlight' ? (
                <SpotlightLayout
                    promotedViewModel={promoted}
                    thumbnailViewModels={rest}
                />
            ) : layoutMode === 'sidebar' ? (
                <SidebarLayout
                    promotedViewModel={promoted}
                    sideViewModels={rest}
                    width={width}
                />
            ) : (
                <AutoLayout remoteViewModels={remoteViewModels} />
            )}

            <SelfView />
        </section>
    );
}
