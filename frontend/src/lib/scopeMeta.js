import { reactive } from 'vue';
import { api } from '../services/api';

// What the scope picker knows about the categories, brands and products it has shown, keyed by id.
// A saved scope holds only ids, so names, pictures and group membership are looked up once here and
// shared by every part of the picker (group list, product grids, the "selected" summary).
export function createScopeMeta() {
  const state = reactive({ products: {}, categories: {}, brands: {} });

  function remember(kind, row) {
    state[kind][row.id] = { ...state[kind][row.id], ...row };
    if (kind !== 'products') return;
    // A product row also names its category and brand, which is all the summary needs of them.
    if (row.categoryId != null && !state.categories[row.categoryId]) state.categories[row.categoryId] = { id: row.categoryId, name: row.category };
    if (row.brandId != null && !state.brands[row.brandId]) state.brands[row.brandId] = { id: row.brandId, name: row.brand };
  }

  // Fetches the ids that are not known yet, a page of twenty at a time.
  async function hydrate(kind, ids) {
    const missing = [...new Set(ids)].filter((id) => !state[kind][id]);
    if (!missing.length) return;
    let page = 0, data;
    do {
      data = await api(`/admin/options/${kind}?` + new URLSearchParams({ page: page++, ids: missing.join(',') }));
      for (const row of data.content) remember(kind, row);
    } while (!data.last);
  }

  return { state, remember, hydrate };
}
