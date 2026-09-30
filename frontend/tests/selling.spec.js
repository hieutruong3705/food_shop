import {test,expect} from '@playwright/test';
import fs from 'node:fs';
test('admin can stop and resume selling; stopped and sold-out cart items cannot proceed',async({page})=>{
  test.setTimeout(90000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^\uFEFF/,''));
  await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);await page.getByRole('button',{name:'Đăng nhập →'}).click();await expect(page).toHaveURL(/\/$/);
  const token=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[token.headerName]:token.token};
  const category=(await(await page.request.get('/api/v1/categories')).json())[0];
  const brand={id:(await(await page.request.get('/api/v1/admin/categories/'+category.id)).json()).brandIds[0]};
  const key='selling-'+Date.now();let id,itemId;
  try{
    const response=await page.request.post('/api/v1/admin/products',{headers,data:{name:key,sku:key,slug:key,categoryId:category.id,brandId:brand.id,price:1230000,quantity:2,warrantyMonths:12,images:['/tech/laptop.svg']}});expect(response.status()).toBe(201);id=(await response.json()).id;
    const cart=await page.request.post('/api/v1/cart/items',{headers,data:{productId:id,quantity:1}});expect(cart.ok()).toBe(true);itemId=(await cart.json()).items.find(i=>i.productId===id).id;
    await page.goto('/admin/products');await page.getByLabel('Tìm trong quản trị').fill(key);await page.locator('main').getByRole('button',{name:'Tìm kiếm',exact:true}).click();
    const row=page.getByRole('row').filter({has:page.getByRole('cell',{name:'#'+id,exact:true})});
    page.once('dialog',d=>d.accept());await row.getByRole('button',{name:'Ngừng bán',exact:true}).click();await expect(row.getByRole('button',{name:'Bán lại',exact:true})).toBeVisible();
    expect((await page.request.get('/api/v1/products/'+id)).status()).toBe(404);
    await page.goto('/cart');let line=page.locator('.cart-line').filter({hasText:key});await expect(line.getByRole('checkbox')).toBeDisabled();await expect(line).toContainText('đã ngừng bán');
    await page.goto('/admin/products');await page.getByLabel('Tìm trong quản trị').fill(key);await page.getByLabel('Lọc kinh doanh').selectOption('inactive');await page.locator('main').getByRole('button',{name:'Tìm kiếm',exact:true}).click();
    await expect(row.getByRole('button',{name:'Bán lại',exact:true})).toBeVisible();await page.screenshot({path:'test-results/admin-resume-selling.png',fullPage:true});
    page.once('dialog',d=>d.accept());await row.getByRole('button',{name:'Bán lại',exact:true}).click();await expect(page.getByRole('status')).toContainText('Đã mở bán lại');await expect(row).toHaveCount(0);
    const detail=await(await page.request.get('/api/v1/products/'+id)).json();expect(detail.product.quantity).toBe(2);
    await page.goto('/cart');line=page.locator('.cart-line').filter({hasText:key});await expect(line.getByRole('checkbox')).toBeEnabled();await expect(line.getByRole('checkbox')).toBeChecked();
    const edit=await(await page.request.get('/api/v1/admin/products/'+id)).json();edit.quantity=0;expect((await page.request.put('/api/v1/admin/products/'+id,{headers,data:edit})).ok()).toBe(true);
    await page.reload();await expect(line.getByRole('checkbox')).toBeDisabled();await expect(line).toContainText('đã hết hàng');
    for(const box of await page.locator('.cart-line input[type=checkbox]:enabled').all())await box.uncheck();
    await expect(page.getByRole('link',{name:'Tiến hành đặt hàng →',exact:true})).toHaveCount(0);
    await page.screenshot({path:'test-results/cart-unavailable.png',fullPage:true});
    for(const sort of ['newest','popular','price-asc','price-desc']){
      const products=await(await page.request.get('/api/v1/products?size=60&sort='+sort)).json();let out=false;
      for(const product of products.content){if(product.quantity===0)out=true;else expect(out).toBe(false);}
    }
  }finally{
    if(itemId)await page.request.delete('/api/v1/cart/items/'+itemId,{headers});
    if(id)await page.request.delete('/api/v1/admin/products/'+id,{headers});
  }
});
