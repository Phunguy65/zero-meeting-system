"use client";

import Link from "next/link";
import { useLocale, useTranslations } from "next-intl";
import type { ReactNode } from "react";

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

function BellIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="currentColor"
      viewBox="0 0 24 24"
    >
      <path d="M12 2a4 4 0 0 0-4 4v1.1A6.99 6.99 0 0 0 5 13v3l-1.5 1.5V19h17v-1.5L19 16v-3a6.99 6.99 0 0 0-3-5.9V6a4 4 0 0 0-4-4Zm0 20a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 22Z" />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg
      aria-hidden="true"
      className="h-6 w-6"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      viewBox="0 0 24 24"
    >
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-3.5-3.5" />
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

type WorkspaceShellProps = {
  activeTab: "home" | "schedule" | "profile";
  rightMode?: "compact" | "search";
  children: ReactNode;
};

export function WorkspaceShell({
  activeTab,
  rightMode = "compact",
  children,
}: WorkspaceShellProps) {
  const locale = useLocale();
  const t = useTranslations("workspace.common");
  const basePath = `/${locale}/workspace`;

  const tabs = [
    { id: "home", label: t("navHome"), href: basePath },
    { id: "schedule", label: t("navSchedule"), href: `${basePath}/schedule` },
    { id: "profile", label: t("navProfile"), href: `${basePath}/profile` },
  ] as const;

  return (
    <main className="min-h-screen bg-[#f7f8fb] text-[#111827]">
      <header className="sticky top-0 z-40 border-b border-[#e5eaf2] bg-white/92 backdrop-blur">
        <div className="mx-auto flex max-w-[1600px] items-center justify-between px-6 py-5 sm:px-8 lg:px-10">
          <div className="flex items-center gap-8 lg:gap-14">
            <Link
              className="text-[2.05rem] font-semibold tracking-tight text-[#1a73e8]"
              href={basePath}
            >
              {t("brand")}
            </Link>

            <nav className="hidden items-center gap-8 text-[1.08rem] sm:flex">
              {tabs.map((tab) => {
                const isActive = tab.id === activeTab;

                return (
                  <Link
                    className={`border-b-[3px] pb-2 transition-colors ${
                      isActive
                        ? "border-[#1a73e8] font-medium text-[#1a73e8]"
                        : "border-transparent text-[#334155] hover:text-[#1a73e8]"
                    }`}
                    href={tab.href}
                    key={tab.id}
                  >
                    {tab.label}
                  </Link>
                );
              })}
            </nav>
          </div>

          <div className="flex items-center gap-4 sm:gap-6">
            {rightMode === "search" ? (
              <>
                <div className="hidden items-center gap-3 rounded-full bg-[#f2f4f8] px-5 py-3 text-[#667085] shadow-inner sm:flex sm:min-w-[290px]">
                  <SearchIcon />
                  <span className="text-[1.05rem]">
                    {t("searchPlaceholder")}
                  </span>
                </div>
                <button
                  className="inline-flex h-11 w-11 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
                  type="button"
                >
                  <BellIcon />
                </button>
              </>
            ) : (
              <button
                className="inline-flex h-11 w-11 items-center justify-center rounded-full text-[#475467] transition-colors hover:bg-[#eef3fd] hover:text-[#1a73e8]"
                type="button"
              >
                <QuestionIcon />
              </button>
            )}

            <button
              className="inline-flex h-12 w-12 items-center justify-center rounded-full bg-[linear-gradient(135deg,_#243b67_0%,_#4e7fd4_100%)] text-white shadow-[0_14px_28px_-18px_rgba(26,115,232,0.85)]"
              type="button"
            >
              <UserIcon />
            </button>
          </div>
        </div>
      </header>

      <div className="mx-auto max-w-[1600px] px-6 py-8 sm:px-8 lg:px-10 lg:py-10">
        {children}
      </div>
    </main>
  );
}
