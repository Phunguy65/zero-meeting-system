"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import { useState } from "react";

function QuestionIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm.06 15.5a1.25 1.25 0 1 1 0-2.5 1.25 1.25 0 0 1 0 2.5ZM14 10.3c-.56.46-1.14.86-1.14 1.7v.5h-1.75v-.7c0-1.08.72-1.74 1.36-2.27.55-.46 1.03-.84 1.03-1.45 0-.77-.58-1.3-1.48-1.3-.9 0-1.56.48-2.12 1.2L8.5 6.86C9.39 5.67 10.63 5 12.18 5c2.06 0 3.56 1.2 3.56 3.06 0 1.09-.68 1.76-1.74 2.24Z" />
    </svg>
  );
}

function SettingsIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="m19.14 12.94.05-.94-.05-.94 1.64-1.28a.8.8 0 0 0 .19-1.03l-1.55-2.68a.8.8 0 0 0-.98-.35l-1.94.78a7.4 7.4 0 0 0-1.63-.94l-.3-2.06a.82.82 0 0 0-.8-.68h-3.1a.82.82 0 0 0-.8.68l-.3 2.06c-.58.22-1.13.53-1.63.94l-1.94-.78a.8.8 0 0 0-.98.35L3.03 8.75a.8.8 0 0 0 .19 1.03l1.64 1.28-.05.94.05.94-1.64 1.28a.8.8 0 0 0-.19 1.03l1.55 2.68c.2.35.62.5.98.35l1.94-.78c.5.4 1.05.72 1.63.94l.3 2.06c.07.4.4.68.8.68h3.1c.4 0 .73-.29.8-.68l.3-2.06c.58-.22 1.13-.53 1.63-.94l1.94.78c.36.15.78 0 .98-.35l1.55-2.68a.8.8 0 0 0-.19-1.03l-1.64-1.28ZM12 15.2A3.2 3.2 0 1 1 12 8.8a3.2 3.2 0 0 1 0 6.4Z" />
    </svg>
  );
}

function UserIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 12a4.5 4.5 0 1 0 0-9 4.5 4.5 0 0 0 0 9Zm0 2c-4.4 0-8 2.69-8 6v1h16v-1c0-3.31-3.6-6-8-6Z" />
    </svg>
  );
}

function MicIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-7 w-7"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 15a3 3 0 0 0 3-3V7a3 3 0 1 0-6 0v5a3 3 0 0 0 3 3Zm5-3a5 5 0 0 1-10 0H5a7 7 0 0 0 6 6.92V22h2v-3.08A7 7 0 0 0 19 12h-2Z" />
    </svg>
  );
}

function VideoIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-7 w-7"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M14 7a2 2 0 0 1 2 2v1.56l3.2-2.4A1 1 0 0 1 21 8.96v6.08a1 1 0 0 1-1.8.8L16 13.44V15a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h9Z" />
    </svg>
  );
}

function DotsIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-7 w-7"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <circle cx="12" cy="5" r="2" />
      <circle cx="12" cy="12" r="2" />
      <circle cx="12" cy="19" r="2" />
    </svg>
  );
}

function ArrowRightIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeLinecap="round"
      strokeWidth="2.2"
      viewBox="0 0 24 24"
    >
      <path d="M5 12h14M13 5l7 7-7 7" />
    </svg>
  );
}

type Attendee = {
  name: string;
  initials: string;
  palette: string;
};

const ATTENDEES: Attendee[] = [
  {
    name: "Sarah Chen",
    initials: "SC",
    palette: "from-[#0f172a] to-[#334155]",
  },
  {
    name: "Marcus Wright",
    initials: "MW",
    palette: "from-[#1d4ed8] to-[#60a5fa]",
  },
  {
    name: "Elena Rodriguez",
    initials: "ER",
    palette: "from-[#b91c1c] to-[#fb7185]",
  },
];

export function GreenRoomScreen() {
  const locale = useLocale();
  const t = useTranslations("greenRoom");
  const [micEnabled, setMicEnabled] = useState(true);
  const [videoEnabled, setVideoEnabled] = useState(true);

  return (
    <main className="min-h-screen bg-[#f8fafc] text-[#15191f]">
      <header className="sticky top-0 z-40 border-b border-[#e5eaf2] bg-white/94 backdrop-blur">
        <div className="mx-auto flex max-w-[1600px] items-center justify-between px-6 py-4 sm:px-8 lg:px-10">
          <Link
            className="text-[1.85rem] font-semibold tracking-tight text-[#1a73e8]"
            href={`/${locale}/workspace`}
          >
            Zero Meeting
          </Link>

          <div className="flex items-center gap-4 sm:gap-5">
            <button
              className="inline-flex h-11 w-11 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
              type="button"
            >
              <QuestionIcon />
            </button>
            <button
              className="inline-flex h-11 w-11 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
              type="button"
            >
              <SettingsIcon />
            </button>
            <button
              className="inline-flex h-12 w-12 items-center justify-center rounded-full bg-[linear-gradient(135deg,_#243b67_0%,_#4e7fd4_100%)] text-white shadow-[0_14px_28px_-18px_rgba(26,115,232,0.85)]"
              type="button"
            >
              <UserIcon />
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto grid min-h-[calc(100vh-80px)] max-w-[1600px] gap-8 px-6 py-7 sm:px-8 lg:grid-cols-[1.12fr_0.82fr] lg:px-10 lg:py-8">
        <section className="flex items-center">
          <div className="relative aspect-[1.6] w-full overflow-hidden rounded-[1.7rem] bg-[linear-gradient(135deg,_#111827_0%,_#2b313b_32%,_#111827_100%)] shadow-[0_26px_70px_-38px_rgba(15,23,42,0.35)]">
            <div className="absolute inset-0 bg-[radial-gradient(circle_at_40%_50%,_rgba(255,213,128,0.12),_transparent_28%),linear-gradient(90deg,_rgba(0,0,0,0.52)_0%,_rgba(0,0,0,0.12)_45%,_rgba(0,0,0,0.62)_100%)]" />
            <div className="absolute left-[43%] top-[20%] h-[46%] w-[12%] rounded-[0.9rem] border border-white/12 bg-[linear-gradient(180deg,_rgba(42,46,52,0.5),_rgba(24,28,33,0.68))] shadow-[0_20px_40px_-26px_rgba(0,0,0,0.8)]" />
            <div className="absolute left-[47%] top-[40%] h-[10%] w-[7%] rotate-45 border-b-[6px] border-r-[6px] border-[#1f2937] opacity-80" />

            <span className="absolute left-6 top-6 rounded-2xl bg-black/38 px-4 py-1.5 text-[0.95rem] font-medium text-white backdrop-blur">
              {t("preview")}
            </span>

            <div className="absolute bottom-6 left-1/2 flex -translate-x-1/2 items-center gap-4 rounded-[2rem] bg-white/88 px-6 py-4 shadow-[0_24px_60px_-36px_rgba(15,23,42,0.55)] backdrop-blur">
              <button
                className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-[#111827] transition-colors ${
                  micEnabled ? "bg-[#f3f4f6]" : "bg-[#fee2e2] text-[#b42318]"
                }`}
                onClick={() => setMicEnabled((value) => !value)}
                type="button"
              >
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm">
                  <MicIcon />
                </span>
                <span className="text-[0.8rem] font-medium uppercase tracking-[0.12em]">
                  {t("mic")}
                </span>
              </button>

              <button
                className={`flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl px-2.5 py-2 text-[#111827] transition-colors ${
                  videoEnabled ? "bg-[#f3f4f6]" : "bg-[#fee2e2] text-[#b42318]"
                }`}
                onClick={() => setVideoEnabled((value) => !value)}
                type="button"
              >
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm">
                  <VideoIcon />
                </span>
                <span className="text-[0.8rem] font-medium uppercase tracking-[0.12em]">
                  {t("video")}
                </span>
              </button>

              <button
                className="flex min-w-[78px] flex-col items-center gap-1.5 rounded-2xl bg-[#f3f4f6] px-2.5 py-2 text-[#111827] transition-colors hover:bg-[#e9eef7]"
                type="button"
              >
                <span className="flex h-12 w-12 items-center justify-center rounded-full bg-white/90 shadow-sm">
                  <DotsIcon />
                </span>
                <span className="text-[0.8rem] font-medium uppercase tracking-[0.12em]">
                  {t("check")}
                </span>
              </button>
            </div>
          </div>
        </section>

        <aside className="flex items-center">
          <div className="w-full max-w-[400px] lg:ml-auto">
            <p className="text-[1.7rem] leading-none text-[#344054]">
              {t("ready")}
            </p>
            <h1 className="mt-3 text-5xl font-semibold leading-[0.98] tracking-tight text-[#15191f] xl:text-[4.2rem]">
              {t("meetingTitle")}
            </h1>

            <p className="mt-9 text-[1.18rem] text-[#344054]">
              {t("alreadyInMeeting", { count: ATTENDEES.length })}
            </p>

            <div className="mt-6 space-y-6">
              {ATTENDEES.map((attendee) => (
                <div className="flex items-center gap-5" key={attendee.name}>
                  <div
                    className={`flex h-12 w-12 items-center justify-center rounded-full bg-gradient-to-br ${attendee.palette} text-sm font-semibold text-white shadow-[0_14px_28px_-18px_rgba(15,23,42,0.45)]`}
                  >
                    {attendee.initials}
                  </div>
                  <span className="text-[1.4rem] font-medium tracking-tight text-[#15191f]">
                    {attendee.name}
                  </span>
                </div>
              ))}
            </div>

            <Link
              className="mt-10 flex h-16 w-full items-center justify-center gap-3 rounded-[1.1rem] bg-[#1a73e8] px-8 text-[1.55rem] font-semibold text-white shadow-[0_24px_50px_-26px_rgba(26,115,232,0.95)] transition-all hover:-translate-y-0.5 hover:bg-[#1765cc]"
              href={`/${locale}/workspace/meeting-room`}
            >
              {t("joinNow")}
              <ArrowRightIcon />
            </Link>

            <p className="mt-5 text-center text-base text-[#475467]">
              {t("otherJoinOptions")}{" "}
              <button
                className="font-medium text-[#1a73e8] transition-colors hover:text-[#1765cc]"
                type="button"
              >
                {t("phoneAudio")}
              </button>
            </p>
          </div>
        </aside>
      </div>
    </main>
  );
}
