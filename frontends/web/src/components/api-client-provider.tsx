'use client';

import type { ReactNode } from 'react';
import { configureApiClient } from '@/lib/api/client.ts';
import { webErrorTranslator } from '@/lib/api/error-translator.ts';

configureApiClient(
    process.env.NEXT_PUBLIC_API_BASE_URL ?? '',
    webErrorTranslator,
);

export function ApiClientProvider({ children }: { children: ReactNode }) {
    return children;
}
