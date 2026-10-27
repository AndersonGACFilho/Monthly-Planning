import { useMutation, useQueryClient } from '@tanstack/react-query';
import { apiFetch } from '../../api/http';
import { linkOf, type HalResource } from '../hal';

/** Options of one action call. */
export interface ActionCall {
  /** HTTP method; defaults to `POST`. */
  method?: 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  /** JSON body. */
  body?: unknown;
  /** Version being edited, sent as `If-Match` (API-04). */
  version?: string | null;
}

/**
 * Runs a business action offered by a HAL link (API-09). The button that uses it should only be rendered
 * when `available` is true, so the interface never repeats the backend's state rules (DRY).
 *
 * @param resource - resource whose `_links` may hold the action
 * @param rel - link relation, such as `delete`, `inactivate` or `rename`
 * @returns whether the action is offered, its href and a mutation that calls it and refreshes cached data
 */
export function useAction(resource: HalResource | undefined | null, rel: string) {
  const queryClient = useQueryClient();
  const href = linkOf(resource, rel);
  const mutation = useMutation({
    mutationFn: ({ method = 'POST', body, version }: ActionCall = {}) => {
      if (!href) throw new Error(`Action ${rel} is not available`);
      const headers: Record<string, string> = {};
      if (body !== undefined) headers['Content-Type'] = 'application/json';
      if (version) headers['If-Match'] = version;
      return apiFetch<unknown>(href, {
        method,
        headers,
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    },
    onSuccess: () => queryClient.invalidateQueries(),
  });
  return { available: Boolean(href), href, ...mutation };
}
