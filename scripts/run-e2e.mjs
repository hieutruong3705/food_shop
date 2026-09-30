import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {spawn,spawnSync} from 'node:child_process';
const root=fileURLToPath(new URL('..',import.meta.url));
export function startE2e(){
const jar=path.join(root,'target','techshop.jar');
if(!fs.existsSync(jar))throw new Error('Build frontend and run mvnw package before browser tests.');
const credentials=JSON.parse(fs.readFileSync(path.join(root,'data/e2e-credentials.json'),'utf8'));
// Resolve the actual JDK executable: the Windows javapath shim starts another
// process, which would otherwise survive when its launcher is terminated.
const info=spawnSync('java',['-XshowSettings:properties','-version'],{encoding:'utf8',windowsHide:true});
const home=process.env.JAVA_HOME||info.stderr?.match(/java\.home\s*=\s*([^\r\n]+)/)?.[1]?.trim();
const java=home?path.join(home,'bin',process.platform==='win32'?'java.exe':'java'):'java';
// CLI values outrank any developer DB_* / SPRING_* environment overrides.
const args=['-jar',jar,'--spring.profiles.active=dev,e2e','--spring.config.import=',
  '--spring.datasource.url=jdbc:h2:file:./data/techshop-e2e;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE',
  '--spring.datasource.username=sa','--spring.datasource.password=',
  '--spring.datasource.driver-class-name=org.h2.Driver','--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect',
  '--spring.jpa.hibernate.ddl-auto=create-drop','--spring.flyway.enabled=false',
  '--server.address=127.0.0.1','--server.port=18080','--shop.demo.enabled=true','--shop.chat.enabled=false','--shop.vnpay.enabled=false'];
const log=fs.openSync(path.join(root,'data/e2e-server.log'),'w');
const child=spawn(java,args,{cwd:root,stdio:['ignore',log,log],windowsHide:true,env:{...process.env,DEMO_ADMIN_PASSWORD:credentials.admin,DEMO_USER_PASSWORD:credentials.user}});
fs.closeSync(log);
return child;
}
