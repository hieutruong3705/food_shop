<script setup>
import { computed } from 'vue';
import ScopePicker from './ScopePicker.vue';
import { money, rateText } from '../lib/shop';

// The promotion and voucher form: information, discount, time, status and what it applies to,
// with a live preview of what shoppers will see. It edits the same `form` object the admin page saves.
const props = defineProps({ modelValue: { type: Object, required: true }, type: { type: String, required: true } });
const emit = defineEmits(['update:modelValue']);
const voucher = computed(() => props.type === 'vouchers');
const f = computed(() => props.modelValue);
const rateKey = computed(() => (voucher.value ? 'discount' : 'discountRate'));
const rate = computed(() => Math.min(100, Math.max(0, Number(f.value[rateKey.value]) || 0)));
const presets = [5, 10, 15, 20, 30, 50];

function set(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value });
}
function toLocal(date) {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
}
function startNow() {
  set('startDate', toLocal(new Date()));
}
function lastFor(days) {
  const from = f.value.startDate ? new Date(f.value.startDate) : new Date();
  set('endDate', toLocal(new Date(from.getTime() + days * 86400000)));
}

const status = computed(() => {
  if (!f.value.active) return { text: 'Đang tắt', tone: 'off' };
  const now = new Date(), start = f.value.startDate ? new Date(f.value.startDate) : null, end = f.value.endDate ? new Date(f.value.endDate) : null;
  if (start && now < start) return { text: 'Sắp diễn ra', tone: 'soon' };
  if (end && now > end) return { text: 'Đã kết thúc', tone: 'off' };
  return { text: 'Đang chạy', tone: 'on' };
});

// Preview numbers: a 10,000,000 ₫ product for a promotion, a 2,000,000 ₫ order for a voucher.
const sample = 10000000, order = 2000000;
const salePrice = computed(() => Math.round(sample * (1 - rate.value / 100)));
const voucherCut = computed(() => {
  const raw = (order * rate.value) / 100, cap = Number(f.value.maximumDiscount);
  return cap > 0 ? Math.min(raw, cap) : raw;
});
</script>

<template>
  <div class="offer-form">
    <section class="card">
      <header><h3>Thông tin</h3></header>
      <template v-if="voucher">
        <div class="field">
          <label for="offer-code">Mã giảm giá</label>
          <input id="offer-code" :value="f.code" required maxlength="50" autocomplete="off" placeholder="Ví dụ: TECH10" @input="set('code', $event.target.value.toUpperCase())" />
          <small>Khách nhập mã này ở bước thanh toán. Mã không phân biệt hoa thường.</small>
        </div>
      </template>
      <template v-else>
        <div class="field">
          <label for="offer-name">Tên chương trình</label>
          <input id="offer-name" :value="f.name" required maxlength="100" placeholder="Ví dụ: Sale cuối tuần" @input="set('name', $event.target.value)" />
        </div>
        <div class="field">
          <label for="offer-description">Mô tả</label>
          <textarea id="offer-description" :value="f.description" rows="3" maxlength="2000" placeholder="Hiện ở trang chủ để khách biết ưu đãi này dành cho ai." @input="set('description', $event.target.value)"></textarea>
        </div>
      </template>
    </section>

    <section class="card">
      <header><h3>Mức giảm</h3></header>
      <div class="split">
        <div class="stack">
          <div class="field">
            <label for="offer-rate">Giảm giá (%)</label>
            <input id="offer-rate" :value="f[rateKey]" type="number" min="1" max="100" step="0.5" required @input="set(rateKey, $event.target.value)" />
          </div>
          <div class="presets" role="group" aria-label="Mức giảm nhanh">
            <button v-for="p in presets" :key="p" type="button" :aria-pressed="rate === p" @click="set(rateKey, String(p))">{{ p }}%</button>
          </div>
          <template v-if="voucher">
            <div class="field">
              <label for="offer-minimum">Giá trị đơn tối thiểu (VND)</label>
              <input id="offer-minimum" :value="f.minimumOrder" type="number" min="0" step="1000" @input="set('minimumOrder', $event.target.value)" />
              <small>Đơn thấp hơn mức này chưa dùng được mã.</small>
            </div>
            <div class="field">
              <label for="offer-maximum">Giảm tối đa (VND)</label>
              <input id="offer-maximum" :value="f.maximumDiscount" type="number" min="0" step="1000" placeholder="Không giới hạn" @input="set('maximumDiscount', $event.target.value)" />
              <small>Để trống nếu không giới hạn số tiền giảm.</small>
            </div>
            <div class="field">
              <label for="offer-limit">Số lượt sử dụng tối đa</label>
              <input id="offer-limit" :value="f.usageLimit" type="number" min="1" step="1" placeholder="Không giới hạn" @input="set('usageLimit', $event.target.value)" />
              <small v-if="f.usedCount">Đã dùng {{ f.usedCount }} lượt.</small>
              <small v-else>Để trống nếu không giới hạn số lượt.</small>
            </div>
          </template>
        </div>

        <aside class="preview" aria-label="Xem trước với khách hàng">
          <span class="kicker">Khách sẽ thấy</span>
          <template v-if="voucher">
            <div class="ticket">
              <strong class="big">Giảm {{ rateText(rate) }}%</strong>
              <span class="code">{{ f.code || 'MÃ-GIẢM-GIÁ' }}</span>
              <small>Đơn từ {{ money(f.minimumOrder) }}<template v-if="Number(f.maximumDiscount) > 0"> · Giảm tối đa {{ money(Number(f.maximumDiscount)) }}</template></small>
            </div>
            <p class="example">Ví dụ đơn {{ money(order) }} được giảm <b>{{ money(voucherCut) }}</b>.</p>
          </template>
          <template v-else>
            <div class="mock">
              <span class="frame"><em class="tag">Giảm {{ rateText(rate) }}%</em></span>
              <span class="name">Sản phẩm mẫu</span>
              <b class="now">{{ money(salePrice) }}</b>
              <del>{{ money(sample) }}</del>
              <small class="save">Tiết kiệm {{ money(sample - salePrice) }}</small>
            </div>
            <p class="example">Nhãn giảm giá hiện ở góc ảnh sản phẩm, trong giỏ hàng và trang chi tiết.</p>
          </template>
        </aside>
      </div>
    </section>

    <section class="card">
      <header><h3>Thời gian và trạng thái</h3><span class="state" :class="status.tone">{{ status.text }}</span></header>
      <div class="two">
        <div class="field">
          <label for="offer-start">{{ voucher ? 'Bắt đầu (để trống nếu không giới hạn)' : 'Bắt đầu' }}</label>
          <input id="offer-start" :value="f.startDate" type="datetime-local" :required="!voucher" @input="set('startDate', $event.target.value)" />
        </div>
        <div class="field">
          <label for="offer-end">{{ voucher ? 'Kết thúc (để trống nếu không giới hạn)' : 'Kết thúc' }}</label>
          <input id="offer-end" :value="f.endDate" type="datetime-local" :required="!voucher" @input="set('endDate', $event.target.value)" />
        </div>
      </div>
      <div class="presets" role="group" aria-label="Thời gian nhanh">
        <button type="button" @click="startNow">Bắt đầu ngay</button>
        <button type="button" @click="lastFor(7)">Kéo dài 7 ngày</button>
        <button type="button" @click="lastFor(30)">Kéo dài 30 ngày</button>
      </div>
      <div class="switch-row">
        <span><strong>Kích hoạt</strong><small>Tắt để tạm ngừng mà không xóa {{ voucher ? 'mã' : 'chương trình' }}.</small></span>
        <button type="button" class="switch" role="switch" :aria-checked="!!f.active" aria-label="Kích hoạt" @click="set('active', !f.active)"><i></i></button>
      </div>
    </section>

    <section class="card">
      <ScopePicker :model-value="modelValue" :kind="voucher ? 'voucher' : 'promotion'" :rate="voucher ? 0 : rate" @update:model-value="emit('update:modelValue', $event)" />
    </section>
  </div>
</template>

<style scoped>
.offer-form { display: grid; gap: 16px; }
.card { border: 1px solid #dce3e6; border-radius: 14px; background: #fff; padding: 20px; display: grid; gap: 14px; min-width: 0; }
.card header { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.card h3 { margin: 0; font-size: 16px; }
.field { display: grid; gap: 6px; }
.field > label { display: block; font-size: 14px; font-weight: 600; color: #2a5366; }
.field small { font-weight: 400; color: #606d76; font-size: 12px; }
.two { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.split { display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 300px); gap: 20px; align-items: start; }
.stack { display: grid; gap: 14px; }
.presets { display: flex; flex-wrap: wrap; gap: 6px; }
.presets button { background: #eef2f4; color: #2a5366; padding: 7px 13px; font-size: 13px; font-weight: 600; border: 1px solid transparent; }
.presets button[aria-pressed="true"] { background: #0085c3; color: #fff; }
.preview { display: grid; gap: 10px; background: #f5f7f8; border-radius: 12px; padding: 14px; }
.kicker { font-size: 11px; letter-spacing: 1px; text-transform: uppercase; color: #606d76; font-weight: 700; }
.mock { display: grid; gap: 5px; background: #fff; border: 1px solid #e5e9ec; border-radius: 12px; padding: 10px; }
.mock .frame { position: relative; height: 96px; border-radius: 8px; background: linear-gradient(135deg, #e6ebee, #f4f6f7); }
.tag { position: absolute; top: 8px; left: 8px; background: #df3c43; color: #fff; font-style: normal; font-size: 12px; font-weight: 700; border-radius: 5px; padding: 4px 8px; }
.mock .name { font-size: 13px; font-weight: 600; }
.now { color: #d82f37; font-size: 18px; }
.mock del { color: #7b8581; font-size: 12px; }
.save { color: #c0392b; font-size: 12px; font-weight: 600; }
.ticket { display: grid; gap: 8px; background: #fff4d6; border-radius: 12px; padding: 14px; }
.ticket .big { font-size: 24px; color: #007bb4; }
.code { border: 1px dashed #b98a14; border-radius: 8px; background: #fff; padding: 8px 10px; font-weight: 700; letter-spacing: 1px; overflow-wrap: anywhere; }
.ticket small { color: #6b5a2a; font-size: 12px; }
.example { margin: 0; font-size: 12px; color: #606d76; }
.state { font-size: 12px; font-weight: 700; border-radius: 999px; padding: 4px 12px; }
.state.on { background: #e2eef3; color: #007bb4; }
.state.soon { background: #e7effb; color: #2456a6; }
.state.off { background: #eceef0; color: #5a6770; }
.switch-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 4px; }
.switch-row span { display: grid; gap: 2px; font-size: 14px; }
.switch-row small { color: #606d76; font-size: 12px; }
.switch { width: 46px; height: 26px; padding: 0; border-radius: 999px; background: #c9cfd4; position: relative; gap: 0; flex-shrink: 0; }
.switch i { position: absolute; top: 3px; left: 3px; width: 20px; height: 20px; border-radius: 50%; background: #fff; transition: transform .15s; }
.switch[aria-checked="true"] { background: #0085c3; }
.switch[aria-checked="true"] i { transform: translateX(20px); }
@media (max-width: 820px) { .split, .two { grid-template-columns: 1fr; } }
</style>
