<script setup>
import {ref,nextTick} from 'vue';
import {api} from '../services/api';
const open=ref(false),message=ref(''),busy=ref(false),error=ref(''),log=ref(null),input=ref(null),provider=ref('');
const messages=ref([{role:'bot',text:'Xin chào 👋 Tôi là trợ lý TechShop AI. Tôi có thể giúp bạn tìm sản phẩm, so sánh thiết bị và tư vấn theo nhu cầu.'}]);
const suggestions=['Tư vấn laptop','Tư vấn điện thoại','So sánh sản phẩm','Sản phẩm dưới 20 triệu'];
async function scroll(){await nextTick();log.value?.scrollTo({top:log.value.scrollHeight,behavior:'smooth'});}
async function show(){open.value=true;await nextTick();input.value?.focus();scroll();api('/api/chat/status').then(result=>provider.value=result.provider).catch(()=>{});}
// The server rejects a turn over 1,000 characters or a history over 4,000 in total, so send the newest turns that fit.
const MAX_TURN=1000,MAX_HISTORY=4000,MAX_TURNS=8;
function recentHistory(){
  const turns=messages.value.slice(1).filter(item=>!item.failed).slice(-MAX_TURNS).map(item=>({role:item.role==='bot'?'assistant':'user',text:item.text.slice(0,MAX_TURN)}));
  let total=0,keep=0;
  for(let i=turns.length-1;i>=0;i--){total+=turns[i].text.length;if(total>MAX_HISTORY)break;keep++;}
  const kept=turns.slice(turns.length-keep);
  while(kept.length&&kept[0].role==='assistant')kept.shift();
  return kept;
}
async function send(text=message.value){
  if(busy.value||!text.trim())return;
  const value=text.trim(),history=recentHistory();
  message.value='';error.value='';const turn={role:'user',text:value};messages.value.push(turn);busy.value=true;scroll();
  try{const result=await api('/api/chat',{method:'POST',body:JSON.stringify({message:value,history})});provider.value=result.provider||provider.value;messages.value.push({role:'bot',text:result.reply});}
  catch(e){turn.failed=true;error.value=e.message;message.value=value;}
  finally{busy.value=false;scroll();}
}
</script>
<template>
  <button v-if="!open" class="chat-launcher" aria-label="Mở TechShop AI" @click="show"><span>✦</span> TechShop AI</button>
  <section v-else class="tech-chat" role="dialog" aria-modal="false" aria-label="TechShop AI" @keydown.esc="open=false">
    <header><div><strong>✦ TechShop AI</strong><small><i></i> {{ error?'Tạm gián đoạn':'Online' }}{{ provider==='gemini'?' · Gemini':provider==='mock'?' · Phản hồi mẫu':'' }}</small></div><button aria-label="Thu nhỏ trò chuyện" @click="open=false">−</button><button aria-label="Đóng trò chuyện" @click="open=false">×</button></header>
    <div ref="log" class="chat-log" role="log" aria-live="polite" aria-relevant="additions text"><div v-for="(item,i) in messages" :key="i" class="chat-message" :class="item.role">{{ item.text }}</div><div v-if="busy" class="chat-typing" role="status" aria-label="Đang trả lời"><i></i><i></i><i></i></div></div>
    <div class="chat-suggestions"><button v-for="suggestion in suggestions" :key="suggestion" :disabled="busy" @click="send(suggestion)">{{ suggestion }}</button></div>
    <p v-if="error" class="chat-error" role="alert">{{ error }} Bạn có thể gửi lại.</p>
    <form @submit.prevent="send()"><input ref="input" v-model="message" aria-label="Tin nhắn cho TechShop AI" placeholder="Nhập câu hỏi của bạn…" maxlength="1000" :disabled="busy" /><button :disabled="busy||!message.trim()" aria-label="Gửi tin nhắn">Gửi</button></form>
  </section>
</template>
