import { sanitizeUrl as braintreeSanitizeUrl } from "@braintree/sanitize-url"

// Sanitize URLs in the same way that swagger-ui does
export function sanitizeUrl(url) {
  if(typeof url !== "string" || url === "") {
    return ""
  }

  return braintreeSanitizeUrl(url)
}
