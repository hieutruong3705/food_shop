import {test,expect} from '@playwright/test';
import fs from 'node:fs';

test('category brands, scoped offers, sale tags and the price range persist through the admin UI',async({page})=>{
  test.setTimeout(120000);
  const credentials=JSON.parse(fs.readFileSync('../data/e2e-credentials.json','utf8').replace(/^\uFEFF/,''));
  await page.goto('/login');
  await page.getByLabel('Email',{exact:true}).fill('admin@techshop.test');
  await page.getByLabel('Mật khẩu',{exact:true}).fill(credentials.admin);
  await page.getByRole('button',{name:'Đăng nhập →'}).click();
  await expect(page).toHaveURL(/\/$/);
  const token=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[token.headerName]:token.token};
  const key='facet-'+Date.now(),created=[],productIds=[];
  async function create(type,data){const response=await page.request.post('/api/v1/admin/'+type,{headers,data});expect(response.status(),await response.text()).toBe(201);const id=(await response.json()).id;created.push([type,id]);return id;}
  async function detail(type,id){return (await page.request.get('/api/v1/admin/'+type+'/'+id)).json();}
  async function edit(type,id){
    await page.goto('/admin/'+type);
    await page.getByLabel('Tìm trong quản trị').fill(key);
    await page.locator('main').getByRole('button',{name:'Tìm kiếm',exact:true}).click();
    await page.getByRole('row').filter({has:page.getByRole('cell',{name:'#'+id,exact:true})}).getByRole('button',{name:'Sửa',exact:true}).click();
    await expect(page.getByRole('button',{name:'Lưu thay đổi',exact:true})).toBeVisible();
  }
  async function save(){await page.getByRole('button',{name:'Lưu thay đổi',exact:true}).click();await expect(page.getByRole('status')).toContainText('Đã lưu');}
  async function choose(label,name){
    const selector=page.getByRole('region',{name:label,exact:true});
    await selector.getByRole('searchbox').fill(name);
    await selector.getByRole('checkbox',{name:new RegExp(name)}).check();
    await selector.getByRole('button',{name:'Đóng danh sách',exact:true}).click();
  }
  let category,otherCategory,brandA,brandB,brandC;
  try{
    brandA=await create('brands',{name:key+' A'});brandB=await create('brands',{name:key+' B'});brandC=await create('brands',{name:key+' C'});
    category=await create('categories',{name:key,brandIds:[brandA]});
    otherCategory=await create('categories',{name:key+' other',brandIds:[brandC]});
    await edit('categories',category);
    await expect(page.getByRole('button',{name:'Bỏ '+key+' A',exact:true})).toBeVisible();
    await choose('Thương hiệu áp dụng',key+' B');await choose('Thương hiệu áp dụng',key+' C');await save();
    expect((await detail('categories',category)).brandIds.sort()).toEqual([brandA,brandB,brandC].sort());
    await edit('categories',category);await page.getByRole('button',{name:'Bỏ '+key+' C',exact:true}).click();await save();
    expect((await detail('categories',category)).brandIds.sort()).toEqual([brandA,brandB].sort());

    for(let i=0;i<2;i++)productIds.push(await create('products',{name:key+'-'+i,sku:key+'-'+i,slug:key+'-'+i,categoryId:category,brandId:brandA,price:1000000*(i+1),quantity:3,warrantyMonths:i?36:12,images:['/tech/laptop.svg'],specs:[{name:'Phiên bản',value:'Demo'},{name:'Kết nối',value:'USB-C'},{name:'Bảo hành',value:'999 tháng'},{name:'RAM',value:'16 GB'}]}));
    await page.goto('/admin/products');await page.getByRole('button',{name:'+ Thêm mới',exact:true}).click();
    await page.getByLabel('Danh mục',{exact:true}).selectOption(String(category));
    await expect(page.getByLabel('Thương hiệu',{exact:true}).locator('option:not([disabled])')).toHaveText([new RegExp(key+' A'),new RegExp(key+' B')]);
    await page.getByLabel('Thương hiệu',{exact:true}).selectOption(String(brandB));
    await page.getByLabel('Danh mục',{exact:true}).selectOption(String(otherCategory));
    await expect(page.getByLabel('Thương hiệu',{exact:true})).toHaveValue('');
    await expect(page.getByLabel('Thương hiệu',{exact:true}).locator('option:not([disabled])')).toHaveText([new RegExp(key+' C')]);

    // Shared scope editor: whole store, categories, brands or hand-picked products; never mixed.
    const mode=name=>page.getByRole('button',{name:new RegExp('^'+name)});
    const applyAs=name=>page.getByRole('group',{name:'Cách áp dụng',exact:true}).getByRole('button',{name:new RegExp('^'+name)});
    async function openGroup(label,search,pattern){
      const groups=page.getByRole('region',{name:label,exact:true});
      await groups.getByRole('searchbox').fill(search);
      await groups.getByRole('button',{name:pattern}).click();
    }
    async function pickProduct(name){
      const search=page.getByRole('searchbox',{name:/^Tìm sản phẩm/}).last();
      await search.fill(name);
      await page.getByRole('checkbox',{name,exact:true}).check();
    }
    for(const type of ['promotions','vouchers']){
      const id=await create(type,{name:key,code:key,discountRate:10,discount:10,scopeType:'ALL',active:true,startDate:new Date(Date.now()-3600000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString()});
      const cases=[
        // [mode button, expected scope type, products the preview counts]
        ['Theo danh mục','CATEGORY',2],['Theo thương hiệu','BRAND',1],['Theo thương hiệu','BRAND',2],['Sản phẩm cụ thể','PRODUCT',1],['Toàn bộ cửa hàng','ALL',2],
      ];
      for(const [label,scope,count] of cases){
        await edit(type,id);
        await mode(label).click();
        let expected={categoryIds:[],brandIds:[],productIds:[]};
        if(scope==='CATEGORY'){
          // Whole category: every product in it.
          await openGroup('Chọn danh mục',key,new RegExp('^'+key+' \\d'));
          await applyAs('Toàn bộ danh mục').click();
          expected.categoryIds=[category];
        }else if(scope==='BRAND'&&count===1){
          // A single product of a brand, not the whole brand.
          await openGroup('Chọn thương hiệu',key+' A',new RegExp('^'+key+' A'));
          await applyAs('Chọn từng sản phẩm').click();
          await page.getByRole('checkbox',{name:key+'-0',exact:true}).check();
          expected.productIds=[productIds[0]];
        }else if(scope==='BRAND'){
          // Switching the same brand to "whole" replaces its single products.
          await openGroup('Chọn thương hiệu',key+' A',new RegExp('^'+key+' A'));
          await applyAs('Toàn bộ thương hiệu').click();
          expected.brandIds=[brandA];
        }else if(scope==='PRODUCT'){
          await pickProduct(key+'-0');
          expected.productIds=[productIds[0]];
        }
        await expect(page.locator('.scope-preview')).toContainText(scope==='ALL'?/Áp dụng cho \d+ sản phẩm/:'Áp dụng cho '+count+' sản phẩm');
        if(scope==='PRODUCT')await expect(page.locator('.scope-samples img')).toHaveCount(1);
        if(type==='promotions'&&scope!=='ALL')await expect(page.locator('.scope-samples .mini-tag').first()).toHaveText('Giảm 10%');
        await save();
        const saved=await detail(type,id);expect(saved.scopeType).toBe(scope);
        expect(saved.categoryIds).toEqual(expected.categoryIds);
        expect(saved.brandIds).toEqual(expected.brandIds);
        expect(saved.productIds).toEqual(expected.productIds);
        await edit(type,id);
        await expect(mode(label)).toHaveAttribute('aria-pressed','true');
        if(scope==='BRAND'&&count===1)await expect(page.locator('.tray-chip')).toContainText(key+' A · 1 sản phẩm');
        if(type==='promotions'){
          const result=await(await page.request.get('/api/v1/products?categoryId='+category+'&promotionId='+id)).json();
          expect(result.totalElements).toBe(scope==='ALL'?2:count);
          expect(result.content.every(p=>p.discountedPrice===p.price*.9&&p.discountRate===10&&p.promotionName===key)).toBe(true);
        }
      }
      await page.screenshot({path:'test-results/'+type+'-scope.png',fullPage:true});
      if(type==='promotions'){
        // The storefront tags each product on sale with the promotion's rate.
        await edit(type,id);await mode('Theo danh mục').click();
        await openGroup('Chọn danh mục',key,new RegExp('^'+key+' \\d'));await applyAs('Toàn bộ danh mục').click();await save();
        await page.goto('/products?categoryId='+category);
        await expect(page.locator('.product-card .badge')).toHaveText(['Giảm 10%','Giảm 10%']);
        await expect(page.locator('.product-card .sale-save').first()).toContainText('Tiết kiệm');
      }
      expect((await page.request.delete('/api/v1/admin/'+type+'/'+id,{headers})).ok()).toBe(true);
    }

    await page.goto('/products?categoryId='+category);
    await expect(page.locator('.product-card')).toHaveCount(2);
    await expect(page.locator('.brand-filter-grid button')).toHaveCount(2); // Includes assigned B with no products.
    // The warranty filter was removed; the price range starts at the cheapest product.
    await expect(page.getByRole('slider',{name:/^Bảo hành/})).toHaveCount(0);
    await expect(page.getByRole('slider',{name:'Giá từ',exact:true})).toHaveAttribute('min','1000000');
    await expect(page.getByRole('slider',{name:'Giá đến',exact:true})).toHaveAttribute('max','2000000');
    await expect(page.locator('.catalog-filters summary').filter({hasText:/^(Phiên bản|Kết nối)$/})).toHaveCount(0);
    const priceSlider=page.getByRole('slider',{name:'Giá từ',exact:true});
    await priceSlider.scrollIntoViewIfNeeded();
    const track=await priceSlider.boundingBox();
    await page.mouse.move(track.x+9,track.y+track.height/2);await page.mouse.down();
    await page.mouse.move(track.x+track.width/2,track.y+track.height/2,{steps:12});await page.mouse.up();
    // Dragging only changes the draft; the button applies it.
    await expect(page).not.toHaveURL(/minPrice=/);await expect(page.locator('.product-card')).toHaveCount(2);
    await page.getByRole('button',{name:'Áp dụng bộ lọc',exact:true}).click();
    await expect(page).toHaveURL(/minPrice=1[45]\d{5}/);await expect(page.locator('.product-card')).toHaveCount(1);
    await page.reload();await expect(page.getByRole('button',{name:/^Bỏ Giá /})).toHaveCount(1);
    await page.getByRole('button',{name:'Danh mục sản phẩm',exact:true}).click();
    const menu=page.locator('#header-categories');await menu.getByRole('button',{name:'Xem nhóm '+key,exact:true}).click();
    await expect(menu.locator('.mega-brands a')).toHaveText([key+' A',key+' B']);
    await menu.locator('.mega-brands').getByRole('link',{name:key+' A',exact:true}).click();
    await expect(page).toHaveURL(/minPrice=/);await expect(page).toHaveURL(new RegExp('categoryId='+category));
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.screenshot({path:'test-results/price-filter.png',fullPage:true});
  }finally{
    for(const [type,id] of created.reverse()){
      if(type==='products'){const p=await detail(type,id);await page.request.put('/api/v1/admin/products/'+id,{headers,data:{...p,active:false,technologyCatalog:false}});}
      else if(['promotions','vouchers'].includes(type))await page.request.delete('/api/v1/admin/'+type+'/'+id,{headers});
    }
  }
});
