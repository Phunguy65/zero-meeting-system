import type { LocalParticipant, RemoteParticipant } from 'livekit-client';

export type ParticipantRole = 'HOST' | 'PARTICIPANT';

export type ParticipantViewModel = {
    identity: string;
    displayName: string;
    isMicEnabled: boolean;
    isCameraEnabled: boolean;
    isLocal: boolean;
    role?: ParticipantRole;
    livekitParticipant: LocalParticipant | RemoteParticipant;
};

export type MeetingLayoutMode = 'auto' | 'tiled' | 'spotlight' | 'sidebar';
