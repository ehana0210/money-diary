import { chromium } from 'playwright';
import path from 'path';
import fs from 'fs';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.join(__dirname, '..');
const appUrl = `file://${path.join(root, 'app/src/main/assets/index.html')}`;
const width = 731;
const height = 1300;

const sampleEntries = [
  { id: 1, type: 'income', date: '2026-05-19', amount: 10000, category: 'allowance', memo: '주간 용돈' },
  { id: 2, type: 'expense', date: '2026-05-18', amount: 5000, category: 'school', memo: '' },
  { id: 3, type: 'expense', date: '2026-05-20', amount: 3000, category: 'snack', memo: '과자' },
  { id: 4, type: 'income', date: '2026-05-10', amount: 20000, category: 'allowance', memo: '' },
  { id: 5, type: 'expense', date: '2026-05-12', amount: 4500, category: 'game', memo: '' },
  { id: 6, type: 'expense', date: '2026-05-15', amount: 2000, category: 'transport', memo: '버스' },
];

const shots = [
  { tab: 'record', file: 'onestore-screenshot-1-record.jpg' },
  { tab: 'stats', file: 'onestore-screenshot-2-stats.jpg' },
  { tab: 'calendar', file: 'onestore-screenshot-3-calendar.jpg' },
  { tab: 'category', file: 'onestore-screenshot-4-category.jpg' },
];

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width, height }, deviceScaleFactor: 1 });

await page.goto(appUrl, { waitUntil: 'networkidle' });
await page.evaluate((entries) => {
  localStorage.setItem('money-diary-entries', JSON.stringify(entries));
}, sampleEntries);
await page.reload({ waitUntil: 'networkidle' });

for (const shot of shots) {
  await page.evaluate((tab) => {
    document.querySelectorAll('.tab-btn').forEach((btn) => {
      btn.classList.toggle('active', btn.dataset.tab === tab);
    });
    document.querySelectorAll('.panel').forEach((panel) => {
      panel.classList.toggle('active', panel.id === `panel-${tab}`);
    });
    if (tab === 'stats' && typeof renderStats === 'function') renderStats();
    if (tab === 'calendar' && typeof renderCalendar === 'function') renderCalendar();
    if (tab === 'category' && typeof renderCategoryManager === 'function') renderCategoryManager();
    if (tab === 'record' && typeof render === 'function') render();
  }, shot.tab);

  await page.waitForTimeout(300);
  const outPath = path.join(root, shot.file);
  await page.screenshot({ path: outPath, type: 'jpeg', quality: 88 });
  const kb = Math.round(fs.statSync(outPath).size / 1024);
  console.log(`${shot.file}: ${width}x${height}, ${kb}KB`);
}

await browser.close();
