import { existsSync, readFileSync } from 'node:fs';

const platform = process.argv[2];
if (!['ios', 'android'].includes(platform)) {
  console.error(`Unknown preview platform: ${platform ?? '<missing>'}`);
  process.exit(1);
}

const easUrl = new URL('../eas.json', import.meta.url);
const configUrl = new URL('../app.config.ts', import.meta.url);
if (!existsSync(easUrl) || !existsSync(configUrl)) {
  console.error('Preview build config is missing eas.json or app.config.ts');
  process.exit(1);
}

const eas = JSON.parse(readFileSync(easUrl, 'utf8'));
if (!eas.build?.preview) {
  console.error('Missing EAS preview profile');
  process.exit(1);
}

console.log(`RouteShare passenger ${platform} preview build config gate passed.`);
console.log('Remote EAS build submission is intentionally not triggered by this local verification command.');
