import {startE2e} from '../../scripts/run-e2e.mjs';
export default async function setup(){
  const url='http://127.0.0.1:18080/api/v1/categories';
  const probe=async()=>{try{return (await fetch(url,{signal:AbortSignal.timeout(1000)})).ok;}catch{return false;}};
  if(await probe())throw new Error('Port 18080 already serves an application; refusing to reuse its database.');
  const child=startE2e();let startupError;
  child.on('error',error=>{startupError=error;});
  async function stop(){
    if(child.exitCode!==null||!child.pid)return;
    const exited=new Promise(resolve=>child.once('exit',resolve));
    child.kill();
    const timer=setTimeout(()=>child.kill('SIGKILL'),5000);
    await exited;clearTimeout(timer);
  }
  try{
    const deadline=Date.now()+90000;
    while(Date.now()<deadline){
      if(startupError)throw startupError;
      if(child.exitCode!==null)throw new Error('E2E backend failed; see data/e2e-server.log.');
      if(await probe())return stop;
      await new Promise(resolve=>setTimeout(resolve,250));
    }
    throw new Error('E2E backend startup timed out; see data/e2e-server.log.');
  }catch(error){await stop();throw error;}
}
