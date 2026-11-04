import { ArrowDown, ArrowUp } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { TemplateSettingsBlocksItem } from '../../api/generated/model';

/** Every statement block, in the standard order (RF-82). */
export const STATEMENT_BLOCKS: readonly TemplateSettingsBlocksItem[] = [
  'SUMMARY',
  'CARDS',
  'FIXED_EXPENSES',
  'VARIABLE_EXPENSES',
  'COMMITTED',
  'PAYMENT',
  'NOTE',
];

/** Props of {@link BlockOrderList}. */
export interface BlockOrderListProps {
  /** Blocks shown, in order. */
  blocks: TemplateSettingsBlocksItem[];
  /** Called with the new list. */
  onChange: (blocks: TemplateSettingsBlocksItem[]) => void;
  /** Keeps at least one block ticked. */
  requireOne?: boolean;
}

/**
 * Statement blocks to tick and reorder: the shown ones first, in their order, then the others (TPL-02, IMP-60).
 *
 * @param props - {@link BlockOrderListProps}
 * @returns the list
 */
export function BlockOrderList({ blocks, onChange, requireOne = false }: BlockOrderListProps) {
  const { t } = useTranslation();
  const move = (index: number, delta: number) => {
    const next = [...blocks];
    const [item] = next.splice(index, 1);
    next.splice(index + delta, 0, item!);
    onChange(next);
  };
  const toggle = (block: TemplateSettingsBlocksItem) =>
    onChange(blocks.includes(block) ? blocks.filter((b) => b !== block) : [...blocks, block]);

  return (
    <>
      {[...blocks, ...STATEMENT_BLOCKS.filter((b) => !blocks.includes(b))].map((block) => {
        const index = blocks.indexOf(block);
        return (
          <div key={block} className="flex items-center gap-2">
            <label className="flex flex-1 items-center gap-2">
              <input
                type="checkbox"
                checked={index >= 0}
                disabled={requireOne && index >= 0 && blocks.length === 1}
                onChange={() => toggle(block)}
              />
              {t(`templates.blockNames.${block}`)}
            </label>
            <button type="button" className="icon-button" aria-label={t('templates.moveUp')} disabled={index <= 0} onClick={() => move(index, -1)}>
              <ArrowUp size={14} aria-hidden />
            </button>
            <button
              type="button"
              className="icon-button"
              aria-label={t('templates.moveDown')}
              disabled={index < 0 || index === blocks.length - 1}
              onClick={() => move(index, 1)}
            >
              <ArrowDown size={14} aria-hidden />
            </button>
          </div>
        );
      })}
    </>
  );
}
