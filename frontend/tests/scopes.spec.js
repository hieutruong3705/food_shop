import {test,expect} from '@playwright/test';
import fs from 'node:fs';

test('category brands, scoped offers and warranty sliders persist through the admin UI',async({page})=>{
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

    // Both scope forms share the same searchable component and must round-trip all four choices.
    for(const type of ['promotions','vouchers']){
      const id=await create(type,{name:key,code:key,discountRate:10,discount:10,scopeType:'ALL',active:true,startDate:new Date(Date.now()-3600000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString()});
      for(const [scope,label,name,field,ids] of [
        ['CATEGORY','Danh mục áp dụng',key,'categoryIds',[category,otherCategory]],
        ['BRAND','Thương hiệu áp dụng',key+' A','brandIds',[brandA]],
        ['PRODUCT','Sản phẩm áp dụng',key+'-0','productIds',[productIds[0]]],
        ['ALL',null,null,null,[]],
      ]){
        await edit(type,id);await page.getByRole('combobox',{name:'Phạm vi áp dụng',exact:true}).selectOption(scope);
        await expect(page.locator('.search-multi')).toHaveCount(scope==='ALL'?0:1);
        if(scope==='CATEGORY'){
          const selector=page.getByRole('region',{name:label,exact:true});await selector.getByRole('searchbox').fill(name);
          await selector.getByRole('checkbox',{name:new RegExp('^'+key+' #')}).check();
          await selector.getByRole('checkbox',{name:new RegExp('^'+key+' other #')}).check();
        }else if(label)await choose(label,name);
        await save();
        const saved=await detail(type,id);expect(saved.scopeType).toBe(scope);
        for(const f of ['categoryIds','brandIds','productIds'])expect(saved[f].sort()).toEqual(f===field?ids.sort():[]);
        await edit(type,id);await expect(page.getByRole('combobox',{name:'Phạm vi áp dụng',exact:true})).toHaveValue(scope);
        if(label)await expect(page.getByRole('region',{name:label,exact:true}).locator('.selection-chips button')).toHaveCount(ids.length);
        if(type==='promotions'){
          const result=await(await page.request.get('/api/v1/products?categoryId='+category+'&promotionId='+id)).json();
          expect(result.totalElements).toBe(scope==='PRODUCT'?1:2);
          expect(result.content.every(p=>p.discountedPrice===p.price*.9)).toBe(true);
        }
      }
      await page.screenshot({path:'test-results/'+type+'-scope.png',fullPage:true});
      expect((await page.request.delete('/api/v1/admin/'+type+'/'+id,{headers})).ok()).toBe(true);
    }

    await page.goto('/products?categoryId='+category);
    await expect(page.locator('.product-card')).toHaveCount(2);
    await expect(page.locator('.brand-filter-grid button')).toHaveCount(2); // Includes assigned B with no products.
    await expect(page.getByRole('slider',{name:'Bảo hành từ',exact:true})).toHaveAttribute('min','12');
    await expect(page.getByRole('slider',{name:'Bảo hành đến',exact:true})).toHaveAttribute('max','36');
    await expect(page.locator('.catalog-filters summary').filter({hasText:/^(Phiên bản|Kết nối)$/})).toHaveCount(0);
    const warrantySlider=page.getByRole('slider',{name:'Bảo hành từ',exact:true});
    await warrantySlider.scrollIntoViewIfNeeded();
    const track=await warrantySlider.boundingBox();
    await page.mouse.move(track.x+9,track.y+track.height/2);await page.mouse.down();
    await page.mouse.move(track.x+track.width/2,track.y+track.height/2,{steps:12});await page.mouse.up();
    // Releasing a thumb applies the warranty range; there is no separate button.
    await expect(page).toHaveURL(/minWarranty=24/);await expect(page.locator('.product-card')).toHaveCount(1);
    await page.reload();await expect(page.getByRole('slider',{name:'Bảo hành từ',exact:true})).toHaveValue('24');
    await page.getByRole('button',{name:'Danh mục sản phẩm',exact:true}).click();
    const menu=page.locator('#header-categories');await menu.getByRole('button',{name:'Xem nhóm '+key,exact:true}).click();
    await expect(menu.locator('.mega-brands a')).toHaveText([key+' A',key+' B']);
    await menu.locator('.mega-brands').getByRole('link',{name:key+' A',exact:true}).click();
    await expect(page).toHaveURL(/minWarranty=24/);await expect(page).toHaveURL(new RegExp('categoryId='+category));
    await expect(page.locator('.product-card')).toHaveCount(1);
    await page.screenshot({path:'test-results/warranty-filter.png',fullPage:true});
  }finally{
    for(const [type,id] of created.reverse()){
      if(type==='products'){const p=await detail(type,id);await page.request.put('/api/v1/admin/products/'+id,{headers,data:{...p,active:false,technologyCatalog:false}});}
      else if(['promotions','vouchers'].includes(type))await page.request.delete('/api/v1/admin/'+type+'/'+id,{headers});
    }
  }
});
