import { test, expect } from "@playwright/test";
import fs from "node:fs";
const credentials = JSON.parse(
  fs
    .readFileSync("../data/e2e-credentials.json", "utf8")
    .replace(/^\uFEFF/, ""),
);
async function login(page, role) {
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill(`${role}@techshop.test`);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(credentials[role]);
  await page.getByRole("button", { name: "Đăng nhập →" }).click();
  await expect(page).toHaveURL(/\/$/);
}
test("uploaded product image previews and persists after save and reload", async ({ page }) => {
  await login(page, "admin");
  const catalog = await (await page.request.get('/api/v1/products?size=1')).json();
  const sample = catalog.content[0];
  expect(sample).toBeTruthy();
  async function openEditor() {
    await page.goto('/admin/products');
    await page.getByLabel('Tìm trong quản trị').fill(sample.name);
    await page.locator('main').getByRole('button', { name: 'Tìm kiếm', exact: true }).click();
    await page.getByRole('row').filter({ has: page.getByRole('cell', { name: '#' + sample.id, exact: true }) }).getByRole('button', { name: 'Sửa', exact: true }).click();
  }
  await openEditor();
  const imagesField = page.getByLabel("Đường dẫn ảnh, mỗi dòng một ảnh", { exact: true });
  const original = await imagesField.inputValue();
  const id = (await page.getByRole("heading", { name: /Chỉnh sửa #/ }).textContent()).match(/#(\d+)/)[1];
  const png = await page.evaluate(() => {
    const canvas = document.createElement("canvas"); canvas.width = 32; canvas.height = 32;
    const ctx = canvas.getContext("2d"); ctx.fillStyle = "#176c4c"; ctx.fillRect(0, 0, 32, 32);
    return canvas.toDataURL("image/png").split(",")[1];
  });
  let saved = false;
  try {
    await page.getByLabel('Tải ảnh (PNG/JPEG/WebP, tối đa 5 MB)', { exact: true }).setInputFiles({ name: "preview-test.png", mimeType: "image/png", buffer: Buffer.from(png, "base64") });
    await expect(page.getByRole("status")).toContainText("Đã tải ảnh lên");
    const uploaded = page.locator('.admin-image-preview').filter({ has: page.locator('img[src^="/uploads/"]') }).last();
    await expect(uploaded.locator("img")).toBeVisible();
    await expect.poll(() => uploaded.locator("img").evaluate(img => img.complete && img.naturalWidth === 32)).toBe(true);
    const url = await uploaded.locator("img").getAttribute("src");
    expect(fs.existsSync("../data/e2e-uploads/" + url.split('/').at(-1))).toBe(true);
    expect(fs.existsSync("../data" + url)).toBe(false);
    await uploaded.getByRole("button", { name: "Đặt làm ảnh đại diện" }).click();
    await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
    await expect(page.getByRole("status")).toContainText("Đã lưu"); saved = true;
    await page.reload();
    await openEditor();
    await expect(page.locator('.admin-image-preview img').first()).toHaveAttribute("src", url);
    const response = await page.request.get(`/api/v1/products/${id}`);
    expect(response.ok()).toBe(true);
    expect((await response.json()).images[0]).toBe(url);
    await page.goto(`/products/${id}`);
    await expect(page.locator('.detail-image img')).toHaveAttribute("src", url);
    await expect.poll(() => page.locator('.detail-image img').evaluate(img => img.complete && img.naturalWidth === 32)).toBe(true);
  } finally {
    if (saved) {
      await openEditor();
      await imagesField.fill(original);
      await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
      await expect(page.getByRole("status")).toContainText("Đã lưu");
    }
  }
});
test("desktop and mobile catalog, no overflow, product detail", async ({
  page,
}) => {
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  await page.goto("/");
  await expect(page.getByRole('region', {name:'Banner TechShop'})).toBeVisible();
  await expect(page.getByTestId("featured-products").locator(".product-card")).toHaveCount(8);
  for (const badge of await page.locator('.product-card .badge').all()) await expect(badge).toHaveText(/^Giảm \d+(,\d+)?%$/);
  await page.screenshot({
    path: "test-results/home-desktop.png",
    fullPage: true,
  });
  await page.goto("/products");
  await expect(page.locator(".product-card")).toHaveCount(12);
  await page.getByLabel("Tìm sản phẩm").fill("NovaBook");
  await page.getByRole("button", { name: "Áp dụng", exact: true }).click();
  await expect(page.locator(".product-card")).toHaveCount(4);
  await page.locator(".product-card").first().click();
  await expect(
    page.getByRole("heading", { name: "Thông số kỹ thuật" }),
  ).toBeVisible();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/");
  await expect(page.getByTestId("featured-products").locator(".product-card")).toHaveCount(8);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/home-mobile.png",
    fullPage: true,
  });
  expect(errors).toEqual([]);
});
test("customer COD checkout and admin fulfillment", async ({
  page,
  browser,
}) => {
  await login(page, "user");
  // Repeated fulfillment tests consume demo stock; choose a purchasable item.
  const availableResponse = await page.request.get('/api/v1/products?search=NovaBook&size=12&sort=price-asc');
  expect(availableResponse.ok()).toBe(true);
  const available = await availableResponse.json();
  const purchaseProduct = available.content.find(product => product.quantity >= 2);
  expect(purchaseProduct, 'A demo laptop needs at least two units for checkout').toBeTruthy();
  await page.goto("/products?search=NovaBook&sort=price-asc");
  await page.locator(`.product-card[href="/products/${purchaseProduct.id}"]`).click();
  await page.getByRole("button", { name: "Thêm vào giỏ hàng" }).click();
  await expect(page.getByRole("status")).toContainText("Đã thêm");
  await page.goto("/cart");
  await expect(page.locator(".cart-line")).toHaveCount(1);
  await page.getByRole("spinbutton").fill("2");
  await page.getByRole("spinbutton").press("Tab");
  await expect(page.getByRole("spinbutton")).toBeEnabled();
  await page.getByRole("link", { name: "Tiến hành đặt hàng" }).click();
  await page.getByLabel("Họ tên", { exact: true }).fill("Khách kiểm thử");
  await page.getByLabel("Số điện thoại").fill("0901234567");
  await page
    .getByLabel("Địa chỉ đầy đủ")
    .fill("123 Đường kiểm thử, Thành phố Hồ Chí Minh");
  await page.getByLabel("Ghi chú giao hàng").fill("Gọi trước khi giao");
  await page.getByLabel("Mã giảm giá").fill("TECH10");
  await page.getByRole("button", { name: "Áp dụng mã giảm giá" }).click();
  await expect(page.getByRole("status")).toContainText("Đã cập nhật");
  await page.getByRole("button", { name: "Xác nhận đặt hàng" }).click();
  await expect(page).toHaveURL(/\/orders\/\d+$/);
  const id = page.url().split("/").pop();
  await expect(page.locator(".status")).toHaveText("Chờ xác nhận");
  await expect(page.getByText("Ghi chú: Gọi trước khi giao")).toBeVisible();
  const context = await browser.newContext();
  const admin = await context.newPage();
  await login(admin, "admin");
  await admin.goto("/admin/orders");
  const row = admin
    .getByRole("row")
    .filter({ has: admin.getByRole("cell", { name: `#${id}`, exact: true }) });
  await expect(admin.locator('.site-header')).toHaveCount(0);
  await expect(admin.locator('.admin-sidebar')).toBeVisible();
  await expect(row.getByRole('button',{name:'Hoàn thành',exact:true})).toHaveCount(0);
  await row.getByRole('button',{name:'Xác nhận',exact:true}).click();
  await expect(row).toContainText('Đã xác nhận');
  await row.getByRole('button',{name:'Chuyển sang đang giao',exact:true}).click();
  await expect(row).toContainText('Đang giao');
  await row.getByRole('button',{name:'Hoàn thành',exact:true}).click();
  await expect(row).toContainText('Hoàn thành');
  await expect(row.getByRole('button',{name:'Hủy',exact:true})).toHaveCount(0);
  await page.reload();
  await expect(page.locator(".status")).toHaveText("Hoàn thành");
  await page.goto("/products?search=NovaBook&sort=price-asc");
  await page.locator(`.product-card[href="/products/${purchaseProduct.id}"]`).click();
  const existingReview = page.getByRole("button", { name: "Xóa đánh giá", exact: true });
  if (await existingReview.count()) {
    page.once("dialog", dialog => dialog.accept());
    await existingReview.click();
  }
  await page.getByLabel("Nội dung", { exact: true }).fill("Đánh giá từ luồng mua hàng thực tế");
  await page.getByRole("button", { name: "Gửi đánh giá", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Đã lưu đánh giá");
  await page.getByRole("button", { name: "Sửa đánh giá", exact: true }).click();
  await page.getByLabel("Nội dung", { exact: true }).fill("Đã cập nhật đánh giá");
  await page.getByRole("button", { name: "Lưu đánh giá", exact: true }).click();
  await expect(page.getByText("Đã cập nhật đánh giá", { exact: true })).toBeVisible();
  page.once("dialog", dialog => dialog.accept());
  await page.getByRole("button", { name: "Xóa đánh giá", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Đã xóa đánh giá");
  await context.close();
});
test("admin modules and customer routes load and refresh without server errors", async ({ page }) => {
  const failures = [];
  page.on("pageerror", error => failures.push(error.message));
  page.on("response", response => { if (response.status() >= 500) failures.push(`${response.status()} ${response.url()}`); });
  await login(page, "admin");
  for (const resource of ["categories", "brands", "banners", "vouchers", "promotions", "users", "reviews", "orders", "policies"]) {
    await page.goto(`/admin/${resource}`);
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.getByRole("alert")).toHaveCount(0);
  }
  await page.goto("/admin/vouchers");
  await page.getByRole("button", { name: "Sửa", exact: true }).first().click();
  await expect(page.getByLabel("Giá trị đơn tối thiểu (VND)", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("status")).toContainText("Đã lưu");
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await login(page, "user");
  for (const path of ["/account", "/orders", "/policies", "/cart"]) {
    await page.goto(path);
    await page.reload();
    await expect(page.locator("h1")).toBeVisible();
    await expect(page.getByRole("alert")).toHaveCount(0);
  }
  expect(failures).toEqual([]);
});
test("admin product edit, policy edit and report download", async ({
  page,
}) => {
  await login(page, "admin");
  await page.goto("/admin/products");
  await page.getByRole("button", { name: "Sửa", exact: true }).first().click();
  await expect(
    page.getByLabel("Tên sản phẩm", { exact: true }),
  ).not.toHaveValue("");
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("status")).toContainText("Đã lưu");
  await page.goto("/admin/policies");
  await page.getByRole("button", { name: "Sửa", exact: true }).first().click();
  await page.getByRole("button", { name: "Lưu thay đổi" }).click();
  await expect(page.getByRole("status")).toContainText("Đã lưu");
  await page.goto("/admin");
  const download = page.waitForEvent("download");
  await page.getByRole("button", { name: "Tải báo cáo CSV" }).click();
  expect((await download).suggestedFilename()).toBe("orders.csv");
  await page.screenshot({
    path: "test-results/admin-desktop.png",
    fullPage: true,
  });
});
test("mobile admin and forbidden route", async ({ page }) => {
  await login(page, "admin");
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "Tổng quan cửa hàng" }),
  ).toBeVisible();
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await page.screenshot({
    path: "test-results/admin-mobile.png",
    fullPage: true,
  });
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await login(page, "user");
  await page.goto("/admin");
  await expect(
    page.getByRole("heading", { name: "Bạn không có quyền truy cập" }),
  ).toBeVisible();
});

test("option chips show images, clear unavailable combinations and remain readable on mobile", async ({ page }) => {
  await login(page,'admin');
  const token=await(await page.request.get('/api/v1/auth/csrf')).json(),headers={[token.headerName]:token.token};
  const category=(await(await page.request.get('/api/v1/categories')).json())[0];
  const brand={id:(await(await page.request.get('/api/v1/admin/categories/'+category.id)).json()).brandIds[0]};
  const key='option-fixture-'+Date.now();let product,promotion;
  try {
  const created=await page.request.post('/api/v1/admin/products',{headers,data:{name:key,sku:key,slug:key,categoryId:category.id,brandId:brand.id,price:5990000,quantity:0,warrantyMonths:12,images:['/tech/phone.svg'],variants:[['Đen','256 GB',2],['Trắng','128 GB',2],['Trắng','256 GB',0]].map(([color,size,quantity],i)=>({sku:key+'-'+i,price:5990000,quantity,active:true,imageUrl:'/tech/phone.svg',options:{'Màu sắc':color,'Dung lượng':size}}))}});
  expect(created.status()).toBe(201);product=await created.json();
  const offer=await page.request.post('/api/v1/admin/promotions',{headers,data:{name:key,discountRate:5,startDate:new Date(Date.now()-86400000).toISOString(),endDate:new Date(Date.now()+86400000).toISOString(),active:true,productIds:[product.id],brandIds:[]}});
  expect(offer.status()).toBe(201);promotion=await offer.json();
  await page.goto('/products/' + product.id);
  await page.getByRole('radio', { name: 'Màu sắc: Đen', exact: true }).check();
  await page.getByRole('radio', { name: 'Dung lượng: 256 GB', exact: true }).check();
  await expect(page.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeEnabled();
  await page.getByRole('radio', { name: 'Màu sắc: Trắng', exact: true }).check();
  await expect(page.getByRole('radio', { name: 'Dung lượng: 256 GB', exact: true })).toBeDisabled();
  await expect(page.getByRole('radio', { name: 'Dung lượng: 256 GB', exact: true })).not.toBeChecked();
  await expect(page.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeDisabled();
  await page.getByRole('radio', { name: 'Dung lượng: 128 GB', exact: true }).check();
  await expect(page.locator('.discount-label')).toHaveText('Giảm 5%');
  await expect(page.getByRole('button', { name: 'Thêm vào giỏ hàng' })).toBeEnabled();
  await expect(page.locator('.option-chip.chosen img')).toBeVisible();
  await page.screenshot({ path: 'test-results/options-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  expect(await page.locator('.site-header nav').evaluate(el => parseFloat(getComputedStyle(el).fontSize))).toBeGreaterThanOrEqual(12);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await page.screenshot({ path: 'test-results/options-mobile.png', fullPage: true });
  } finally {
    if(promotion)await page.request.delete('/api/v1/admin/promotions/'+promotion.id,{headers});
    if(product)await page.request.delete('/api/v1/admin/products/'+product.id,{headers});
  }
});

test("admin creates custom variants, customer selects and cancels with stock restored", async ({ page }) => {
  test.setTimeout(90000);
  await login(page, "admin");
  await page.goto("/admin/products");
  await page.getByRole("button", { name: "+ Thêm mới", exact: true }).click();
  const key = "e2e-variant-" + Date.now();
  await page.getByLabel("Tên sản phẩm", { exact: true }).fill(key);
  await page.getByLabel("SKU", { exact: true }).fill(key);
  await page.getByLabel("Slug", { exact: true }).fill(key);
  const category = page.getByLabel("Danh mục", { exact: true });
  await expect(category.locator("option").first()).toBeAttached();
  await category.selectOption({ index: 1 });
  await expect(page.getByLabel("Thương hiệu", { exact: true }).locator("option:not([disabled])").first()).toBeAttached();
  await page.getByLabel("Thương hiệu", { exact: true }).selectOption({ index: 1 });
  await page.getByLabel("Đường dẫn ảnh, mỗi dòng một ảnh", { exact: true }).fill("/tech/phone.svg");
  for (const [i, color, price, stock] of [[1, "Đen", 2000000, 2], [2, "Trắng", 3000000, 1]]) {
    await page.getByRole("button", { name: "Thêm phiên bản", exact: true }).click();
    await page.getByLabel("SKU phiên bản " + i, { exact: true }).fill(key + "-" + i);
    await page.getByLabel("Giá phiên bản " + i, { exact: true }).fill(String(price));
    await page.getByLabel("Tồn kho phiên bản " + i, { exact: true }).fill(String(stock));
    await page.getByLabel("Giá trị " + i + ".1", { exact: true }).fill(color);
    const row = page.locator(".variant-row").nth(i - 1);
    await row.getByRole("button", { name: "Thêm thuộc tính" }).click();
    await page.getByLabel("Tên thuộc tính " + i + ".2", { exact: true }).fill("Phiên bản");
    await page.getByLabel("Giá trị " + i + ".2", { exact: true }).fill("256 GB");
    await page.getByLabel("Ảnh phiên bản " + i, { exact: true }).fill(i === 1 ? "/tech/phone.svg" : "/tech/tablet.svg");
  }
  await page.getByRole("button", { name: "Lưu thay đổi", exact: true }).click();
  await expect(page.getByRole("status")).toContainText("Đã lưu");
  const catalog = await (await page.request.get("/api/v1/products?search=" + key)).json();
  const id = catalog.content[0].id;
  const product = await (await page.request.get("/api/v1/products/" + id)).json();
  expect(product.variants).toHaveLength(2);
  await page.getByRole("button", { name: "Đăng xuất", exact: true }).click();
  await login(page, "user");
  await page.goto("/products/" + id);
  await expect(page.getByRole("button", { name: "Thêm vào giỏ hàng" })).toBeDisabled();
  await page.getByRole("radio", { name: "Màu sắc: Trắng", exact: true }).check();
  await page.getByRole("radio", { name: "Phiên bản: 256 GB", exact: true }).check();
  await expect(page.getByRole("radio", { name: "Màu sắc: Trắng", exact: true })).toBeChecked();
  await expect(page.locator('.option-chip.chosen').filter({ hasText: 'Trắng' })).toHaveCSS('border-top-color', 'rgb(223, 36, 53)');
  await expect(page.locator(".detail-image img")).toHaveAttribute("src", "/tech/tablet.svg");
  await expect(page.getByText("SKU: " + key + "-2", { exact: true })).toBeVisible();
  await page.getByRole("button", { name: "Thêm vào giỏ hàng" }).click();
  await expect(page.getByRole("status")).toContainText("Đã thêm");
  await page.goto("/cart");
  await expect(page.locator(".cart-line").filter({ hasText: key })).toContainText("Trắng");
  await page.getByRole("link", { name: "Tiến hành đặt hàng" }).click();
  await page.getByLabel("Họ tên", { exact: true }).fill("Khách phiên bản");
  await page.getByLabel("Số điện thoại").fill("0901234567");
  await page.getByLabel("Địa chỉ đầy đủ").fill("123 Đường kiểm thử");
  await page.getByRole("button", { name: "Xác nhận đặt hàng" }).click();
  await expect(page).toHaveURL(/\/orders\/\d+$/);
  await expect(page.getByRole("link", { name: new RegExp(key + ".*Trắng") })).toBeVisible();
  const orderId = page.url().split("/").pop();
  const ordered = await (await page.request.get("/api/v1/orders/" + orderId)).json();
  expect(ordered.items.find(i => i.productId === id).variantId).toBe(product.variants[1].id);
  page.once("dialog", dialog => dialog.accept());
  await page.getByRole("button", { name: "Hủy đơn", exact: false }).click();
  await expect(page.locator(".status")).toHaveText("Đã hủy");
  const restored = await (await page.request.get("/api/v1/products/" + id)).json();
  expect(restored.variants.map(v => v.quantity)).toEqual([2, 1]);
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/products/" + id);
  await expect(page.getByRole("radio", { name: "Màu sắc: Trắng", exact: true })).toBeAttached();
  await page.getByRole("radio", { name: "Màu sắc: Trắng", exact: true }).check();
  await page.getByRole("radio", { name: "Phiên bản: 256 GB", exact: true }).check();
  await page.screenshot({ path: "test-results/variant-mobile.png", fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBeTruthy();
  await page.getByRole('button', { name: 'Đăng xuất', exact: true }).click();
  await login(page, 'admin');
  const csrf = await (await page.request.get('/api/v1/auth/csrf')).json();
  const archived = await page.request.delete('/api/v1/admin/products/' + id, { headers: { [csrf.headerName]: csrf.token } });
  expect(archived.ok()).toBeTruthy();
});
