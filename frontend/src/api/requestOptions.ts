/**
 * Request options that send the edited version as `If-Match`, so two screens never overwrite each other
 * (API-04).
 *
 * @param version - version of the resource being edited
 * @returns options for a generated API function
 */
export function ifMatch(version: number | undefined): RequestInit {
  return version === undefined ? {} : { headers: { 'If-Match': `"${version}"` } };
}
