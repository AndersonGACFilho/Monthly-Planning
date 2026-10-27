import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { FilterableList } from './FilterableList';

interface Row {
  id: number;
  name: string;
  value: number;
}

const rows: Row[] = [
  { id: 1, name: 'Mercado', value: 50 },
  { id: 2, name: 'Farmácia', value: 20 },
  { id: 3, name: 'Posto', value: 80 },
];

function names() {
  return within(screen.getAllByRole('rowgroup')[1]!)
    .getAllByRole('row')
    .map((row) => row.firstElementChild?.textContent);
}

describe('FilterableList', () => {
  it('RF-41: sorts by any sortable column, both directions', () => {
    render(
      <FilterableList
        rows={rows}
        rowKey={(r) => r.id}
        initialSort="name"
        columns={[
          { key: 'name', header: 'Nome', sortValue: (r) => r.name, render: (r) => r.name },
          { key: 'value', header: 'Valor', numeric: true, sortValue: (r) => r.value, render: (r) => r.value },
        ]}
      />,
    );
    expect(names()).toEqual(['Farmácia', 'Mercado', 'Posto']);
    fireEvent.click(screen.getByRole('button', { name: 'Valor' }));
    expect(names()).toEqual(['Farmácia', 'Mercado', 'Posto']);
    fireEvent.click(screen.getByRole('button', { name: 'Valor' }));
    expect(names()).toEqual(['Posto', 'Mercado', 'Farmácia']);
  });

  it('RF-35, RF-42: text search filters rows and reports the visible ones', () => {
    const onVisible = vi.fn();
    render(
      <FilterableList
        rows={rows}
        rowKey={(r) => r.id}
        searchText={(r) => r.name}
        onVisibleRowsChange={onVisible}
        columns={[{ key: 'name', header: 'Nome', render: (r) => r.name }]}
      />,
    );
    fireEvent.change(screen.getByRole('searchbox'), { target: { value: 'post' } });
    expect(names()).toEqual(['Posto']);
    expect(onVisible).toHaveBeenLastCalledWith([rows[2]]);
  });
});
