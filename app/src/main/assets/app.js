const STORAGE_KEY = 'money-diary-entries';
const CATEGORIES_KEY = 'money-diary-categories';

const DEFAULT_CATEGORIES = {
  expense: [
    { value: 'snack', label: '간식', icon: '🍪', custom: false },
    { value: 'school', label: '학용품', icon: '📚', custom: false },
    { value: 'game', label: '게임', icon: '🎮', custom: false },
    { value: 'transport', label: '교통', icon: '🚌', custom: false },
    { value: 'etc', label: '기타', icon: '📝', custom: false },
  ],
  income: [
    { value: 'allowance', label: '용돈', icon: '💰', custom: false },
    { value: 'gift', label: '선물', icon: '🎁', custom: false },
    { value: 'reward', label: '용돈 보너스', icon: '⭐', custom: false },
    { value: 'etc', label: '기타', icon: '📝', custom: false },
  ],
};

const ICON_OPTIONS = ['🍪', '📚', '🎮', '🚌', '💰', '🎁', '⭐', '📝', '🍔', '☕', '👕', '🎬', '💊', '🐾', '🏠', '💼', '🎵', '⚽'];

const form = document.getElementById('entry-form');
const dateMonthSelect = document.getElementById('date-month');
const dateDaySelect = document.getElementById('date-day');
const amountInput = document.getElementById('amount');
const categorySelect = document.getElementById('category');
const memoInput = document.getElementById('memo');
const filterSelect = document.getElementById('filter');
const entryList = document.getElementById('entry-list');
const emptyState = document.getElementById('empty-state');
const entryPagination = document.getElementById('entry-pagination');
const pagePrevBtn = document.getElementById('page-prev');
const pageNextBtn = document.getElementById('page-next');
const pageInfoEl = document.getElementById('page-info');
const balanceEl = document.getElementById('balance');
const totalIncomeEl = document.getElementById('total-income');
const totalExpenseEl = document.getElementById('total-expense');
const typeInputs = document.querySelectorAll('input[name="type"]');

const statsPeriodLabel = document.getElementById('stats-period-label');
const statsIncomeEl = document.getElementById('stats-income');
const statsExpenseEl = document.getElementById('stats-expense');
const statsBalanceEl = document.getElementById('stats-balance');
const statsCategoryList = document.getElementById('stats-category-list');
const statsEmpty = document.getElementById('stats-empty');
const periodBtns = document.querySelectorAll('.period-btn');

const calMonthLabel = document.getElementById('cal-month-label');
const calendarGrid = document.getElementById('calendar-grid');
const calPrevBtn = document.getElementById('cal-prev');
const calNextBtn = document.getElementById('cal-next');
const calDayTitle = document.getElementById('cal-day-title');
const calDayList = document.getElementById('cal-day-list');
const calDayEmpty = document.getElementById('cal-day-empty');

const categoryForm = document.getElementById('category-form');
const catIconSelect = document.getElementById('cat-icon');
const catLabelInput = document.getElementById('cat-label');
const categoryListEl = document.getElementById('category-list');
const catTypeInputs = document.querySelectorAll('input[name="cat-type"]');

const tabBtns = document.querySelectorAll('.tab-btn');
const panels = document.querySelectorAll('.panel');

const modalOverlay = document.getElementById('modal-overlay');
const modalMessage = document.getElementById('modal-message');
const modalActions = document.getElementById('modal-actions');

let statsPeriod = 'week';
let calendarYear = new Date().getFullYear();
let calendarMonth = new Date().getMonth();
let selectedCalDate = getTodayString();
let currentPage = 1;

const PAGE_SIZE = 5;

function closeModal() {
  modalOverlay.classList.add('hidden');
  modalActions.innerHTML = '';
}

function showAlert(message) {
  modalMessage.textContent = message;
  modalActions.innerHTML = '<button type="button" class="btn-primary modal-btn" data-action="ok">확인</button>';
  modalOverlay.classList.remove('hidden');
  modalActions.querySelector('[data-action="ok"]').addEventListener('click', closeModal, { once: true });
}

function showConfirm(message, onConfirm) {
  modalMessage.textContent = message;
  modalActions.innerHTML = `
    <button type="button" class="modal-btn modal-cancel" data-action="cancel">아니요</button>
    <button type="button" class="btn-primary modal-btn" data-action="confirm">네</button>
  `;
  modalOverlay.classList.remove('hidden');

  modalActions.querySelector('[data-action="cancel"]').addEventListener('click', closeModal, { once: true });
  modalActions.querySelector('[data-action="confirm"]').addEventListener('click', () => {
    closeModal();
    onConfirm();
  }, { once: true });
}

function formatMoney(amount) {
  return `${amount.toLocaleString('ko-KR')}원`;
}

function formatDate(dateStr) {
  const date = new Date(dateStr + 'T00:00:00');
  return `${date.getMonth() + 1}월 ${date.getDate()}일`;
}

function getTodayString() {
  return toDateString(new Date());
}

function toDateString(date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function initDatePicker() {
  dateMonthSelect.innerHTML = Array.from({ length: 12 }, (_, i) => {
    const month = i + 1;
    return `<option value="${month}">${month}월</option>`;
  }).join('');

  const today = new Date();
  dateMonthSelect.value = String(today.getMonth() + 1);
  updateDayOptions(today.getDate());
}

function updateDayOptions(preferredDay) {
  const month = parseInt(dateMonthSelect.value, 10);
  const year = new Date().getFullYear();
  const daysInMonth = new Date(year, month, 0).getDate();
  const currentDay = preferredDay || parseInt(dateDaySelect.value, 10) || 1;

  dateDaySelect.innerHTML = Array.from({ length: daysInMonth }, (_, i) => {
    const day = i + 1;
    return `<option value="${day}">${day}일</option>`;
  }).join('');

  dateDaySelect.value = String(Math.min(currentDay, daysInMonth));
}

function getFormDateString() {
  const year = new Date().getFullYear();
  const month = String(dateMonthSelect.value).padStart(2, '0');
  const day = String(dateDaySelect.value).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function setDatePickerFromString(dateStr) {
  const date = parseDate(dateStr);
  dateMonthSelect.value = String(date.getMonth() + 1);
  updateDayOptions(date.getDate());
}

function parseDate(dateStr) {
  return new Date(dateStr + 'T00:00:00');
}

function loadEntries() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

function saveEntries(entries) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(entries));
}

function loadCategories() {
  try {
    const raw = localStorage.getItem(CATEGORIES_KEY);
    if (raw) return JSON.parse(raw);
  } catch {
    /* ignore */
  }
  return JSON.parse(JSON.stringify(DEFAULT_CATEGORIES));
}

function saveCategories(categories) {
  localStorage.setItem(CATEGORIES_KEY, JSON.stringify(categories));
}

function getCategoryInfo(type, value) {
  const categories = loadCategories();
  const found = categories[type].find((c) => c.value === value);
  if (found) return found;
  return { value, label: '삭제된 카테고리', icon: '❓', custom: false };
}

function getSelectedType() {
  return document.querySelector('input[name="type"]:checked').value;
}

function getSelectedCatType() {
  return document.querySelector('input[name="cat-type"]:checked').value;
}

function updateCategoryOptions(type) {
  const categories = loadCategories();
  categorySelect.innerHTML = categories[type]
    .map((c) => `<option value="${c.value}">${c.icon} ${c.label}</option>`)
    .join('');
}

function getWeekRange(baseDate = new Date()) {
  const date = new Date(baseDate);
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  const monday = new Date(date);
  monday.setDate(date.getDate() + diff);
  const sunday = new Date(monday);
  sunday.setDate(monday.getDate() + 6);
  return { start: monday, end: sunday };
}

function getMonthRange(baseDate = new Date()) {
  const date = new Date(baseDate);
  const start = new Date(date.getFullYear(), date.getMonth(), 1);
  const end = new Date(date.getFullYear(), date.getMonth() + 1, 0);
  return { start, end };
}

function filterByRange(entries, start, end) {
  const startStr = toDateString(start);
  const endStr = toDateString(end);
  return entries.filter((e) => e.date >= startStr && e.date <= endStr);
}

function sumByType(entries, type) {
  return entries.filter((e) => e.type === type).reduce((sum, e) => sum + e.amount, 0);
}

function renderEntryItem(entry, showDate = true) {
  const cat = getCategoryInfo(entry.type, entry.category);
  const sign = entry.type === 'income' ? '+' : '-';
  const memo = entry.memo ? ` · ${entry.memo}` : '';
  const meta = showDate ? formatDate(entry.date) + memo : (entry.memo || cat.label);

  return `
    <li class="entry-item" data-id="${entry.id}">
      <div class="entry-icon ${entry.type}">${cat.icon}</div>
      <div class="entry-info">
        <div class="entry-title">${cat.label}</div>
        <div class="entry-meta">${meta}</div>
      </div>
      <span class="entry-amount ${entry.type}">${sign}${formatMoney(entry.amount)}</span>
      <button type="button" class="btn-delete" aria-label="삭제">×</button>
    </li>
  `;
}

function renderSummary(entries) {
  const income = sumByType(entries, 'income');
  const expense = sumByType(entries, 'expense');

  balanceEl.textContent = formatMoney(income - expense);
  totalIncomeEl.textContent = formatMoney(income);
  totalExpenseEl.textContent = formatMoney(expense);
}

function renderList(entries) {
  const filter = filterSelect.value;
  const filtered = filter === 'all' ? entries : entries.filter((e) => e.type === filter);
  const sorted = [...filtered].sort((a, b) => a.date.localeCompare(b.date) || a.id - b.id);

  const totalPages = Math.max(1, Math.ceil(sorted.length / PAGE_SIZE));
  if (currentPage > totalPages) currentPage = totalPages;
  if (currentPage < 1) currentPage = 1;

  const start = (currentPage - 1) * PAGE_SIZE;
  const pageItems = sorted.slice(start, start + PAGE_SIZE);

  entryList.innerHTML = pageItems.map((entry) => renderEntryItem(entry)).join('');
  emptyState.classList.toggle('hidden', sorted.length > 0);
  entryPagination.classList.toggle('hidden', sorted.length <= PAGE_SIZE);

  pageInfoEl.textContent = `${currentPage} / ${totalPages}`;
  pagePrevBtn.disabled = currentPage <= 1;
  pageNextBtn.disabled = currentPage >= totalPages;
}

function renderStats() {
  const entries = loadEntries();
  const range = statsPeriod === 'week' ? getWeekRange() : getMonthRange();
  const filtered = filterByRange(entries, range.start, range.end);

  const income = sumByType(filtered, 'income');
  const expense = sumByType(filtered, 'expense');

  const startLabel = formatDate(toDateString(range.start));
  const endLabel = formatDate(toDateString(range.end));
  statsPeriodLabel.textContent =
    statsPeriod === 'week'
      ? `이번 주 (${startLabel} ~ ${endLabel})`
      : `${startLabel} ~ ${endLabel}`;

  statsIncomeEl.textContent = formatMoney(income);
  statsExpenseEl.textContent = formatMoney(expense);
  statsBalanceEl.textContent = formatMoney(income - expense);

  const expenses = filtered.filter((e) => e.type === 'expense');
  const byCategory = {};

  expenses.forEach((e) => {
    byCategory[e.category] = (byCategory[e.category] || 0) + e.amount;
  });

  const sortedCats = Object.entries(byCategory).sort((a, b) => b[1] - a[1]);
  const maxAmount = sortedCats[0]?.[1] || 0;

  statsCategoryList.innerHTML = sortedCats
    .map(([catValue, amount]) => {
      const cat = getCategoryInfo('expense', catValue);
      const pct = maxAmount ? Math.round((amount / maxAmount) * 100) : 0;
      const share = expense ? Math.round((amount / expense) * 100) : 0;

      return `
        <li class="stats-cat-item">
          <div class="stats-cat-header">
            <span>${cat.icon} ${cat.label}</span>
            <span>${formatMoney(amount)} (${share}%)</span>
          </div>
          <div class="stats-bar-track">
            <div class="stats-bar-fill" style="width:${pct}%"></div>
          </div>
        </li>
      `;
    })
    .join('');

  statsEmpty.classList.toggle('hidden', sortedCats.length > 0);
}

function getDaySummary(entries, dateStr) {
  const dayEntries = entries.filter((e) => e.date === dateStr);
  const income = sumByType(dayEntries, 'income');
  const expense = sumByType(dayEntries, 'expense');
  return { count: dayEntries.length, income, expense, net: income - expense };
}

function renderCalendar() {
  const entries = loadEntries();
  calMonthLabel.textContent = `${calendarMonth + 1}월`;

  const firstDay = new Date(calendarYear, calendarMonth, 1);
  const lastDay = new Date(calendarYear, calendarMonth + 1, 0);
  const startOffset = firstDay.getDay() === 0 ? 6 : firstDay.getDay() - 1;
  const todayStr = getTodayString();

  let cells = '';

  for (let i = 0; i < startOffset; i++) {
    cells += '<div class="cal-cell empty"></div>';
  }

  for (let day = 1; day <= lastDay.getDate(); day++) {
    const dateStr = toDateString(new Date(calendarYear, calendarMonth, day));
    const summary = getDaySummary(entries, dateStr);
    const isToday = dateStr === todayStr;
    const isSelected = dateStr === selectedCalDate;

    let dotClass = '';
    let amountHint = '';
    if (summary.count > 0) {
      if (summary.net > 0) dotClass = 'dot-income';
      else if (summary.net < 0) dotClass = 'dot-expense';
      else dotClass = 'dot-neutral';
      amountHint = summary.net >= 0 ? `+${summary.net.toLocaleString()}` : summary.net.toLocaleString();
    }

    cells += `
      <button type="button" class="cal-cell${isToday ? ' today' : ''}${isSelected ? ' selected' : ''}${summary.count ? ' has-entry' : ''}"
        data-date="${dateStr}">
        <span class="cal-day">${day}</span>
        ${summary.count ? `<span class="cal-dot ${dotClass}"></span>` : ''}
        ${summary.count ? `<span class="cal-amount">${amountHint}</span>` : ''}
      </button>
    `;
  }

  calendarGrid.innerHTML = cells;
  renderCalendarDayList(entries);
}

function renderCalendarDayList(entries) {
  const dayEntries = entries
    .filter((e) => e.date === selectedCalDate)
    .sort((a, b) => b.id - a.id);

  calDayTitle.textContent = formatDate(selectedCalDate);
  calDayList.innerHTML = dayEntries.map((e) => renderEntryItem(e, false)).join('');
  calDayEmpty.classList.toggle('hidden', dayEntries.length > 0);
}

function renderCategoryManager() {
  const type = getSelectedCatType();
  const categories = loadCategories()[type];

  categoryListEl.innerHTML = categories
    .map(
      (cat) => `
      <li class="category-item">
        <span class="category-item-label">${cat.icon} ${cat.label}</span>
        ${
          cat.custom
            ? `<button type="button" class="btn-text-delete" data-value="${cat.value}">삭제</button>`
            : '<span class="category-badge">기본</span>'
        }
      </li>
    `
    )
    .join('');
}

function render() {
  const entries = loadEntries();
  renderSummary(entries);
  renderList(entries);
  renderStats();
  renderCalendar();
  renderCategoryManager();
}

function addEntry(data) {
  const entries = loadEntries();
  entries.push({
    id: Date.now(),
    type: data.type,
    date: data.date,
    amount: data.amount,
    category: data.category,
    memo: data.memo.trim(),
  });
  saveEntries(entries);
  currentPage = Math.max(1, Math.ceil(entries.filter((e) => {
    const filter = filterSelect.value;
    return filter === 'all' || e.type === filter;
  }).length / PAGE_SIZE));
  render();
}

function deleteEntry(id) {
  saveEntries(loadEntries().filter((e) => e.id !== id));
  render();
}

function addCategory(type, label, icon) {
  const categories = loadCategories();
  const trimmed = label.trim();
  if (!trimmed) return;

  const duplicate = categories[type].some((c) => c.label === trimmed);
  if (duplicate) {
    showAlert('같은 이름의 카테고리가 이미 있어요.');
    return;
  }

  categories[type].push({
    value: `custom-${Date.now()}`,
    label: trimmed,
    icon,
    custom: true,
  });
  saveCategories(categories);
  updateCategoryOptions(getSelectedType());
  renderCategoryManager();
}

function deleteCategory(type, value) {
  const entries = loadEntries();
  const inUse = entries.some((e) => e.type === type && e.category === value);
  if (inUse) {
    showAlert('이 카테고리를 사용 중이라 지울 수 없어요.');
    return;
  }

  const categories = loadCategories();
  categories[type] = categories[type].filter((c) => c.value !== value);
  saveCategories(categories);
  updateCategoryOptions(getSelectedType());
  renderCategoryManager();
}

function switchTab(tab) {
  tabBtns.forEach((btn) => btn.classList.toggle('active', btn.dataset.tab === tab));
  panels.forEach((panel) => panel.classList.toggle('active', panel.id === `panel-${tab}`));
}

typeInputs.forEach((input) => {
  input.addEventListener('change', () => updateCategoryOptions(getSelectedType()));
});

catTypeInputs.forEach((input) => {
  input.addEventListener('change', renderCategoryManager);
});

form.addEventListener('submit', (e) => {
  e.preventDefault();
  const type = getSelectedType();
  const amount = parseInt(amountInput.value, 10);
  if (!amount || amount <= 0) return;

  addEntry({
    type,
    date: getFormDateString(),
    amount,
    category: categorySelect.value,
    memo: memoInput.value,
  });

  amountInput.value = '';
  memoInput.value = '';
});

entryList.addEventListener('click', handleDeleteClick);
calDayList.addEventListener('click', handleDeleteClick);

function handleDeleteClick(e) {
  const btn = e.target.closest('.btn-delete');
  if (!btn) return;
  const id = Number(btn.closest('.entry-item').dataset.id);
  showConfirm('이 내역을 지울까요?', () => deleteEntry(id));
}

filterSelect.addEventListener('change', () => {
  currentPage = 1;
  renderList(loadEntries());
});

pagePrevBtn.addEventListener('click', () => {
  if (currentPage > 1) {
    currentPage -= 1;
    renderList(loadEntries());
  }
});

pageNextBtn.addEventListener('click', () => {
  currentPage += 1;
  renderList(loadEntries());
});

periodBtns.forEach((btn) => {
  btn.addEventListener('click', () => {
    statsPeriod = btn.dataset.period;
    periodBtns.forEach((b) => b.classList.toggle('active', b === btn));
    renderStats();
  });
});

calPrevBtn.addEventListener('click', () => {
  calendarMonth -= 1;
  if (calendarMonth < 0) {
    calendarMonth = 11;
    calendarYear -= 1;
  }
  renderCalendar();
});

calNextBtn.addEventListener('click', () => {
  calendarMonth += 1;
  if (calendarMonth > 11) {
    calendarMonth = 0;
    calendarYear += 1;
  }
  renderCalendar();
});

calendarGrid.addEventListener('click', (e) => {
  const cell = e.target.closest('.cal-cell[data-date]');
  if (!cell) return;
  selectedCalDate = cell.dataset.date;
  setDatePickerFromString(selectedCalDate);
  renderCalendar();
});

dateMonthSelect.addEventListener('change', () => updateDayOptions());

categoryForm.addEventListener('submit', (e) => {
  e.preventDefault();
  addCategory(getSelectedCatType(), catLabelInput.value, catIconSelect.value);
  catLabelInput.value = '';
});

categoryListEl.addEventListener('click', (e) => {
  const btn = e.target.closest('.btn-text-delete');
  if (!btn) return;
  showConfirm('이 카테고리를 지울까요?', () => {
    deleteCategory(getSelectedCatType(), btn.dataset.value);
  });
});

tabBtns.forEach((btn) => {
  btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

catIconSelect.innerHTML = ICON_OPTIONS.map((icon) => `<option value="${icon}">${icon}</option>`).join('');

initDatePicker();
updateCategoryOptions('expense');
render();
