import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

function screenFiles(dir: string): string[] {
  return readdirSync(dir).flatMap((entry) => {
    const path = join(dir, entry);
    if (statSync(path).isDirectory()) return screenFiles(path);
    return /\.(screen|tsx?)$/.test(entry) && !entry.endsWith('.test.ts') ? [path] : [];
  });
}

describe('screen architecture guardrails', () => {
  it('keeps screens from calling raw fetch or parsing backend envelopes', () => {
    const files = screenFiles(join(process.cwd(), 'src', 'screens'));
    expect(files.length).toBeGreaterThan(0);

    for (const file of files) {
      const source = readFileSync(file, 'utf8');
      expect(source, file).not.toMatch(/\bfetch\s*\(/);
      expect(source, file).not.toMatch(/ApiResponse|response\.data|response\.error/);
    }
  });
});
