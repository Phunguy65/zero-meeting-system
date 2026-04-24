"use client";

import { useTranslations } from "next-intl";
import { useState } from "react";
import { WorkspaceShell } from "@/components/workspace-shell";

function CalendarIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      viewBox="0 0 24 24"
    >
      <rect x="4" y="5" width="16" height="15" rx="2" />
      <path d="M8 3v4M16 3v4M4 10h16" />
    </svg>
  );
}

function ClockIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      viewBox="0 0 24 24"
    >
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 3" />
    </svg>
  );
}

function UsersIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M9 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6Zm6-1a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5ZM3 18a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v1H3v-1Zm11 1v-1c0-1.13-.37-2.18-.99-3.03.31-.06.64-.1.99-.1h2a4 4 0 0 1 4 4v.13A1.87 1.87 0 0 1 19.87 19H14Z" />
    </svg>
  );
}

function VideoIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M14 7a2 2 0 0 1 2 2v1.56l3.2-2.4A1 1 0 0 1 21 8.96v6.08a1 1 0 0 1-1.8.8L16 13.44V15a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2h9Z" />
    </svg>
  );
}

function InfoIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Zm0 4.75a1.25 1.25 0 1 1 0 2.5 1.25 1.25 0 0 1 0-2.5Zm1.4 10.5h-2.8v-1.5h.65v-3h-.65v-1.5h2.15v4.5h.65v1.5Z" />
    </svg>
  );
}

function Toggle({
  checked,
  onToggle,
}: {
  checked: boolean;
  onToggle: () => void;
}) {
  return (
    <button
      aria-pressed={checked}
      className={`relative inline-flex h-8 w-14 items-center rounded-full transition-colors ${
        checked ? "bg-[#1a73e8]" : "bg-[#e2e8f0]"
      }`}
      onClick={onToggle}
      type="button"
    >
      <span
        className={`h-7 w-7 rounded-full bg-white shadow transition-transform ${
          checked ? "translate-x-7" : "translate-x-1"
        }`}
      />
    </button>
  );
}

export function WorkspaceScheduleScreen() {
  const t = useTranslations("workspace.schedule");
  const [waitingRoom, setWaitingRoom] = useState(true);
  const [hostVideo, setHostVideo] = useState(false);

  return (
    <WorkspaceShell activeTab="schedule" rightMode="search">
      <section className="mx-auto max-w-[1280px]">
        <div className="max-w-[720px]">
          <h1 className="text-5xl font-semibold tracking-tight text-[#15191f]">
            {t("headline")}
          </h1>
          <p className="mt-4 text-2xl leading-9 text-[#344054] sm:text-[1.1rem]">
            {t("description")}
          </p>
        </div>

        <div className="mt-12 grid gap-8 xl:grid-cols-[1.12fr_0.78fr]">
          <article className="rounded-[2rem] bg-white p-8 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.2)] sm:p-10">
            <form
              className="space-y-8"
              onSubmit={(event) => event.preventDefault()}
            >
              <label className="block">
                <span className="text-[1.6rem] font-semibold tracking-tight text-[#15191f]">
                  {t("topicLabel")}
                </span>
                <input
                  className="mt-4 h-16 w-full rounded-[1.2rem] bg-[#f2f4f7] px-6 text-xl text-[#111827] outline-none ring-1 ring-transparent transition focus:ring-2 focus:ring-[#1a73e8]"
                  placeholder={t("topicPlaceholder")}
                  type="text"
                />
              </label>

              <div className="grid gap-6 sm:grid-cols-2">
                <label className="block">
                  <span className="text-[1.45rem] font-semibold tracking-tight text-[#15191f]">
                    {t("dateLabel")}
                  </span>
                  <div className="mt-4 flex h-16 items-center justify-between rounded-[1.2rem] bg-[#f2f4f7] px-6">
                    <input
                      className="w-full bg-transparent text-xl text-[#111827] outline-none"
                      placeholder={t("datePlaceholder")}
                      type="text"
                    />
                    <span className="text-[#475467]">
                      <CalendarIcon />
                    </span>
                  </div>
                </label>

                <label className="block">
                  <span className="text-[1.45rem] font-semibold tracking-tight text-[#15191f]">
                    {t("timeLabel")}
                  </span>
                  <div className="mt-4 flex h-16 items-center justify-between rounded-[1.2rem] bg-[#f2f4f7] px-6">
                    <input
                      className="w-full bg-transparent text-xl text-[#111827] outline-none"
                      placeholder={t("timePlaceholder")}
                      type="text"
                    />
                    <span className="text-[#475467]">
                      <ClockIcon />
                    </span>
                  </div>
                </label>
              </div>

              <div className="block">
                <span className="text-[1.45rem] font-semibold tracking-tight text-[#15191f]">
                  {t("durationLabel")}
                </span>
                <div className="mt-4 flex h-16 items-center justify-between rounded-[1.2rem] bg-[#f2f4f7] px-6 text-xl text-[#111827]">
                  <span>{t("durationValue")}</span>
                  <span className="text-2xl text-[#667085]">⌄</span>
                </div>
              </div>

              <label className="block">
                <span className="text-[1.45rem] font-semibold tracking-tight text-[#15191f]">
                  {t("inviteesLabel")}
                </span>
                <div className="mt-4 flex h-16 items-center justify-between rounded-[1.2rem] bg-[#f2f4f7] px-6">
                  <input
                    className="w-full bg-transparent text-xl text-[#111827] outline-none"
                    placeholder={t("inviteesPlaceholder")}
                    type="text"
                  />
                  <span className="text-[#475467]">
                    <UsersIcon />
                  </span>
                </div>
              </label>

              <button
                className="h-18 w-full rounded-[1.2rem] bg-[#1a73e8] px-8 text-[1.9rem] font-semibold text-white shadow-[0_22px_46px_-24px_rgba(26,115,232,0.9)] transition-colors hover:bg-[#1765cc]"
                type="submit"
              >
                {t("submit")}
              </button>
            </form>
          </article>

          <div className="space-y-8">
            <article className="overflow-hidden rounded-[2rem] bg-[linear-gradient(145deg,_#20444d_0%,_#18353e_45%,_#132b33_100%)] shadow-[0_26px_70px_-38px_rgba(15,23,42,0.2)]">
              <div className="relative min-h-[320px] p-8">
                <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-black/40 to-transparent" />
                <div className="absolute left-10 top-16 h-40 w-1 bg-[#8597a1]/40" />
                <div className="absolute left-10 top-16 h-20 w-20 rounded-full border-4 border-[#96a8b2]/40 border-l-transparent border-b-transparent" />
                <div className="absolute left-10 top-52 h-6 w-20 rounded-full bg-[#8e7558]" />
                <div className="absolute bottom-16 left-10 h-4 w-32 rounded-full bg-[#6b553d]" />
                <div className="absolute right-10 top-12 h-32 w-28 rounded-[1.2rem] border-4 border-[#5c5042] bg-[linear-gradient(180deg,_#243b44,_#132b33)]" />
                <div className="absolute right-20 top-24 h-12 w-12 rounded-full border-[6px] border-[#a9aca4]" />
                <div className="absolute right-24 top-38 h-4 w-8 rounded-full bg-[#a9aca4]" />
                <div className="absolute right-16 bottom-12 h-34 w-46 rounded-[1.4rem] bg-[linear-gradient(160deg,_#f0f1f4_0%,_#c7ccd5_100%)] shadow-lg" />
                <div className="absolute bottom-10 right-8 h-5 w-20 rounded-full bg-[#525b63]" />
                <p className="absolute bottom-8 left-8 max-w-[260px] text-xl font-medium text-white">
                  {t("visualCaption")}
                </p>
              </div>
            </article>

            <article className="rounded-[2rem] bg-[#f2f4f7] p-8 shadow-[0_26px_70px_-38px_rgba(15,23,42,0.16)]">
              <h2 className="text-[2rem] font-semibold tracking-tight text-[#15191f]">
                {t("settingsTitle")}
              </h2>

              <div className="mt-7 space-y-5">
                <div className="flex items-center justify-between rounded-[1.2rem] bg-white p-5">
                  <div className="flex items-center gap-4">
                    <span className="text-[#1a73e8]">
                      <CalendarIcon />
                    </span>
                    <div>
                      <p className="text-[1.45rem] font-semibold text-[#15191f]">
                        {t("waitingRoomTitle")}
                      </p>
                      <p className="mt-1 text-lg text-[#475467]">
                        {t("waitingRoomDescription")}
                      </p>
                    </div>
                  </div>
                  <Toggle
                    checked={waitingRoom}
                    onToggle={() => setWaitingRoom((value) => !value)}
                  />
                </div>

                <div className="flex items-center justify-between rounded-[1.2rem] bg-white p-5">
                  <div className="flex items-center gap-4">
                    <span className="text-[#1a73e8]">
                      <VideoIcon />
                    </span>
                    <div>
                      <p className="text-[1.45rem] font-semibold text-[#15191f]">
                        {t("hostVideoTitle")}
                      </p>
                      <p className="mt-1 text-lg text-[#475467]">
                        {t("hostVideoDescription")}
                      </p>
                    </div>
                  </div>
                  <Toggle
                    checked={hostVideo}
                    onToggle={() => setHostVideo((value) => !value)}
                  />
                </div>
              </div>

              <div className="mt-7 flex gap-4 rounded-[1.2rem] bg-[#dce9ff] p-5 text-[#344054]">
                <span className="text-[#1a73e8]">
                  <InfoIcon />
                </span>
                <p className="text-lg leading-8">{t("note")}</p>
              </div>
            </article>
          </div>
        </div>

        <footer className="mt-12 text-center text-[1.2rem] text-[#667085]">
          {t("footer")}
        </footer>
      </section>
    </WorkspaceShell>
  );
}
