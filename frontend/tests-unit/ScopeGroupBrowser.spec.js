import { beforeEach, describe, expect, it, vi } from 'vitest';
import { flushPromises, mount } from '@vue/test-utils';

vi.mock('../src/services/api', () => ({ api: vi.fn() }));
import { api } from '../src/services/api';
import { createScopeMeta } from '../src/lib/scopeMeta';
import ScopeGroupBrowser from '../src/components/ScopeGroupBrowser.vue';
import ScopePicker from '../src/components/ScopePicker.vue';

const page = (content) => ({ content, last: true, totalElements: content.length });

function backend(url) {
  if (url.startsWith('/admin/options/categories')) return Promise.resolve(page([{ id: 1, name: 'Laptop', productCount: 3 }, { id: 2, name: 'Chuột', productCount: 2 }]));
  if (url.startsWith('/admin/options/products')) return Promise.resolve(page([{ id: 11, name: 'XPS', price: 1000, categoryId: 1, category: 'Laptop', active: true, quantity: 1 }]));
  if (url.startsWith('/admin/offers/preview')) return Promise.resolve({ valid: true, total: 0, inStock: 0, sample: [] });
  return Promise.resolve(page([]));
}

function mountBrowser(props) {
  const meta = createScopeMeta();
  const wrapper = mount(ScopeGroupBrowser, { props: { kind: 'categories', ...props }, global: { provide: { scopeMeta: meta } } });
  return { wrapper, meta };
}

describe('category and brand browser', () => {
  beforeEach(() => { api.mockReset(); api.mockImplementation(backend); });

  it('takes a whole category and drops the single products it already covers', async () => {
    const { wrapper, meta } = mountBrowser({ whole: [], products: [11] });
    meta.remember('products', { id: 11, name: 'XPS', categoryId: 1, category: 'Laptop' });
    await flushPromises();
    const mode = wrapper.find('[aria-label="Cách áp dụng"]').findAll('button');
    await mode[0].trigger('click');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ whole: [1], products: [] });
  });

  it('gives a category back without touching products of other categories', async () => {
    const { wrapper, meta } = mountBrowser({ whole: [1], products: [21] });
    meta.remember('products', { id: 21, name: 'Mouse', categoryId: 2, category: 'Chuột' });
    await flushPromises();
    await wrapper.find('[aria-label="Cách áp dụng"]').findAll('button')[1].trigger('click');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ whole: [] });
  });

  it('summarises whole groups and single products per group and removes them from the chips', async () => {
    const { wrapper, meta } = mountBrowser({ whole: [2], products: [11, 12] });
    meta.remember('products', { id: 11, name: 'XPS', categoryId: 1, category: 'Laptop' });
    meta.remember('products', { id: 12, name: 'Latitude', categoryId: 1, category: 'Laptop' });
    meta.remember('categories', { id: 2, name: 'Chuột' });
    await flushPromises();
    const chips = wrapper.findAll('.tray-chip .open').map((b) => b.text());
    expect(chips).toEqual(['Chuột · toàn bộ', 'Laptop · 2 sản phẩm']);
    await wrapper.findAll('.tray-chip .drop')[1].trigger('click');
    expect(wrapper.emitted('change').at(-1)[0]).toEqual({ products: [] });
  });
});

describe('scope picker', () => {
  beforeEach(() => { api.mockReset(); api.mockImplementation(backend); });

  it('clears every list when the mode changes and ignores a click on the current mode', async () => {
    const wrapper = mount(ScopePicker, { props: { kind: 'promotion', modelValue: { scopeType: 'CATEGORY', categoryIds: [1], brandIds: [], productIds: [11] } } });
    await flushPromises();
    const buttons = wrapper.find('[aria-label="Chọn phạm vi áp dụng"]').findAll('button');
    await buttons[1].trigger('click');
    expect(wrapper.emitted('update:modelValue')).toBeUndefined();
    await buttons[2].trigger('click');
    expect(wrapper.emitted('update:modelValue').at(-1)[0]).toMatchObject({ scopeType: 'BRAND', categoryIds: [], brandIds: [], productIds: [] });
  });
});
