/**
 * Link to the Entries tab showing the card entries of one category in a period (RF-12, RF-38).
 *
 * @param period     - billing period, `yyyy-MM`
 * @param categoryId - category; for expenses without one, the id of the system "not classified" category
 * @param member     - member filter of the overview, or `null` for everyone
 * @returns the path with its query
 */
export function categoryEntriesPath(period: string, categoryId: number, member: number | null): string {
  const query = new URLSearchParams({ billingPeriod: period, category: String(categoryId) });
  if (member != null) query.set('member', String(member));
  return `/entries?${query}`;
}
