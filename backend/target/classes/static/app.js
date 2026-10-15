const state = {
  query: 'What was my total spend by category for April 2026?',
  start: '2026-04-01',
  end: '2026-04-30',
};

const elements = {
  startDate: document.getElementById('startDate'),
  endDate: document.getElementById('endDate'),
  refreshBtn: document.getElementById('refreshBtn'),
  totalSpend: document.getElementById('totalSpend'),
  topCategory: document.getElementById('topCategory'),
  transactionCount: document.getElementById('transactionCount'),
  categoryGrid: document.getElementById('categoryGrid'),
  dateRangeLabel: document.getElementById('dateRangeLabel'),
  agentInput: document.getElementById('agentInput'),
  askBtn: document.getElementById('askBtn'),
  responseBox: document.getElementById('agentResponse'),
  csvFileInput: document.getElementById('csvFileInput'),
  uploadBtn: document.getElementById('uploadBtn'),
  uploadStatus: document.getElementById('uploadStatus'),
};

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(value ?? 0);
}

function formatDateLabel(value) {
  if (!value) return 'Selected period';
  const date = new Date(`${value}T00:00:00`);
  return new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(date);
}

function formatDateRangeLabel(start, end) {
  if (!start || !end) return 'Selected period';

  const startDate = new Date(`${start}T00:00:00`);
  const endDate = new Date(`${end}T00:00:00`);
  const sameMonth = startDate.getMonth() === endDate.getMonth() && startDate.getFullYear() === endDate.getFullYear();

  if (sameMonth) {
    return new Intl.DateTimeFormat('en-US', { month: 'long', year: 'numeric' }).format(startDate);
  }

  return `${new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(startDate)} – ${new Intl.DateTimeFormat('en-US', { month: 'short', day: 'numeric', year: 'numeric' }).format(endDate)}`;
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function renderAgentResponse(text) {
  const raw = String(text ?? '').trim();
  if (!raw) {
    return '<p class="empty-state">No response returned.</p>';
  }

  const lines = raw
    .replace(/\r/g, '')
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  const formatted = [];
  let currentList = [];

  const flushList = () => {
    if (!currentList.length) return;
    formatted.push(`<ul>${currentList.map((item) => `<li>${item}</li>`).join('')}</ul>`);
    currentList = [];
  };

  lines.forEach((line) => {
    const sectionMatch = line.match(/^\*\*(Summary|Breakdown|Insight)\*\*:?\s*(.*)$/i);
    if (sectionMatch) {
      flushList();
      const label = escapeHtml(sectionMatch[1]);
      const details = escapeHtml(sectionMatch[2]).trim();
      formatted.push(`<h4>${label}</h4>`);
      if (details) {
        formatted.push(`<p>${details}</p>`);
      }
      return;
    }

    if (/^[-*] /.test(line)) {
      currentList.push(escapeHtml(line.replace(/^[-*]\s*/, '')));
      return;
    }

    flushList();
    formatted.push(`<p>${escapeHtml(line)}</p>`);
  });

  flushList();
  return `<h3>Agent insight</h3>${formatted.join('')}`;
}

function setResponse(text) {
  elements.responseBox.innerHTML = renderAgentResponse(text);
}

function setEmptyResponse() {
  elements.responseBox.innerHTML = '<p class="empty-state">The agent response will appear here.</p>';
}

async function fetchSpendByCategory() {
  const [spendResponse, countResponse] = await Promise.all([
    fetch(`/api/analytics/spend-by-category?start=${state.start}&end=${state.end}`),
    fetch(`/api/analytics/transaction-count?start=${state.start}&end=${state.end}`),
  ]);

  if (!spendResponse.ok) {
    throw new Error('Unable to load category spend data.');
  }

  if (!countResponse.ok) {
    throw new Error('Unable to load transaction counts.');
  }

  const items = await spendResponse.json();
  const transactionCount = await countResponse.json();
  const totalSpend = items.reduce((sum, item) => sum + Number(item.totalSpend ?? 0), 0);
  const topCategory = items[0] ? items[0].categoryName : '—';

  elements.totalSpend.textContent = formatCurrency(totalSpend);
  elements.topCategory.textContent = topCategory;
  elements.transactionCount.textContent = Number(transactionCount ?? 0).toLocaleString();
  elements.dateRangeLabel.textContent = formatDateRangeLabel(state.start, state.end);

  const mostSpend = items[0] ? Number(items[0].totalSpend ?? 0) : 0;
  elements.categoryGrid.innerHTML = items
    .map((item) => {
      const share = totalSpend > 0 ? ((Number(item.totalSpend ?? 0) / totalSpend) * 100) : 0;
      return `
        <div class="category-card">
          <h3>${item.categoryName}</h3>
          <div class="amount">${formatCurrency(item.totalSpend)}</div>
          <div class="share">${share.toFixed(1)}% of period total</div>
        </div>
      `;
    })
    .join('');

  if (!items.length) {
    elements.categoryGrid.innerHTML = '<div class="category-card"><h3>No data</h3><div class="amount">$0.00</div><div class="share">No transactions available for this date range.</div></div>';
  }
}

function parseDateValue(value) {
  if (!value) return null;

  const trimmed = value.trim();
  const isoMatch = trimmed.match(/\d{4}-\d{2}-\d{2}/);
  if (isoMatch) {
    return new Date(`${isoMatch[0]}T00:00:00`);
  }

  const slashMatch = trimmed.match(/\d{1,2}\/\d{1,2}\/\d{4}/);
  if (slashMatch) {
    const [month, day, year] = slashMatch[0].split('/');
    return new Date(`${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}T00:00:00`);
  }

  const parsed = new Date(trimmed);
  return Number.isNaN(parsed.getTime()) ? null : parsed;
}

async function setDateRangeFromUploadedFile(file) {
  const text = await file.text();
  const rows = text.split(/\r?\n/).filter((line) => line.trim());
  const dateValues = [];

  for (const row of rows) {
    const matches = [...row.matchAll(/\d{4}-\d{2}-\d{2}|\d{1,2}\/\d{1,2}\/\d{4}/g)];
    if (!matches.length) continue;

    const parsed = parseDateValue(matches[0][0]);
    if (parsed) {
      dateValues.push(parsed);
    }
  }

  if (!dateValues.length) {
    return;
  }

  const minDate = new Date(Math.min(...dateValues.map((date) => date.getTime())));
  const maxDate = new Date(Math.max(...dateValues.map((date) => date.getTime())));

  const toInputValue = (date) => {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  };

  state.start = toInputValue(minDate);
  state.end = toInputValue(maxDate);
  elements.startDate.value = state.start;
  elements.endDate.value = state.end;
}

async function uploadCsvFile() {
  const file = elements.csvFileInput.files[0];
  if (!file) {
    elements.uploadStatus.textContent = 'Select a CSV file before uploading.';
    return;
  }

  const formData = new FormData();
  formData.append('file', file);

  elements.uploadStatus.textContent = `Uploading ${file.name}...`;

  try {
    const response = await fetch('/api/upload', {
      method: 'POST',
      body: formData,
    });

    if (!response.ok) {
      throw new Error('Upload failed.');
    }

    const result = await response.json();
    const inserted = result.insertedRows ?? 0;
    const total = result.totalRows ?? 0;
    const errorCount = (result.errors ?? []).length;

    await setDateRangeFromUploadedFile(file);
    await fetchSpendByCategory();

    elements.uploadStatus.textContent = `Uploaded ${file.name}: ${inserted}/${total} rows imported${errorCount ? ` (${errorCount} row issues)` : ''}.`;
    setResponse(`CSV upload complete. ${inserted} of ${total} rows were imported into the transaction dataset.`);
  } catch (error) {
    elements.uploadStatus.textContent = 'CSV upload failed. Please verify the file format and try again.';
    setResponse(error.message || 'CSV upload failed.');
  }
}

async function askAgent() {
  const query = elements.agentInput.value.trim();
  if (!query) {
    setResponse('Please enter a question about your transactions.');
    return;
  }

  setEmptyResponse();
  elements.responseBox.innerHTML = '<p>Analyzing your transaction data...</p>';

  try {
    const response = await fetch('/api/agent/query', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ query }),
    });

    if (!response.ok) {
      throw new Error('The agent is unavailable right now.');
    }

    const payload = await response.json();
    setResponse(payload.response || 'No response returned.');
  } catch (error) {
    setResponse(error.message || 'Something went wrong while contacting the agent.');
  }
}

function bindEvents() {
  elements.refreshBtn.addEventListener('click', () => {
    state.start = elements.startDate.value;
    state.end = elements.endDate.value;
    fetchSpendByCategory().catch(() => {
      elements.totalSpend.textContent = '$0.00';
      elements.topCategory.textContent = '—';
      elements.transactionCount.textContent = '0';
      elements.categoryGrid.innerHTML = '<div class="category-card"><h3>No data</h3><div class="amount">$0.00</div><div class="share">Unable to load category totals.</div></div>';
    });
  });

  elements.askBtn.addEventListener('click', askAgent);
  elements.uploadBtn.addEventListener('click', uploadCsvFile);

  document.querySelectorAll('.chip').forEach((chip) => {
    chip.addEventListener('click', () => {
      const query = chip.dataset.query;
      elements.agentInput.value = query;
      askAgent();
    });
  });

  elements.agentInput.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && (event.metaKey || event.ctrlKey)) {
      askAgent();
    }
  });
}

document.addEventListener('DOMContentLoaded', () => {
  elements.agentInput.value = state.query;
  bindEvents();
  fetchSpendByCategory().catch((error) => {
    setEmptyResponse();
    console.error(error);
  });
});
