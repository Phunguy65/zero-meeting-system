import createMiddleware from "next-intl/middleware";
import { routing } from "./src/i18n/request";

export default createMiddleware(routing);

export const config = {
  matcher: ["/", "/(vi|en)/:path*", "/((?!_next|_vercel|.*\\..*).*)"],
};
