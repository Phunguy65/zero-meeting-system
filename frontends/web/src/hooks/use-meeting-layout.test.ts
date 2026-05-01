import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useMeetingLayout } from './use-meeting-layout.ts';

describe('useMeetingLayout', () => {
    it('initializes in auto mode with no pinned participant', () => {
        const { result } = renderHook(() => useMeetingLayout());
        expect(result.current.mode).toBe('auto');
        expect(result.current.pinnedIdentity).toBeNull();
    });

    it('switches layout mode via setMode', () => {
        const { result } = renderHook(() => useMeetingLayout());

        act(() => {
            result.current.setMode('spotlight');
        });

        expect(result.current.mode).toBe('spotlight');
    });

    it('stores pinned participant identity via setPinnedIdentity', () => {
        const { result } = renderHook(() => useMeetingLayout());

        act(() => {
            result.current.setPinnedIdentity('user-123');
        });

        expect(result.current.pinnedIdentity).toBe('user-123');
    });

    it('clears pinned identity when set to null', () => {
        const { result } = renderHook(() => useMeetingLayout());

        act(() => {
            result.current.setPinnedIdentity('user-123');
        });

        act(() => {
            result.current.setPinnedIdentity(null);
        });

        expect(result.current.pinnedIdentity).toBeNull();
    });

    describe('deriveColumnCount', () => {
        describe('auto mode', () => {
            it('returns 1 column below 480px width', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(5, 479)).toBe(1);
            });

            it('returns min(2, count) between 480px and 768px', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(1, 600)).toBe(1);
                expect(result.current.deriveColumnCount(3, 600)).toBe(2);
            });

            it('returns 1 column for a single participant above 768px', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(1, 1024)).toBe(1);
            });

            it('returns 2 columns for 2-4 participants above 768px', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(2, 1024)).toBe(2);
                expect(result.current.deriveColumnCount(4, 1024)).toBe(2);
            });

            it('returns 3 columns for 5-9 participants above 768px', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(5, 1024)).toBe(3);
                expect(result.current.deriveColumnCount(9, 1024)).toBe(3);
            });

            it('returns 4 columns for 10+ participants above 768px', () => {
                const { result } = renderHook(() => useMeetingLayout());
                expect(result.current.deriveColumnCount(10, 1024)).toBe(4);
            });
        });

        describe('tiled mode', () => {
            it('caps at 2 columns regardless of participant count', () => {
                const { result } = renderHook(() => useMeetingLayout());

                act(() => {
                    result.current.setMode('tiled');
                });

                expect(result.current.deriveColumnCount(10, 1024)).toBe(2);
            });

            it('returns 1 column below 480px', () => {
                const { result } = renderHook(() => useMeetingLayout());

                act(() => {
                    result.current.setMode('tiled');
                });

                expect(result.current.deriveColumnCount(4, 479)).toBe(1);
            });
        });

        describe('spotlight mode', () => {
            it('returns 1 column', () => {
                const { result } = renderHook(() => useMeetingLayout());

                act(() => {
                    result.current.setMode('spotlight');
                });

                expect(result.current.deriveColumnCount(8, 1024)).toBe(1);
            });
        });

        describe('sidebar mode', () => {
            it('returns 1 column', () => {
                const { result } = renderHook(() => useMeetingLayout());

                act(() => {
                    result.current.setMode('sidebar');
                });

                expect(result.current.deriveColumnCount(8, 1024)).toBe(1);
            });
        });
    });
});
