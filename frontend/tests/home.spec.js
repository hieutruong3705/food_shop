import {test,expect} from '@playwright/test';
import fs from 'node:fs';

test('home page shows the promotion with a countdown, shop blocks, recently viewed and a green footer',async({page})=>{
  test.setTimeout(90000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^﻿/,''));
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(/\/$/);
  const csrf=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[csrf.headerName]:csrf.token};
  const key='Home sale '+Date.now();
  // Rate 100 wins over any other running promotion (ties go to the newest).
  const created=await page.request.post('/api/v1/admin/promotions',{headers,data:{name:key,description:'Ưu đãi kiểm thử trang chủ',discountRate:100,active:true,scopeType:'ALL',
    startDate:new Date(Date.now()-3600000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString()}});
  expect(created.status()).toBe(201);
  const promotionId=(await created.json()).id;
  try{
    await page.goto('/');
    const promo=page.locator('.promo-row');
    await expect(promo).toBeVisible();
    await expect(promo.getByRole('heading',{level:2})).toContainText(key);
    await expect(promo.locator('.promo-rate')).toHaveText('Giảm 100%');
    await expect(promo.getByRole('timer')).toBeVisible();
    await expect(promo.locator('.product-card .badge').first()).toHaveText('Giảm 100%');
    await expect(promo.locator('.product-card')).not.toHaveCount(0);

    const home=await(await page.request.get('/api/v1/home')).json();
    await expect(page.getByRole('heading',{name:'Thương hiệu nổi bật',exact:true})).toBeVisible();
    await expect(page.getByRole('heading',{name:'Bán chạy nhất',exact:true})).toHaveCount(home.bestSellers.length?1:0);
    for(const row of home.categoryRows)await expect(page.getByRole('region',{name:row.category.name,exact:true}).locator('.product-card')).toHaveCount(row.products.length);
    await expect(page.getByRole('heading',{name:'Bạn vừa xem',exact:true})).toHaveCount(0);

    // A product that was opened comes back as "Bạn vừa xem".
    await page.locator('[data-testid="featured-products"] .product-card').first().click();
    await expect(page).toHaveURL(/\/products\/\d+/);
    const name=await page.getByRole('heading',{level:1}).innerText();
    await page.goto('/');
    const recent=page.locator('section[aria-labelledby="recent-title"]');
    await expect(recent).toContainText(name);

    const footer=page.locator('footer');
    await expect(footer).toHaveCSS('background-color','rgb(7, 63, 55)');
    await expect(footer.getByRole('link',{name:'Về đầu trang ↑'})).toBeVisible();

    await page.setViewportSize({width:390,height:844});
    await page.reload();
    await expect(promo).toBeVisible();
    expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.screenshot({path:'test-results/home-mobile.png',fullPage:true});
  }finally{
    await page.request.delete('/api/v1/admin/promotions/'+promotionId,{headers});
  }
});
