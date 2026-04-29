import { redirect } from "next/navigation";
import { routing } from "@/i18n/request";

export default function Home() {
  redirect(`/${routing.defaultLocale}`);
}
