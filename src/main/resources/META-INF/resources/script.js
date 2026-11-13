const API_BASE = '';
const TOKEN_KEY = 'punchclock_jwt';

const state = {
    authMode: 'login',
    token: localStorage.getItem(TOKEN_KEY),
    userEmail: '',
    userRole: 'User',
    entries: [],
    categories: [],
    tags: [],
    summaries: {},
    editingEntryId: null,
    editingCategoryId: null,
    editingTagId: null,
    categoryFilter: ''
};

const pad2 = (n) => String(n).padStart(2, '0');

const toIsoDate = (date) =>
    `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;

const toTimeInput = (date) =>
    `${pad2(date.getHours())}:${pad2(date.getMinutes())}`;

const parseJwtPayload = (token) => {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const json = decodeURIComponent(
            atob(base64)
                .split('')
                .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(json);
    } catch {
        return null;
    }
};

const showToast = (message, type = 'info') => {
    const container = document.querySelector('#toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 3400);
};

const showAuthMessage = (message, type = 'error') => {
    const box = document.querySelector('#authMessage');
    if (!message) {
        box.hidden = true;
        box.textContent = '';
        return;
    }
    box.className = `inline-alert ${type}`;
    box.textContent = message;
    box.hidden = false;
};

const apiFetch = async (path, options = {}) => {
    const headers = { ...(options.headers || {}) };
    if (state.token) {
        headers['Authorization'] = `Bearer ${state.token}`;
    }
    if (options.body && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers
    });

    if (response.status === 401 && state.token) {
        logout('Sitzung abgelaufen. Bitte erneut anmelden.');
        throw new Error('Unauthorized');
    }

    return response;
};

/* Authentication */
const setAuthMode = (mode) => {
    state.authMode = mode;
    document.querySelector('#tabLoginBtn').classList.toggle('active', mode === 'login');
    document.querySelector('#tabRegisterBtn').classList.toggle('active', mode === 'register');
    document.querySelector('#authSubmitBtn').textContent =
        mode === 'login' ? 'Anmelden' : 'Konto erstellen';
    showAuthMessage('');
};

const applySessionFromToken = (token) => {
    const payload = parseJwtPayload(token);
    if (!payload || (payload.exp && payload.exp * 1000 < Date.now())) {
        localStorage.removeItem(TOKEN_KEY);
        state.token = null;
        return false;
    }
    state.token = token;
    state.userEmail = payload.upn || payload.sub || 'Benutzer';
    const groups = Array.isArray(payload.groups) ? payload.groups : [];
    state.userRole = groups.includes('Admin') ? 'Admin' : (groups[0] || 'User');
    localStorage.setItem(TOKEN_KEY, token);
    return true;
};

const handleAuthSubmit = async (e) => {
    e.preventDefault();
    showAuthMessage('');
    const email = document.querySelector('#authEmail').value.trim();
    const password = document.querySelector('#authPassword').value;

    try {
        if (state.authMode === 'register') {
            const regRes = await fetch(`${API_BASE}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
            if (regRes.status === 409) {
                showAuthMessage('Diese E-Mail-Adresse ist bereits registriert.', 'error');
                return;
            }
            if (!regRes.ok) {
                showAuthMessage('Registrierung fehlgeschlagen. Bitte Eingaben prüfen.', 'error');
                return;
            }
            setAuthMode('login');
            showAuthMessage('Konto erstellt! Sie werden angemeldet...', 'success');
        }

        const loginRes = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (!loginRes.ok) {
            showAuthMessage('Ungültige E-Mail-Adresse oder falsches Passwort.', 'error');
            return;
        }

        const token = (await loginRes.text()).trim();
        if (applySessionFromToken(token)) {
            document.querySelector('#authForm').reset();
            await enterApp();
        }
    } catch {
        showAuthMessage('Server nicht erreichbar.', 'error');
    }
};

const logout = (reason = '') => {
    localStorage.removeItem(TOKEN_KEY);
    state.token = null;
    state.entries = [];
    state.categories = [];
    state.tags = [];
    state.summaries = {};
    stopEntryEdit();
    document.querySelector('#appView').hidden = true;
    document.querySelector('#authView').hidden = false;
    if (reason) {
        showAuthMessage(reason, 'error');
    }
};

const enterApp = async () => {
    document.querySelector('#authView').hidden = true;
    document.querySelector('#appView').hidden = false;

    document.querySelector('#currentUserEmail').textContent = state.userEmail;
    const roleBadge = document.querySelector('#currentUserRole');
    roleBadge.textContent = state.userRole;
    roleBadge.classList.toggle('admin', state.userRole === 'Admin');

    setDefaultEntryTimes();
    await loadAllData();
};

/* Data Loading */
const loadAllData = async () => {
    try {
        const [entriesRes, categoriesRes, tagsRes, summariesRes] = await Promise.all([
            apiFetch('/entries'),
            apiFetch('/categories'),
            apiFetch('/tags'),
            apiFetch('/statistics/time-summaries')
        ]);

        if (entriesRes.ok) state.entries = await entriesRes.json();
        if (categoriesRes.ok) state.categories = await categoriesRes.json();
        if (tagsRes.ok) state.tags = await tagsRes.json();
        if (summariesRes.ok) state.summaries = await summariesRes.json();

        renderCategoryOptions();
        renderTagSelector();
        renderEntries();
        renderStatistics();
        renderMasterData();
        renderKpis();
    } catch {
        // Handled by apiFetch on 401 or toast on network error
    }
};

const refreshEntriesAndStats = async () => {
    const [entriesRes, summariesRes] = await Promise.all([
        apiFetch('/entries'),
        apiFetch('/statistics/time-summaries')
    ]);
    if (entriesRes.ok) state.entries = await entriesRes.json();
    if (summariesRes.ok) state.summaries = await summariesRes.json();
    renderEntries();
    renderStatistics();
    renderKpis();
};

/* Formatting & Helpers */
const hmsToSeconds = (hms) => {
    const [h = 0, m = 0, s = 0] = hms.split(':').map(Number);
    return h * 3600 + m * 60 + s;
};

const secondsToHms = (totalSeconds) => {
    const sec = Math.max(0, Math.floor(totalSeconds));
    const h = Math.floor(sec / 3600);
    const m = Math.floor((sec % 3600) / 60);
    const s = sec % 60;
    return `${pad2(h)}:${pad2(m)}:${pad2(s)}`;
};

const formatDateTime = (iso) => {
    const d = new Date(iso);
    return d.toLocaleString('de-CH', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
};

const calculateDurationLabel = (checkIn, checkOut) => {
    const diffSec = (new Date(checkOut) - new Date(checkIn)) / 1000;
    return secondsToHms(diffSec);
};

/* KPIs & Statistics */
const renderKpis = () => {
    const summaryValues = Object.values(state.summaries);
    const totalSeconds = summaryValues.reduce((acc, hms) => acc + hmsToSeconds(hms), 0);
    const todayKey = toIsoDate(new Date());

    document.querySelector('#kpiTotalTime').textContent = secondsToHms(totalSeconds);
    document.querySelector('#kpiTodayTime').textContent = state.summaries[todayKey] || '00:00:00';
    document.querySelector('#kpiEntryCount').textContent = String(state.entries.length);
    document.querySelector('#kpiActiveDays').textContent = String(summaryValues.length);
};

const renderStatistics = () => {
    const container = document.querySelector('#statsList');
    container.innerHTML = '';
    const dates = Object.keys(state.summaries).sort().reverse();

    if (dates.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'muted-text';
        empty.textContent = 'Noch keine Zeiteinträge für die Auswertung vorhanden.';
        container.appendChild(empty);
        return;
    }

    const targetSeconds = 8 * 3600;
    dates.forEach((dateStr) => {
        const hms = state.summaries[dateStr];
        const sec = hmsToSeconds(hms);
        const percent = Math.min(100, Math.round((sec / targetSeconds) * 100));

        const row = document.createElement('div');
        row.className = 'stat-row';

        const dateEl = document.createElement('span');
        dateEl.className = 'stat-date';
        dateEl.textContent = new Date(`${dateStr}T00:00:00`).toLocaleDateString('de-CH', {
            weekday: 'short',
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });

        const track = document.createElement('div');
        track.className = 'stat-bar-track';
        const fill = document.createElement('div');
        fill.className = `stat-bar-fill ${sec >= targetSeconds ? 'overtime' : ''}`;
        fill.style.width = `${percent}%`;
        track.appendChild(fill);

        const dur = document.createElement('span');
        dur.className = 'stat-duration';
        dur.textContent = hms;

        row.append(dateEl, track, dur);
        container.appendChild(row);
    });
};

/* Entries Form & Table */
const setDefaultEntryTimes = () => {
    if (state.editingEntryId !== null) return;
    const form = document.querySelector('#createEntryForm');
    const today = toIsoDate(new Date());
    form.elements['checkInDate'].value = today;
    form.elements['checkInTime'].value = '08:00';
    form.elements['checkOutDate'].value = today;
    form.elements['checkOutTime'].value = '17:00';
};

const renderCategoryOptions = () => {
    const entrySelect = document.querySelector('#entryCategory');
    const filterSelect = document.querySelector('#categoryFilter');
    const currentEntryVal = entrySelect.value;
    const currentFilterVal = state.categoryFilter;

    entrySelect.innerHTML = '<option value="">Keine Kategorie</option>';
    filterSelect.innerHTML = '<option value="">Alle Kategorien</option>';

    state.categories.forEach((cat) => {
        const opt1 = document.createElement('option');
        opt1.value = String(cat.id);
        opt1.textContent = cat.categoryName;
        entrySelect.appendChild(opt1);

        const opt2 = document.createElement('option');
        opt2.value = String(cat.id);
        opt2.textContent = cat.categoryName;
        filterSelect.appendChild(opt2);
    });

    entrySelect.value = currentEntryVal;
    filterSelect.value = currentFilterVal;
};

const renderTagSelector = (selectedIds = []) => {
    const container = document.querySelector('#entryTagsSelector');
    container.innerHTML = '';

    if (state.tags.length === 0) {
        const hint = document.createElement('span');
        hint.className = 'muted-text';
        hint.textContent = 'Noch keine Tags angelegt (unter „Kategorien & Tags“ erstellbar).';
        container.appendChild(hint);
        return;
    }

    const selectedSet = new Set(selectedIds.map(Number));
    state.tags.forEach((tag) => {
        const label = document.createElement('label');
        label.className = 'tag-option';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.value = String(tag.id);
        checkbox.checked = selectedSet.has(tag.id);

        const text = document.createElement('span');
        text.textContent = tag.title;

        label.append(checkbox, text);
        container.appendChild(label);
    });
};

const getSelectedTagIds = () =>
    Array.from(document.querySelectorAll('#entryTagsSelector input[type="checkbox"]:checked')).map(
        (cb) => Number(cb.value)
    );

const startEntryEdit = (entry) => {
    const form = document.querySelector('#createEntryForm');
    const [checkInDate, checkInTime] = entry.checkIn.split('T');
    const [checkOutDate, checkOutTime] = entry.checkOut.split('T');

    form.elements['checkInDate'].value = checkInDate;
    form.elements['checkInTime'].value = checkInTime.substring(0, 5);
    form.elements['checkOutDate'].value = checkOutDate;
    form.elements['checkOutTime'].value = checkOutTime.substring(0, 5);
    form.elements['categoryId'].value = entry.categoryId ? String(entry.categoryId) : '';
    renderTagSelector(entry.tagIds || []);

    state.editingEntryId = entry.id;
    document.querySelector('#entryFormTitle').textContent = `Eintrag #${entry.id} bearbeiten`;
    document.querySelector('#entrySubmitBtn').textContent = 'Aktualisieren';
    document.querySelector('#cancelEdit').hidden = false;
};

const stopEntryEdit = () => {
    const form = document.querySelector('#createEntryForm');
    state.editingEntryId = null;
    form.reset();
    renderTagSelector([]);
    setDefaultEntryTimes();
    document.querySelector('#entryFormTitle').textContent = 'Neuer Zeiteintrag';
    document.querySelector('#entrySubmitBtn').textContent = 'Speichern';
    document.querySelector('#cancelEdit').hidden = true;
};

const normalizeTime = (timeStr) => (timeStr.length === 5 ? `${timeStr}:00` : timeStr);

const handleEntrySubmit = async (e) => {
    e.preventDefault();
    const form = e.target;
    const checkIn = `${form.elements['checkInDate'].value}T${normalizeTime(form.elements['checkInTime'].value)}`;
    const checkOut = `${form.elements['checkOutDate'].value}T${normalizeTime(form.elements['checkOutTime'].value)}`;

    if (new Date(checkOut) <= new Date(checkIn)) {
        showToast('Check-Out muss nach dem Check-In liegen.', 'error');
        return;
    }

    const categoryRaw = form.elements['categoryId'].value;
    const payload = {
        checkIn,
        checkOut,
        categoryId: categoryRaw ? Number(categoryRaw) : null,
        tagIds: getSelectedTagIds()
    };

    const isEditing = state.editingEntryId !== null;
    const path = isEditing ? `/entries/${state.editingEntryId}` : '/entries';
    const method = isEditing ? 'PUT' : 'POST';

    const res = await apiFetch(path, {
        method,
        body: JSON.stringify(payload)
    });

    if (!res.ok) {
        showToast('Eintrag konnte nicht gespeichert werden (Eingaben prüfen).', 'error');
        return;
    }

    stopEntryEdit();
    await refreshEntriesAndStats();
    showToast(isEditing ? 'Eintrag aktualisiert.' : 'Zeiteintrag gespeichert.', 'success');
};

const deleteEntry = async (id) => {
    if (state.userRole !== 'Admin') {
        showToast('Nur Administratoren dürfen Zeiteinträge löschen.', 'error');
        return;
    }
    const res = await apiFetch(`/entries/${id}`, { method: 'DELETE' });
    if (res.ok || res.status === 404) {
        if (state.editingEntryId === id) {
            stopEntryEdit();
        }
        await refreshEntriesAndStats();
        showToast(`Eintrag #${id} gelöscht.`, 'success');
    } else if (res.status === 403) {
        showToast('Keine Berechtigung zum Löschen (Admin-Rolle erforderlich).', 'error');
    }
};

const renderEntries = () => {
    const tbody = document.querySelector('#entryDisplay');
    tbody.innerHTML = '';

    const categoryMap = new Map(state.categories.map((c) => [c.id, c.categoryName]));
    const tagMap = new Map(state.tags.map((t) => [t.id, t.title]));

    const filtered = state.entries
        .filter((entry) => !state.categoryFilter || String(entry.categoryId) === state.categoryFilter)
        .slice()
        .sort((a, b) => new Date(b.checkIn) - new Date(a.checkIn));

    if (filtered.length === 0) {
        const tr = document.createElement('tr');
        const td = document.createElement('td');
        td.colSpan = 6;
        td.className = 'empty-cell';
        td.textContent = 'Keine Zeiteinträge gefunden.';
        tr.appendChild(td);
        tbody.appendChild(tr);
        return;
    }

    const isAdmin = state.userRole === 'Admin';

    filtered.forEach((entry) => {
        const tr = document.createElement('tr');

        const idCell = document.createElement('td');
        idCell.textContent = `#${entry.id}`;

        const inCell = document.createElement('td');
        inCell.textContent = formatDateTime(entry.checkIn);

        const outCell = document.createElement('td');
        outCell.textContent = formatDateTime(entry.checkOut);

        const durCell = document.createElement('td');
        const durSpan = document.createElement('span');
        durSpan.className = 'duration-pill';
        durSpan.textContent = calculateDurationLabel(entry.checkIn, entry.checkOut);
        durCell.appendChild(durSpan);

        const metaCell = document.createElement('td');
        const badges = document.createElement('div');
        badges.className = 'badges-wrap';

        if (entry.categoryId && categoryMap.has(entry.categoryId)) {
            const cat = document.createElement('span');
            cat.className = 'cat-badge';
            cat.textContent = categoryMap.get(entry.categoryId);
            badges.appendChild(cat);
        }

        (entry.tagIds || []).forEach((tagId) => {
            if (tagMap.has(tagId)) {
                const chip = document.createElement('span');
                chip.className = 'tag-chip';
                chip.textContent = tagMap.get(tagId);
                badges.appendChild(chip);
            }
        });

        if (badges.childElementCount === 0) {
            const dash = document.createElement('span');
            dash.className = 'muted-text';
            dash.textContent = '—';
            badges.appendChild(dash);
        }
        metaCell.appendChild(badges);

        const actionCell = document.createElement('td');
        actionCell.className = 'col-actions';
        const actions = document.createElement('div');
        actions.className = 'row-actions';

        const editBtn = document.createElement('button');
        editBtn.type = 'button';
        editBtn.className = 'btn btn-outline btn-xs';
        editBtn.textContent = 'Bearbeiten';
        editBtn.addEventListener('click', () => startEntryEdit(entry));

        const delBtn = document.createElement('button');
        delBtn.type = 'button';
        delBtn.className = 'btn btn-danger btn-xs';
        delBtn.textContent = 'Löschen';
        delBtn.disabled = !isAdmin;
        if (!isAdmin) {
            delBtn.title = 'Nur Administratoren dürfen Einträge löschen';
        }
        delBtn.addEventListener('click', () => deleteEntry(entry.id));

        actions.append(editBtn, delBtn);
        actionCell.appendChild(actions);

        tr.append(idCell, inCell, outCell, durCell, metaCell, actionCell);
        tbody.appendChild(tr);
    });
};

/* Categories & Tags Management */
const renderMasterData = () => {
    const catList = document.querySelector('#categoryList');
    catList.innerHTML = '';
    if (state.categories.length === 0) {
        const li = document.createElement('li');
        li.className = 'muted-text';
        li.textContent = 'Noch keine Kategorien erfasst.';
        catList.appendChild(li);
    } else {
        state.categories.forEach((cat) => {
            const li = document.createElement('li');
            li.className = 'entity-item';
            const name = document.createElement('span');
            name.textContent = cat.categoryName;

            const actions = document.createElement('div');
            actions.className = 'row-actions';

            const editBtn = document.createElement('button');
            editBtn.type = 'button';
            editBtn.className = 'btn btn-outline btn-xs';
            editBtn.textContent = 'Umbenennen';
            editBtn.addEventListener('click', () => {
                state.editingCategoryId = cat.id;
                document.querySelector('#categoryNameInput').value = cat.categoryName;
                document.querySelector('#categorySubmitBtn').textContent = 'Speichern';
                document.querySelector('#categoryCancelBtn').hidden = false;
            });

            const delBtn = document.createElement('button');
            delBtn.type = 'button';
            delBtn.className = 'btn btn-danger btn-xs';
            delBtn.textContent = 'Löschen';
            delBtn.addEventListener('click', async () => {
                const res = await apiFetch(`/categories/${cat.id}`, { method: 'DELETE' });
                if (res.ok) {
                    showToast('Kategorie gelöscht.', 'success');
                    await loadAllData();
                }
            });

            actions.append(editBtn, delBtn);
            li.append(name, actions);
            catList.appendChild(li);
        });
    }

    const tagList = document.querySelector('#tagList');
    tagList.innerHTML = '';
    if (state.tags.length === 0) {
        const li = document.createElement('li');
        li.className = 'muted-text';
        li.textContent = 'Noch keine Tags erfasst.';
        tagList.appendChild(li);
    } else {
        state.tags.forEach((tag) => {
            const li = document.createElement('li');
            li.className = 'entity-item';
            const title = document.createElement('span');
            title.textContent = tag.title;

            const actions = document.createElement('div');
            actions.className = 'row-actions';

            const editBtn = document.createElement('button');
            editBtn.type = 'button';
            editBtn.className = 'btn btn-outline btn-xs';
            editBtn.textContent = 'Umbenennen';
            editBtn.addEventListener('click', () => {
                state.editingTagId = tag.id;
                document.querySelector('#tagTitleInput').value = tag.title;
                document.querySelector('#tagSubmitBtn').textContent = 'Speichern';
                document.querySelector('#tagCancelBtn').hidden = false;
            });

            const delBtn = document.createElement('button');
            delBtn.type = 'button';
            delBtn.className = 'btn btn-danger btn-xs';
            delBtn.textContent = 'Löschen';
            delBtn.addEventListener('click', async () => {
                const res = await apiFetch(`/tags/${tag.id}`, { method: 'DELETE' });
                if (res.ok) {
                    showToast('Tag gelöscht.', 'success');
                    await loadAllData();
                }
            });

            actions.append(editBtn, delBtn);
            li.append(title, actions);
            tagList.appendChild(li);
        });
    }
};

const resetCategoryForm = () => {
    state.editingCategoryId = null;
    document.querySelector('#categoryForm').reset();
    document.querySelector('#categorySubmitBtn').textContent = 'Hinzufügen';
    document.querySelector('#categoryCancelBtn').hidden = true;
};

const resetTagForm = () => {
    state.editingTagId = null;
    document.querySelector('#tagForm').reset();
    document.querySelector('#tagSubmitBtn').textContent = 'Hinzufügen';
    document.querySelector('#tagCancelBtn').hidden = true;
};

const handleCategorySubmit = async (e) => {
    e.preventDefault();
    const categoryName = document.querySelector('#categoryNameInput').value.trim();
    if (!categoryName) return;

    const isEditing = state.editingCategoryId !== null;
    const res = await apiFetch(isEditing ? `/categories/${state.editingCategoryId}` : '/categories', {
        method: isEditing ? 'PUT' : 'POST',
        body: JSON.stringify({ categoryName })
    });

    if (res.ok) {
        resetCategoryForm();
        await loadAllData();
        showToast(isEditing ? 'Kategorie aktualisiert.' : 'Kategorie erstellt.', 'success');
    } else {
        showToast('Kategorie konnte nicht gespeichert werden.', 'error');
    }
};

const handleTagSubmit = async (e) => {
    e.preventDefault();
    const title = document.querySelector('#tagTitleInput').value.trim();
    if (!title) return;

    const isEditing = state.editingTagId !== null;
    const res = await apiFetch(isEditing ? `/tags/${state.editingTagId}` : '/tags', {
        method: isEditing ? 'PUT' : 'POST',
        body: JSON.stringify({ title })
    });

    if (res.ok) {
        resetTagForm();
        await loadAllData();
        showToast(isEditing ? 'Tag aktualisiert.' : 'Tag erstellt.', 'success');
    } else {
        showToast('Tag konnte nicht gespeichert werden.', 'error');
    }
};

/* Navigation & Presets */
const switchSection = (sectionId) => {
    document.querySelectorAll('.view-section').forEach((sec) => {
        sec.hidden = sec.id !== sectionId;
    });
    document.querySelectorAll('.nav-btn').forEach((btn) => {
        btn.classList.toggle('active', btn.dataset.section === sectionId);
    });
};

document.addEventListener('DOMContentLoaded', () => {
    // Auth events
    document.querySelector('#tabLoginBtn').addEventListener('click', () => setAuthMode('login'));
    document.querySelector('#tabRegisterBtn').addEventListener('click', () => setAuthMode('register'));
    document.querySelector('#authForm').addEventListener('submit', handleAuthSubmit);
    document.querySelector('#fillAdminBtn').addEventListener('click', () => {
        setAuthMode('login');
        document.querySelector('#authEmail').value = 'admin@example.com';
        document.querySelector('#authPassword').value = 'admin123';
    });
    document.querySelector('#logoutBtn').addEventListener('click', () => logout());

    // Navigation
    document.querySelectorAll('.nav-btn').forEach((btn) => {
        btn.addEventListener('click', () => switchSection(btn.dataset.section));
    });

    // Entry events & presets
    document.querySelector('#createEntryForm').addEventListener('submit', handleEntrySubmit);
    document.querySelector('#cancelEdit').addEventListener('click', stopEntryEdit);
    document.querySelector('#categoryFilter').addEventListener('change', (e) => {
        state.categoryFilter = e.target.value;
        renderEntries();
    });

    document.querySelector('#presetWorkdayBtn').addEventListener('click', () => {
        const form = document.querySelector('#createEntryForm');
        const today = toIsoDate(new Date());
        form.elements['checkInDate'].value = today;
        form.elements['checkInTime'].value = '08:00';
        form.elements['checkOutDate'].value = today;
        form.elements['checkOutTime'].value = '17:00';
    });

    document.querySelector('#presetNowBtn').addEventListener('click', () => {
        const form = document.querySelector('#createEntryForm');
        const now = new Date();
        const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000);
        form.elements['checkInDate'].value = toIsoDate(oneHourAgo);
        form.elements['checkInTime'].value = toTimeInput(oneHourAgo);
        form.elements['checkOutDate'].value = toIsoDate(now);
        form.elements['checkOutTime'].value = toTimeInput(now);
    });

    // Category & Tag forms
    document.querySelector('#categoryForm').addEventListener('submit', handleCategorySubmit);
    document.querySelector('#categoryCancelBtn').addEventListener('click', resetCategoryForm);
    document.querySelector('#tagForm').addEventListener('submit', handleTagSubmit);
    document.querySelector('#tagCancelBtn').addEventListener('click', resetTagForm);

    // Restore session if valid JWT exists
    if (state.token && applySessionFromToken(state.token)) {
        enterApp();
    }
});
