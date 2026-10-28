import { type IconName, iconNames } from 'lucide-react/dynamic';

/** Icons offered first, before any search: the usual household spending groups. */
export const SUGGESTED_ICONS: readonly IconName[] = [
  'shopping-cart', 'utensils', 'car', 'heart-pulse', 'party-popper', 'repeat', 'house', 'shirt',
  'graduation-cap', 'plane', 'paw-print', 'circle-ellipsis', 'baby', 'fuel', 'gift', 'pill', 'wrench', 'zap',
  'bus', 'bike', 'coffee', 'pizza', 'beer', 'dumbbell', 'gamepad-2', 'film', 'music', 'book-open', 'smartphone',
  'laptop', 'wifi', 'droplets', 'flame', 'receipt', 'credit-card', 'wallet', 'piggy-bank', 'landmark',
  'briefcase', 'hammer', 'sofa', 'scissors', 'stethoscope', 'tree-palm', 'ticket', 'store', 'package',
];

/** Result of {@link searchIcons}: the icons to draw and how many matched in all. */
export interface IconMatches {
  icons: IconName[];
  total: number;
}

/**
 * Finds icons whose name contains every word of the query; with no query, the suggested icons come first.
 *
 * @param query - text typed by the user
 * @param limit - most icons returned, so the grid stays light
 * @returns the icons to draw and the number of matches
 */
export function searchIcons(query: string, limit: number): IconMatches {
  const words = query.trim().toLowerCase().split(/[\s-]+/).filter(Boolean);
  const matches = words.length === 0
    ? [...new Set([...SUGGESTED_ICONS, ...iconNames])]
    : iconNames.filter((name) => words.every((word) => name.includes(word)));
  return { icons: matches.slice(0, limit), total: matches.length };
}
