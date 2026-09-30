import { test, expect } from '@playwright/test';
import fs from 'node:fs';
test('admin icons and logos persist; category menu and specification filters use database', async ({page}) => {
  test.setTimeout(90000);
  const credentials = JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^\uFEFF/,''));
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(/\/$/);
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  const headers = {[csrf.headerName]:csrf.token};
  async function create(type,data) { const r=await page.request.post('/api/v1/admin/'+type,{headers,data}); expect(r.status()).toBe(201); return (await r.json()).id; }
  const key='facet-'+Date.now(); const products=[]; let category,brand;
  try {
    category=await create('categories',{name:key,image:'/tech/monitor.svg'});
    brand=await create('brands',{name:key,logoUrl:'/tech/laptop.svg'});
    expect((await page.request.put('/api/v1/admin/categories/'+category,{headers,data:{name:key,image:'/tech/monitor.svg',brandIds:[brand]}})).ok()).toBe(true);
    for(let i=0;i<2;i++) products.push(await create('products',{name:key+'-'+i,sku:key+'-'+i,slug:key+'-'+i,categoryId:category,brandId:brand,warrantyMonths:12,price:1000000*(i+1),quantity:i===0?4:0,images:['/tech/laptop.svg'],specs:[{name:'RAM',value:i===0?'16 GB':'32 GB'},{name:'Tần số quét',value:'144 Hz'}]}));
    // Exercise the shared upload UI on categories and brands, not just URL fields.
    const png = await page.evaluate(() => { const c=document.createElement('canvas');c.width=32;c.height=32;const ctx=c.getContext('2d');ctx.fillStyle='#009977';ctx.fillRect(0,0,32,32);return c.toDataURL('image/png').split(',')[1]; });
    for(const [type,id,field] of [['categories',category,'image'],['brands',brand,'logoUrl']]) {
      await page.goto('/admin/'+type);
      await page.getByLabel('Tìm trong quản trị').fill(key);
      await page.locator('main').getByRole('button',{name:'Tìm kiếm',exact:true}).click();
      await page.getByRole('row').filter({has:page.getByRole('cell',{name:'#'+id,exact:true})}).getByRole('button',{name:'Sửa',exact:true}).click();
      await page.getByLabel('Tải ảnh (PNG/JPEG/WebP, tối đa 5 MB)',{exact:true}).setInputFiles({name:key+'.png',mimeType:'image/png',buffer:Buffer.from(png,'base64')});
      await expect(page.getByRole('status')).toContainText('Đã tải ảnh lên');
      await expect(page.locator('.admin-image-preview img')).toHaveAttribute('src',/^\/uploads\//);
      await page.getByRole('button',{name:'Lưu thay đổi',exact:true}).click();
      await expect(page.getByRole('status')).toContainText('Đã lưu');
      const saved=await (await page.request.get('/api/v1/admin/'+type+'/'+id)).json();
      expect(saved[field]).toMatch(/^\/uploads\//);
    }
    await page.goto('/products?categoryId='+category);
    await expect(page.locator('.product-card')).toHaveCount(2);
    const brandButton=page.locator('.brand-filter-grid').getByRole('button',{name:key,exact:true});
    await expect(brandButton.locator('img')).toHaveAttribute('src',/^\/uploads\//);
    await brandButton.click();
    await expect(brandButton).toHaveAttribute('aria-pressed','true');
    await page.getByRole('checkbox',{name:'16 GB',exact:false}).check();
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.reload();
    await expect(page.getByRole('checkbox',{name:'16 GB',exact:false})).toBeChecked();
    await page.getByRole('checkbox',{name:'32 GB',exact:false}).check();
    await expect(page.locator('.product-card')).toHaveCount(2);
    await page.getByRole('checkbox',{name:'Sẵn hàng',exact:false}).check();
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.getByRole('checkbox',{name:'144 Hz',exact:false}).check();
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.screenshot({path:'test-results/catalog-filters-desktop.png',fullPage:true});
    await page.getByRole('button',{name:'Danh mục sản phẩm',exact:true}).click();
    const menu=page.locator('#header-categories');
    await menu.getByRole('button',{name:'Xem nhóm '+key,exact:true}).click();
    await expect(menu.locator('.mega-category-list img[src^="/uploads/"]')).toBeVisible();
    await expect(menu.locator('.mega-columns>section')).toHaveCount(3);
    await expect(menu.getByRole('heading',{name:'RAM',exact:true})).toHaveCount(0);
    await page.screenshot({path:'test-results/category-mega-menu.png',fullPage:true});
    await menu.getByRole('button',{name:'Áp dụng giá',exact:true}).click();
    await expect(page).toHaveURL(/inStock=true/);
    await expect(page).toHaveURL(/minPrice=/);
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.getByRole('checkbox',{name:'Sẵn hàng',exact:false}).uncheck();
    await page.getByRole('checkbox',{name:'16 GB',exact:false}).uncheck();
    await expect(page.locator('.product-card')).toHaveCount(1);
    await expect(page.locator('.product-card')).toContainText(key+'-1');
    await page.setViewportSize({width:390,height:844});
    await page.getByRole('button',{name:'Mở bộ lọc',exact:true}).click();
    await expect(page.getByRole('checkbox',{name:'32 GB',exact:false})).toBeChecked();
    expect(await page.evaluate(() => document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.screenshot({path:'test-results/catalog-filters-mobile.png',fullPage:true});
    await page.getByRole('button',{name:'Danh mục sản phẩm',exact:true}).click();
    await menu.getByRole('button',{name:'Xem nhóm '+key,exact:true}).click();
    await expect(menu.getByRole('heading',{name:'Chọn thương hiệu',exact:true})).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.keyboard.press('Escape');
    await expect(menu).toHaveCount(0);
  } finally {
    for(const id of products) await page.request.delete('/api/v1/admin/products/'+id,{headers});
    // Product history retains these references; inactive fixtures no longer appear publicly.
  }
});
