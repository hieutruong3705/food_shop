<script setup>
import { ref, computed, watch } from "vue";
import { useRoute } from "vue-router";
import { api, download } from "../services/api";
import { resources } from "../lib/admin";
import { money, date, statuses, paymentStatuses, useTask, imageUrl, imageError, rateText } from "../lib/shop";
import TaskState from "../components/TaskState.vue";
import SearchMultiSelect from '../components/SearchMultiSelect.vue';
import OfferForm from '../components/OfferForm.vue';
const route = useRoute(),
  type = computed(() => route.params.resource || ""),
  config = computed(() => resources[type.value]),
  isOffer = computed(() => ['promotions', 'vouchers'].includes(type.value)),
  rows = ref([]),
  page = ref(0),
  pages = ref(0),
  stats = ref(null),
  editing = ref(null),
  form = ref({}),
  selected = ref(null),
  choices = ref({ categories: [], brands: [], products: [] });
const filters = ref({});
const search = ref(""),
  statusFilter = ref(""),
  startDate = ref(""),
  endDate = ref("");
const { busy, error, message, run } = useTask();
async function load() {
  editing.value = null;
  selected.value = null;
  if (!type.value) {
    stats.value = await api(
      "/admin/dashboard?" +
        new URLSearchParams(
          Object.fromEntries(
            Object.entries({
              startDate: startDate.value,
              endDate: endDate.value,
            }).filter(([, v]) => v),
          ),
        ),
    );
    return;
  }
  if (!config.value) throw new Error("Trang quản trị không tồn tại");
  const data = await api(
    `/admin/${type.value}?page=${page.value}&size=20&search=${encodeURIComponent(search.value)}&status=${statusFilter.value}&${new URLSearchParams(filters.value)}`,
  );
  rows.value = Array.isArray(data) ? data : data.content;
  pages.value = data.totalPages || 1;
  if (page.value >= pages.value) { page.value = pages.value - 1; return load(); }
  if (["products", "vouchers"].includes(type.value)) {
    [choices.value.categories, choices.value.brands] = await Promise.all([
      allOptions("categories"),
      allOptions("brands"),
    ]);
  }
}
watch(
  type,
  () => {
    page.value = 0;
    search.value = "";
    filters.value = { categoryId:'', brandId:'' };
    statusFilter.value = "";
    run(load);
  },
  { immediate: true },
);
async function allOptions(resource) {
  let result = [],
    index = 0,
    data;
  do {
    data = await api(`/admin/${resource}?page=${index++}&size=100`);
    result.push(...data.content);
  } while (!data.last);
  return result;
}
function edit(row) {
  run(async () => {
    editing.value = null;
    const fresh = row ? await api(`/admin/${type.value}/${row.id}`) : {};
    form.value = {
      active: true,
      technologyCatalog: true,
      all: true,
      price: 0,
      quantity: 0,
      warrantyMonths: 24,
      minimumOrder: 0,
      role: "USER",
      categoryIds: [],
      productIds: [],
      brandIds: [],
      variants: [],
      ...JSON.parse(JSON.stringify(fresh)),
    };
    form.value.variants = (form.value.variants || []).map(v => ({ ...v, attributes: Object.entries(v.options || {}).map(([name, value]) => ({ name, value })) }));
    if(type.value==='products'){choices.value.categories=await allOptions('categories');await loadProductBrands(false);}
    if(['vouchers','promotions'].includes(type.value))form.value.scopeType=fresh.scopeType||'ALL';
    for (const [key, , kind] of config.value.fields) {
      if (kind === "lines")
        form.value[key] = (form.value[key] || []).join("\n");
      if (kind === "specs")
        form.value[key] = (form.value[key] || [])
          .map((s) => `${s.name}: ${s.value}`)
          .join("\n");
      if (kind === "datetime-local" && form.value[key]) {
        const d = new Date(form.value[key]);
        form.value[key] = new Date(d.getTime() - d.getTimezoneOffset() * 60000)
          .toISOString()
          .slice(0, 16);
      }
    }
    if (isOffer.value && !row) form.value[type.value === 'vouchers' ? 'discount' : 'discountRate'] = 10;
    if (type.value === 'banners' && !form.value.placement) form.value.placement = 'HOME_HERO';
    if (type.value === 'promotions' && !row) {
      const now = new Date(), local = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
      form.value.startDate = local(now);
      form.value.endDate = local(new Date(now.getTime() + 7 * 86400000));
    }
    editing.value = row?.id || "new";
  });
}
function save() {
  run(async () => {
    const body = { ...form.value };
    if (type.value === 'products') body.variants = form.value.variants.map(v => {
      const attrs = v.attributes.map(a => [a.name.trim(), a.value.trim()]);
      if (attrs.some(([n, value]) => !n || !value) || new Set(attrs.map(([n]) => n.toLowerCase())).size !== attrs.length) throw new Error('Điền tên và giá trị thuộc tính, không trùng tên.');
      return { id: v.id, sku: v.sku, price: Number(v.price), quantity: Number(v.quantity), active: v.active, imageUrl: v.imageUrl || '', options: Object.fromEntries(attrs) };
    });
    for (const [key, , kind] of config.value.fields) {
      if (kind === "number" || ["category", "brand"].includes(kind))
        body[key] = ["maximumDiscount", "usageLimit"].includes(key) && (body[key] == null || body[key] === "") ? null : Number(body[key]);
      if (kind === "lines")
        body[key] = (body[key] || "")
          .split("\n")
          .map((s) => s.trim())
          .filter(Boolean);
      if (kind === "specs")
        body[key] = (body[key] || "")
          .split("\n")
          .filter((s) => s.trim())
          .map((s) => {
            const i = s.indexOf(":");
            if (i < 1) throw new Error("Thông số cần định dạng Tên: Giá trị");
            return { name: s.slice(0, i).trim(), value: s.slice(i + 1).trim() };
          });
      if (kind === "datetime-local")
        body[key] = body[key] ? new Date(body[key]).toISOString() : null;
    }
    const saved = await api(
      `/admin/${type.value}${editing.value === "new" ? "" : `/${editing.value}`}`,
      {
        method: editing.value === "new" ? "POST" : "PUT",
        body: JSON.stringify(body),
      },
    );
    await load();
    // Promotions may overlap others; saving still succeeds and the admin sees which ones.
    message.value = ["Đã lưu thay đổi.", ...(saved?.warnings || [])].join(" ");
  });
}
const rowPicture = (row) => row.logoUrl || row.imageUrl || (type.value === 'categories' ? row.image : '');
function offerStatus(row){
  if(!row.active)return{text:'Đã tắt',tone:'off'};
  if(row.usageLimit&&row.usedCount>=row.usageLimit)return{text:'Hết lượt',tone:'off'};
  const now=Date.now();
  if(row.startDate&&now<new Date(row.startDate))return{text:'Sắp diễn ra',tone:'soon'};
  if(row.endDate&&now>new Date(row.endDate))return{text:'Đã kết thúc',tone:'off'};
  return{text:'Đang chạy',tone:'on'};
}
const offerWindow=(row)=>row.startDate||row.endDate?`${row.startDate?date(row.startDate):'Từ trước'} → ${row.endDate?date(row.endDate):'không giới hạn'}`:'Không giới hạn thời gian';
let brandRequest=0;
async function loadProductBrands(clear=true){
  const request=++brandRequest;choices.value.brands=[];if(clear)form.value.brandId='';
  if(!form.value.categoryId)return;
  let index=0,data,list=[];do{data=await api('/admin/options/brands?categoryId='+form.value.categoryId+'&page='+index++);list.push(...data.content);}while(!data.last);
  if(request===brandRequest)choices.value.brands=list;
}
function remove(row) {
  if (type.value === 'products') {
    const active = !row.active;
    if (!window.confirm(active ? 'Bán lại sản phẩm này? Tồn kho và trạng thái từng phiên bản được giữ nguyên.' : 'Ngừng kinh doanh sản phẩm này?')) return;
    return run(async () => {
      await api(`/admin/products/${row.id}/status`, { method:'PATCH', body:JSON.stringify({active}) });
      await load();
      message.value = active ? (row.quantity > 0 ? 'Đã mở bán lại sản phẩm.' : 'Đã mở bán lại. Sản phẩm đang hết hàng; cần cập nhật tồn kho để khách có thể mua.') : 'Đã ngừng bán sản phẩm.';
    });
  }
  if (
    !window.confirm(
      type.value === "products"
        ? "Ngừng kinh doanh sản phẩm này?"
        : "Xác nhận xóa mục này?",
    )
  )
    return;
  run(async () => {
    await api(`/admin/${type.value}/${row.id}`, { method: "DELETE" });
    await load();
    message.value = "Đã cập nhật.";
  });
}
function showOrder(row) {
  run(async () => (selected.value = await api(`/admin/orders/${row.id}`)));
}
const actionLabels={CONFIRMED:'Xác nhận',SHIPPING:'Chuyển sang đang giao',COMPLETED:'Hoàn thành',CANCELLED:'Hủy'};
const bannerPlacements={HOME_HERO:'Banner chính',VOUCHER_PAGE:'Trang mã giảm giá',HOME_BANNER_1:'Banner 1',HOME_BANNER_2:'Banner 2',HOME_BANNER_3:'Banner 3',HOME_BRAND_1_1:'Thương hiệu 1.1',HOME_BRAND_1_2:'Thương hiệu 1.2',HOME_BRAND_1_3:'Thương hiệu 1.3',HOME_BRAND_1_4:'Thương hiệu 1.4',HOME_GROUP_1:'Nhóm sản phẩm 1 (2 ô)',HOME_GROUP_2:'Nhóm sản phẩm 2 (1 ô)',HOME_GROUP_3:'Nhóm sản phẩm 3 (2 ô)',HOME_GROUP_4:'Nhóm sản phẩm 4 (1 ô)',HOME_GROUP_5:'Nhóm sản phẩm 5 (2 ô)',HOME_RECENT:'Bạn vừa xem (1 ô)'};
function transition(status,row=selected.value) {
  if(status==='CANCELLED'&&!window.confirm(`Hủy đơn #${row.id}? Tồn kho sẽ được hoàn lại.`))return;
  run(async () => {
    const keepDetail=selected.value?.id===row.id;
    const updated = await api(`/admin/orders/${row.id}/status`, {
      method: "PATCH",
      body: JSON.stringify({ status }),
    });
    await load();
    if(keepDetail)selected.value=updated;
    message.value = `Đơn #${row.id}: ${statuses[updated.status]}.`;
  });
}
function navigate(delta) {
  page.value += delta;
  run(load);
}
function archiveOrder() {
  if (
    !window.confirm(
      selected.value.archived
        ? "Khôi phục đơn hàng?"
        : "Lưu trữ đơn hàng? Lịch sử khách hàng và doanh thu vẫn được giữ.",
    )
  )
    return;
  run(async () => {
    await api(
      "/admin/orders/" +
        selected.value.id +
        (selected.value.archived ? "/restore" : ""),
      { method: selected.value.archived ? "POST" : "DELETE" },
    );
    await load();
    message.value = "Đã cập nhật lưu trữ đơn hàng.";
  });
}
function exportFile(path, name) {
  run(() => download(path, name));
}
async function upload(event, variant) {
  const file = event.target.files[0];
  if (!file) return;
  await run(async () => {
    const data = new FormData();
    data.append("file", file);
    const r = await api("/admin/uploads", { method: "POST", body: data });
    if (variant) variant.imageUrl = r.url;
    else if (type.value === "products")
      form.value.images = [form.value.images, r.url].filter(Boolean).join("\n");
    else
      form.value[
        type.value === "brands"
          ? "logoUrl"
          : type.value === "categories"
            ? "image"
            : "imageUrl"
      ] = r.url;
    message.value = "Đã tải ảnh lên. Bấm Lưu thay đổi để lưu ảnh vào sản phẩm hoặc nội dung đang chỉnh sửa.";
  });
  event.target.value = "";
}
// The form is drawn as cards; a resource without sections gets one card, and fields no card lists follow in a last one.
const formSections = computed(() => {
  const fields = config.value?.fields || [];
  const sections = (config.value?.sections || [{ title: 'Thông tin', keys: fields.map(f => f[0]) }]).map(s => ({ ...s, fields: fields.filter(f => s.keys.includes(f[0])) }));
  const used = new Set(sections.flatMap(s => s.keys));
  const rest = fields.filter(f => !used.has(f[0]));
  return rest.length ? [...sections, { title: 'Khác', wide: true, keys: [], fields: rest }] : sections;
});
const imageField = computed(() => ({ products: "images", categories: "image", brands: "logoUrl", banners: "imageUrl" })[type.value]);
const previewImages = computed(() => {
  if (!imageField.value) return [];
  const value = form.value[imageField.value] || "";
  return type.value === "products" ? String(value).split("\n").map(s => s.trim()).filter(Boolean) : [value].filter(Boolean);
});
function removeImage(index) {
  if (type.value === "products") form.value.images = previewImages.value.filter((_, i) => i !== index).join("\n");
  else form.value[imageField.value] = "";
}
function makeCover(index) {
  const images = [...previewImages.value];
  images.unshift(...images.splice(index, 1));
  form.value.images = images.join("\n");
}
function addVariant() {
  form.value.variants.push({ sku: '', price: form.value.price || 0, quantity: 0, active: true, imageUrl: '', attributes: [{ name: 'Màu sắc', value: '' }] });
}
const transitions = computed(() => selected.value?.allowedStatuses || []);
</script>
<template>
  <div class="admin-page">
    <section class="admin-content">
      <p v-if="['categories','brands'].includes(type)" class="notice">Gán Thương hiệu áp dụng trong form Danh mục. Sản phẩm chỉ được chọn hãng đã gán; không thể bỏ hãng còn được sản phẩm trong danh mục sử dụng.</p>
      <div class="section-heading">
        <div>
          <span class="eyebrow">TECHSHOP / QUẢN TRỊ</span>
          <h1>{{ config?.title || "Tổng quan cửa hàng" }}</h1>
        </div>
        <button
          v-if="config && !config.noCreate"
          @click="edit(null)"
          :disabled="busy"
        >
          + Thêm mới
        </button>
      </div>
      <TaskState :busy="busy" :error="error" :message="message" />
      <form v-if="!type" class="filters report-filter" @submit.prevent="run(load)">
        <label>Từ ngày<input v-model="startDate" type="date" /></label
        ><label>Đến ngày<input v-model="endDate" type="date" /></label
        ><button :disabled="busy">Lọc báo cáo</button>
      </form>
      <form
        v-if="type"
        class="filters"
        @submit.prevent="
          page = 0;
          run(load);
        "
      >
        <input
          v-model="search"
          aria-label="Tìm trong quản trị"
          placeholder="Tìm tên, mã hoặc nội dung…"
        /><select
          v-if="type === 'orders'"
          v-model="statusFilter"
          aria-label="Trạng thái đơn"
        >
          <option value="">Tất cả trạng thái</option>
          <option v-for="(label, key) in statuses" :key="key" :value="key">
            {{ label }}
          </option>
        </select>
        <select
          v-if="type === 'users'"
          v-model="filters.role"
          aria-label="Lọc quyền"
        >
          <option value="">Tất cả quyền</option>
          <option>USER</option>
          <option>ADMIN</option>
        </select>
        <select
          v-if="type === 'users'"
          v-model="filters.locked"
          aria-label="Lọc khóa tài khoản"
        >
          <option value="">Mọi tài khoản</option>
          <option value="true">Đã khóa</option>
          <option value="false">Đang mở</option>
        </select>
        <select
          v-if="type === 'reviews'"
          v-model="filters.rating"
          aria-label="Lọc số sao"
        >
          <option value="">Tất cả số sao</option>
          <option v-for="n in 5" :key="n" :value="n">{{ n }} sao</option>
        </select>
        <select
          v-if="['products', 'vouchers'].includes(type)"
          v-model="filters.categoryId"
          aria-label="Lọc danh mục"
        >
          <option value="">Tất cả danh mục</option>
          <option v-if="type === 'vouchers'" value="-1">
            Voucher toàn cửa hàng
          </option>
          <option v-for="c in choices.categories" :key="c.id" :value="c.id">
            {{ c.name }}
          </option>
        </select>
        <select
          v-if="type === 'products'"
          v-model="filters.brandId"
          aria-label="Lọc thương hiệu"
        >
          <option value="">Tất cả thương hiệu</option>
          <option v-for="b in choices.brands" :key="b.id" :value="b.id">
            {{ b.name }}
          </option>
        </select>
        <select
          v-if="type === 'products'"
          v-model="statusFilter"
          aria-label="Lọc kinh doanh"
        >
          <option value="">Tất cả trạng thái</option>
          <option value="active">Đang kinh doanh</option>
          <option value="inactive">Ngừng kinh doanh</option>
        </select>
        <select
          v-if="type === 'orders'"
          v-model="filters.archived"
          aria-label="Lọc lưu trữ"
        >
          <option value="false">Đơn hiện tại</option>
          <option value="true">Đã lưu trữ</option>
        </select>
        <select
          v-if="type === 'orders'"
          v-model="filters.refundRequired"
          aria-label="Lọc hoàn tiền"
        >
          <option value="">Mọi thanh toán</option>
          <option value="true">Cần hoàn tiền</option>
        </select>
        <button :disabled="busy">Tìm kiếm</button>
      </form>
      <template v-if="!type && stats"
        ><div class="stats-grid">
          <article
            v-for="(label, key) in {
              revenue: 'Doanh thu hoàn thành',
              newOrders: 'Đơn phát sinh (trừ đơn hủy)',
              completedOrders: 'Đơn hoàn thành trong kỳ',
              cancelledOrders: 'Đơn tạo trong kỳ đã hủy',
              pending: 'Chờ xác nhận',
              products: 'Sản phẩm',
              users: 'Người dùng',
              lowStock: 'Sản phẩm sắp hết',
              refundRequired: 'Đơn cần hoàn tiền',
            }"
            :key="key"
            class="panel"
          >
            <small>{{ label }}</small
            ><strong>{{
              key === "revenue" ? money(stats[key]) : stats[key]
            }}</strong>
          </article>
        </div>
        <div class="panel section">
          <h2>Doanh thu theo ngày</h2>
          <div class="revenue-chart">
            <div
              v-for="(value, day) in stats.chart"
              :key="day"
              :title="day + ': ' + money(value)"
            >
              <span
                :style="{
                  height:
                    Math.max(
                      2,
                      (value / Math.max(1, ...Object.values(stats.chart))) *
                        130,
                    ) + 'px',
                }"
              ></span
              ><small>{{ day.slice(8) }}</small>
            </div>
          </div>
          <h2>Sản phẩm bán chạy</h2>
          <p v-if="!stats.bestSelling.length" class="muted">
            Chưa có dữ liệu bán hàng.
          </p>
          <ul v-else class="best-sellers">
            <li v-for="p in stats.bestSelling" :key="p.productId">
              <img :src="imageUrl(p.productImage)" :alt="p.productName" @error="imageError" />
              <strong>{{ p.productName }}</strong>
              <span>{{ p.quantitySold }} sản phẩm</span>
            </li>
          </ul>
        </div>
        <div class="panel section">
          <h2>Báo cáo đơn hàng</h2>
          <p>Xuất dữ liệu đơn hàng để đối soát.</p>
          <button
            :disabled="busy"
            @click="exportFile('/admin/reports/orders', 'orders.csv')"
          >
            Tải báo cáo CSV
          </button>
        </div></template
      >
      <form v-if="editing" class="panel form admin-form" @submit.prevent="save">
        <div class="section-heading">
          <h2>
            {{ editing === "new" ? "Thêm mới" : `Chỉnh sửa #${editing}` }}
          </h2>
          <button type="button" class="secondary" @click="editing = null">
            Đóng
          </button>
        </div>
        <OfferForm v-if="isOffer" v-model="form" :type="type" />
        <template v-else>
          <section v-for="section in formSections" :key="section.title" class="admin-card">
            <header><h3>{{ section.title }}</h3><p v-if="section.hint" class="muted">{{ section.hint }}</p></header>
            <div v-if="section.fields.length" class="admin-fields" :class="{ wide: section.wide }">
              <label
          v-for="[key, label, kind] in section.fields"
          :key="key"
          :class="{ 'checkbox-label': kind === 'checkbox', full: ['textarea', 'lines', 'specs'].includes(kind) }"
          >{{ label
          }}<input
            v-if="kind === 'checkbox'"
            type="checkbox"
            v-model="form[key]" /><textarea
            v-else-if="['textarea', 'lines', 'specs'].includes(kind)"
            v-model="form[key]"
            :rows="kind === 'textarea' ? 4 : 3" /><select
            v-else-if="kind === 'role'"
            v-model="form[key]"
          >
            <option>USER</option>
            <option>ADMIN</option></select
          ><select
            v-else-if="kind === 'bannerPlacement'"
            v-model="form[key]"
          >
            <option v-for="(label, value) in bannerPlacements" :key="value" :value="value">{{ label }}</option></select
          ><select
            v-else-if="
              [
                'category',
                'brand',
                'categories',
                'brands',
                'products',
              ].includes(kind)
            "
            v-model="form[key]"
            :multiple="['categories', 'brands', 'products'].includes(kind)"
            :aria-label="label"
            :required="['category', 'brand'].includes(kind)"
            @change="kind==='category' && type==='products' && run(()=>loadProductBrands())"
          >
            <option v-if="['category','brand'].includes(kind)" value="" disabled>Chọn {{ label.toLowerCase() }}</option>
            <option
              v-for="c in choices[
                kind === 'category'
                  ? 'categories'
                  : kind === 'brand'
                    ? 'brands'
                    : kind
              ]"
              :key="c.id"
              :value="c.id"
            >
              {{ c.name }} (#{{ c.id }})
            </option></select
          ><input
            v-else
            v-model="form[key]"
            :type="kind || 'text'"
            :disabled="type === 'products' && form.variants?.length > 0 && ['price', 'quantity'].includes(key)"
            :min="kind === 'number' ? 0 : undefined"
            :required="
              [
                'name',
                'sku',
                'slug',
                'price',
                'quantity',
                'code',
              ].includes(key) || (type === 'promotions' && ['startDate', 'endDate'].includes(key))
            " /></label>
            </div>
            <p v-if="section.notice === 'brands' && form.categoryId && !choices.brands.length" class="notice">Danh mục chưa gán thương hiệu. Hãy chọn hãng tại Quản trị → Danh mục trước khi lưu sản phẩm.</p>
            <SearchMultiSelect v-if="section.brandPicker" v-model="form.brandIds" resource="brands" label="Thương hiệu áp dụng" />
            <template v-if="section.media">
              <label>Tải ảnh (PNG/JPEG/WebP, tối đa 5 MB)<input
              type="file"
              accept="image/png,image/jpeg,image/webp"
              @change="upload($event)"
              :disabled="busy" /></label>
                      <section class="image-preview-section" aria-label="Xem trước ảnh">
                    <h3>Xem trước ảnh</h3>
                    <p class="muted">Ảnh được tải lên máy chủ. Bấm Lưu thay đổi để gắn ảnh với mục này.</p>
                    <p v-if="!previewImages.length" class="muted">Chưa có ảnh. Chọn tệp ở trên hoặc nhập đường dẫn ảnh.</p>
                    <div class="admin-image-grid">
                      <figure v-for="(url, index) in previewImages" :key="url + index" class="admin-image-preview">
                        <a :href="imageUrl(url)" target="_blank" rel="noopener noreferrer" aria-label="Mở ảnh kích thước đầy đủ">
                          <img :src="imageUrl(url)" :alt="`Ảnh xem trước ${index + 1}`" @error="imageError" />
                        </a>
                        <figcaption>{{ type === 'products' && index === 0 ? 'Ảnh đại diện' : `Ảnh ${index + 1}` }}</figcaption>
                        <button v-if="type === 'products' && index > 0" type="button" class="secondary" :disabled="busy" @click="makeCover(index)">Đặt làm ảnh đại diện</button>
                        <button type="button" class="text-button" :disabled="busy" @click="removeImage(index)">Bỏ ảnh khỏi mục này</button>
                      </figure>
                    </div>
                  </section>
            </template>
          </section>
        </template>
        <section v-if="type === 'products'" class="variant-editor" aria-label="Tùy chọn sản phẩm">
          <h3>Tùy chọn / Phiên bản sản phẩm</h3>
          <p class="muted">Mỗi phiên bản có SKU, giá và tồn kho riêng. Bạn có thể thêm Màu sắc, RAM, Dung lượng hoặc thuộc tính khác. Giá chung là giá thấp nhất; tồn kho chung là tổng các phiên bản đang bán.</p>
          <fieldset v-for="(v, i) in form.variants" :key="v.id || 'new-' + i" class="panel variant-row">
            <legend>Phiên bản {{ i + 1 }}</legend>
            <label>SKU phiên bản {{ i + 1 }}<input v-model="v.sku" required maxlength="80" /></label>
            <div class="two-column">
              <label>Giá phiên bản {{ i + 1 }}<input v-model.number="v.price" type="number" min="0" max="1000000000" step="1" required /></label>
              <label>Tồn kho phiên bản {{ i + 1 }}<input v-model.number="v.quantity" type="number" min="0" max="1000000" step="1" required /></label>
            </div>
            <div v-for="(a, j) in v.attributes" :key="j" class="variant-attribute">
              <label>Tên thuộc tính {{ i + 1 }}.{{ j + 1 }}<input v-model="a.name" required maxlength="80" placeholder="Ví dụ: Màu sắc" /></label>
              <label>Giá trị {{ i + 1 }}.{{ j + 1 }}<input v-model="a.value" required maxlength="100" placeholder="Ví dụ: Đen" /></label>
              <button type="button" class="secondary" :disabled="busy || v.attributes.length === 1" @click="v.attributes.splice(j, 1)">Bỏ thuộc tính</button>
            </div>
            <button type="button" class="secondary" :disabled="busy || v.attributes.length >= 6" @click="v.attributes.push({name: '', value: ''})">Thêm thuộc tính</button>
            <label>Ảnh phiên bản {{ i + 1 }}<input v-model="v.imageUrl" maxlength="500" placeholder="Để trống để dùng ảnh sản phẩm" /></label>
            <label>Tải ảnh phiên bản {{ i + 1 }}<input type="file" accept="image/png,image/jpeg,image/webp" :disabled="busy" @change="upload($event, v)" /></label>
            <img v-if="v.imageUrl" class="variant-image-preview" :src="imageUrl(v.imageUrl)" :alt="`Xem trước phiên bản ${i + 1}`" @error="imageError" />
            <label class="checkbox-label"><input type="checkbox" v-model="v.active" />Đang bán phiên bản {{ i + 1 }}</label>
            <button v-if="!v.id" type="button" class="secondary" :disabled="busy" @click="form.variants.splice(i, 1)">Bỏ phiên bản mới</button>
            <p v-else class="muted">Bỏ chọn Đang bán để ngừng phiên bản và giữ lịch sử đơn hàng.</p>
          </fieldset>
          <button type="button" class="secondary" :disabled="busy || form.variants.length >= 100" @click="addVariant">Thêm phiên bản</button>
        </section>
        <button :disabled="busy">Lưu thay đổi</button>
      </form>
      <article v-if="selected" class="panel section">
        <div class="section-heading">
          <h2>Đơn #{{ selected.id }} · {{ statuses[selected.status] }}</h2>
          <button class="secondary" @click="selected = null">Đóng</button>
        </div>
        <p>{{ selected.receiverName }} · {{ selected.receiverPhone }}</p>
        <p>{{ selected.receiverAddress }}</p>
        <p v-if="selected.note">Ghi chú: {{ selected.note }}</p>
        <p>{{ selected.paymentMethod }} · {{ paymentStatuses[selected.paymentStatus] || selected.paymentStatus }}</p>
        <p v-if="selected.refundRequired" class="notice error" role="alert">
          Cần hoàn tiền: VNPay báo đã thu tiền sau khi đơn bị hủy hoặc hết hạn. Mã giao dịch VNPay: {{ selected.paymentReference || "—" }}.
        </p>
        <div v-for="item in selected.items" :key="item.id" class="summary-row">
          <span>{{ item.name }}{{ item.variantLabel ? " · " + item.variantLabel : "" }} × {{ item.quantity }}</span
          ><strong>{{ money(item.price * item.quantity) }}</strong>
        </div>
        <div class="summary-row total">
          <strong>Tổng thanh toán</strong
          ><strong>{{ money(selected.totalPrice) }}</strong>
        </div>
        <div class="actions">
          <button
            v-for="s in transitions"
            :key="s"
            :disabled="busy"
            @click="transition(s)"
          >
            {{ actionLabels[s] }}</button
          ><button
            class="secondary"
            :disabled="busy"
            @click="
              exportFile(
                `/admin/orders/${selected.id}/invoice`,
                `hoa-don-${selected.id}.pdf`,
              )
            "
          >
            Hóa đơn PDF
          </button>
          <button
            v-if="['COMPLETED', 'CANCELLED'].includes(selected.status)"
            class="secondary"
            :disabled="busy"
            @click="archiveOrder"
          >
            {{ selected.archived ? "Khôi phục" : "Lưu trữ" }}
          </button>
        </div>
      </article>
      <div v-if="type && config" class="panel table-wrap">
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>
                {{
                  type === "orders"
                    ? "Khách hàng"
                    : type === "reviews"
                      ? "Đánh giá"
                      : "Tên / Mã"
                }}
              </th>
              <th>Thông tin</th>
              <th>Thao tác</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in rows" :key="row.id">
              <td>#{{ row.id }}</td>
              <td>
                <img v-if="type === 'products' && row.images?.length" class="admin-product-thumbnail" :src="imageUrl(row.images[0])" :alt="row.name" @error="imageError" />
                <img v-else-if="rowPicture(row)" class="admin-product-thumbnail" :src="imageUrl(rowPicture(row))" alt="" @error="imageError" />
                {{
                  row.name ||
                  row.code ||
                  row.fullName ||
                  row.receiverName ||
                  row.author
                }}<small v-if="row.email">{{ row.email }}</small>
                <p v-if="row.content && type === 'reviews'">
                  {{ row.content }}
                </p>
              </td>
              <td>
                <template v-if="type === 'products'"
                  >{{ money(row.price) }}<span v-if="row.discountRate > 0" class="offer-status on" :title="row.promotionName">Đang giảm {{ rateText(row.discountRate) }}%</span
                  ><small>{{ row.sku }} · {{ row.categoryName || 'Chưa có danh mục' }} · {{ row.brandName || 'Chưa có hãng' }}</small
                  ><small
                    >Tồn: {{ row.quantity }} ·
                    {{ row.active ? (row.quantity > 0 ? "Đang bán" : "Đang bán · Hết hàng") : "Ngừng bán" }}</small
                  ></template
                ><template v-else-if="type === 'orders'"
                  >{{ money(row.totalPrice)
                  }}<small
                    >{{ statuses[row.status] }} ·
                    {{ date(row.createdAt) }}<template v-if="row.refundRequired"> · <b class="refund-flag">Cần hoàn tiền</b></template></small
                  ></template
                ><template v-else-if="type === 'users'"
                  >{{ row.role }} ·
                  {{ row.locked ? "Đã khóa" : "Hoạt động" }}</template
                ><template v-else-if="type === 'reviews'"
                  >{{ row.rating }} ★<small>{{
                    row.productName
                  }}</small></template
                ><template v-else-if="isOffer"
                  ><span class="offer-cell"
                    ><b class="offer-rate">Giảm {{ row.discount ?? row.discountRate }}%</b
                    ><span class="offer-status" :class="offerStatus(row).tone">{{ offerStatus(row).text }}</span></span
                  ><small>{{ row.scopeDescription }}</small
                  ><small>{{ offerWindow(row) }}<template v-if="type === 'vouchers'"> · Đã dùng {{ row.usedCount }}{{ row.usageLimit ? '/' + row.usageLimit : '' }}</template></small></template
                ><template v-else-if="type === 'categories'"
                  >{{ row.productCount }} sản phẩm<small>{{ row.brandNames?.length ? row.brandNames.join(', ') : 'Chưa gán thương hiệu' }}</small></template
                ><template v-else-if="type === 'brands'"
                  >{{ row.productCount }} sản phẩm<small>{{ row.description || 'Chưa có mô tả' }}</small></template
                ><template v-else-if="type === 'banners'"
                  >{{ row.active ? 'Đang hiển thị' : 'Ẩn' }}<small>{{ bannerPlacements[row.placement] || row.placement }} · {{ row.link }}</small></template
                ><template v-else>{{
                  row.active === false ? "Ẩn" : "—"
                }}</template>
              </td>
              <td>
                <div class="actions">
                  <button
                    v-if="type === 'orders'"
                    class="secondary"
                    @click="showOrder(row)"
                    :disabled="busy"
                  >
                    Chi tiết</button>
                  <template v-if="type === 'orders'"><button v-for="next in row.allowedStatuses" :key="next" :disabled="busy" :class="{'secondary':next==='CANCELLED'}" @click="transition(next,row)">{{ actionLabels[next] }}</button></template>
                  <template v-else
                    ><button
                      v-if="!config.noEdit"
                      class="secondary"
                      @click="edit(row)"
                      :disabled="busy"
                    >
                      Sửa</button
                    ><button
                      class="text-button"
                      @click="remove(row)"
                      :disabled="busy"
                    >
                      {{ type === "products" ? (row.active ? "Ngừng bán" : "Bán lại") : "Xóa" }}
                    </button></template
                  >
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-if="!busy && !rows.length" class="empty">Chưa có dữ liệu.</p>
      </div>
      <div v-if="type && pages > 1" class="pagination">
        <button @click="navigate(-1)" :disabled="busy || page === 0">
          ← Trước</button
        ><span>{{ page + 1 }} / {{ pages }}</span
        ><button @click="navigate(1)" :disabled="busy || page + 1 >= pages">
          Sau →
        </button>
      </div>
    </section>
  </div>
</template>
