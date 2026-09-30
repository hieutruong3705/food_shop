import {test,expect} from '@playwright/test';
import fs from 'node:fs';

test('a promotion tags the product page and the cart, and the scope picker fits a phone',async({page})=>{
  test.setTimeout(90000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^﻿/,''));
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(/\/$/);
  const csrf=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[csrf.headerName]:csrf.token};
  const found=await(await page.request.get('/api/v1/products?search=Nova%20Connect%201&size=5')).json();
  const product=found.content.find(p=>p.name==='Hub USB-C Nova Connect 1');
  expect(product).toBeTruthy();
  const key='Tag '+Date.now();
  const created=await page.request.post('/api/v1/admin/promotions',{headers,data:{name:key,discountRate:40,active:true,scopeType:'PRODUCT',productIds:[product.id],
    startDate:new Date(Date.now()-3600000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString()}});
  expect(created.status()).toBe(201);
  const promotionId=(await created.json()).id;
  try{
    await page.request.post('/api/v1/cart/items',{headers,data:{productId:product.id,quantity:1}});
    await page.goto('/cart');
    const line=page.locator('.cart-line').filter({hasText:'Hub USB-C Nova Connect 1'});
    await expect(line.locator('.badge-inline')).toHaveText('Giảm 40%');
    await expect(line.locator('del.cart-list-price')).toBeVisible();

    await page.goto('/products/'+product.id);
    await expect(page.locator('.discount-label')).toHaveText('Giảm 40%');
    await expect(page.locator('.sale-banner')).toContainText(key);
    await expect(page.locator('.sale-banner')).toContainText('Tiết kiệm');
    await expect(page.locator('.breadcrumb').getByRole('link',{name:'Phụ kiện'})).toBeVisible();

    await page.setViewportSize({width:390,height:844});
    await page.goto('/admin/promotions');
    await page.getByRole('button',{name:'+ Thêm mới',exact:true}).click();
    await page.getByRole('button',{name:/^Theo danh mục/}).click();
    await page.locator('.group').first().click();
    await expect(page.locator('.scope-picker .pick').first()).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.screenshot({path:'test-results/scope-picker-mobile.png',fullPage:true});
  }finally{
    await page.request.delete('/api/v1/admin/promotions/'+promotionId,{headers});
    const cart=await(await page.request.get('/api/v1/cart')).json();
    for(const item of cart.items)await page.request.delete('/api/v1/cart/items/'+item.id,{headers});
  }
});

test('voucher list filters by category and the category filter also finds single-product vouchers',async({page})=>{
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^﻿/,''));
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(/\/$/);
  const csrf=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[csrf.headerName]:csrf.token};
  const hub=(await(await page.request.get('/api/v1/products?search=Nova%20Connect%201&size=5')).json()).content[0];
  const categories=await(await page.request.get('/api/v1/categories')).json();
  const accessories=categories.find(c=>c.name==='Phụ kiện');
  const code='PICK'+Date.now();
  const made=await page.request.post('/api/v1/admin/vouchers',{headers,data:{code,discount:5,active:true,scopeType:'CATEGORY',productIds:[hub.id]}});
  expect(made.status()).toBe(201);
  const id=(await made.json()).id;
  try{
    await page.goto('/admin/vouchers');
    await page.getByLabel('Lọc danh mục').selectOption(String(accessories.id));
    await page.getByLabel('Tìm trong quản trị').fill(code);
    await page.locator('main').getByRole('button',{name:'Tìm kiếm',exact:true}).click();
    await expect(page.getByRole('row').filter({hasText:code})).toHaveCount(1);
    await expect(page.getByRole('row').filter({hasText:code})).toContainText('Sản phẩm '+hub.name);
  }finally{
    await page.request.delete('/api/v1/admin/vouchers/'+id,{headers});
  }
});

test('a visitor who adds to the cart is sent to log in and the product is added afterwards',async({page})=>{
  test.setTimeout(60000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^﻿/,''));
  const hub=(await(await page.request.get('/api/v1/products?search=Nova%20Connect%202&size=5')).json()).content[0];
  await page.goto('/products/'+hub.id);
  await page.getByRole('button',{name:/^Thêm vào giỏ hàng/}).click();
  await expect(page).toHaveURL(/\/login/);
  await page.getByLabel('Email',{exact:true}).fill('user@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.user);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(new RegExp('/products/'+hub.id));
  await expect(page.getByRole('status')).toContainText('Đã thêm sản phẩm vào giỏ hàng');
  const csrf=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[csrf.headerName]:csrf.token};
  const cart=await(await page.request.get('/api/v1/cart')).json();
  expect(cart.items.map(i=>i.productId)).toContain(hub.id);
  for(const item of cart.items)await page.request.delete('/api/v1/cart/items/'+item.id,{headers});
});
