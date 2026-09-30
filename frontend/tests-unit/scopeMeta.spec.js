import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../src/services/api', () => ({ api: vi.fn() }));
import { api } from '../src/services/api';
import { createScopeMeta } from '../src/lib/scopeMeta';

describe('scope meta', () => {
  beforeEach(() => api.mockReset());

  it('remembers a product and the category and brand it names', () => {
    const meta = createScopeMeta();
    meta.remember('products', { id: 7, name: 'Dell XPS', category: 'Laptop', categoryId: 2, brand: 'Dell', brandId: 5 });
    expect(meta.state.products[7].name).toBe('Dell XPS');
    expect(meta.state.categories[2]).toEqual({ id: 2, name: 'Laptop' });
    expect(meta.state.brands[5]).toEqual({ id: 5, name: 'Dell' });
  });

  it('keeps the richer group row when a product row names the same group', () => {
    const meta = createScopeMeta();
    meta.remember('categories', { id: 2, name: 'Laptop', image: '/c.png', productCount: 9 });
    meta.remember('products', { id: 7, name: 'Dell XPS', category: 'Laptop', categoryId: 2 });
    expect(meta.state.categories[2].productCount).toBe(9);
  });

  it('asks only for ids it does not know, following every page', async () => {
    const meta = createScopeMeta();
    meta.remember('products', { id: 1, name: 'Known' });
    api.mockResolvedValueOnce({ content: [{ id: 2, name: 'Two' }], last: false }).mockResolvedValueOnce({ content: [{ id: 3, name: 'Three' }], last: true });
    await meta.hydrate('products', [1, 2, 3, 2]);
    expect(api).toHaveBeenCalledTimes(2);
    expect(api.mock.calls[0][0]).toContain('ids=2%2C3');
    expect(Object.keys(meta.state.products)).toEqual(['1', '2', '3']);
  });

  it('makes no request when everything is known', async () => {
    const meta = createScopeMeta();
    meta.remember('brands', { id: 1, name: 'Dell' });
    await meta.hydrate('brands', [1]);
    expect(api).not.toHaveBeenCalled();
  });
});
