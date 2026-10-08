import {test,expect} from '@playwright/test';
test('AI chat keeps context, reports quota errors and retries without failed history',async({page})=>{
  const requests=[];
  await page.route('**/api/chat/status',route=>route.fulfill({json:{provider:'ai'}}));
  await page.route('**/api/chat',route=>{
    requests.push(route.request().postDataJSON());
    if(requests.length===2)return route.fulfill({status:429,json:{message:'Trợ lý đã chạm giới hạn sử dụng. Vui lòng thử lại sau.'}});
    return route.fulfill({json:{reply:requests.length===1?'Bạn cần laptop trong ngân sách bao nhiêu?':'Với ngân sách này, hãy ưu tiên RAM 16 GB.',provider:'ai'}});
  });
  await page.goto('/');await page.getByRole('button',{name:'Mở TechShop AI'}).click();
  const chat=page.getByRole('dialog',{name:'TechShop AI'});
  await expect(chat.locator('header')).toContainText('Online');
  await chat.getByRole('button',{name:'Tư vấn laptop',exact:true}).click();
  await expect(chat.locator('.chat-message.bot')).toHaveCount(2);
  expect(requests[0].history).toEqual([]);
  await chat.getByRole('textbox').fill('Khoảng 20 triệu');await chat.getByRole('textbox').press('Enter');
  await expect(chat.getByRole('alert')).toContainText('giới hạn');
  await expect(chat.getByRole('textbox')).toHaveValue('Khoảng 20 triệu');
  await expect(chat.locator('header')).toContainText('Tạm gián đoạn');
  await chat.getByRole('button',{name:'Gửi tin nhắn'}).click();
  await expect(chat.locator('.chat-message.bot')).toHaveCount(3);
  expect(requests[2].history).toEqual(requests[1].history);
  expect(requests[2].history.map(t=>t.role)).toEqual(['user','assistant']);
  await expect(chat.getByRole('alert')).toHaveCount(0);
  await expect(chat.locator('header')).toContainText('Online');
  await page.setViewportSize({width:390,height:844});expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)).toBe(true);
});
