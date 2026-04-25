'use client';

import { Eye, EyeOff, Loader2 } from 'lucide-react';
import Link from 'next/link';
import { useRef, useState } from 'react';
import { GoogleSignInButton } from './social-button.tsx';

type AuthVariant = 'login' | 'register';

type FieldErrors = {
    email?: string;
    password?: string;
};

type AuthFormLabels = {
    title: string;
    subtitle: string;
    nameLabel?: string;
    namePlaceholder?: string;
    emailLabel: string;
    emailPlaceholder: string;
    passwordLabel: string;
    passwordPlaceholder: string;
    showPassword: string;
    hidePassword: string;
    forgotPassword?: string;
    agreementPrefix?: string;
    agreementTerms?: string;
    agreementMiddle?: string;
    agreementPrivacy?: string;
    agreementSuffix?: string;
    submit: string;
    divider: string;
    google: string;
    switchPrefix: string;
    switchAction: string;
};

type AuthFormProps = {
    variant: AuthVariant;
    locale: string;
    labels: AuthFormLabels;
    onEmailSubmit: (email: string, password: string) => Promise<void>;
    onGoogleSignIn: () => Promise<void>;
    loading: boolean;
    googleLoading: boolean;
    bannerError: string | null;
    fieldErrors: FieldErrors;
};

export function AuthForm({
    variant,
    locale,
    labels,
    onEmailSubmit,
    onGoogleSignIn,
    loading,
    googleLoading,
    bannerError,
    fieldErrors,
}: AuthFormProps) {
    const [passwordHidden, setPasswordHidden] = useState(true);
    const emailRef = useRef<HTMLInputElement>(null);
    const passwordRef = useRef<HTMLInputElement>(null);
    const isSubmitting = loading || googleLoading;

    function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
        event.preventDefault();
        const email = emailRef.current?.value ?? '';
        const password = passwordRef.current?.value ?? '';
        void onEmailSubmit(email, password);
    }

    return (
        <div className='bg-surface px-6 py-8 sm:px-10 sm:py-12 lg:px-14 lg:py-16'>
            <div className='mx-auto flex h-full w-full max-w-[560px] flex-col'>
                <div>
                    <h2 className='text-5xl font-semibold leading-tight tracking-tight text-text-primary sm:text-[3.35rem]'>
                        {labels.title}
                    </h2>
                    <p className='mt-4 text-xl leading-8 text-text-secondary sm:text-[1.15rem]'>
                        {labels.subtitle}
                    </p>
                </div>

                <form
                    className='mt-10 flex flex-col gap-7'
                    onSubmit={handleSubmit}
                >
                    {bannerError ? (
                        <p
                            className='rounded-xl bg-red-50 px-5 py-4 text-base text-red-700'
                            role='alert'
                        >
                            {bannerError}
                        </p>
                    ) : null}

                    {variant === 'register' && labels.nameLabel ? (
                        <label className='flex flex-col gap-3'>
                            <span className='text-lg font-medium text-text-primary'>
                                {labels.nameLabel}
                            </span>
                            <input
                                className='h-16 rounded-full bg-surface-input-alt px-6 text-xl text-text-primary outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-primary'
                                placeholder={labels.namePlaceholder}
                                type='text'
                            />
                        </label>
                    ) : null}

                    <label className='flex flex-col gap-3'>
                        <span className='text-lg font-medium uppercase tracking-[0.05em] text-text-primary'>
                            {labels.emailLabel}
                        </span>
                        <input
                            ref={emailRef}
                            className={`h-16 rounded-full bg-surface-input-alt px-6 text-xl text-text-primary outline-none ring-1 transition focus:ring-2 focus:ring-primary ${
                                fieldErrors.email
                                    ? 'ring-red-400'
                                    : 'ring-transparent'
                            }`}
                            placeholder={labels.emailPlaceholder}
                            type='email'
                        />
                        {fieldErrors.email ? (
                            <p
                                className='px-2 text-sm text-red-600'
                                role='alert'
                            >
                                {fieldErrors.email}
                            </p>
                        ) : null}
                    </label>

                    <label className='flex flex-col gap-3'>
                        <div className='flex items-center justify-between gap-4'>
                            <span className='text-lg font-medium uppercase tracking-[0.05em] text-text-primary'>
                                {labels.passwordLabel}
                            </span>
                            {variant === 'login' && labels.forgotPassword ? (
                                <Link
                                    className='text-base font-medium text-brand-blue transition-colors hover:text-primary-dark'
                                    href={`/${locale}/login`}
                                >
                                    {labels.forgotPassword}
                                </Link>
                            ) : null}
                        </div>
                        <div
                            className={`flex h-16 items-center rounded-full bg-surface-input-alt pr-5 ring-1 transition focus-within:ring-2 focus-within:ring-primary ${
                                fieldErrors.password
                                    ? 'ring-red-400'
                                    : 'ring-transparent'
                            }`}
                        >
                            <input
                                ref={passwordRef}
                                className='h-full w-full bg-transparent px-6 text-xl text-text-primary outline-none'
                                placeholder={labels.passwordPlaceholder}
                                type={passwordHidden ? 'password' : 'text'}
                            />
                            {variant === 'register' ? (
                                <button
                                    aria-label={
                                        passwordHidden
                                            ? labels.showPassword
                                            : labels.hidePassword
                                    }
                                    className='inline-flex items-center justify-center rounded-full p-2 text-text-subtle transition-colors hover:bg-surface hover:text-brand-blue'
                                    onClick={() => setPasswordHidden((v) => !v)}
                                    type='button'
                                >
                                    {passwordHidden ? (
                                        <EyeOff className='h-6 w-6' />
                                    ) : (
                                        <Eye className='h-6 w-6' />
                                    )}
                                </button>
                            ) : null}
                        </div>
                        {fieldErrors.password ? (
                            <p
                                className='px-2 text-sm text-red-600'
                                role='alert'
                            >
                                {fieldErrors.password}
                            </p>
                        ) : null}
                    </label>

                    {variant === 'register' && labels.agreementPrefix ? (
                        <label className='mt-1 flex items-start gap-4 text-lg leading-9 text-text-secondary'>
                            <input
                                className='mt-1 h-6 w-6 rounded-md border border-border-input accent-primary'
                                type='checkbox'
                            />
                            <span>
                                {labels.agreementPrefix}{' '}
                                <Link
                                    className='font-medium text-brand-blue hover:text-primary-dark'
                                    href={`/${locale}/register`}
                                >
                                    {labels.agreementTerms}
                                </Link>{' '}
                                {labels.agreementMiddle}{' '}
                                <Link
                                    className='font-medium text-brand-blue hover:text-primary-dark'
                                    href={`/${locale}/register`}
                                >
                                    {labels.agreementPrivacy}
                                </Link>{' '}
                                {labels.agreementSuffix}
                            </span>
                        </label>
                    ) : null}

                    <button
                        className='mt-2 flex h-16 items-center justify-center rounded-full bg-[linear-gradient(135deg,_var(--primary)_0%,_var(--primary-hover)_100%)] px-8 text-2xl font-semibold text-white shadow-[0_24px_50px_-26px_rgba(26,115,232,0.95)] transition-all hover:-translate-y-0.5 hover:shadow-[0_28px_55px_-24px_rgba(26,115,232,1)] disabled:opacity-60 disabled:hover:translate-y-0'
                        disabled={isSubmitting}
                        type='submit'
                    >
                        {loading ? (
                            <Loader2 className='h-5 w-5 animate-spin' />
                        ) : (
                            labels.submit
                        )}
                    </button>
                </form>

                <div className='mt-9'>
                    <div className='flex items-center gap-5 text-base uppercase tracking-[0.18em] text-text-subtle'>
                        <span className='h-px flex-1 bg-border-muted' />
                        <span>{labels.divider}</span>
                        <span className='h-px flex-1 bg-border-muted' />
                    </div>

                    <GoogleSignInButton
                        disabled={isSubmitting}
                        label={labels.google}
                        loading={googleLoading}
                        onClick={() => void onGoogleSignIn()}
                    />
                </div>

                <p className='mt-10 text-center text-[1.15rem] leading-8 text-text-secondary'>
                    {labels.switchPrefix}{' '}
                    <Link
                        className='font-semibold text-brand-blue hover:text-primary-dark'
                        href={`/${locale}/${variant === 'login' ? 'register' : 'login'}`}
                    >
                        {labels.switchAction}
                    </Link>
                </p>
            </div>
        </div>
    );
}
