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

function setResponse(text) {
  elements.responseBox.innerHTML = `<h3>Agent insight</h3><p>${text}</p>`;
}

function setEmptyResponse() {
  elements.responseBox.innerHTML = '<p class="empty-state">The agent response will appear here.</p>';
}

async function fetchSpendByCategory() {
  const response = await fetch(`/api/analytics/spend-by-category?start=${state.start}&end=${state.end}`);
  if (!response.ok) {
    throw new Error('Unable to load category spend data.');
  }

  const items = await response.json();
  const totalSpend = items.reduce((sum, item) => sum + Number(item.totalSpend ?? 0), 0);
  const topCategory = items[0] ? items[0].categoryName : '—';

  elements.totalSpend.textContent = formatCurrency(totalSpend);
  elements.topCategory.textContent = topCategory;
  elements.transactionCount.textContent = items.length;
  elements.dateRangeLabel.textContent = `${formatDateLabel(state.start)} - ${formatDateLabel(state.end)}`;

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
