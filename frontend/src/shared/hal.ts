/** Any HAL resource as sent by the API (API-08). */
export interface HalResource {
  _links?: Record<string, { href?: string } | undefined>;
}

/**
 * Returns the `href` of a link, if the resource has it (API-09: actions exist only when their link does).
 *
 * @param resource - HAL resource
 * @param rel - link relation, such as `rename` or `delete`
 * @returns the href, or `undefined`
 */
export function linkOf(resource: HalResource | undefined | null, rel: string): string | undefined {
  return resource?._links?.[rel]?.href;
}

/**
 * Reads the embedded list of a HAL collection.
 *
 * @param page - HAL collection or page
 * @param rel - collection relation, such as `members`
 * @returns the items, or an empty list
 */
export function embedded<T>(page: { _embedded?: Record<string, T[] | undefined> } | undefined, rel: string): T[] {
  return page?._embedded?.[rel] ?? [];
}
