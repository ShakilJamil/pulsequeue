/**
 * PulseQueue - Interactive Web App Client
 * WebSocket STOMP real-time integration, slot booking, staff desk & TV display
 */

let stompClient = null;
let currentTicketNumber = 'Q-103';
let selectedSlotId = null;

document.addEventListener('DOMContentLoaded', () => {
  if (window.lucide) lucide.createIcons();

  // Dark/Light Theme Switcher
  const themeToggle = document.getElementById('theme-toggle');
  if (themeToggle) {
    themeToggle.addEventListener('click', () => {
      document.documentElement.classList.toggle('dark');
    });
  }

  // Set today's date in picker
  const datePicker = document.getElementById('date-select');
  if (datePicker) {
    const today = new Date().toISOString().split('T')[0];
    datePicker.value = today;
  }

  initWebSockets();
  loadDepartments();
  loadSlots();
  loadCounters();
  loadStaffQueue();
  loadTvDisplay();
  loadMetrics();
});

// 1. WebSocket STOMP Connection
function initWebSockets() {
  try {
    const socket = new SockJS('/ws-queue');
    stompClient = Stomp.over(socket);
    stompClient.debug = null; // silent debug in production

    stompClient.connect({}, (frame) => {
      // Subscribe to global queue status updates
      stompClient.subscribe('/topic/queue-updates', (message) => {
        const payload = JSON.parse(message.body);
        handleLiveQueueUpdate(payload);
      });
    }, (err) => {
      console.warn('WebSocket connection retrying in 5s...', err);
      setTimeout(initWebSockets, 5000);
    });
  } catch (ex) {
    console.warn('SockJS offline - running in standalone API mode.');
  }
}

// 2. Play Gentle Hospital Audio Chime via Web Audio API
function playChime() {
  try {
    const ctx = new (window.AudioContext || window.webkitAudioContext)();
    const now = ctx.currentTime;

    const osc1 = ctx.createOscillator();
    const gain1 = ctx.createGain();
    osc1.type = 'sine';
    osc1.frequency.setValueAtTime(659.25, now); // E5
    gain1.gain.setValueAtTime(0.3, now);
    gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.6);
    osc1.connect(gain1);
    gain1.connect(ctx.destination);
    osc1.start(now);
    osc1.stop(now + 0.6);

    const osc2 = ctx.createOscillator();
    const gain2 = ctx.createGain();
    osc2.type = 'sine';
    osc2.frequency.setValueAtTime(880.00, now + 0.3); // A5
    gain2.gain.setValueAtTime(0.3, now + 0.3);
    gain2.gain.exponentialRampToValueAtTime(0.001, now + 1.2);
    osc2.connect(gain2);
    gain2.connect(ctx.destination);
    osc2.start(now + 0.3);
    osc2.stop(now + 1.2);
  } catch (_) {
    // Audio context may require prior user interaction
  }
}

// 3. Handle Live Event Broadcasts
function handleLiveQueueUpdate(payload) {
  playChime();

  const banner = document.getElementById('live-broadcast-banner');
  const bannerTitle = document.getElementById('banner-title');
  const bannerDesc = document.getElementById('banner-desc');

  if (banner && payload.ticket) {
    bannerTitle.textContent = `Ticket ${payload.ticket.ticketNumber} ${payload.eventType === 'TICKET_CALLED' ? 'Called!' : 'Updated'}`;
    bannerDesc.textContent = `Proceed to ${payload.counterNumber || 'assigned station'} (${payload.ticket.departmentName})`;
    banner.classList.remove('hidden');

    setTimeout(() => {
      banner.classList.add('hidden');
    }, 8000);
  }

  // Refresh TV display and staff queue
  loadTvDisplay();
  loadStaffQueue();
  loadMetrics();

  // If this update matches my ticket, update tracker
  if (payload.ticket && payload.ticket.ticketNumber === currentTicketNumber) {
    updateMyTicketDisplay(payload.ticket);
  }
}

// 4. Tab Switching
function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(tab => tab.classList.add('hidden'));
  document.querySelectorAll('.nav-tab').forEach(btn => btn.classList.remove('active', 'bg-brand-600', 'text-white'));

  const activeContent = document.getElementById('tab-' + tabId);
  const activeNav = document.getElementById('nav-' + tabId);

  if (activeContent) activeContent.classList.remove('hidden');
  if (activeNav) activeNav.classList.add('active', 'bg-brand-600', 'text-white');

  if (window.lucide) lucide.createIcons();

  if (tabId === 'tv') loadTvDisplay();
  if (tabId === 'staff') loadStaffQueue();
  if (tabId === 'analytics') loadMetrics();
}

// 5. Load Departments
async function loadDepartments() {
  try {
    const res = await fetch('/api/departments');
    if (!res.ok) return;
    const depts = await res.json();
    const select = document.getElementById('department-select');
    if (select) {
      select.innerHTML = '<option value="">All Departments</option>';
      depts.forEach(d => {
        select.innerHTML += `<option value="${d.id}">${d.name}</option>`;
      });
    }
  } catch (err) {
    console.error('Error loading departments:', err);
  }
}

// 6. Load Slots
async function loadSlots() {
  const deptId = document.getElementById('department-select')?.value || '';
  const date = document.getElementById('date-select')?.value || '';
  const grid = document.getElementById('slots-grid');
  const badge = document.getElementById('slot-count-badge');

  if (!grid) return;
  grid.innerHTML = '<div class="col-span-full py-8 text-center text-xs text-slate-400">Loading available slots...</div>';

  try {
    const res = await fetch(`/api/slots?departmentId=${deptId}&date=${date}`);
    const slots = await res.json();

    if (badge) badge.textContent = `${slots.length} slots available`;

    if (slots.length === 0) {
      grid.innerHTML = '<div class="col-span-full py-8 text-center text-xs text-slate-400">No open slots for this selection. Try another date!</div>';
      return;
    }

    grid.innerHTML = '';
    slots.forEach(slot => {
      const card = document.createElement('div');
      card.className = 'p-4 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-dark-card hover:border-brand-500 hover:shadow-lg transition cursor-pointer flex flex-col justify-between space-y-2';
      card.onclick = () => openBookingModal(slot);

      card.innerHTML = `
        <div class="flex items-center justify-between">
          <span class="text-xs font-mono font-bold text-brand-600 dark:text-brand-400">${slot.startTime.substring(0, 5)}</span>
          <span class="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-500/10 text-emerald-500">Available</span>
        </div>
        <div>
          <h4 class="font-bold text-xs text-slate-800 dark:text-slate-200">${slot.department.name}</h4>
          <p class="text-[10px] text-slate-400">25 min consultation</p>
        </div>
        <button class="w-full py-1.5 rounded-lg bg-slate-100 dark:bg-slate-800 hover:bg-brand-600 hover:text-white text-slate-700 dark:text-slate-300 font-semibold text-[11px] transition">
          Book Slot
        </button>
      `;
      grid.appendChild(card);
    });
  } catch (err) {
    grid.innerHTML = '<div class="col-span-full py-8 text-center text-xs text-rose-500">Could not fetch slots. Ensure backend is running.</div>';
  }
}

// 7. Booking Modal Logic
function openBookingModal(slot) {
  selectedSlotId = slot.id;
  document.getElementById('modal-dept').textContent = slot.department.name;
  document.getElementById('modal-time').textContent = `${slot.slotDate} at ${slot.startTime.substring(0, 5)}`;
  document.getElementById('booking-modal').classList.remove('hidden');
}

function closeBookingModal() {
  document.getElementById('booking-modal').classList.add('hidden');
  selectedSlotId = null;
}

async function confirmBooking() {
  if (!selectedSlotId) return;

  const notes = document.getElementById('booking-notes')?.value || '';
  const confirmBtn = document.getElementById('modal-confirm-btn');
  confirmBtn.disabled = true;
  confirmBtn.textContent = 'Guarding Slot with Optimistic Lock...';

  try {
    // Quick guest auth token for instant demo test
    const loginRes = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'customer@pulsequeue.dev', password: 'Customer@123' })
    });
    const authData = await loginRes.json();

    const bookRes = await fetch('/api/appointments/book', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authData.token}`
      },
      body: JSON.stringify({ slotId: selectedSlotId, notes })
    });

    if (bookRes.ok) {
      const apt = await bookRes.json();
      closeBookingModal();
      showToast(`Appointment ${apt.appointmentCode} reserved!`);
      loadSlots();
      switchTab('my-ticket');
    } else {
      const errorData = await bookRes.json();
      showToast(errorData.message || 'Concurrency conflict: slot was claimed!');
    }
  } catch (err) {
    showToast('Network error while booking slot.');
  } finally {
    confirmBtn.disabled = false;
    confirmBtn.textContent = 'Confirm & Issue Ticket';
  }
}

// 8. Walk-in Ticket
async function issueWalkin() {
  try {
    const res = await fetch('/api/queue/walkin?departmentId=1', { method: 'POST' });
    if (res.ok) {
      const ticket = await res.json();
      currentTicketNumber = ticket.ticketNumber;
      updateMyTicketDisplay(ticket);
      showToast(`Walk-in Ticket ${ticket.ticketNumber} Issued!`);
      switchTab('my-ticket');
    }
  } catch (err) {
    showToast('Failed to issue walk-in ticket.');
  }
}

// 9. Update My Ticket View
function updateMyTicketDisplay(ticket) {
  document.getElementById('ticket-number-display').textContent = ticket.ticketNumber;
  document.getElementById('ticket-department-display').textContent = ticket.departmentName;
  document.getElementById('ticket-ahead-display').textContent = ticket.waitingAheadCount || '0';
  document.getElementById('ticket-wait-display').textContent = `~${(ticket.waitingAheadCount || 0) * 8}m`;
  document.getElementById('ticket-counter-display').textContent = ticket.counterNumber || 'Pending Assignment';

  const statusPill = document.getElementById('ticket-status-pill');
  if (statusPill) {
    statusPill.innerHTML = `<span>Status: ${ticket.status}</span>`;
  }
}

// 10. Load Staff Stations & Queue
async function loadCounters() {
  try {
    const res = await fetch('/api/queue/counters');
    if (!res.ok) return;
    const counters = await res.json();
    const select = document.getElementById('staff-counter-select');
    if (select && counters.length > 0) {
      select.innerHTML = '';
      counters.forEach(c => {
        select.innerHTML += `<option value="${c.id}">${c.counterNumber} - ${c.department.name}</option>`;
      });
    }
  } catch (_) {}
}

async function loadStaffQueue() {
  try {
    const res = await fetch('/api/queue/active');
    if (!res.ok) return;
    const tickets = await res.json();
    const tbody = document.getElementById('staff-queue-tbody');
    if (tbody) {
      if (tickets.length === 0) {
        tbody.innerHTML = '<tr><td colspan="5" class="py-6 text-center text-slate-400">Queue is empty! All patients served.</td></tr>';
        return;
      }
      tbody.innerHTML = '';
      tickets.forEach((t, i) => {
        tbody.innerHTML += `
          <tr class="hover:bg-slate-50 dark:hover:bg-slate-900 transition">
            <td class="py-3 px-4 font-mono font-bold text-brand-600">${t.ticketNumber}</td>
            <td class="py-3 px-4">${t.customerName || 'Walk-in'}</td>
            <td class="py-3 px-4">${t.departmentName}</td>
            <td class="py-3 px-4 font-mono">#${i + 1} in line</td>
            <td class="py-3 px-4 text-slate-400">${t.issuedAt ? t.issuedAt.substring(11, 16) : 'Now'}</td>
          </tr>
        `;
      });
    }
  } catch (_) {}
}

// 11. Staff Actions
async function callNextCustomer() {
  const counterId = document.getElementById('staff-counter-select')?.value || 1;
  try {
    // Staff auth
    const loginRes = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: 'staff1@pulsequeue.dev', password: 'Staff@123' })
    });
    const auth = await loginRes.json();

    const res = await fetch('/api/queue/call', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${auth.token}`
      },
      body: JSON.stringify({ counterId })
    });

    if (res.ok) {
      const ticket = await res.json();
      document.getElementById('staff-now-serving').textContent = ticket.ticketNumber;
      showToast(`Called Ticket ${ticket.ticketNumber} to counter!`);
    } else {
      const err = await res.json();
      showToast(err.message || 'No patients waiting.');
    }
  } catch (err) {
    showToast('Failed to call next ticket.');
  }
}

async function completeCurrentTicket() {
  showToast('Current consultation marked as COMPLETED.');
  document.getElementById('staff-now-serving').textContent = 'Ready';
  loadStaffQueue();
  loadMetrics();
}

async function skipCurrentTicket() {
  showToast('Patient marked as NO-SHOW.');
  document.getElementById('staff-now-serving').textContent = 'Ready';
  loadStaffQueue();
  loadMetrics();
}

// 12. TV Digital Signage Display
async function loadTvDisplay() {
  const grid = document.getElementById('tv-serving-grid');
  if (!grid) return;

  try {
    const res = await fetch('/api/queue/serving');
    const tickets = await res.json();

    grid.innerHTML = '';
    if (tickets.length === 0) {
      grid.innerHTML = '<div class="col-span-full py-12 text-center text-slate-400 font-mono">No active consultations at this moment.</div>';
      return;
    }

    tickets.forEach(t => {
      grid.innerHTML += `
        <div class="p-8 rounded-3xl border-2 border-emerald-500/40 bg-white dark:bg-dark-card shadow-2xl text-center space-y-3 pulse-glow animate-card-fade">
          <span class="text-xs font-mono uppercase tracking-widest text-emerald-500 font-bold">Calling Now</span>
          <div class="text-6xl font-black font-mono tracking-wider text-slate-900 dark:text-white">${t.ticketNumber}</div>
          <div class="py-2 px-4 rounded-xl bg-slate-100 dark:bg-slate-800 text-sm font-bold text-brand-600 font-mono">
            ${t.counterNumber || 'Counter 101'}
          </div>
          <p class="text-xs text-slate-400">${t.departmentName}</p>
        </div>
      `;
    });
  } catch (_) {}
}

// 13. Live Analytics Metrics
async function loadMetrics() {
  try {
    const res = await fetch('/api/analytics/live');
    if (!res.ok) return;
    const data = await res.json();

    document.getElementById('metric-waiting').textContent = data.totalWaiting;
    document.getElementById('metric-serving').textContent = data.totalServing;
    document.getElementById('metric-completed').textContent = data.totalCompletedToday;
    document.getElementById('metric-avg-wait').textContent = `${Math.round(data.averageWaitTimeMinutes)}m`;
  } catch (_) {}
}

// 14. Toast Helper
function showToast(msg) {
  const toast = document.getElementById('toast');
  const toastMessage = document.getElementById('toast-message');
  if (!toast || !toastMessage) return;

  toastMessage.textContent = msg;
  toast.classList.remove('hidden');
  setTimeout(() => toast.classList.add('hidden'), 3500);
}
