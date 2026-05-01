const ACCESS_TOKEN_KEY = 'access_token';
const REFRESH_TOKEN_KEY = 'refresh_token';
const MAX_AGE_SECONDS = 30 * 24 * 60 * 60;

export async function setAuthCookies(
    accessToken: string,
    refreshToken: string,
): Promise<void> {
    const secure = process.env.NODE_ENV === 'production';
    const options = {
        path: '/',
        sameSite: 'lax' as const,
        maxAge: MAX_AGE_SECONDS,
        secure,
    };

    await cookieStore.set({
        name: ACCESS_TOKEN_KEY,
        value: accessToken,
        ...options,
    });
    await cookieStore.set({
        name: REFRESH_TOKEN_KEY,
        value: refreshToken,
        ...options,
    });
}

export async function clearAuthCookies(): Promise<void> {
    await cookieStore.delete({ name: ACCESS_TOKEN_KEY, path: '/' });
    await cookieStore.delete({ name: REFRESH_TOKEN_KEY, path: '/' });
}

export async function getAccessToken(): Promise<string | undefined> {
    const cookie = await cookieStore.get(ACCESS_TOKEN_KEY);
    return cookie?.value;
}
