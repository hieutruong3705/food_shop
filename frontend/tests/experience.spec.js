import {test,expect} from '@playwright/test';
import fs from 'node:fs';
test('database banners slide manually and automatically; offers and mock chat work on mobile',async({page})=>{
  test.setTimeout(90000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^\uFEFF/,''));
  await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);await page.getByRole('button',{name:'Đăng nhập →'}).click();await expect(page).toHaveURL(/\/$/);
  const token=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[token.headerName]:token.token};const created=[];
  async function create(type,data){const r=await page.request.post('/api/v1/admin/'+type,{headers,data});expect(r.status()).toBe(201);const id=(await r.json()).id;created.push([type,id]);return id;}
  const key='carousel-'+Date.now();
  try{
    await create('banners',{name:key+' A',imageUrl:'/tech/laptop.svg',link:'/products',active:true,technologyCatalog:true});
    await create('banners',{name:key+' B',imageUrl:'/tech/phone.svg',link:'/products',active:true,technologyCatalog:true});
    await create('vouchers',{code:key,discount:99,all:true,active:true,minimumOrder:1000000,maximumDiscount:100000,usageLimit:1});
    await create('vouchers',{code:key+'-NEXT',discount:98,all:true,active:true,minimumOrder:2000000,maximumDiscount:50000,usageLimit:1});
    const category=(await(await page.request.get('/api/v1/categories')).json())[0],brand={id:(await(await page.request.get('/api/v1/admin/categories/'+category.id)).json()).brandIds[0]};
    const productId=await create('products',{name:key,sku:key,slug:key,categoryId:category.id,brandId:brand.id,price:1230000,quantity:2,warrantyMonths:12,images:['/tech/laptop.svg'],active:false,technologyCatalog:false});
    await create('promotions',{name:key,description:'Khuyến mãi cho sản phẩm tạm ngừng bán',discountRate:97,active:true,startDate:new Date(Date.now()-3600000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString(),productIds:[productId],brandIds:[]});
    await page.clock.install();await page.goto('/');const hero=page.getByRole('region',{name:'Banner TechShop'});
    await expect(hero.getByRole('img',{name:key+' B',exact:true})).toBeVisible();
    await expect(hero.locator('.banner-caption')).toHaveCount(0);
    await expect(page.getByRole('button',{name:/tạm dừng|bật tự chuyển/i})).toHaveCount(0);
    // The top bar links to the voucher page with a plain label, like its other entries.
    const topVoucher=page.locator('.topbar-offer.voucher');
    await expect(topVoucher).toHaveText('Mã giảm giá');
    await expect(topVoucher).toHaveAttribute('href','/vouchers');
    await page.mouse.move(0,0);await page.clock.runFor(4600);
    await expect(hero.getByRole('img',{name:key+' A',exact:true})).toBeVisible();
    await hero.getByRole('button',{name:'Banner trước',exact:true}).click();await page.clock.runFor(400);
    await expect(hero.getByRole('img',{name:key+' B',exact:true})).toBeVisible();
    // Keep the UI regression deterministic; real provider transport is tested separately.
    await page.route('**/api/chat/status',route=>route.fulfill({json:{provider:'mock'}}));
    await page.route('**/api/chat',route=>route.fulfill({json:{reply:'Bạn muốn tư vấn thiết bị theo ngân sách nào?',provider:'mock'}}));
    await page.getByRole('button',{name:'Mở TechShop AI'}).click();
    const chat=page.getByRole('dialog',{name:'TechShop AI'});await expect(chat).toContainText('Xin chào 👋');
    await chat.getByRole('button',{name:'Tư vấn laptop',exact:true}).click();await expect(chat.locator('.chat-message.bot')).toHaveCount(2);
    await chat.getByRole('textbox').fill('So sánh sản phẩm');await chat.getByRole('textbox').press('Enter');await expect(chat.locator('.chat-message.bot')).toHaveCount(3);
    await page.screenshot({path:'test-results/experience-desktop.png',fullPage:true});
    await page.setViewportSize({width:390,height:844});await expect(chat).toBeVisible();expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
    await page.screenshot({path:'test-results/experience-mobile.png',fullPage:true});
    await chat.getByRole('button',{name:'Thu nhỏ trò chuyện'}).click();await page.getByRole('button',{name:'Mở TechShop AI'}).click();await expect(chat.locator('.chat-message.bot')).toHaveCount(3);
    await chat.getByRole('button',{name:'Đóng trò chuyện'}).click();await expect(chat).toHaveCount(0);
  }finally{for(const [type,id] of created.reverse())await page.request.delete('/api/v1/admin/'+type+'/'+id,{headers});}
});
