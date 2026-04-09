import { client } from "@/generated/client.gen";
import { createJsendMiddleware } from "./jsend-middleware";
import type { ErrorTranslator } from "./types";

/**
 * Configures the shared @hey-api client with the JSend unwrap middleware.
 *
 * Call this once at application startup (e.g., in a layout or provider).
 *
 * @param baseUrl    — API gateway base URL (defaults to empty string for relative URLs)
 * @param translator — optional i18n hook for error message translation
 */
export function configureApiClient(baseUrl = "", translator?: ErrorTranslator) {
  client.setConfig({ baseUrl });
  client.interceptors.response.use(createJsendMiddleware(translator));
}

export { client };
