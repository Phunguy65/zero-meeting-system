"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useEffect, useState } from "react";

function VideoCameraIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M14 7a2 2 0 0 1 2 2v1.56l3.2-2.4A1 1 0 0 1 21 8.96v6.08a1 1 0 0 1-1.8.8L16 13.44V15a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h9Z" />
    </svg>
  );
}

function ShieldIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 2 4 5v6c0 5.25 3.4 10.16 8 11.75C16.6 21.16 20 16.25 20 11V5l-8-3Zm3.4 8.2-4.02 4.4a1 1 0 0 1-1.46.02l-2.1-2.18 1.44-1.38 1.36 1.42 3.28-3.6 1.5 1.32Z" />
    </svg>
  );
}

function DeviceIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M4 5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5Zm14 4h-1v8H7v1a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-7a2 2 0 0 0-2-2Zm-7 9h2v1h-2v-1Z" />
    </svg>
  );
}

function PeopleIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm6-1a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5ZM3 18a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v1H3v-1Zm11 1v-1c0-1.13-.37-2.18-.99-3.03.31-.06.64-.1.99-.1h2a4 4 0 0 1 4 4v.13A1.87 1.87 0 0 1 19.87 19H14Z" />
    </svg>
  );
}

function QuestionIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.06 15.5a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5ZM14 10.3c-.56.46-1.14.86-1.14 1.7v.5h-1.75v-.7c0-1.08.72-1.74 1.36-2.27.55-.46 1.03-.84 1.03-1.45 0-.77-.58-1.3-1.48-1.3-.9 0-1.56.48-2.12 1.2L8.5 6.86C9.39 5.67 10.63 5 12.18 5c2.06 0 3.56 1.2 3.56 3.06 0 1.09-.68 1.76-1.74 2.24Z" />
    </svg>
  );
}

function MessageIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M5 4a3 3 0 0 0-3 3v12l4.5-3H19a3 3 0 0 0 3-3V7a3 3 0 0 0-3-3H5Zm2 4h10v2H7V8Zm0 4h6v2H7v-2Z" />
    </svg>
  );
}

function SettingsIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="m19.14 12.94.05-.94-.05-.94 1.64-1.28a.8.8 0 0 0 .19-1.03l-1.55-2.68a.8.8 0 0 0-.98-.35l-1.94.78a7.4 7.4 0 0 0-1.63-.94l-.3-2.06a.82.82 0 0 0-.8-.68h-3.1a.82.82 0 0 0-.8.68l-.3 2.06c-.58.22-1.13.53-1.63.94l-1.94-.78a.8.8 0 0 0-.98.35L3.03 8.75a.8.8 0 0 0 .19 1.03l1.64 1.28-.05.94.05.94-1.64 1.28a.8.8 0 0 0-.19 1.03l1.55 2.68c.2.35.62.5.98.35l1.94-.78c.5.4 1.05.72 1.63.94l.3 2.06c.07.4.4.68.8.68h3.1c.4 0 .73-.29.8-.68l.3-2.06c.58-.22 1.13-.53 1.63-.94l1.94.78c.36.15.78 0 .98-.35l1.55-2.68a.8.8 0 0 0-.19-1.03l-1.64-1.28ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z" />
    </svg>
  );
}

function GlobeIcon({ className = "" }: { className?: string }) {
  return (
    <svg
      aria-hidden="true"
      className={className}
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      viewBox="0 0 24 24"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M3.5 9h17M3.5 15h17M12 3c2.4 2.4 3.6 5.4 3.6 9S14.4 18.6 12 21c-2.4-2.4-3.6-5.4-3.6-9S9.6 5.4 12 3Z" />
    </svg>
  );
}

const PREVIEW_TILES = [
  "bg-[#d7d7d7] text-[#4a4a4a]",
  "bg-[#b9d0ff] text-[#1a73e8]",
  "bg-[#c9e7ff] text-[#1a73e8]",
  "bg-[#ededed] text-[#4a4a4a]",
];

export function HomeScreen() {
  const t = useTranslations("home");
  const locale = useLocale();
  const [now, setNow] = useState("");
  const [joinCode, setJoinCode] = useState("");
  const hasJoinValue = joinCode.trim().length > 0;

  useEffect(() => {
    const formatter = new Intl.DateTimeFormat(
      locale === "vi" ? "vi-VN" : "en-US",
      {
        hour: "numeric",
        minute: "2-digit",
        hour12: true,
        weekday: "short",
        month: "short",
        day: "numeric",
      },
    );

    const updateTime = () => {
      setNow(formatter.format(new Date()));
    };

    updateTime();
    const timer = window.setInterval(updateTime, 60_000);

    return () => window.clearInterval(timer);
  }, [locale]);

  const featureCards = [
    {
      title: t("featureSecurityTitle"),
      description: t("featureSecurityBody"),
      icon: ShieldIcon,
    },
    {
      title: t("featureDeviceTitle"),
      description: t("featureDeviceBody"),
      icon: DeviceIcon,
    },
    {
      title: t("featureScaleTitle"),
      description: t("featureScaleBody"),
      icon: PeopleIcon,
    },
  ];

  const footerLinks = [
    t("footerAbout"),
    t("footerPrivacy"),
    t("footerTerms"),
    t("footerHelp"),
    t("footerSecurity"),
  ];

  return (
    <main className="min-h-screen bg-[#f8fafc] text-[#202124]">
      <div className="mx-auto flex min-h-screen w-full max-w-[1600px] flex-col px-5 pb-6 pt-5 sm:px-8 lg:px-10">
        <header className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <Link
            className="inline-flex items-center gap-3 self-start text-[22px] font-medium tracking-tight text-[#202124]"
            href={`/${locale}/home`}
          >
            <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-[#1a73e8] text-white shadow-[0_10px_30px_-18px_rgba(26,115,232,0.9)]">
              <VideoCameraIcon className="h-5 w-5" />
            </span>
            <span>{t("brand")}</span>
          </Link>

          <div className="flex flex-col gap-4 sm:flex-row sm:items-center lg:gap-7">
            <div className="flex items-center gap-4 text-sm text-[#3c4043] lg:gap-5">
              <span className="hidden sm:inline">{now}</span>
              <div className="inline-flex items-center rounded-full border border-[#d2d7e1] bg-white p-1 shadow-sm">
                <Link
                  className={`rounded-full px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] transition-colors ${
                    locale === "vi"
                      ? "bg-[#1a73e8] text-white"
                      : "text-[#1a73e8] hover:bg-[#eef3fd]"
                  }`}
                  href="/vi/home"
                >
                  VI
                </Link>
                <Link
                  className={`rounded-full px-3 py-1.5 text-xs font-semibold uppercase tracking-[0.2em] transition-colors ${
                    locale === "en"
                      ? "bg-[#1a73e8] text-white"
                      : "text-[#1a73e8] hover:bg-[#eef3fd]"
                  }`}
                  href="/en/home"
                >
                  EN
                </Link>
              </div>
              <button
                className="inline-flex h-9 w-9 items-center justify-center rounded-full text-[#5f6368] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
                type="button"
              >
                <QuestionIcon className="h-5 w-5" />
              </button>
              <button
                className="inline-flex h-9 w-9 items-center justify-center rounded-full text-[#5f6368] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
                type="button"
              >
                <MessageIcon className="h-5 w-5" />
              </button>
              <button
                className="inline-flex h-9 w-9 items-center justify-center rounded-full text-[#5f6368] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
                type="button"
              >
                <SettingsIcon className="h-5 w-5" />
              </button>
            </div>

            <div className="flex items-center gap-3">
              <Link
                className="inline-flex items-center justify-center rounded-full px-4 py-2.5 text-base font-medium text-[#1a73e8] transition-colors hover:bg-[#eef3fd]"
                href={`/${locale}/login`}
              >
                {t("login")}
              </Link>
              <Link
                className="inline-flex items-center justify-center rounded-2xl bg-[#1a73e8] px-6 py-3 text-base font-medium text-white shadow-[0_16px_32px_-18px_rgba(26,115,232,1)] transition-transform hover:-translate-y-0.5 hover:bg-[#1765cc]"
                href={`/${locale}/register`}
              >
                {t("register")}
              </Link>
            </div>
          </div>
        </header>

        <section className="grid flex-1 items-start gap-10 pt-10 lg:grid-cols-[minmax(0,1fr)_minmax(460px,690px)] lg:gap-14 lg:pt-16">
          <div className="max-w-[760px]">
            <h1 className="max-w-[720px] text-5xl font-medium leading-[1.06] tracking-tight text-[#202124] sm:text-6xl lg:text-[4.8rem]">
              {t("headline")}
            </h1>
            <p className="mt-8 max-w-[640px] text-xl leading-10 text-[#3c4043] sm:text-[1.08rem] sm:leading-9">
              {t("description")}
            </p>

            <div className="mt-10 flex flex-col gap-4 xl:flex-row xl:items-center">
              <button
                className="group inline-flex h-16 items-center justify-center gap-3 rounded-2xl bg-[linear-gradient(135deg,_#1a73e8_0%,_#0f5ed7_100%)] px-6 text-[1.12rem] font-medium text-white shadow-[0_22px_46px_-24px_rgba(26,115,232,0.95)] ring-1 ring-[#1a73e8]/20 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-[0_28px_52px_-24px_rgba(26,115,232,0.95)]"
                type="button"
              >
                <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-white/18 transition-colors group-hover:bg-white/24">
                  <svg
                    aria-hidden="true"
                    className="h-5 w-5"
                    fill="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path d="M11 5h2v14h-2zM5 11h14v2H5z" />
                  </svg>
                </span>
                {t("newMeeting")}
              </button>

              <div className="flex h-16 w-full max-w-[520px] items-center gap-3 rounded-2xl border border-[#d2d7e1] bg-white px-5 text-[#5f6368] shadow-[0_18px_40px_-35px_rgba(15,23,42,0.45)]">
                <svg
                  aria-hidden="true"
                  className="h-5 w-5 text-[#3c4043]"
                  fill="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path d="M4 5a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V5Zm3 1v3h3V6H7Zm4 0v3h3V6h-3Zm4 0v3h2V6h-2ZM7 10v3h3v-3H7Zm4 0v3h3v-3h-3Zm4 0v3h2v-3h-2ZM7 14v3h3v-3H7Zm4 0v3h3v-3h-3Zm4 0v3h2v-3h-2Z" />
                </svg>
                <input
                  className="h-full w-full bg-transparent text-[1.08rem] text-[#202124] outline-none placeholder:text-[#5f6368]"
                  onChange={(event) => setJoinCode(event.target.value)}
                  placeholder={t("joinPlaceholder")}
                  type="text"
                  value={joinCode}
                />
              </div>

              <button
                className={`inline-flex h-16 items-center justify-center rounded-2xl px-5 text-[1.12rem] font-medium transition-all ${
                  hasJoinValue
                    ? "cursor-pointer bg-[#e8f0fe] text-[#1a73e8] shadow-[0_16px_30px_-24px_rgba(26,115,232,0.85)] hover:bg-[#1a73e8] hover:text-white"
                    : "cursor-not-allowed text-[#9aa0a6]"
                }`}
                disabled={!hasJoinValue}
                type="button"
              >
                {t("join")}
              </button>
            </div>

            <div className="mt-10 h-px w-full max-w-[760px] bg-[#e0e3eb]" />

            <Link
              className="mt-10 inline-flex text-lg text-[#1a73e8] transition-colors hover:text-[#1765cc]"
              href={`/${locale}`}
            >
              {t("learnMore")}
            </Link>
          </div>

          <div className="rounded-[1.9rem] border border-[#e2e7ef] bg-[#f9fbff] p-6 shadow-[0_24px_60px_-34px_rgba(15,23,42,0.28)] sm:p-8">
            <div className="grid grid-cols-2 gap-5">
              {PREVIEW_TILES.map((tileClass, index) => (
                <div
                  className={`flex aspect-[1.35] items-center justify-center rounded-[1.1rem] ${tileClass}`}
                  key={tileClass}
                >
                  <VideoCameraIcon className="h-10 w-10" />
                  <span className="sr-only">{index + 1}</span>
                </div>
              ))}
            </div>

            <p className="px-4 pb-3 pt-10 text-center text-[1.05rem] text-[#3c4043] sm:text-[1.12rem]">
              {t("previewCaption")}
            </p>
          </div>
        </section>

        <section className="grid gap-14 pb-20 pt-20 text-center md:grid-cols-3 md:gap-12 lg:pt-28">
          {featureCards.map(({ title, description, icon: Icon }) => (
            <article className="mx-auto max-w-sm" key={title}>
              <span className="mx-auto inline-flex h-14 w-14 items-center justify-center rounded-full bg-[#eef3fd] text-[#1a73e8]">
                <Icon className="h-8 w-8" />
              </span>
              <h2 className="mt-7 text-[2rem] font-normal tracking-tight text-[#202124]">
                {title}
              </h2>
              <p className="mt-6 text-[1.02rem] leading-9 text-[#3c4043]">
                {description}
              </p>
            </article>
          ))}
        </section>

        <footer className="mt-auto flex flex-col gap-5 border-t border-[#e7ebf3] py-8 text-[#5f6368] lg:flex-row lg:items-center lg:justify-between">
          <nav className="flex flex-wrap items-center gap-x-10 gap-y-4 text-lg">
            {footerLinks.map((label) => (
              <Link
                className="transition-colors hover:text-[#1a73e8]"
                href={`/${locale}`}
                key={label}
              >
                {label}
              </Link>
            ))}
          </nav>

          <div className="flex items-center gap-4 text-base">
            <GlobeIcon className="h-7 w-7" />
            <p>{t("copyright")}</p>
          </div>
        </footer>
      </div>
    </main>
  );
}
