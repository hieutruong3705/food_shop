import { defineConfig } from "@playwright/test";
import fs from 'node:fs';
import {randomBytes} from 'node:crypto';
import {fileURLToPath} from 'node:url';
const baseURL='http://127.0.0.1:18080';
if(process.env.E2E_URL && process.env.E2E_URL!==baseURL)throw new Error('E2E_URL must be '+baseURL+'; browser tests cannot run against the development MySQL database.');
const root=fileURLToPath(new URL('..',import.meta.url));
fs.mkdirSync(new URL('../data/',import.meta.url),{recursive:true});
const credentialFile=new URL('../data/e2e-credentials.json',import.meta.url);
if(!fs.existsSync(credentialFile))fs.writeFileSync(credentialFile,JSON.stringify({admin:randomBytes(24).toString('base64url'),user:randomBytes(24).toString('base64url')}));
export default defineConfig({
  testDir: "./tests",
  fullyParallel: false,
  workers: 1,
  timeout: 45000,
  use: {
    baseURL,
    channel: "chrome",
    headless: true,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  globalSetup:'./tests/e2e-setup.js',
  reporter: [["list"], ["html", { open: "never" }]],
});
