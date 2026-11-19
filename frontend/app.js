/* ════════════════════════════════════════════════════
   EDCMS Frontend — app.js
   Full API client + SPA controller
   ════════════════════════════════════════════════════ */

'use strict';

/* ──────────────── API CLIENT ──────────────── */
const API = {
  get baseUrl() {
    return document.getElementById('api-base-url').value.replace(/\/$/, '');
  },

  async request(method, path, body = null) {
    const opts = {
      method,
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
    };
    if (body !== null) opts.body = JSON.stringify(body);
    const res = await fetch(`${this.baseUrl}${path}`, opts);
    if (res.status === 204) return null;
    const data = await res.json().catch(() => ({}));
    if (!res.ok) {
      const validationErrors = Object.values(data.fieldErrors || {}).filter(Boolean);
      const msg = validationErrors.length
        ? validationErrors.join(' ')
        : data.message || data.error || `HTTP ${res.status}`;
      throw new Error(msg);
    }
    return data;
  },

  get:    (p)    => API.request('GET',    p),
  post:   (p, b) => API.request('POST',   p, b),
  put:    (p, b) => API.request('PUT',    p, b),
  patch:  (p, b) => API.request('PATCH',  p, b || {}),
  delete: (p)    => API.request('DELETE', p),

  // Projects
  projects: {
    list:         ()       => API.get('/api/projects'),
    get:          (id)     => API.get(`/api/projects/${id}`),
    create:       (b)      => API.post('/api/projects', b),
    update:       (id, b)  => API.put(`/api/projects/${id}`, b),
    changeStatus: (id, b)  => API.patch(`/api/projects/${id}/status`, b),
    delete:       (id)     => API.delete(`/api/projects/${id}`),
  },

  // Components
  components: {
    list:       ()     => API.get('/api/components'),
    listByType: (t)    => API.get(`/api/components?type=${t}`),
    get:        (id)   => API.get(`/api/components/${id}`),
    create:     (b)    => API.post('/api/components', b),
    update:     (id,b) => API.put(`/api/components/${id}`, b),
    deprecate:  (id)   => API.patch(`/api/components/${id}/deprecate`),
  },

  // Versions
  versions: {
    list:      (cId)        => API.get(`/api/components/${cId}/versions`),
    get:       (cId, vId)   => API.get(`/api/components/${cId}/versions/${vId}`),
    create:    (cId, b)     => API.post(`/api/components/${cId}/versions`, b),
    approve:   (cId, vId)   => API.patch(`/api/components/${cId}/versions/${vId}/approve`),
    deprecate: (cId, vId)   => API.patch(`/api/components/${cId}/versions/${vId}/deprecate`),
  },

  // Assemblies
  assemblies: {
    listByProject: (pId)       => API.get(`/api/projects/${pId}/assemblies`),
    get:           (id)        => API.get(`/api/assemblies/${id}`),
    create:        (pId, b)    => API.post(`/api/projects/${pId}/assemblies`, b),
    addComponent:  (id, b)     => API.post(`/api/assemblies/${id}/components`, b),
    removeComponent: (id, vId) => API.delete(`/api/assemblies/${id}/components/${vId}`),
    delete:        (id)        => API.delete(`/api/assemblies/${id}`),
  },

  // Users
  users: {
    list:   ()    => API.get('/api/users'),
    get:    (id)  => API.get(`/api/users/${id}`),
    create: (b)   => API.post('/api/users', b),
    delete: (id)  => API.delete(`/api/users/${id}`),
  },

  // Dependencies
  dependencies: {
    listByVersion: (vId) => API.get(`/api/component-versions/${vId}/dependencies`),
    create:        (b)   => API.post('/api/dependencies', b),
    delete:        (id)  => API.delete(`/api/dependencies/${id}`),
  },

  // Compatibility
  compatibility: {
    rules:  ()    => API.get('/api/compatibility-rules'),
    create: (b)   => API.post('/api/compatibility-rules', b),
    check:  (b)   => API.post('/api/compatibility/check', b),
  },

  // Impact Analysis
  impact: {
    byVersion:   (vId) => API.get(`/api/impact-analysis/component-versions/${vId}`),
    byComponent: (cId) => API.get(`/api/impact-analysis/components/${cId}`),
  },
};

/* ──────────────── TOAST ──────────────── */
const Toast = {
  show(msg, type = 'info', duration = 4000) {
    const icons = {
      success: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>`,
      error:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>`,
      info:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>`,
      warning: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>`,
    };
    const t = document.createElement('div');
    t.className = `toast ${type}`;
    const icon = document.createElement('span');
    icon.className = 'toast-icon';
    icon.innerHTML = icons[type] || icons.info;
    const message = document.createElement('span');
    message.textContent = String(msg);
    t.append(icon, message);
    document.getElementById('toast-container').appendChild(t);
    setTimeout(() => { t.classList.add('toast-out'); setTimeout(() => t.remove(), 300); }, duration);
  },
  success: (m) => Toast.show(m, 'success'),
  error:   (m) => Toast.show(m, 'error'),
  info:    (m) => Toast.show(m, 'info'),
  warning: (m) => Toast.show(m, 'warning'),
};

/* ──────────────── MODAL ──────────────── */
const Modal = {
  overlay: null,
  submitBtn: null,
  _onSubmit: null,

  init() {
    this.overlay   = document.getElementById('modal-overlay');
    this.submitBtn = document.getElementById('modal-submit');
    document.getElementById('modal-close').addEventListener('click',  () => Modal.close());
    document.getElementById('modal-cancel').addEventListener('click', () => Modal.close());
    this.overlay.addEventListener('click', (e) => { if (e.target === this.overlay) Modal.close(); });
  },

  open({ title, body, submitLabel = 'Save', onSubmit, submitClass = 'btn-primary' }) {
    document.getElementById('modal-title').textContent = title;
    document.getElementById('modal-body').innerHTML = body;
    document.getElementById('modal-cancel').style.display = '';
    this.submitBtn.textContent = submitLabel;
    this.submitBtn.className = `btn ${submitClass}`;
    this._onSubmit = onSubmit;
    this.submitBtn.onclick = async () => {
      this.submitBtn.disabled = true;
      this.submitBtn.textContent = 'Saving…';
      try {
        await onSubmit();
      } catch (err) {
        Toast.error(err.message || 'The request could not be completed.');
      } finally {
        this.submitBtn.disabled = false;
        this.submitBtn.textContent = submitLabel;
      }
    };
    this.overlay.classList.add('open');
  },

  close() {
    this.overlay.classList.remove('open');
    this._onSubmit = null;
  },

  val: (id) => document.getElementById(id)?.value?.trim() ?? '',
};

let _usersForForms = [];

async function loadCreatorUsers() {
  const users = await API.users.list();
  _usersForForms = users;
  if (!users.length) {
    Toast.warning('Create a user first. The API requires a creator for this operation.');
    navigate('users');
    openCreateUserModal();
    return false;
  }
  return true;
}

function creatorField(selectedId = _usersForForms[0]?.id) {
  return `
    <div class="form-group">
      <label for="m-created-by">Created By *</label>
      <select id="m-created-by" required>
        <option value="">— select a user —</option>
        ${_usersForForms.map(user => `<option value="${user.id}" ${String(user.id) === String(selectedId) ? 'selected' : ''}>${esc(user.username)} (${esc(user.role)})</option>`).join('')}
      </select>
    </div>`;
}

function selectedCreatorId() {
  const id = Number(Modal.val('m-created-by'));
  if (!id) Toast.warning('Select the user creating this record.');
  return id || null;
}

/* ──────────────── HELPERS ──────────────── */
function badge(text, type) {
  return `<span class="badge badge-${type?.toLowerCase()}">${text}</span>`;
}
function projectStatusBadge(s) {
  const map = { PLANNING:'planning', ACTIVE:'active', COMPLETED:'completed', ARCHIVED:'archived' };
  return badge(s, map[s] || 'planning');
}
function versionStatusBadge(s) {
  const map = { DRAFT:'draft', APPROVED:'approved', DEPRECATED:'deprecated' };
  return badge(s, map[s] || 'draft');
}
function componentTypeBadge(t) {
  const labels = { MECHANICAL:'⚙️ Mechanical', ELECTRICAL:'⚡ Electrical', SOFTWARE:'💻 Software' };
  return badge(labels[t] || t, t?.toLowerCase());
}
function roleBadge(r) {
  return badge(r, r?.toLowerCase());
}
function depTypeBadge(t) {
  const map = { REQUIRES:'requires', RECOMMENDS:'recommends', CONFLICTS_WITH:'conflicts' };
  return badge(t?.replace('_',' '), map[t] || 'requires');
}

function emptyState(msg, icon = '📭') {
  return `<div class="empty-state"><span class="state-icon">${icon}</span>${msg}</div>`;
}
function loadingState() {
  return `<div class="loading-state"><div class="spinner"></div>Loading…</div>`;
}
function errorState(msg) {
  return `<div class="error-state"><span class="state-icon">⚠️</span>${msg}</div>`;
}

function actionBtn(icon, title, classes = '') {
  return `<button class="action-btn ${classes}" title="${title}">${icon}</button>`;
}
const ICONS = {
  edit:      `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>`,
  delete:    `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/><path d="M10 11v6"/><path d="M14 11v6"/><path d="M9 6V4h6v2"/></svg>`,
  approve:   `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>`,
  deprecate: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/></svg>`,
  eye:       `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`,
};

/* ──────────────── NAVIGATION ──────────────── */
let currentSection = 'dashboard';

function navigate(section) {
  document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`section-${section}`)?.classList.add('active');
  document.getElementById(`nav-${section}`)?.classList.add('active');
  const titles = {
    dashboard: 'Dashboard', projects: 'Projects', components: 'Components',
    versions: 'Component Versions', assemblies: 'Assemblies', users: 'Users',
    dependencies: 'Dependencies', compatibility: 'Compatibility', impact: 'Impact Analysis',
  };
  document.getElementById('topbar-title').textContent = titles[section] || section;
  currentSection = section;
  loadSection(section);
}

function loadSection(section) {
  switch (section) {
    case 'dashboard':    loadDashboard();    break;
    case 'projects':     loadProjects();     break;
    case 'components':   loadComponents();   break;
    case 'assemblies':   populateProjectSelect(); break;
    case 'versions':     populateVersionComponentSelect(); break;
    case 'users':        loadUsers();        break;
    case 'dependencies':
      populateDependencyVersionSelect();
      break;
    case 'compatibility':
      loadCompatRules();
      populateDependencyVersionSelect();
      break;
  }
}

/* ──────────────── API STATUS CHECK ──────────────── */
async function checkApiStatus() {
  const dot  = document.getElementById('status-dot');
  const text = document.getElementById('status-text');
  try {
    await API.projects.list();
    dot.className  = 'status-dot online';
    text.textContent = 'API Online';
  } catch {
    dot.className  = 'status-dot offline';
    text.textContent = 'API Offline';
  }
}

/* ════════════════════════════════════════════════════
   DASHBOARD
   ════════════════════════════════════════════════════ */
async function loadDashboard() {
  try {
    const [projects, components, users] = await Promise.all([
      API.projects.list(),
      API.components.list(),
      API.users.list(),
    ]);

    document.getElementById('stat-val-projects').textContent   = projects.length;
    document.getElementById('stat-val-components').textContent = components.length;
    document.getElementById('stat-val-users').textContent      = users.length;

    // Assemblies count (sum across all projects)
    let totalAssemblies = 0;
    await Promise.all(projects.map(async p => {
      try {
        const a = await API.assemblies.listByProject(p.id);
        totalAssemblies += a.length;
      } catch {}
    }));
    document.getElementById('stat-val-assemblies').textContent = totalAssemblies;

    // Recent projects list
    const pList = document.getElementById('dashboard-projects-list');
    if (!projects.length) {
      pList.innerHTML = emptyState('No projects yet', '📁');
    } else {
      pList.innerHTML = projects.slice(0, 6).map(p => `
        <div class="dash-project-item">
          <div>
            <div class="dash-project-name">${esc(p.name)}</div>
          </div>
          ${projectStatusBadge(p.status)}
        </div>`).join('');
    }

    // Components by type bar chart
    const mech = components.filter(c => c.componentType === 'MECHANICAL').length;
    const elec = components.filter(c => c.componentType === 'ELECTRICAL').length;
    const soft = components.filter(c => c.componentType === 'SOFTWARE').length;
    const max  = Math.max(mech, elec, soft, 1);
    document.getElementById('type-bars').innerHTML = [
      { label: 'Mechanical', count: mech, cls: 'mechanical' },
      { label: 'Electrical', count: elec, cls: 'electrical' },
      { label: 'Software',   count: soft, cls: 'software'   },
    ].map(({ label, count, cls }) => `
      <div class="type-bar-row">
        <span class="type-bar-label">${label}</span>
        <div class="type-bar-track">
          <div class="type-bar-fill ${cls}" style="width:${(count/max)*100}%"></div>
        </div>
        <span class="type-bar-count">${count}</span>
      </div>`).join('');

  } catch (err) {
    Toast.error('Dashboard load failed: ' + err.message);
  }
}

/* ════════════════════════════════════════════════════
   PROJECTS
   ════════════════════════════════════════════════════ */
async function loadProjects() {
  const wrapper = document.getElementById('projects-table-wrapper');
  wrapper.innerHTML = loadingState();
  try {
    const data = await API.projects.list();
    if (!data.length) { wrapper.innerHTML = emptyState('No projects yet. Create one!', '📁'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Name</th><th>Description</th><th>Status</th><th>Created</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(p => `
            <tr data-id="${p.id}">
              <td class="td-mono td-muted">#${p.id}</td>
              <td><strong>${esc(p.name)}</strong></td>
              <td class="td-muted">${esc(p.description || '—')}</td>
              <td>${projectStatusBadge(p.status)}</td>
              <td class="td-muted td-mono">${fmtDate(p.createdAt)}</td>
              <td>
                <div class="table-actions">
                  ${actionBtn(ICONS.edit,   'Edit Project')}
                  ${actionBtn(ICONS.delete, 'Delete Project', 'danger')}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    // bind actions
    wrapper.querySelectorAll('tr[data-id]').forEach(row => {
      const id = Number(row.dataset.id);
      const proj = data.find(p => p.id === id);
      row.querySelectorAll('.action-btn')[0].onclick = () => openEditProjectModal(proj);
      row.querySelectorAll('.action-btn')[1].onclick = () => confirmDelete('project', id, loadProjects);
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load projects: ' + err.message);
  }
}

async function openCreateProjectModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  Modal.open({
    title: '➕ New Project',
    body: `
      <div class="form-grid">
        <div class="form-group">
          <label>Project Name *</label>
          <input id="m-proj-name" placeholder="e.g. Drone Assembly v2" />
        </div>
        <div class="form-group">
          <label>Description</label>
          <textarea id="m-proj-desc" placeholder="Short description…"></textarea>
        </div>
        ${creatorField()}
      </div>`,
    submitLabel: 'Create Project',
    onSubmit: async () => {
      const name = Modal.val('m-proj-name');
      const description = Modal.val('m-proj-desc');
      const createdById = selectedCreatorId();
      if (!name) { Toast.warning('Project name is required.'); return; }
      if (!createdById) return;
      await API.projects.create({ name, description, createdById });
      Toast.success('Project created!');
      Modal.close();
      loadProjects();
      if (currentSection === 'dashboard') loadDashboard();
    },
  });
}

async function openEditProjectModal(proj) {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  const statuses = ['PLANNING','ACTIVE','COMPLETED','ARCHIVED'];
  Modal.open({
    title: '✏️ Edit Project',
    body: `
      <div class="form-grid">
        <div class="form-group">
          <label>Project Name *</label>
          <input id="m-proj-name" value="${esc(proj.name)}" />
        </div>
        <div class="form-group">
          <label>Description</label>
          <textarea id="m-proj-desc">${esc(proj.description || '')}</textarea>
        </div>
        <div class="form-group">
          <label>Change Status</label>
          <select id="m-proj-status">
            ${statuses.map(s => `<option value="${s}" ${s===proj.status?'selected':''}>${s}</option>`).join('')}
          </select>
        </div>
        ${creatorField(proj.createdById)}
      </div>`,
    submitLabel: 'Save Changes',
    onSubmit: async () => {
      const name = Modal.val('m-proj-name');
      const description = Modal.val('m-proj-desc');
      const newStatus = Modal.val('m-proj-status');
      const createdById = selectedCreatorId();
      if (!name) { Toast.warning('Name is required.'); return; }
      if (!createdById) return;
      await API.projects.update(proj.id, { name, description, createdById });
      if (newStatus !== proj.status) {
        await API.projects.changeStatus(proj.id, { status: newStatus });
      }
      Toast.success('Project updated!');
      Modal.close();
      loadProjects();
    },
  });
}

/* ════════════════════════════════════════════════════
   COMPONENTS
   ════════════════════════════════════════════════════ */
let _currentComponentFilter = 'ALL';
let _allComponents = [];

async function loadComponents(type = _currentComponentFilter) {
  _currentComponentFilter = type;
  const wrapper = document.getElementById('components-table-wrapper');
  wrapper.innerHTML = loadingState();
  try {
    const data = type === 'ALL' ? await API.components.list() : await API.components.listByType(type);
    _allComponents = data.length ? data : _allComponents;
    if (!data.length) { wrapper.innerHTML = emptyState('No components found.', '📦'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Name</th><th>Code</th><th>Type</th><th>Manufacturer</th><th>Status</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(c => `
            <tr data-id="${c.id}">
              <td class="td-mono td-muted">#${c.id}</td>
              <td><strong>${esc(c.name)}</strong><br><span class="td-muted" style="font-size:11px">${esc(c.description||'')}</span></td>
              <td class="td-mono">${esc(c.componentCode||'—')}</td>
              <td>${componentTypeBadge(c.componentType)}</td>
              <td class="td-muted">${esc(c.manufacturer||'—')}</td>
              <td>${badge(c.status||'ACTIVE', c.status==='DEPRECATED'?'deprecated':'approved')}</td>
              <td>
                <div class="table-actions">
                  ${actionBtn(ICONS.edit,      'Edit Component')}
                  ${actionBtn(ICONS.deprecate, 'Deprecate Component', 'danger')}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    wrapper.querySelectorAll('tr[data-id]').forEach(row => {
      const id = Number(row.dataset.id);
      const comp = data.find(c => c.id === id);
      row.querySelectorAll('.action-btn')[0].onclick = () => openEditComponentModal(comp);
      row.querySelectorAll('.action-btn')[1].onclick = () => deprecateComponent(comp);
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load components: ' + err.message);
  }
}

async function openCreateComponentModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  Modal.open({
    title: '➕ New Component',
    body: `
      <div class="form-grid form-grid-2">
        <div class="form-group">
          <label>Name *</label>
          <input id="m-comp-name" placeholder="e.g. Servo Motor 200N" />
        </div>
        <div class="form-group">
          <label>Component Code *</label>
          <input id="m-comp-code" placeholder="e.g. MECH-MOTOR-001" />
        </div>
        <div class="form-group">
          <label>Type *</label>
          <select id="m-comp-type">
            <option value="MECHANICAL">⚙️ Mechanical</option>
            <option value="ELECTRICAL">⚡ Electrical</option>
            <option value="SOFTWARE">💻 Software</option>
          </select>
        </div>
        <div class="form-group">
          <label>Manufacturer</label>
          <input id="m-comp-mfr" placeholder="e.g. Bosch" />
        </div>
      </div>
      <div class="form-group">
        <label>Description</label>
        <textarea id="m-comp-desc" placeholder="Brief description…"></textarea>
      </div>
      ${creatorField()}`,
    submitLabel: 'Create Component',
    onSubmit: async () => {
      const name          = Modal.val('m-comp-name');
      const componentCode = Modal.val('m-comp-code');
      const componentType = Modal.val('m-comp-type');
      const manufacturer  = Modal.val('m-comp-mfr');
      const description   = Modal.val('m-comp-desc');
      const createdById = selectedCreatorId();
      if (!name || !componentCode) { Toast.warning('Name and code are required.'); return; }
      if (!createdById) return;
      await API.components.create({ name, componentCode, componentType, manufacturer, description, createdById });
      Toast.success('Component created!');
      Modal.close();
      loadComponents();
      populateVersionComponentSelect();
    },
  });
}

function openEditComponentModal(comp) {
  Modal.open({
    title: '✏️ Edit Component',
    body: `
      <div class="form-grid form-grid-2">
        <div class="form-group">
          <label>Name *</label>
          <input id="m-comp-name" value="${esc(comp.name)}" />
        </div>
        <div class="form-group">
          <label>Manufacturer</label>
          <input id="m-comp-mfr" value="${esc(comp.manufacturer||'')}" />
        </div>
      </div>
      <div class="form-group">
        <label>Description</label>
        <textarea id="m-comp-desc">${esc(comp.description||'')}</textarea>
      </div>
      <p class="hint-text" style="margin-top:4px">⚠️ Component code and type are immutable after creation.</p>`,
    submitLabel: 'Save Changes',
    onSubmit: async () => {
      const name         = Modal.val('m-comp-name');
      const manufacturer = Modal.val('m-comp-mfr');
      const description  = Modal.val('m-comp-desc');
      if (!name) { Toast.warning('Name is required.'); return; }
      await API.components.update(comp.id, {
        name, manufacturer, description,
        componentCode: comp.componentCode,
        componentType: comp.componentType,
        createdById: comp.createdById,
      });
      Toast.success('Component updated!');
      Modal.close();
      loadComponents();
    },
  });
}

async function deprecateComponent(comp) {
  if (!confirm(`Deprecate "${comp.name}"? No new versions can be created.`)) return;
  try {
    await API.components.deprecate(comp.id);
    Toast.success('Component deprecated.');
    loadComponents();
  } catch (err) { Toast.error(err.message); }
}

/* ════════════════════════════════════════════════════
   VERSIONS
   ════════════════════════════════════════════════════ */
async function populateVersionComponentSelect() {
  try {
    const comps = await API.components.list();
    _allComponents = comps;
    const sel = document.getElementById('version-component-select');
    const previousValue = sel.value;
    sel.innerHTML = '<option value="">— choose a component —</option>' +
      comps.map(c => `<option value="${c.id}">${esc(c.name)} (${c.componentCode})</option>`).join('');
    if (comps.some(c => String(c.id) === previousValue)) sel.value = previousValue;
    if (currentSection === 'versions' && sel.value) loadVersions(sel.value);
  } catch (err) {
    Toast.error(`Failed to load components: ${err.message}`);
  }
}

async function loadVersions(componentId) {
  const wrapper = document.getElementById('versions-table-wrapper');
  if (!componentId) { wrapper.innerHTML = '<div class="empty-state">Select a component above.</div>'; return; }
  wrapper.innerHTML = loadingState();
  try {
    const data = await API.versions.list(componentId);
    if (!data.length) { wrapper.innerHTML = emptyState('No versions yet. Create one!', '🔖'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Version</th><th>Status</th><th>Release Notes</th><th>Created</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(v => `
            <tr data-vid="${v.id}" data-cid="${componentId}">
              <td class="td-mono td-muted">#${v.id}</td>
              <td class="td-mono"><strong>${esc(v.versionNumber)}</strong></td>
              <td>${versionStatusBadge(v.status)}</td>
              <td class="td-muted">${esc(v.releaseNotes||'—')}</td>
              <td class="td-muted td-mono">${fmtDate(v.createdAt)}</td>
              <td>
                <div class="table-actions">
                  ${v.status === 'DRAFT'     ? actionBtn(ICONS.approve,   'Approve Version',   'success') : ''}
                  ${v.status !== 'DEPRECATED'? actionBtn(ICONS.deprecate, 'Deprecate Version', 'danger')  : ''}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    wrapper.querySelectorAll('tr[data-vid]').forEach(row => {
      const vid = Number(row.dataset.vid);
      const cid = Number(row.dataset.cid);
      const ver = data.find(v => v.id === vid);
      const btns = row.querySelectorAll('.action-btn');
      let idx = 0;
      if (ver.status === 'DRAFT') {
        btns[idx++].onclick = () => approveVersion(cid, vid);
      }
      if (ver.status !== 'DEPRECATED' && btns[idx]) {
        btns[idx].onclick = () => deprecateVersion(cid, vid);
      }
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load versions: ' + err.message);
  }
}

async function openCreateVersionModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  const sel = document.getElementById('version-component-select');
  const selectedCid = sel.value;
  Modal.open({
    title: '➕ New Component Version',
    body: `
      <div class="form-grid">
        <div class="form-group">
          <label>Component *</label>
          <select id="m-ver-comp">
            <option value="">— select —</option>
            ${_allComponents.map(c=>`<option value="${c.id}" ${c.id==selectedCid?'selected':''}>${esc(c.name)}</option>`).join('')}
          </select>
        </div>
        <div class="form-group">
          <label>Version Number *</label>
          <input id="m-ver-num" placeholder="e.g. v1.2.0" />
        </div>
        <div class="form-group">
          <label>Release Notes</label>
          <textarea id="m-ver-notes" placeholder="What changed in this version?"></textarea>
        </div>
        ${creatorField()}
      </div>`,
    submitLabel: 'Create Version',
    onSubmit: async () => {
      const componentId   = Modal.val('m-ver-comp');
      const versionNumber = Modal.val('m-ver-num');
      const releaseNotes  = Modal.val('m-ver-notes');
      const createdById = selectedCreatorId();
      if (!componentId || !versionNumber) { Toast.warning('Component and version number are required.'); return; }
      if (!createdById) return;
      await API.versions.create(componentId, { versionNumber, releaseNotes, createdById });
      Toast.success('Version created as DRAFT!');
      Modal.close();
      if (sel.value == componentId) loadVersions(componentId);
      await populateDependencyVersionSelect();
    },
  });
}

async function approveVersion(cId, vId) {
  if (!confirm('Approve this version? It will be available for production assemblies.')) return;
  try {
    await API.versions.approve(cId, vId);
    Toast.success('Version approved!');
    loadVersions(cId);
    await populateDependencyVersionSelect();
  } catch (err) { Toast.error(err.message); }
}

async function deprecateVersion(cId, vId) {
  if (!confirm('Deprecate this version?')) return;
  try {
    await API.versions.deprecate(cId, vId);
    Toast.success('Version deprecated.');
    loadVersions(cId);
    await populateDependencyVersionSelect();
  } catch (err) { Toast.error(err.message); }
}

/* ════════════════════════════════════════════════════
   ASSEMBLIES
   ════════════════════════════════════════════════════ */
async function populateProjectSelect() {
  try {
    const projects = await API.projects.list();
    const sel = document.getElementById('assembly-project-select');
    const previousValue = sel.value;
    sel.innerHTML = '<option value="">— choose a project —</option>' +
      projects.map(p => `<option value="${p.id}">${esc(p.name)}</option>`).join('');
    if (projects.some(p => String(p.id) === previousValue)) sel.value = previousValue;
    if (currentSection === 'assemblies' && sel.value) loadAssemblies(sel.value);
  } catch (err) {
    Toast.error(`Failed to load projects: ${err.message}`);
  }
}

async function loadAssemblies(projectId) {
  const wrapper = document.getElementById('assemblies-table-wrapper');
  if (!projectId) { wrapper.innerHTML = '<div class="empty-state">Select a project above.</div>'; return; }
  wrapper.innerHTML = loadingState();
  try {
    const data = await API.assemblies.listByProject(projectId);
    if (!data.length) { wrapper.innerHTML = emptyState('No assemblies in this project.', '🏗️'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Name</th><th>Description</th><th>Components</th><th>Created</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(a => `
            <tr data-id="${a.id}">
              <td class="td-mono td-muted">#${a.id}</td>
              <td><strong>${esc(a.name)}</strong></td>
              <td class="td-muted">${esc(a.description||'—')}</td>
              <td><span class="badge badge-approved">${(a.components||[]).length} components</span></td>
              <td class="td-muted td-mono">${fmtDate(a.createdAt)}</td>
              <td>
                <div class="table-actions">
                  ${actionBtn(ICONS.eye,    'View Assembly Details')}
                  ${actionBtn(ICONS.delete, 'Delete Assembly', 'danger')}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    wrapper.querySelectorAll('tr[data-id]').forEach(row => {
      const id = Number(row.dataset.id);
      const asm = data.find(a => a.id === id);
      row.querySelectorAll('.action-btn')[0].onclick = () => viewAssemblyDetails(asm, projectId);
      row.querySelectorAll('.action-btn')[1].onclick = () => confirmDelete('assembly', id, () => loadAssemblies(projectId));
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load assemblies: ' + err.message);
  }
}

async function viewAssemblyDetails(asm, projectId) {
  if (!_allVersionsFlat.length) await populateDependencyVersionSelect();
  try {
    viewAssemblyDetailsModal(await API.assemblies.get(asm.id), projectId);
  } catch (err) {
    Toast.error(`Failed to load assembly details: ${err.message}`);
  }
}

function viewAssemblyDetailsModal(asm, projectId) {
  const components = asm.components || [];
  const approvedVersions = _allVersionsFlat.filter(v => v.status === 'APPROVED');
  const canAddComponents = approvedVersions.length > 0;
  Modal.open({
    title: `🏗️ ${esc(asm.name)}`,
    body: `
      <p class="hint-text">${esc(asm.description||'No description.')}</p>
      <div class="card-header" style="padding:10px 0;border:none"><h3>Bill of Materials (${components.length} components)</h3></div>
      ${!components.length ? emptyState('No components in this assembly.','📦') : `
        <div class="table-wrapper">
          <table>
            <thead><tr><th>Component</th><th>Version</th><th>Quantity</th><th>Notes</th><th>Actions</th></tr></thead>
            <tbody>
              ${components.map(item=>`
                <tr>
                  <td><strong>${esc(item.componentName || item.componentCode || `Component #${item.componentId}`)}</strong></td>
                  <td class="td-mono">${esc(item.versionNumber || `#${item.componentVersionId}`)} ${versionStatusBadge(item.versionStatus || 'APPROVED')}</td>
                  <td>${item.quantity}</td>
                  <td class="td-muted">${esc(item.notes || '—')}</td>
                  <td><button type="button" class="action-btn danger" data-remove-version="${item.componentVersionId}" title="Remove component">${ICONS.delete}</button></td>
                </tr>`).join('')}
            </tbody>
          </table>
        </div>`}
      ${canAddComponents ? `
        <div class="bom-add-form">
          <h3>Add approved component version</h3>
          <div class="form-group">
            <label for="m-asm-component-version">Component version</label>
            <select id="m-asm-component-version">
              <option value="">— choose a version —</option>
              ${approvedVersions.map(v=>`<option value="${v.id}">${esc(v.componentName)} — ${esc(v.versionNumber)}</option>`).join('')}
            </select>
          </div>
          <div class="form-grid form-grid-2">
            <div class="form-group"><label for="m-asm-component-quantity">Quantity</label><input id="m-asm-component-quantity" type="number" min="1" value="1" /></div>
            <div class="form-group"><label for="m-asm-component-notes">Notes</label><input id="m-asm-component-notes" maxlength="1000" /></div>
          </div>
        </div>` : '<p class="hint-text">No approved component versions are available to add.</p>'}`,
    submitLabel: canAddComponents ? 'Add Component' : 'Close',
    onSubmit: async () => {
      if (!canAddComponents) { Modal.close(); return; }
      const componentVersionId = Number(Modal.val('m-asm-component-version'));
      const quantity = Number(Modal.val('m-asm-component-quantity'));
      const notes = Modal.val('m-asm-component-notes');
      if (!componentVersionId) { Toast.warning('Choose an approved component version.'); return; }
      if (!Number.isInteger(quantity) || quantity < 1) { Toast.warning('Quantity must be at least 1.'); return; }
      const updated = await API.assemblies.addComponent(asm.id, { componentVersionId, quantity, notes });
      Toast.success('Component added to assembly.');
      loadAssemblies(projectId);
      viewAssemblyDetailsModal(updated, projectId);
    },
  });
  document.querySelectorAll('[data-remove-version]').forEach(button => {
    button.addEventListener('click', async () => {
      const versionId = Number(button.dataset.removeVersion);
      if (!confirm('Remove this component version from the assembly?')) return;
      try {
        await API.assemblies.removeComponent(asm.id, versionId);
        Toast.success('Component removed from assembly.');
        const updated = await API.assemblies.get(asm.id);
        loadAssemblies(projectId);
        viewAssemblyDetailsModal(updated, projectId);
      } catch (err) {
        Toast.error(`Failed to remove component: ${err.message}`);
      }
    });
  });
}

async function openCreateAssemblyModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  const sel = document.getElementById('assembly-project-select');
  const selectedPid = sel.value;
  Modal.open({
    title: '➕ New Assembly',
    body: `
      <div class="form-group">
        <label>Project *</label>
        <select id="m-asm-proj">
          <option value="">— select —</option>
          ${Array.from(sel.options).slice(1).map(o=>`<option value="${o.value}" ${o.value==selectedPid?'selected':''}>${o.text}</option>`).join('')}
        </select>
      </div>
      <div class="form-group">
        <label>Assembly Name *</label>
        <input id="m-asm-name" placeholder="e.g. Fuselage Assembly Rev A" />
      </div>
      <div class="form-group">
        <label>Description</label>
        <textarea id="m-asm-desc" placeholder="Assembly description…"></textarea>
      </div>
      ${creatorField()}`,
    submitLabel: 'Create Assembly',
    onSubmit: async () => {
      const projectId   = Modal.val('m-asm-proj');
      const name        = Modal.val('m-asm-name');
      const description = Modal.val('m-asm-desc');
      const createdById = selectedCreatorId();
      if (!projectId || !name) { Toast.warning('Project and name are required.'); return; }
      if (!createdById) return;
      await API.assemblies.create(projectId, { name, description, createdById });
      Toast.success('Assembly created!');
      Modal.close();
      if (sel.value == projectId) loadAssemblies(projectId);
    },
  });
}

/* ════════════════════════════════════════════════════
   USERS
   ════════════════════════════════════════════════════ */
async function loadUsers() {
  const wrapper = document.getElementById('users-table-wrapper');
  wrapper.innerHTML = loadingState();
  try {
    const data = await API.users.list();
    if (!data.length) { wrapper.innerHTML = emptyState('No users yet.', '👤'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Username</th><th>Email</th><th>Full Name</th><th>Role</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(u => `
            <tr data-id="${u.id}">
              <td class="td-mono td-muted">#${u.id}</td>
              <td class="td-mono"><strong>${esc(u.username)}</strong></td>
              <td class="td-muted">${esc(u.email)}</td>
              <td>${esc(u.fullName||'—')}</td>
              <td>${roleBadge(u.role)}</td>
              <td>
                <div class="table-actions">
                  ${actionBtn(ICONS.delete, 'Delete User', 'danger')}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    wrapper.querySelectorAll('tr[data-id]').forEach(row => {
      const id = Number(row.dataset.id);
      row.querySelector('.action-btn').onclick = () => confirmDelete('user', id, loadUsers);
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load users: ' + err.message);
  }
}

function openCreateUserModal() {
  Modal.open({
    title: '➕ New User',
    body: `
      <div class="form-grid form-grid-2">
        <div class="form-group">
          <label>Username *</label>
          <input id="m-usr-uname" placeholder="john_doe" />
        </div>
        <div class="form-group">
          <label>Email *</label>
          <input id="m-usr-email" type="email" placeholder="john@example.com" />
        </div>
        <div class="form-group">
          <label>Password *</label>
          <input id="m-usr-password" type="password" minlength="8" autocomplete="new-password" />
        </div>
        <div class="form-group">
          <label>Role *</label>
          <select id="m-usr-role">
            <option value="ENGINEER">Engineer</option>
            <option value="ADMIN">Admin</option>
            <option value="VIEWER">Viewer</option>
          </select>
        </div>
      </div>`,
    submitLabel: 'Create User',
    onSubmit: async () => {
      const username = Modal.val('m-usr-uname');
      const email    = Modal.val('m-usr-email');
      const password = Modal.val('m-usr-password');
      const role     = Modal.val('m-usr-role');
      if (!username || !email) { Toast.warning('Username and email are required.'); return; }
      if (password.length < 8) { Toast.warning('Password must be at least 8 characters.'); return; }
      await API.users.create({ username, email, password, role });
      Toast.success('User created!');
      Modal.close();
      _usersForForms = await API.users.list();
      loadUsers();
    },
  });
}

/* ════════════════════════════════════════════════════
   DEPENDENCIES
   ════════════════════════════════════════════════════ */
let _allVersionsFlat = [];

async function populateDependencyVersionSelect() {
  const sel = document.getElementById('dep-version-select');
  try {
    const comps = await API.components.list();
    const versionGroups = await Promise.all(comps.map(c => API.versions.list(c.id)));
    const allVersions = comps.flatMap((component, index) =>
      versionGroups[index].map(version => ({
        ...version,
        componentName: component.name,
        componentId: component.id,
      }))
    );
    _allVersionsFlat = allVersions;
    const previousValue = sel.value;
    sel.innerHTML = '<option value="">— choose a component version —</option>' +
      allVersions.map(v => `<option value="${v.id}">${esc(v.componentName)} — ${esc(v.versionNumber)} [${v.status}]</option>`).join('');
    if (allVersions.some(v => String(v.id) === previousValue)) sel.value = previousValue;
    if (currentSection === 'dependencies' && sel.value) loadDependencies(sel.value);
  } catch (err) {
    _allVersionsFlat = [];
    sel.innerHTML = '<option value="">Version list unavailable</option>';
    Toast.error(`Failed to load component versions: ${err.message}`);
  }
}

async function loadDependencies(versionId) {
  const wrapper = document.getElementById('deps-table-wrapper');
  const graph = document.getElementById('dependency-graph');
  if (!versionId) {
    wrapper.innerHTML = '<div class="empty-state">Select a component version above.</div>';
    graph.innerHTML = '<div class="empty-state">Select a component version to view its dependency graph.</div>';
    return;
  }
  wrapper.innerHTML = loadingState();
  graph.innerHTML = loadingState();
  try {
    const data = await API.dependencies.listByVersion(versionId);
    renderDependencyGraph(graph, versionId, data);
    if (!data.length) { wrapper.innerHTML = emptyState('No dependencies declared.', '🔗'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Source Version</th><th>Type</th><th>Target Version</th><th>Description</th><th>Actions</th>
        </tr></thead>
        <tbody>
          ${data.map(d => `
            <tr data-id="${d.id}">
              <td class="td-mono td-muted">#${d.id}</td>
              <td><strong>${esc(d.sourceComponentName || `Version #${d.sourceVersionId || versionId}`)}</strong><br><span class="td-muted td-mono">${esc(d.sourceVersionNumber || '')}</span></td>
              <td>${depTypeBadge(d.dependencyType)}</td>
              <td><strong>${esc(d.targetComponentName || `Version #${d.targetVersionId}`)}</strong><br><span class="td-muted td-mono">${esc(d.targetVersionNumber || '')}</span></td>
              <td class="td-muted">${esc(d.description || '—')}</td>
              <td>
                <div class="table-actions">
                  ${actionBtn(ICONS.delete, 'Delete Dependency', 'danger')}
                </div>
              </td>
            </tr>`).join('')}
        </tbody>
      </table>`;
    wrapper.querySelectorAll('tr[data-id]').forEach(row => {
      const id = Number(row.dataset.id);
      row.querySelector('.action-btn').onclick = async () => {
        if (!confirm('Remove this dependency?')) return;
        try { await API.dependencies.delete(id); Toast.success('Dependency removed.'); loadDependencies(versionId); }
        catch (err) { Toast.error(err.message); }
      };
    });
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load dependencies: ' + err.message);
    graph.innerHTML = errorState('Failed to load dependency graph: ' + err.message);
  }
}

function renderDependencyGraph(container, versionId, dependencies) {
  const selectedVersion = _allVersionsFlat.find(version => String(version.id) === String(versionId));
  const sourceName = selectedVersion
    ? `${selectedVersion.componentName} — ${selectedVersion.versionNumber}`
    : `Version #${versionId}`;
  if (!dependencies.length) {
    container.innerHTML = `<div class="dependency-graph-empty">
      <div class="dependency-node">${esc(sourceName)}<span>Selected version</span></div>
      <p>No outgoing dependencies declared.</p>
    </div>`;
    return;
  }

  container.innerHTML = `<div class="dependency-graph-flow">
    <div class="dependency-node dependency-node-source">${esc(sourceName)}<span>Selected version</span></div>
    <div class="dependency-edges">
      ${dependencies.map(dependency => `
        <div class="dependency-edge">
          <span class="dependency-edge-type">${badge(esc(dependency.dependencyType?.replace('_', ' ') || 'Dependency'), dependency.dependencyType === 'CONFLICTS_WITH' ? 'conflicts' : 'requires')}</span>
          <span class="dependency-edge-arrow" aria-hidden="true">→</span>
          <div class="dependency-node">${esc(dependency.targetComponentName || `Version #${dependency.targetVersionId}`)}<span>${esc(dependency.targetVersionNumber || `Version #${dependency.targetVersionId}`)}</span></div>
        </div>`).join('')}
    </div>
  </div>`;
}

async function openCreateDependencyModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  if (!_allVersionsFlat.length) await populateDependencyVersionSelect();
  if (!_allVersionsFlat.length) {
    Toast.warning('No component versions are available for a dependency.');
    return;
  }
  const versionOptions = _allVersionsFlat.map(v =>
    `<option value="${v.id}">${esc(v.componentName)} — ${esc(v.versionNumber)} [${v.status}]</option>`).join('');
  Modal.open({
    title: '➕ Declare Dependency',
    body: `
      <div class="form-grid">
        <div class="form-group">
          <label>Source Version (depends ON) *</label>
          <select id="m-dep-src">${versionOptions}</select>
        </div>
        <div class="form-group">
          <label>Dependency Type *</label>
          <select id="m-dep-type">
            <option value="REQUIRES">REQUIRES</option>
            <option value="RECOMMENDS">RECOMMENDS</option>
            <option value="CONFLICTS_WITH">CONFLICTS_WITH</option>
          </select>
        </div>
        <div class="form-group">
          <label>Target Version *</label>
          <select id="m-dep-tgt">${versionOptions}</select>
        </div>
      </div>
      ${creatorField()}`,
    submitLabel: 'Declare Dependency',
    onSubmit: async () => {
      const sourceVersionId  = Number(Modal.val('m-dep-src'));
      const targetVersionId  = Number(Modal.val('m-dep-tgt'));
      const dependencyType   = Modal.val('m-dep-type');
      const createdById = selectedCreatorId();
      if (!sourceVersionId || !targetVersionId) { Toast.warning('Both versions are required.'); return; }
      if (!createdById) return;
      if (sourceVersionId === targetVersionId) { Toast.warning('A version cannot depend on itself.'); return; }
      await API.dependencies.create({ sourceVersionId, targetVersionId, dependencyType, createdById });
      Toast.success('Dependency declared!');
      Modal.close();
      const sel = document.getElementById('dep-version-select');
      if (!sel.value) sel.value = String(sourceVersionId);
      loadDependencies(sel.value);
    },
  });
}

/* ════════════════════════════════════════════════════
   COMPATIBILITY
   ════════════════════════════════════════════════════ */
async function loadCompatRules() {
  const wrapper = document.getElementById('compat-rules-wrapper');
  wrapper.innerHTML = loadingState();
  try {
    const data = await API.compatibility.rules();
    if (!data.length) { wrapper.innerHTML = emptyState('No compatibility rules declared.', '🔗'); return; }
    wrapper.innerHTML = `
      <table>
        <thead><tr>
          <th>ID</th><th>Version A</th><th>Rule</th><th>Version B</th><th>Reason</th>
        </tr></thead>
        <tbody>
          ${data.map(r => `
            <tr>
              <td class="td-mono td-muted">#${r.id}</td>
              <td class="td-mono">${esc(r.versionAComponentCode || '')} ${esc(r.versionANumber || `#${r.versionAId}`)}</td>
              <td>${badge(r.compatible ? 'COMPATIBLE' : 'INCOMPATIBLE', r.compatible ? 'compatible' : 'incompatible')}</td>
              <td class="td-mono">${esc(r.versionBComponentCode || '')} ${esc(r.versionBNumber || `#${r.versionBId}`)}</td>
              <td class="td-muted">${esc(r.ruleDescription || '—')}</td>
            </tr>`).join('')}
        </tbody>
      </table>`;
  } catch (err) {
    wrapper.innerHTML = errorState('Failed to load rules: ' + err.message);
  }
}

async function runCompatCheck() {
  const raw = document.getElementById('compat-check-ids').value.trim();
  const ids = raw.split(',').map(s => Number(s.trim())).filter(Boolean);
  if (ids.length < 2) { Toast.warning('Enter at least 2 version IDs.'); return; }
  const result = document.getElementById('compat-check-result');
  result.innerHTML = loadingState();
  try {
    const data = await API.compatibility.check({ componentVersionIds: ids });
    if (data.allCompatible) {
      result.innerHTML = `<div class="compat-result compatible">
        <h4>✅ All Compatible</h4>
        <p>The ${ids.length} selected versions are mutually compatible.</p>
      </div>`;
    } else {
      const violations = data.violations || [];
      result.innerHTML = `<div class="compat-result incompatible">
        <h4>❌ Incompatibilities Found</h4>
        <p>${violations.length} violation(s) detected:</p>
        <ul class="compat-violations">
          ${violations.map(v => `<li>Version ${v.versionAId} ↔ Version ${v.versionBId}: ${esc(v.reason||'INCOMPATIBLE')}</li>`).join('')}
        </ul>
      </div>`;
    }
  } catch (err) {
    result.innerHTML = `<div class="compat-result incompatible"><h4>Error</h4><p>${esc(err.message)}</p></div>`;
  }
}

async function openCreateCompatRuleModal() {
  try {
    if (!await loadCreatorUsers()) return;
  } catch (err) {
    Toast.error(`Failed to load users: ${err.message}`);
    return;
  }
  if (!_allVersionsFlat.length) await populateDependencyVersionSelect();
  if (!_allVersionsFlat.length) {
    Toast.warning('No component versions are available for a compatibility rule.');
    return;
  }
  const versionOptions = _allVersionsFlat.map(v =>
    `<option value="${v.id}">${esc(v.componentName)} — ${esc(v.versionNumber)}</option>`).join('');
  Modal.open({
    title: '➕ New Compatibility Rule',
    body: `
      <div class="form-grid">
        <div class="form-group">
          <label>Version A *</label>
          <select id="m-cr-a">${versionOptions}</select>
        </div>
        <div class="form-group">
          <label>Rule Type *</label>
          <select id="m-cr-type">
            <option value="COMPATIBLE">COMPATIBLE</option>
            <option value="INCOMPATIBLE">INCOMPATIBLE</option>
          </select>
        </div>
        <div class="form-group">
          <label>Version B *</label>
          <select id="m-cr-b">${versionOptions}</select>
        </div>
        <div class="form-group">
          <label>Reason for this rule *</label>
          <textarea id="m-cr-description" minlength="10" maxlength="2000" placeholder="Explain why these versions are compatible or incompatible."></textarea>
        </div>
      </div>
      ${creatorField()}
      <p class="hint-text">Rules are symmetric — declaring (A,B) covers both directions.</p>`,
    submitLabel: 'Create Rule',
    onSubmit: async () => {
      const versionAId = Number(Modal.val('m-cr-a'));
      const versionBId = Number(Modal.val('m-cr-b'));
      const ruleType   = Modal.val('m-cr-type');
      const ruleDescription = Modal.val('m-cr-description');
      const createdById = selectedCreatorId();
      if (!versionAId || !versionBId) { Toast.warning('Both versions are required.'); return; }
      if (!createdById) return;
      if (versionAId === versionBId) { Toast.warning('Choose two different component versions.'); return; }
      if (ruleDescription.length < 10) { Toast.warning('Explain the rule in at least 10 characters.'); return; }
      await API.compatibility.create({
        versionAId,
        versionBId,
        isCompatible: ruleType === 'COMPATIBLE',
        ruleDescription,
        createdById,
      });
      Toast.success('Compatibility rule created!');
      Modal.close();
      loadCompatRules();
    },
  });
}

/* ════════════════════════════════════════════════════
   IMPACT ANALYSIS
   ════════════════════════════════════════════════════ */
async function runImpactByVersion() {
  const id = document.getElementById('impact-version-id').value.trim();
  if (!id) { Toast.warning('Enter a version ID.'); return; }
  const result = document.getElementById('impact-version-result');
  result.innerHTML = loadingState();
  try {
    const data = await API.impact.byVersion(id);
    renderImpactResult(result, data);
  } catch (err) {
    result.innerHTML = errorState(err.message);
  }
}

async function runImpactByComponent() {
  const id = document.getElementById('impact-component-id').value.trim();
  if (!id) { Toast.warning('Enter a component ID.'); return; }
  const result = document.getElementById('impact-component-result');
  result.innerHTML = loadingState();
  try {
    const data = await API.impact.byComponent(id);
    renderImpactResult(result, data);
  } catch (err) {
    result.innerHTML = errorState(err.message);
  }
}

function renderImpactResult(container, data) {
  const assemblies = data.affectedAssemblies || data.assemblies || [];
  if (!assemblies.length) {
    container.innerHTML = `<div class="impact-result">
      <div class="empty-state" style="padding:20px"><span class="state-icon">✅</span>No assemblies affected.</div>
    </div>`;
    return;
  }
  container.innerHTML = `<div class="impact-result">
    <p style="color:var(--accent-amber);font-weight:600;margin-bottom:8px">⚠️ ${assemblies.length} affected assembly(ies):</p>
    <div class="impact-assembly-list">
      ${assemblies.map(a => `
        <div class="impact-assembly-item">
          <strong>${esc(a.name||'Assembly #'+a.id)}</strong><br>
          <span>ID: #${a.id} ${a.description ? '· ' + esc(a.description) : ''}</span>
        </div>`).join('')}
    </div>
  </div>`;
}

/* ════════════════════════════════════════════════════
   SHARED UTILITIES
   ════════════════════════════════════════════════════ */
function esc(str) {
  if (str == null) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function fmtDate(dateStr) {
  if (!dateStr) return '—';
  try {
    return new Date(dateStr).toLocaleDateString('en-IN', { day:'2-digit', month:'short', year:'numeric' });
  } catch { return dateStr; }
}

async function confirmDelete(type, id, onSuccess) {
  if (!confirm(`Delete this ${type}? This cannot be undone.`)) return;
  try {
    switch (type) {
      case 'project':  await API.projects.delete(id);  break;
      case 'assembly': await API.assemblies.delete(id); break;
      case 'user':     await API.users.delete(id);     break;
    }
    Toast.success(`${type.charAt(0).toUpperCase()+type.slice(1)} deleted.`);
    onSuccess?.();
    if (currentSection === 'dashboard') loadDashboard();
  } catch (err) { Toast.error(err.message); }
}

/* ════════════════════════════════════════════════════
   INIT
   ════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  Modal.init();

  const apiBaseUrlInput = document.getElementById('api-base-url');
  const savedApiBaseUrl = localStorage.getItem('edcms-api-base-url');
  if (savedApiBaseUrl) apiBaseUrlInput.value = savedApiBaseUrl;

  // Sidebar navigation
  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => navigate(btn.dataset.section));
  });

  // Sidebar toggle
  document.getElementById('sidebar-toggle').addEventListener('click', () => {
    document.getElementById('sidebar').classList.toggle('collapsed');
  });

  // Refresh
  document.getElementById('refresh-btn').addEventListener('click', () => {
    loadSection(currentSection);
    Toast.info('Refreshed!');
  });

  // Project CRUD
  document.getElementById('btn-create-project').addEventListener('click', openCreateProjectModal);

  // Component CRUD
  document.getElementById('btn-create-component').addEventListener('click', openCreateComponentModal);
  document.querySelectorAll('.filter-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      loadComponents(btn.dataset.type);
    });
  });

  // Version management
  document.getElementById('btn-create-version').addEventListener('click', openCreateVersionModal);
  document.getElementById('btn-load-versions').addEventListener('click', () => {
    const v = document.getElementById('version-component-select').value;
    loadVersions(v);
  });

  // Assembly management
  document.getElementById('btn-create-assembly').addEventListener('click', openCreateAssemblyModal);
  document.getElementById('btn-load-assemblies').addEventListener('click', () => {
    const p = document.getElementById('assembly-project-select').value;
    loadAssemblies(p);
  });

  // User management
  document.getElementById('btn-create-user').addEventListener('click', openCreateUserModal);

  // Dependencies
  document.getElementById('btn-create-dependency').addEventListener('click', openCreateDependencyModal);
  document.getElementById('btn-load-deps').addEventListener('click', () => {
    const v = document.getElementById('dep-version-select').value;
    loadDependencies(v);
  });

  // Compatibility
  document.getElementById('btn-create-compat-rule').addEventListener('click', openCreateCompatRuleModal);
  document.getElementById('btn-run-compat-check').addEventListener('click', runCompatCheck);

  // Impact Analysis
  document.getElementById('btn-impact-version').addEventListener('click', runImpactByVersion);
  document.getElementById('btn-impact-component').addEventListener('click', runImpactByComponent);

  // API URL change → re-check
  apiBaseUrlInput.addEventListener('change', () => {
    localStorage.setItem('edcms-api-base-url', apiBaseUrlInput.value.trim().replace(/\/$/, ''));
    checkApiStatus();
    loadSection(currentSection);
  });

  // Initial load
  checkApiStatus();
  loadDashboard();

  // Status check every 30s
  setInterval(checkApiStatus, 30000);
});
