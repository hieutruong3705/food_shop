<script setup>
import {ref,watch,onUnmounted} from 'vue';
import {api} from '../services/api';
import {imageUrl,imageError,money} from '../lib/shop';
const props=defineProps({modelValue:{type:Array,default:()=>[]},resource:{type:String,required:true},label:{type:String,required:true},categoryId:[String,Number],categoryIds:{type:Array,default:()=>[]}});
const emit=defineEmits(['update:modelValue']);
const query=ref(''),open=ref(false),items=ref([]),labels=ref({}),page=ref(0),last=ref(true),busy=ref(false),error=ref('');
let sequence=0,timer,alive=true;
const url=(index,search,ids=[])=>'/admin/options/'+props.resource+'?'+new URLSearchParams({page:index,search,...(props.categoryId?{categoryId:props.categoryId}:{}),...(props.categoryIds.length?{categoryIds:props.categoryIds.join(',')}:{}),...(ids.length?{ids:ids.join(',')}:{} )});
async function load(append=false){const token=++sequence;busy.value=true;error.value='';if(!append)page.value=0;try{const data=await api(url(page.value,query.value));if(!alive||token!==sequence)return;items.value=append?[...items.value,...data.content]:data.content;last.value=data.last;for(const x of data.content)labels.value[x.id]=x.name;}catch(e){if(token===sequence)error.value=e.message;}finally{if(token===sequence)busy.value=false;}}
async function hydrate(){const ids=props.modelValue.filter(id=>!labels.value[id]);if(!ids.length)return;try{let index=0,data;do{data=await api('/admin/options/'+props.resource+'?'+new URLSearchParams({page:index++,ids:ids.join(',')}));if(!alive)return;for(const x of data.content)labels.value[x.id]=x.name;}while(!data.last);}catch(e){error.value=e.message;}}
function toggle(id){emit('update:modelValue',props.modelValue.includes(id)?props.modelValue.filter(x=>x!==id):[...props.modelValue,id]);}
function more(){page.value++;load(true);}
watch(query,()=>{clearTimeout(timer);timer=setTimeout(()=>load(),250);});
watch(()=>[props.resource,props.categoryId,props.categoryIds.join(',')],()=>{labels.value={};items.value=[];query.value='';hydrate();if(open.value)load();});
watch(()=>props.modelValue,hydrate,{immediate:true});
onUnmounted(()=>{alive=false;sequence++;clearTimeout(timer);});
</script>
<template>
  <section class="search-multi" :aria-label="label" @keydown.esc="open=false">
    <strong>{{ label }}</strong>
    <div class="selection-chips"><button v-for="id in modelValue" :key="id" type="button" :aria-label="`Bỏ ${labels[id]||'#'+id}`" @click="toggle(id)">{{ labels[id]||'#'+id }} <span>×</span></button><small v-if="!modelValue.length">Chưa chọn mục nào</small></div>
    <input v-model="query" type="search" :aria-label="`Tìm ${label.toLowerCase()}`" placeholder="Nhập tên để tìm…" @focus="open=true;load()" />
    <div v-if="open" class="selection-dropdown"><div class="selection-options"><label v-for="item in items" :key="item.id"><input type="checkbox" :checked="modelValue.includes(item.id)" @change="toggle(item.id)" /><img v-if="resource==='products'" class="selection-image" :src="imageUrl(item.image)" alt="" @error="imageError" /><span>{{ item.name }} <small>#{{ item.id }}</small><small v-if="resource==='products'" class="selection-detail">{{ item.sku }} · {{ money(item.price) }} · {{ item.brand }} · {{ item.category }} · Tồn: {{ item.quantity }}</small></span></label></div><p v-if="!busy&&!items.length" class="muted">Không có kết quả.</p><p v-if="busy" role="status">Đang tải…</p><button v-if="!last" type="button" :disabled="busy" @click="more">Xem thêm</button><button type="button" class="text-button" @click="open=false">Đóng danh sách</button></div>
    <p v-if="error" role="alert">{{ error }} <button type="button" @click="load()">Thử lại</button></p>
  </section>
</template>

<style scoped>
.selection-image{width:48px;height:48px;object-fit:contain;flex-shrink:0}.selection-detail{display:block;margin-top:4px;color:#60766e}.selection-options label{align-items:center;gap:10px}
</style>
