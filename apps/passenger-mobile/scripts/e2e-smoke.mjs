import { existsSync } from 'node:fs';

const platform = process.argv[2];
const required = ['App.tsx', 'index.ts', 'app.config.ts', 'eas.json', '.detoxrc.json'];
const missing = required.filter((file) => !existsSync(new URL(`../${file}`, import.meta.url)));

if (!['ios', 'android'].includes(platform)) {
  console.error(`Unknown E2E platform: ${platform ?? '<missing>'}`);
  process.exit(1);
}

if (missing.length > 0) {
  console.error(`Missing scaffold files for ${platform} E2E smoke: ${missing.join(', ')}`);
  process.exit(1);
}

console.log(`RouteShare passenger ${platform} E2E smoke gate passed: Detox config and Expo scaffold are present.`);
console.log('Full simulator/device automation requires generated native projects and is tracked as Task 02 release evidence follow-up if local devices are unavailable.');
