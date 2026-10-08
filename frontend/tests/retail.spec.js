import { test, expect } from '@playwright/test';

test('retail campaigns, featured products and mobile navigation use live catalog', async ({ page }) => {
  await page.goto('/');
  const featured = page.getByTestId('featured-products');
  await expect(featured.locator('.product-card')).toHaveCount(8);
  await expect(page.getByRole('region', {name:'Banner TechShop'})).toBeVisible();
  await page.goto('/products');
  await expect(page.locator('.product-card').first()).toBeVisible();
  await page.goto('/');
  await expect(page.locator('.retail-filter-tabs')).toHaveCount(0);
  await expect(featured.locator('.product-card')).toHaveCount(8);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Danh mục sản phẩm', exact: true }).click();
  await expect(page.locator('#header-categories')).toBeVisible();
  await page.locator('#header-categories').getByRole('link', { name: 'Điện thoại', exact: true }).click();
  await expect(page).toHaveURL(/categoryId=/);
  await expect(page.locator('#header-categories')).toHaveCount(0);
  await expect(page.locator('.product-card').first()).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByLabel('Tìm kiếm sản phẩm').fill('NovaBook');
  await page.locator('.header-search').getByRole('button', { name: 'Tìm kiếm', exact: true }).click();
  await expect(page).toHaveURL(/search=NovaBook/);
  await expect(page.locator('.product-card')).toHaveCount(4);
  for (const width of [360, 768, 1440]) {
    await page.setViewportSize({ width, height: 900 });
    await page.goto('/');
    await expect(featured.locator('.product-card')).toHaveCount(8);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  }
});
