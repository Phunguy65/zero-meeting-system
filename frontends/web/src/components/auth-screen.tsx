"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";

function GoogleIcon() {
  return (
    <svg aria-hidden="true" className="h-6 w-6" viewBox="0 0 24 24">
      <path
        d="M21.8 12.23c0-.77-.07-1.5-.2-2.2H12v4.16h5.48a4.7 4.7 0 0 1-2.03 3.08v2.56h3.3c1.94-1.79 3.05-4.42 3.05-7.6Z"
        fill="#4285F4"
      />
      <path
        d="M12 22c2.76 0 5.08-.91 6.77-2.47l-3.3-2.56c-.92.62-2.08.98-3.47.98-2.66 0-4.92-1.8-5.73-4.21H2.86v2.63A10.22 10.22 0 0 0 12 22Z"
        fill="#34A853"
      />
      <path
        d="M6.27 13.74A6.12 6.12 0 0 1 5.95 12c0-.6.1-1.18.32-1.74V7.63H2.86a10.05 10.05 0 0 0 0 8.74l3.41-2.63Z"
        fill="#FBBC04"
      />
      <path
        d="M12 6.05c1.5 0 2.84.52 3.89 1.52l2.92-2.92C17.07 2.98 14.76 2 12 2 7.99 2 4.5 4.3 2.86 7.63l3.41 2.63c.8-2.41 3.07-4.21 5.73-4.21Z"
        fill="#EA4335"
      />
    </svg>
  );
}

function EyeIcon({ hidden }: { hidden: boolean }) {
  return hidden ? (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
    >
      <path d="m3 3 18 18" />
      <path d="M10.58 10.58a2 2 0 0 0 2.83 2.83" />
      <path d="M9.88 4.24A10.94 10.94 0 0 1 12 4c5 0 9.27 3.11 11 7.5a11.8 11.8 0 0 1-5.17 5.94" />
      <path d="M6.61 6.61A11.84 11.84 0 0 0 1 11.5 11.82 11.82 0 0 0 8.05 17.9" />
    </svg>
  ) : (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeLinejoin="round"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
    >
      <path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7S1 12 1 12Z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  );
}

type AuthVariant = "login" | "register";

type AuthScreenProps = {
  variant: AuthVariant;
};

export function AuthScreen({ variant }: AuthScreenProps) {
  const locale = useLocale();
  const t = useTranslations(`auth.${variant}`);
  const common = useTranslations("auth.common");
  const [passwordHidden, setPasswordHidden] = useState(true);

  return (
    <main className="min-h-screen bg-[linear-gradient(135deg,_#f7faff_0%,_#f4f8fe_48%,_#eef5ff_100%)] text-[#101828]">
      <div className="mx-auto flex min-h-screen max-w-[1660px] flex-col px-4 py-5 sm:px-8">
        <section className="flex flex-1 items-center">
          <div className="grid w-full gap-8 overflow-hidden rounded-[2.15rem] border border-white/80 bg-white/65 shadow-[0_30px_90px_-34px_rgba(15,23,42,0.22)] backdrop-blur lg:grid-cols-[1.12fr_0.88fr]">
            <div
              className={`relative overflow-hidden ${
                variant === "login"
                  ? "bg-[linear-gradient(145deg,_#ffffff_0%,_#f5f8ff_60%,_#edf4ff_100%)]"
                  : "bg-[linear-gradient(145deg,_rgba(255,255,255,0.82)_0%,_rgba(241,246,255,0.84)_55%,_rgba(232,240,255,0.9)_100%)]"
              }`}
            >
              <div className="absolute inset-0">
                <div className="absolute left-[-8%] top-[8%] h-72 w-72 rounded-full bg-[#dfeeff]/70 blur-3xl" />
                <div className="absolute bottom-[-10%] right-[-6%] h-80 w-80 rounded-full bg-[#d9e8ff]/70 blur-3xl" />
                {variant === "register" ? (
                  <div className="absolute inset-0 bg-[linear-gradient(90deg,rgba(255,255,255,0.55),rgba(255,255,255,0.18))]" />
                ) : null}
              </div>

              <div className="relative flex h-full flex-col px-8 py-8 sm:px-12 sm:py-12 lg:px-16">
                <Link
                  className={`self-start text-[2.25rem] font-semibold tracking-tight ${variant === "login" ? "text-[#111827]" : "text-[#1560cc]"}`}
                  href={`/${locale}/home`}
                >
                  Zero Meet
                </Link>

                <div className="mt-14 max-w-[620px] lg:mt-20">
                  {variant === "login" ? (
                    <>
                      <p className="text-sm font-semibold uppercase tracking-[0.28em] text-[#1558c6]">
                        {t("eyebrow")}
                      </p>
                      <h1 className="mt-7 text-6xl font-semibold leading-[0.95] tracking-tight text-[#15191e] sm:text-7xl">
                        {t("brand")}
                      </h1>
                    </>
                  ) : (
                    <h1 className="text-5xl font-semibold leading-[1.02] tracking-tight text-[#15191e] sm:text-6xl">
                      {t("heroLineOne")}
                      <br />
                      <span className="text-[#1560cc]">{t("heroLineTwo")}</span>{" "}
                      {t("heroLineThree")}
                    </h1>
                  )}
                  <p className="mt-7 max-w-[520px] text-2xl leading-[1.45] text-[#334155] sm:text-[1.15rem] sm:leading-10">
                    {t("heroDescription")}
                  </p>
                </div>

                {variant === "login" ? (
                  <div className="relative mt-12 max-w-[700px] rounded-[2rem] border border-white/90 bg-white/85 p-4 shadow-[0_24px_70px_-36px_rgba(15,23,42,0.28)]">
                    <div className="aspect-[1.8] overflow-hidden rounded-[1.5rem] bg-[linear-gradient(135deg,_#d4d7dd_0%,_#f0f1f4_100%)]">
                      <div className="flex h-full flex-col justify-between bg-[radial-gradient(circle_at_top,_rgba(255,255,255,0.85),_rgba(232,235,240,0.25))] p-5">
                        <div className="grid grid-cols-3 gap-3">
                          <div className="h-2.5 w-2.5 rounded-full bg-white/80" />
                          <div className="h-2.5 w-2.5 rounded-full bg-white/60" />
                          <div className="h-2.5 w-2.5 rounded-full bg-white/45" />
                        </div>
                        <div className="grid flex-1 grid-cols-[1.2fr_1fr] gap-5 py-5">
                          <div className="rounded-[1.35rem] bg-[linear-gradient(180deg,_#6d7077,_#40434a)]" />
                          <div className="rounded-[1.35rem] bg-[linear-gradient(180deg,_#f6f8fb,_#dbe3f4)]" />
                        </div>
                        <div className="rounded-[1.2rem] bg-[#1f232b] p-4 text-white">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center gap-3">
                              <div className="h-12 w-12 rounded-full bg-[#7b7f86]" />
                              <div>
                                <div className="h-2.5 w-28 rounded-full bg-white/70" />
                                <div className="mt-2 h-2 w-20 rounded-full bg-white/35" />
                              </div>
                            </div>
                            <div className="flex items-center gap-3">
                              <span className="h-10 w-10 rounded-full bg-white/15" />
                              <span className="h-10 w-10 rounded-full bg-white/15" />
                              <span className="h-10 w-10 rounded-full bg-white/15" />
                            </div>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="mt-auto hidden max-w-[420px] rounded-[2rem] border border-white/85 bg-white/88 p-7 shadow-[0_24px_70px_-36px_rgba(15,23,42,0.28)] lg:block">
                    <div className="flex items-center gap-4">
                      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-[#dbe7ff] text-[#3157b8]">
                        <svg
                          aria-hidden="true"
                          className="h-7 w-7"
                          fill="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path d="M12 2 4 5v6c0 5.25 3.4 10.16 8 11.75C16.6 21.16 20 16.25 20 11V5l-8-3Zm3.4 8.2-4.02 4.4a1 1 0 0 1-1.46.02l-2.1-2.18 1.44-1.38 1.36 1.42 3.28-3.6 1.5 1.32Z" />
                        </svg>
                      </div>
                      <div>
                        <p className="text-2xl font-semibold text-[#1f2937]">
                          {t("securityCardTitle")}
                        </p>
                        <p className="mt-1 text-base text-[#475467]">
                          {t("securityCardDescription")}
                        </p>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            </div>

            <div className="bg-white px-6 py-8 sm:px-10 sm:py-12 lg:px-14 lg:py-16">
              <div className="mx-auto flex h-full w-full max-w-[560px] flex-col">
                <div>
                  <h2 className="text-5xl font-semibold leading-tight tracking-tight text-[#111827] sm:text-[3.35rem]">
                    {t("title")}
                  </h2>
                  <p className="mt-4 text-xl leading-8 text-[#344054] sm:text-[1.15rem]">
                    {t("subtitle")}
                  </p>
                </div>

                <form
                  className="mt-10 flex flex-col gap-7"
                  onSubmit={(event) => event.preventDefault()}
                >
                  {variant === "register" ? (
                    <label className="flex flex-col gap-3">
                      <span className="text-lg font-medium text-[#111827]">
                        {t("nameLabel")}
                      </span>
                      <input
                        className="h-16 rounded-full bg-[#f1f3f7] px-6 text-xl text-[#111827] outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-[#1a73e8]"
                        placeholder={t("namePlaceholder")}
                        type="text"
                      />
                    </label>
                  ) : null}

                  <label className="flex flex-col gap-3">
                    <span className="text-lg font-medium uppercase tracking-[0.05em] text-[#111827]">
                      {t("emailLabel")}
                    </span>
                    <input
                      className="h-16 rounded-full bg-[#f1f3f7] px-6 text-xl text-[#111827] outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-[#1a73e8]"
                      placeholder={t("emailPlaceholder")}
                      type="email"
                    />
                  </label>

                  <label className="flex flex-col gap-3">
                    <div className="flex items-center justify-between gap-4">
                      <span className="text-lg font-medium uppercase tracking-[0.05em] text-[#111827]">
                        {t("passwordLabel")}
                      </span>
                      {variant === "login" ? (
                        <Link
                          className="text-base font-medium text-[#1558c6] transition-colors hover:text-[#0d4db8]"
                          href={`/${locale}/login`}
                        >
                          {t("forgotPassword")}
                        </Link>
                      ) : null}
                    </div>
                    <div className="flex h-16 items-center rounded-full bg-[#f1f3f7] pr-5 ring-1 ring-transparent transition focus-within:ring-2 focus-within:ring-[#1a73e8]">
                      <input
                        className="h-full w-full bg-transparent px-6 text-xl text-[#111827] outline-none"
                        placeholder={t("passwordPlaceholder")}
                        type={passwordHidden ? "password" : "text"}
                      />
                      {variant === "register" ? (
                        <button
                          className="inline-flex items-center justify-center rounded-full p-2 text-[#667085] transition-colors hover:bg-white hover:text-[#1558c6]"
                          onClick={() => setPasswordHidden((value) => !value)}
                          type="button"
                        >
                          <EyeIcon hidden={passwordHidden} />
                        </button>
                      ) : null}
                    </div>
                  </label>

                  {variant === "register" ? (
                    <label className="mt-1 flex items-start gap-4 text-lg leading-9 text-[#344054]">
                      <input
                        className="mt-1 h-6 w-6 rounded-md border border-[#b8c3d9] accent-[#1a73e8]"
                        type="checkbox"
                      />
                      <span>
                        {t("agreementPrefix")}{" "}
                        <Link
                          className="font-medium text-[#1558c6] hover:text-[#0d4db8]"
                          href={`/${locale}/register`}
                        >
                          {t("agreementTerms")}
                        </Link>{" "}
                        {t("agreementMiddle")}{" "}
                        <Link
                          className="font-medium text-[#1558c6] hover:text-[#0d4db8]"
                          href={`/${locale}/register`}
                        >
                          {t("agreementPrivacy")}
                        </Link>{" "}
                        {t("agreementSuffix")}
                      </span>
                    </label>
                  ) : null}

                  <button
                    className="mt-2 h-16 rounded-full bg-[linear-gradient(135deg,_#1a73e8_0%,_#1664d6_100%)] px-8 text-2xl font-semibold text-white shadow-[0_24px_50px_-26px_rgba(26,115,232,0.95)] transition-all hover:-translate-y-0.5 hover:shadow-[0_28px_55px_-24px_rgba(26,115,232,1)]"
                    type="submit"
                  >
                    {t("submit")}
                  </button>
                </form>

                <div className="mt-9">
                  <div className="flex items-center gap-5 text-base uppercase tracking-[0.18em] text-[#667085]">
                    <span className="h-px flex-1 bg-[#e6ebf3]" />
                    <span>{t("divider")}</span>
                    <span className="h-px flex-1 bg-[#e6ebf3]" />
                  </div>

                  <button
                    className="mt-9 flex h-16 w-full items-center justify-center gap-4 rounded-full border border-[#dbe2ee] bg-white px-6 text-[1.15rem] font-medium text-[#111827] transition-colors hover:bg-[#f8fbff]"
                    type="button"
                  >
                    <GoogleIcon />
                    {t("google")}
                  </button>
                </div>

                <p className="mt-10 text-center text-[1.15rem] leading-8 text-[#344054]">
                  {variant === "login" ? t("switchPrefix") : t("switchPrefix")}{" "}
                  <Link
                    className="font-semibold text-[#1558c6] hover:text-[#0d4db8]"
                    href={`/${locale}/${variant === "login" ? "register" : "login"}`}
                  >
                    {t("switchAction")}
                  </Link>
                </p>
              </div>
            </div>
          </div>
        </section>

        <footer className="mt-6 flex flex-col gap-4 border-t border-[#e8eef6] pt-6 text-base text-[#344054] sm:flex-row sm:items-center sm:justify-between">
          <p>{common("copyright")}</p>
          <nav className="flex flex-wrap items-center gap-7">
            <Link className="hover:text-[#1558c6]" href={`/${locale}/home`}>
              {common("privacy")}
            </Link>
            <Link className="hover:text-[#1558c6]" href={`/${locale}/home`}>
              {common("terms")}
            </Link>
            <Link className="hover:text-[#1558c6]" href={`/${locale}/home`}>
              {common("security")}
            </Link>
            <Link className="hover:text-[#1558c6]" href={`/${locale}/home`}>
              {common("contact")}
            </Link>
          </nav>
        </footer>
      </div>
    </main>
  );
}
