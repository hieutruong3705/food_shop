// Products the shopper opened lately, newest first, kept in this browser only.
const KEY = 'techshop.recent';
const LIMIT = 8;

export function recentProducts() {
  try {
    const list = JSON.parse(localStorage.getItem(KEY) || '[]');
    return Array.isArray(list) ? list.filter((p) => p && p.id != null && p.name) : [];
  } catch {
    return [];
  }
}

export function rememberProduct(product) {
  if (!product || product.id == null) return;
  try {
    localStorage.setItem(KEY, JSON.stringify([product, ...recentProducts().filter((p) => p.id !== product.id)].slice(0, LIMIT)));
  } catch {
    // Storage can be blocked or full; the list is a convenience.
  }
}
