import { DynamicIcon, type IconName } from 'lucide-react/dynamic';

/**
 * Icon and colour of a category; the icon name comes from the category record.
 *
 * @param props - icon name and colour
 * @returns the icon in a coloured circle
 */
export function CategoryIcon({ icon, color }: { icon?: string; color?: string }) {
  return (
    <span className="inline-flex size-6 items-center justify-center rounded-full text-white" style={{ background: color }} aria-hidden>
      <DynamicIcon name={(icon ?? 'circle') as IconName} size={14} fallback={() => null} />
    </span>
  );
}
