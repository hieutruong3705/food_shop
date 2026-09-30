import {test,expect} from '@playwright/test';

test('filters are chosen together and applied with one button; there is no warranty filter',async({page})=>{
  await page.goto('/products');
  const filters=page.getByRole('complementary',{name:'Bộ lọc sản phẩm'});
  const applyButton=filters.getByRole('button',{name:'Áp dụng bộ lọc',exact:true});
  await expect(filters.getByRole('slider',{name:'Giá từ',exact:true})).toBeVisible();
  await expect(filters.getByRole('slider',{name:'Giá đến',exact:true})).toBeVisible();
  await expect(filters.getByRole('button',{name:'Áp dụng giá'})).toHaveCount(0);
  await expect(page.locator('.price-presets')).toHaveCount(0);
  await expect(filters.getByText('Bảo hành',{exact:true})).toHaveCount(0);
  await expect(applyButton).toBeDisabled();

  const cards=page.locator('.product-card');
  await expect(cards.first()).toBeVisible();
  const before=await cards.count();
  // Price and stock are picked first; nothing changes until the button is pressed.
  const low=filters.getByRole('spinbutton',{name:'Giá từ (nhập số)'});
  await low.fill('1000000');await low.press('Enter');
  await filters.getByRole('checkbox',{name:/Sẵn hàng/}).check();
  await expect(page).not.toHaveURL(/minPrice=|inStock=/);
  await expect(filters.getByText('Có thay đổi chưa áp dụng')).toBeVisible();
  await expect(cards).toHaveCount(before);
  await applyButton.click();
  await expect(page).toHaveURL(/minPrice=1000000/);
  await expect(page).toHaveURL(/inStock=true/);
  await expect(page).not.toHaveURL(/maxPrice=/);
  await expect(page.getByRole('button',{name:/^Bỏ Giá 1\.000\.000/})).toHaveCount(1);
  await expect(applyButton).toBeDisabled();
  await page.reload();
  await expect(filters.getByRole('spinbutton',{name:'Giá từ (nhập số)'})).toHaveValue('1000000');

  // Chips still remove a filter straight away.
  await page.getByRole('button',{name:/^Bỏ Giá /}).click();
  await expect(page).not.toHaveURL(/minPrice=/);
  await page.getByRole('button',{name:'Bỏ Sẵn hàng',exact:true}).click();
  await expect(cards).toHaveCount(before);

  await page.setViewportSize({width:390,height:844});
  await page.getByRole('button',{name:'Mở bộ lọc',exact:true}).click();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
