// Run from the project root. Default is a read-only preview; --apply backs up and hides exact test fixtures.
import fs from 'node:fs';
const credentials=JSON.parse(fs.readFileSync('data/dev-credentials.json','utf8').replace(/^\uFEFF/,''));
const origin=process.env.SHOP_URL || 'http://127.0.0.1:8080',cookies=new Map();let csrf;
async function api(path,method='GET',body){
  const response=await fetch(origin+'/api/v1'+path,{method,headers:{'Content-Type':'application/json','Cookie':[...cookies].map(([k,v])=>k+'='+v).join('; '),...(csrf?{[csrf.headerName]:csrf.token}:{})},...(body?{body:JSON.stringify(body)}:{})});
  for(const cookie of response.headers.getSetCookie()){const pair=cookie.split(';')[0],at=pair.indexOf('=');cookies.set(pair.slice(0,at),pair.slice(at+1));}
  if(!response.ok)throw new Error(method+' '+path+' returned '+response.status());return response.status===204?null:response.json();
}
csrf=await api('/auth/csrf');await api('/auth/login','POST',{email:'admin@techshop.test',password:credentials.admin});
try{
  const candidates=[];let page=0,data;
  do{data=await api('/admin/products?size=100&page='+page++);for(const p of data.content){
    if(p.name===p.sku && /^(facet-\d{13}-[01]|selling-\d{13}|option-fixture-\d{13}|e2e-variant-\d{13})$/.test(p.name) && (p.active||p.technologyCatalog))candidates.push(p);
  }}while(!data.last);
  console.log(JSON.stringify(candidates.map(p=>({id:p.id,name:p.name,active:p.active})),null,2));
  if(process.argv.includes('--apply')&&candidates.length){
    fs.mkdirSync('data/backups',{recursive:true});const backup='data/backups/test-fixtures-'+Date.now()+'.json';
    fs.writeFileSync(backup,JSON.stringify(candidates,null,2));
    for(const p of candidates)await api('/admin/products/'+p.id,'PUT',{...p,active:false,technologyCatalog:false});
    console.log('Hidden '+candidates.length+' exact fixtures. Backup: '+backup);
  }else console.log('Preview only, '+candidates.length+' matching fixtures.');
}finally{await api('/auth/logout','POST');}
