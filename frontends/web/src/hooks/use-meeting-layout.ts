'use client';

import { useCallback, useState } from 'react';

export type MeetingLayoutMode = 'auto' | 'tiled' | 'spotlight' | 'sidebar';

type MeetingLayoutState = {
    mode: MeetingLayoutMode;
    pinnedIdentity: string | null;
};

type UseMeetingLayoutResult = {
    mode: MeetingLayoutMode;
    pinnedIdentity: string | null;
    setMode: (mode: MeetingLayoutMode) => void;
    setPinnedIdentity: (identity: string | null) => void;
    deriveColumnCount: (
        participantCount: number,
        viewportWidth: number,
    ) => number;
};

function autoColumnCount(
    participantCount: number,
    viewportWidth: number,
): number {
    if (viewportWidth < 480) return 1;
    if (viewportWidth < 768) return Math.min(2, participantCount);

    if (participantCount <= 1) return 1;
    if (participantCount <= 4) return 2;
    if (participantCount <= 9) return 3;
    return 4;
}

/**
 * Manages meeting layout mode selection, participant pinning for spotlight
 * and sidebar modes, and responsive column count derivation.
 */
export function useMeetingLayout(): UseMeetingLayoutResult {
    const [state, setState] = useState<MeetingLayoutState>({
        mode: 'auto',
        pinnedIdentity: null,
    });

    const setMode = useCallback((mode: MeetingLayoutMode) => {
        setState((prev) => ({ ...prev, mode }));
    }, []);

    const setPinnedIdentity = useCallback((identity: string | null) => {
        setState((prev) => ({ ...prev, pinnedIdentity: identity }));
    }, []);

    const deriveColumnCount = useCallback(
        (participantCount: number, viewportWidth: number): number => {
            switch (state.mode) {
                case 'tiled':
                    if (viewportWidth < 480) return 1;
                    if (viewportWidth < 768)
                        return Math.min(2, participantCount);
                    return Math.min(2, participantCount);
                case 'spotlight':
                case 'sidebar':
                    return 1;
                default:
                    return autoColumnCount(participantCount, viewportWidth);
            }
        },
        [state.mode],
    );

    return {
        mode: state.mode,
        pinnedIdentity: state.pinnedIdentity,
        setMode,
        setPinnedIdentity,
        deriveColumnCount,
    };
}
