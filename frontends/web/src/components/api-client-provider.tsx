'use client';

import type { ReactNode } from 'react';
import { useEffect, useRef } from 'react';
import { configureApiClient, ejectApiClient } from '@/lib/api/client.ts';
import { webErrorTranslator } from '@/lib/api/error-translator.ts';

type ApiClientProviderProps = {
    children: ReactNode;
};

export function ApiClientProvider({ children }: ApiClientProviderProps) {
    const initialized = useRef(false);

    useEffect(() => {
        if (!initialized.current) {
            configureApiClient(
                process.env.NEXT_PUBLIC_API_BASE_URL ?? '',
                webErrorTranslator,
            );
            initialized.current = true;
        }
        return () => {
            ejectApiClient();
            initialized.current = false;
        };
    }, []);

    return children;
}
