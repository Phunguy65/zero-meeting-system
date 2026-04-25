'use client';

import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useLocale, useTranslations } from 'next-intl';
import { useState } from 'react';
import { googleLogin, login } from '@/generated/sdk.gen.ts';
import { ApiError, ApiFailError } from '@/lib/api/types.ts';
import { setAuthCookies } from '@/lib/auth/cookies.ts';
import { AuthForm } from './form.tsx';
import { AuthHero } from './hero.tsx';

type AuthVariant = 'login' | 'register';

type AuthContainerProps = {
    variant: AuthVariant;
};

type FieldErrors = {
    email?: string;
    password?: string;
};

const POPUP_ERROR_CODES = new Set([
    'auth/popup-closed-by-user',
    'auth/popup-blocked',
    'auth/cancelled-popup-request',
]);

export function AuthContainer({ variant }: AuthContainerProps) {
    const locale = useLocale();
    const t = useTranslations(`auth.${variant}`);
    const common = useTranslations('auth.common');
    const errorMessages = useTranslations('errors');
    const [loading, setLoading] = useState(false);
    const [googleLoading, setGoogleLoading] = useState(false);
    const [bannerError, setBannerError] = useState<string | null>(null);
    const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
    const router = useRouter();

    function clearErrors() {
        setBannerError(null);
        setFieldErrors({});
    }

    async function handleEmailSubmit(email: string, password: string) {
        if (variant !== 'login') {
            router.push(`/${locale}/workspace`);
            return;
        }

        clearErrors();
        setLoading(true);

        try {
            const { data } = await login({
                body: { email, password },
                throwOnError: true,
            });

            if (data?.accessToken && data?.refreshToken) {
                await setAuthCookies(data.accessToken, data.refreshToken);
            }
            router.push(`/${locale}/workspace`);
        } catch (error) {
            if (error instanceof ApiFailError) {
                if (error.errors.length > 0) {
                    const newFieldErrors: FieldErrors = {};
                    for (const violation of error.errors) {
                        if (
                            violation.field === 'email'
                            || violation.field === 'password'
                        ) {
                            newFieldErrors[violation.field] = violation.message;
                        }
                    }
                    setFieldErrors(newFieldErrors);
                }
                if (error.errors.length === 0) {
                    setBannerError(error.message);
                }
            } else if (error instanceof ApiError) {
                setBannerError(errorMessages('error_server'));
            } else {
                setBannerError(errorMessages('error_network'));
            }
        } finally {
            setLoading(false);
        }
    }

    async function handleGoogleSignIn() {
        clearErrors();
        setGoogleLoading(true);

        try {
            const { signInWithPopup } = await import('firebase/auth');
            const { auth, googleProvider } = await import('@/lib/firebase.ts');

            const result = await signInWithPopup(auth, googleProvider);
            const idToken = await result.user.getIdToken();

            const { data } = await googleLogin({
                body: { idToken },
                throwOnError: true,
            });

            if (data?.accessToken && data?.refreshToken) {
                await setAuthCookies(data.accessToken, data.refreshToken);
            }
            router.push(`/${locale}/workspace`);
        } catch (error: unknown) {
            const firebaseError = error as { code?: string };
            if (
                firebaseError.code
                && POPUP_ERROR_CODES.has(firebaseError.code)
            ) {
                return;
            }

            if (error instanceof ApiFailError) {
                setBannerError(error.message);
            } else if (error instanceof ApiError) {
                setBannerError(errorMessages('error_server'));
            } else {
                setBannerError(errorMessages('error_google_signin_failed'));
            }
        } finally {
            setGoogleLoading(false);
        }
    }

    const heroProps =
        variant === 'login'
            ? {
                  variant: 'login' as const,
                  brand: common('brand'),
                  brandHref: `/${locale}/home`,
                  eyebrow: t('eyebrow'),
                  brandHeadline: t('brand'),
                  heroDescription: t('heroDescription'),
              }
            : {
                  variant: 'register' as const,
                  brand: common('brand'),
                  brandHref: `/${locale}/home`,
                  heroLineOne: t('heroLineOne'),
                  heroLineTwo: t('heroLineTwo'),
                  heroLineThree: t('heroLineThree'),
                  heroDescription: t('heroDescription'),
                  securityCardTitle: t('securityCardTitle'),
                  securityCardDescription: t('securityCardDescription'),
              };

    return (
        <main className='min-h-screen bg-[linear-gradient(135deg,_var(--surface-hero-start)_0%,_var(--surface-hero-mid)_48%,_var(--surface-hero-end)_100%)] text-text-dark'>
            <div className='mx-auto flex min-h-screen max-w-[1660px] flex-col px-4 py-5 sm:px-8'>
                <section className='flex flex-1 items-center'>
                    <div className='grid w-full gap-8 overflow-hidden rounded-[2.15rem] border border-white/80 bg-white/65 shadow-[0_30px_90px_-34px_rgba(15,23,42,0.22)] backdrop-blur lg:grid-cols-[1.12fr_0.88fr]'>
                        <AuthHero {...heroProps} />

                        <AuthForm
                            bannerError={bannerError}
                            fieldErrors={fieldErrors}
                            googleLoading={googleLoading}
                            labels={{
                                title: t('title'),
                                subtitle: t('subtitle'),
                                nameLabel:
                                    variant === 'register'
                                        ? t('nameLabel')
                                        : undefined,
                                namePlaceholder:
                                    variant === 'register'
                                        ? t('namePlaceholder')
                                        : undefined,
                                emailLabel: t('emailLabel'),
                                emailPlaceholder: t('emailPlaceholder'),
                                passwordLabel: t('passwordLabel'),
                                passwordPlaceholder: t('passwordPlaceholder'),
                                showPassword: t('showPassword'),
                                hidePassword: t('hidePassword'),
                                forgotPassword:
                                    variant === 'login'
                                        ? t('forgotPassword')
                                        : undefined,
                                agreementPrefix:
                                    variant === 'register'
                                        ? t('agreementPrefix')
                                        : undefined,
                                agreementTerms:
                                    variant === 'register'
                                        ? t('agreementTerms')
                                        : undefined,
                                agreementMiddle:
                                    variant === 'register'
                                        ? t('agreementMiddle')
                                        : undefined,
                                agreementPrivacy:
                                    variant === 'register'
                                        ? t('agreementPrivacy')
                                        : undefined,
                                agreementSuffix:
                                    variant === 'register'
                                        ? t('agreementSuffix')
                                        : undefined,
                                submit: t('submit'),
                                divider: t('divider'),
                                google: t('google'),
                                switchPrefix: t('switchPrefix'),
                                switchAction: t('switchAction'),
                            }}
                            loading={loading}
                            locale={locale}
                            onEmailSubmit={handleEmailSubmit}
                            onGoogleSignIn={handleGoogleSignIn}
                            variant={variant}
                        />
                    </div>
                </section>

                <footer className='mt-6 flex flex-col gap-4 border-t border-border-muted pt-6 text-base text-text-secondary sm:flex-row sm:items-center sm:justify-between'>
                    <p>
                        {common('copyright', {
                            year: new Date().getFullYear(),
                        })}
                    </p>
                    <nav className='flex flex-wrap items-center gap-7'>
                        <Link
                            className='hover:text-brand-blue'
                            href={`/${locale}/home`}
                        >
                            {common('privacy')}
                        </Link>
                        <Link
                            className='hover:text-brand-blue'
                            href={`/${locale}/home`}
                        >
                            {common('terms')}
                        </Link>
                        <Link
                            className='hover:text-brand-blue'
                            href={`/${locale}/home`}
                        >
                            {common('security')}
                        </Link>
                        <Link
                            className='hover:text-brand-blue'
                            href={`/${locale}/home`}
                        >
                            {common('contact')}
                        </Link>
                    </nav>
                </footer>
            </div>
        </main>
    );
}
