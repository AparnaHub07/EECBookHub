/* ==========================================================================
   EEC BookHub — Frontend Application Logic
   Data is persisted by the Spring Boot REST API backed by PostgreSQL.
   Only the signed-in user snapshot and theme are kept in localStorage.
   ========================================================================== */

const EEC = (function () {

  const DOMAIN = "@eec.srmrmp.edu.in"; // kept internal only — never printed in the UI
  const BASE_PENALTY = 40;   // ₹ charged for the 1st late day
  const PENALTY_STEP = 10;   // ₹ added for every additional late day
  const REMINDER_WINDOW_DAYS = 2;

  const CATEGORIES = [
    { key: "academic", name: "Academic & Textbooks", icon: "🎓", color: "#2F6B4F" },
    { key: "cs", name: "Computer Science & Technology", icon: "💻", color: "#3A6B8A" },
    { key: "science", name: "Science & Mathematics", icon: "🧪", color: "#6B5B95" },
    { key: "engineering", name: "Engineering & Technical", icon: "⚙️", color: "#5A6B47" },
    { key: "competitive", name: "Competitive Exams & Aptitude", icon: "📝", color: "#B8843A" },
    { key: "fiction", name: "Fiction & Novels", icon: "📖", color: "#2F6B4F" },
    { key: "romance", name: "Romance", icon: "❤️", color: "#B8546B" },
    { key: "mystery", name: "Mystery, Thriller & Crime", icon: "🔍", color: "#3D3B4F" },
    { key: "horror", name: "Horror", icon: "👻", color: "#3A3A3A" },
    { key: "fantasy", name: "Fantasy & Science Fiction", icon: "🧙", color: "#4A5B8A" },
    { key: "selfhelp", name: "Self-Help & Personal Growth", icon: "🌱", color: "#4E8A5E" },
    { key: "business", name: "Finance, Business & Entrepreneurship", icon: "💰", color: "#A97B2E" },
    { key: "psychology", name: "Psychology", icon: "🧠", color: "#7A4E8A" },
    { key: "biography", name: "Biography, History & GK", icon: "👤", color: "#8A5A3A" },
    { key: "travel", name: "Travel, Adventure & Lifestyle", icon: "🌍", color: "#2E7A7A" }
  ];

  function catInfo(key) { return CATEGORIES.find(c => c.key === key) || CATEGORIES[0]; }

  /* ---------------- API helpers ---------------- */
  const LOCAL_KEYS = new Set(["eec_session"]);
  let lastError = "";

  function api(method, url, body) {
    lastError = "";
    try {
      const xhr = new XMLHttpRequest();
      xhr.open(method, url, false);
      xhr.withCredentials = true;
      if (body !== undefined) xhr.setRequestHeader("Content-Type", "application/json");
      xhr.send(body !== undefined ? JSON.stringify(body) : null);
      let parsed = null;
      try { parsed = xhr.responseText ? JSON.parse(xhr.responseText) : null; } catch (_) {}
      if (xhr.status >= 200 && xhr.status < 300) return parsed;
      lastError = parsed?.message || `Request failed (${xhr.status})`;
      return null;
    } catch (e) {
      lastError = "Cannot reach the Spring Boot backend.";
      console.error("Backend request failed", e);
      return null;
    }
  }

  const COLLECTION_ENDPOINTS = {
    eec_users: "/api/users",
    eec_books: "/api/books",
    eec_requests: "/api/borrow-requests",
    eec_borrowings: "/api/borrowings",
    eec_notifications: "/api/notifications",
    eec_reviews: "/api/reviews"
  };

  function read(key, fallback) {
    if (LOCAL_KEYS.has(key)) {
      try { const v = localStorage.getItem(key); return v ? JSON.parse(v) : fallback; }
      catch (e) { return fallback; }
    }
    const endpoint = COLLECTION_ENDPOINTS[key];
    if (!endpoint) return fallback;
    const value = api("GET", endpoint);
    return value === null ? fallback : value;
  }
  function write(key, val) {
    if (LOCAL_KEYS.has(key)) { localStorage.setItem(key, JSON.stringify(val)); return true; }
    return false;
  }
  function uid(prefix) { return prefix + "_" + Math.random().toString(36).slice(2, 9); }
  function todayISO() { return new Date().toISOString(); }
  function addDays(dateStr, n) { const d = new Date(dateStr); d.setDate(d.getDate() + n); return d.toISOString(); }
  function fmtDate(dateStr) {
    if (!dateStr) return "—";
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }
  function daysBetween(a, b) { return Math.ceil((new Date(a) - new Date(b)) / (1000 * 60 * 60 * 24)); }
  function getLastError() { return lastError; }

  /* ---------------- penalty engine ---------------- */
  // Day 1 late = ₹40, Day 2 = ₹50, Day 3 = ₹60 ... (+₹10 per additional day)
  function calcPenalty(lateDays) {
    if (lateDays <= 0) return 0;
    return BASE_PENALTY + (lateDays - 1) * PENALTY_STEP;
  }

  /* ---------------- backend health ---------------- */
  function seed() { api("GET", "/api/health"); }

  /* ---------------- auth ---------------- */
  function currentUser() { return read("eec_session", null); }
  function requireAuth() {
    const local = currentUser();
    if (!local) { window.location.href = "login.html"; return null; }
    const me = api("GET", "/api/auth/me");
    if (!me?.ok || !me.user) { localStorage.removeItem("eec_session"); window.location.href = "login.html"; return null; }
    write("eec_session", me.user);
    return me.user;
  }
  function isValidDomain(email) { return email.toLowerCase().trim().endsWith(DOMAIN); }
  function getUserById(id) { return read("eec_users", []).find(u => u.id === id); }

  function login(email, password) {
    const result = api("POST", "/api/auth/login", { email: email.trim().toLowerCase(), password });
    if (!result) return { ok: false, message: lastError };
    if (result.ok) write("eec_session", result.user);
    return result;
  }
  function register(data) {
    const result = api("POST", "/api/auth/register", data);
    if (!result) return { ok: false, message: lastError };
    if (result.ok) write("eec_session", result.user);
    return result;
  }
  function requestPasswordOtp(email) {
    return api("POST", "/api/auth/forgot-password/request", { email }) || { ok:false, message:lastError };
  }
  function resetPassword(email, otp, newPassword) {
    return api("POST", "/api/auth/forgot-password/reset", { email, otp, newPassword }) || { ok:false, message:lastError };
  }
  function updateProfile(data) {
    const result = api("PATCH", "/api/users/me", data);
    if (result?.ok) write("eec_session", result.user);
    return result || { ok:false, message:lastError };
  }
  function updatePhone(phone) { return updateProfile({ phone }); }
  function logout() { api("POST", "/api/auth/logout", {}); localStorage.removeItem("eec_session"); window.location.href = "index.html"; }

  /* ---------------- toasts ---------------- */
  function toast(message, type = "success", title = "") {
    let stack = document.getElementById("toastStack");
    if (!stack) {
      stack = document.createElement("div"); stack.id = "toastStack"; document.body.appendChild(stack);
    }
    const icons = { success: "✅", warn: "⏰", danger: "🔴" };
    const el = document.createElement("div");
    el.className = "toast-c " + type;
    el.innerHTML = `<div>${icons[type] || "🔔"}</div><div><strong style="display:block;font-size:.85rem;">${title || (type === 'success' ? 'Done' : type === 'warn' ? 'Heads up' : 'Notice')}</strong><span style="font-size:.85rem;">${message}</span></div>`;
    stack.appendChild(el);
    setTimeout(() => { el.style.opacity = "0"; el.style.transition = "opacity .3s"; setTimeout(() => el.remove(), 300); }, 3800);
  }

  /* ---------------- confirm modal ---------------- */
  function confirmModal({ title, body, confirmText = "Confirm", confirmClass = "btn-spine", onConfirm }) {
    const id = "cm_" + Math.random().toString(36).slice(2, 8);
    const wrap = document.createElement("div");
    wrap.innerHTML = `
    <div class="modal fade" id="${id}" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header"><h5 class="modal-title font-display">${title}</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
          <div class="modal-body"><p class="mb-0" style="color:var(--text)">${body}</p></div>
          <div class="modal-footer">
            <button class="btn btn-outline-ink" data-bs-dismiss="modal">Cancel</button>
            <button class="btn ${confirmClass}" id="${id}_ok">${confirmText}</button>
          </div>
        </div>
      </div>
    </div>`;
    document.body.appendChild(wrap);
    const modalEl = wrap.querySelector(".modal");
    const modal = new bootstrap.Modal(modalEl);
    modalEl.querySelector("#" + id + "_ok").addEventListener("click", () => { onConfirm && onConfirm(); modal.hide(); });
    modalEl.addEventListener("hidden.bs.modal", () => wrap.remove());
    modal.show();
  }

  /* ---------------- theme ---------------- */
  function initTheme() {
    const saved = localStorage.getItem("eec_theme") || "light";
    document.documentElement.setAttribute("data-theme", saved);
  }
  function toggleTheme() {
    const cur = document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark";
    document.documentElement.setAttribute("data-theme", cur);
    localStorage.setItem("eec_theme", cur);
    document.querySelectorAll(".theme-toggle").forEach(b => b.textContent = cur === "dark" ? "☀️" : "🌙");
  }

  /* ---------------- nav mounts ---------------- */
  function logoImg(cls) { return `<img src="images/eec-logo.png" class="${cls || 'badge'}" alt="Easwari Engineering College emblem">`; }

  function mountPublicNav(active) {
    const el = document.getElementById("siteNav");
    if (!el) return;
    const loggedIn = currentUser();
    const links = [
      ["index.html#about", "About"],
      ["index.html#how", "How It Works"],
      ["available-books.html", "Browse Books"],
      ["about.html", "Contact"]
    ];
    el.innerHTML = `
      <div class="nav-inner">
        <a href="index.html" class="brand">${logoImg()}<span class="brand-name">EEC BookHub<small>Easwari Engineering College</small></span></a>
        <nav class="nav-links">
          ${links.map(l => `<a href="${l[0]}" class="${active === l[1] ? 'active' : ''}">${l[1]}</a>`).join("")}
        </nav>
        <div class="nav-cta">
          <button class="theme-toggle" onclick="EEC.toggleTheme()" title="Toggle theme">🌙</button>
          ${loggedIn
        ? `<a href="dashboard.html" class="btn btn-spine btn-sm">Dashboard</a>`
        : `<a href="login.html" class="btn btn-outline-spine btn-sm">Login</a><a href="register.html" class="btn btn-spine btn-sm">Register</a>`}
        </div>
        <button class="mobile-toggle" onclick="document.getElementById('mobileMenu').classList.toggle('show')">☰</button>
      </div>
      <div class="mobile-menu" id="mobileMenu">
        ${links.map(l => `<a href="${l[0]}">${l[1]}</a>`).join("")}
        ${loggedIn ? `<a href="dashboard.html">Dashboard</a>` : `<a href="login.html">Login</a><a href="register.html">Register</a>`}
      </div>`;
    const t = document.querySelector(".theme-toggle");
    if (t) t.textContent = (localStorage.getItem("eec_theme") === "dark") ? "☀️" : "🌙";
  }

  function mountFooter() {
    const el = document.getElementById("siteFooter");
    if (!el) return;
    el.innerHTML = `
    <div class="container-narrow">
      <div class="row g-4">
        <div class="col-lg-4 col-6">
          <div class="brand mb-3">${logoImg()}<span class="brand-name" style="color:var(--paper)">EEC BookHub<small>Easwari Engineering College</small></span></div>
          <p style="color:rgba(243,238,222,.6); font-size:.87rem; max-width:280px;">A student-to-student book bank that keeps good books circulating across campus instead of gathering dust on a shelf.</p>
        </div>
        <div class="col-lg-2 col-6"><h6>Platform</h6>
          <a href="available-books.html">Browse Books</a><a href="provide-book.html">Provide a Book</a><a href="dashboard.html">Dashboard</a><a href="about.html">How It Works</a>
        </div>
        <div class="col-lg-2 col-6"><h6>Account</h6>
          <a href="login.html">Login</a><a href="register.html">Register</a><a href="profile.html">My Profile</a>
        </div>
        <div class="col-lg-4 col-6"><h6>Verified Student Access</h6>
          <p style="color:rgba(243,238,222,.6); font-size:.87rem;">Registration is restricted to verified Easwari Engineering College students only — every account is checked before it can join.</p>
        </div>
      </div>
      <div class="foot-bottom">
        <span>© ${new Date().getFullYear()} EEC BookHub — Easwari Engineering College. Built by students, for students.</span>
        <span>Share Books. Learn Together. Grow Together.</span>
      </div>
    </div>`;
  }

  const NAV_ITEMS = [
    { key: "home", href: "dashboard.html", icon: "🏠", label: "Home" },
    { key: "search", href: "search-books.html", icon: "🔍", label: "Search Books" },
    { key: "available", href: "available-books.html", icon: "📚", label: "Available Books" },
    { key: "provide", href: "provide-book.html", icon: "➕", label: "Provide a Book" },
    { key: "borrowed", href: "my-borrowed-books.html", icon: "📖", label: "My Borrowed Books" },
    { key: "due", href: "due-date.html", icon: "📅", label: "Due Date / Return" },
    { key: "notifications", href: "notifications.html", icon: "🔔", label: "Notifications" },
    { key: "reviews", href: "reviews.html", icon: "⭐", label: "Reviews" },
    { key: "profile", href: "profile.html", icon: "👤", label: "My Profile" }
  ];

  function unreadCount(userId) {
    return read("eec_notifications", []).filter(n => n.userId === userId && !n.read).length;
  }

  function mountAppShell(active, pageTitle) {
    const user = requireAuth();
    if (!user) return null;
    const sidebar = document.getElementById("sidebarMount");
    const topbar = document.getElementById("topbarMount");
    const uc = unreadCount(user.id);
    if (sidebar) {
      sidebar.innerHTML = `
      <div class="brand">${logoImg()}<span class="brand-name">EEC BookHub<small>Easwari Engineering College</small></span></div>
      <nav class="flex-grow-1">
        ${NAV_ITEMS.map(i => `<a href="${i.href}" class="side-link ${active === i.key ? 'active' : ''}"><span class="ic">${i.icon}</span>${i.label}${i.key === 'notifications' && uc ? `<span class="badge bg-danger rounded-pill ms-auto">${uc}</span>` : ''}</a>`).join("")}
      </nav>
      <div class="sidebar-foot">
        <a href="#" class="side-link" id="logoutBtn"><span class="ic">🚪</span>Logout</a>
      </div>`;
      sidebar.querySelector("#logoutBtn").addEventListener("click", (e) => {
        e.preventDefault();
        confirmModal({
          title: "Log out of EEC BookHub?", body: "Are you sure you want to logout?",
          confirmText: "Logout", confirmClass: "btn-overdue", onConfirm: logout
        });
      });
    }
    if (topbar) {
      topbar.innerHTML = `
      <div class="d-flex align-items-center gap-2">
        <button class="sidebar-toggle" id="sbToggle">☰</button>
        <h3 class="mb-0" style="font-size:1.15rem;">${pageTitle}</h3>
      </div>
      <div class="d-flex align-items-center gap-2">
        <button class="theme-toggle" onclick="EEC.toggleTheme()" title="Toggle theme">🌙</button>
        <a href="notifications.html" class="theme-toggle" style="text-decoration:none; position:relative;">🔔${uc ? `<span class="badge bg-danger rounded-pill" style="position:absolute; top:-4px; right:-4px; font-size:.6rem;">${uc}</span>` : ''}</a>
        <a href="profile.html" class="d-flex align-items-center gap-2" style="text-decoration:none;">
          <div style="width:36px;height:36px;border-radius:50%;background:var(--spine);color:#fff;display:flex;align-items:center;justify-content:center;font-weight:700;font-size:.8rem;">${user.initials}</div>
        </a>
      </div>`;
      const t = topbar.querySelector(".theme-toggle");
      if (t) t.textContent = (localStorage.getItem("eec_theme") === "dark") ? "☀️" : "🌙";
    }
    const scrim = document.getElementById("sidebarScrim");
    const sbToggle = document.getElementById("sbToggle");
    if (sbToggle) {
      sbToggle.addEventListener("click", () => {
        document.querySelector(".app-sidebar").classList.add("show");
        scrim && scrim.classList.add("show");
      });
    }
    if (scrim) scrim.addEventListener("click", () => { document.querySelector(".app-sidebar").classList.remove("show"); scrim.classList.remove("show"); });
    return user;
  }

  /* ---------------- book helpers ---------------- */
  function getBooks() { return read("eec_books", []); }
  function getBook(id) { return getBooks().find(b => b.id === id); }
  function createBook(book) { return api("POST", "/api/books", book); }
  function deleteBook(id) { return api("DELETE", "/api/books/" + encodeURIComponent(id)); }
  function updateBookStatus(id, status) { return api("PATCH", "/api/books/" + encodeURIComponent(id) + "/status", { status }); }
  function getBookContact(id) { return api("GET", "/api/books/" + encodeURIComponent(id) + "/contact") || { visible:false }; }
  function getBorrowingContact(id, party="provider") { return api("GET", "/api/books/borrowing/" + encodeURIComponent(id) + "/contact?party=" + encodeURIComponent(party)) || { visible:false }; }
  function avgRating(bookId) {
    const rv = read("eec_reviews", []).filter(r => r.bookId === bookId);
    if (!rv.length) return null;
    return (rv.reduce((s, r) => s + r.rating, 0) / rv.length).toFixed(1);
  }
  function reviewCount(bookId) { return read("eec_reviews", []).filter(r => r.bookId === bookId).length; }

  function starHTML(rating) {
    if (rating === null) return `<span class="b-rating">New</span>`;
    return `<span class="b-rating">⭐ ${rating}</span>`;
  }

  function bookCoverHTML(book) {
    const c = catInfo(book.category);
    return `<div class="book-cover" style="background:linear-gradient(135deg, ${c.color}, ${c.color}cc);">
      <div class="spine-bar" style="background:${c.color}"></div>
      <div class="cover-icon">${c.icon}</div>
      <div class="cover-title">${book.title}</div>
    </div>`;
  }

  function bookCardHTML(book, opts = {}) {
    const rating = avgRating(book.id);
    const c = catInfo(book.category);
    const statusClass = book.status === "Available" ? "available" : "borrowed";
    const statusDot = book.status === "Available" ? "🟢" : "🔴";
    return `
    <div class="book-card">
      ${bookCoverHTML(book)}
      <div class="book-body">
        <div class="d-flex justify-content-between align-items-start">
          <h3 style="font-size:1rem; margin:0;">${book.title}</h3>
        </div>
        <div class="b-author">${book.author}</div>
        ${starHTML(rating)} <span style="font-size:.78rem;color:var(--text-muted)">(${reviewCount(book.id)} reviews)</span>
        <p class="b-desc">${book.description}</p>
        <div class="book-meta-row">
          <span class="badge-cat">${c.icon} ${c.name.split(' ')[0]}</span>
          <span class="badge-status ${statusClass}">${statusDot} ${book.status}</span>
        </div>
        ${opts.showProvider !== false ? `<div class="input-hint">Provided by ${book.providerName}</div>` : ""}
      </div>
      <div class="book-actions">
        <a href="book-details.html?id=${book.id}" class="btn btn-outline-ink btn-sm">View Details</a>
        ${opts.hideBorrow ? "" : `<button class="btn btn-spine btn-sm" onclick="EEC.uiRequestBorrow('${book.id}')" ${book.status !== 'Available' ? 'disabled' : ''}>Request to Borrow</button>`}
      </div>
    </div>`;
  }

  /* ---------------- borrow lifecycle ---------------- */
  function uiRequestBorrow(bookId) {
    const user = currentUser(); if (!user) { window.location.href = "login.html"; return; }
    const book = getBook(bookId);
    if (!book || book.status !== "Available") return;
    if (book.providerId === user.id) { toast("You can't borrow your own listed book.", "warn"); return; }
    confirmModal({
      title: "Send borrow request?",
      body: `Do you want to send a borrow request for <strong>"${book.title}"</strong> to ${book.providerName}? Their contact number is shared after they accept.`,
      confirmText: "Send Request", confirmClass: "btn-spine",
      onConfirm: () => {
        const result = api("POST", "/api/borrow-requests", { bookId });
        if (result) toast("Your borrow request has been sent to " + book.providerName + ".", "success");
        else toast(lastError || "Could not send the request.", "danger");
      }
    });
  }

  function respondRequest(reqId, accept) {
    return api("POST", "/api/borrow-requests/" + encodeURIComponent(reqId) + "/respond", { accept });
  }

  function requestRenewal(borrowingId) {
    toast("Renewal requests are not enabled yet. Please contact the provider using the number shown for this borrowing.", "warn");
  }

  function computeDueStatus(bw) {
    if (bw.status === "Returned") return { label: "Returned", cls: "available", days: 0 };
    const days = daysBetween(bw.dueDate, todayISO());
    if (days < 0) return { label: "Overdue", cls: "borrowed", days: Math.abs(days) };
    if (days <= REMINDER_WINDOW_DAYS) return { label: "Due Soon", cls: "pending", days };
    return { label: "Active", cls: "available", days };
  }

  function returnBook(borrowingId) {
    return api("POST", "/api/borrowings/" + encodeURIComponent(borrowingId) + "/return", {});
  }

  function getUpiPayment(borrowingId) {
    return api("GET", "/api/payments/upi/" + encodeURIComponent(borrowingId));
  }
  function reportUpiPayment(borrowingId, transactionReference) {
    return api("POST", "/api/payments/upi/report", { borrowingId, transactionReference });
  }
  function confirmUpiPayment(borrowingId, accept) {
    return api("POST", "/api/payments/upi/confirm", { borrowingId, accept });
  }
  function markNotificationRead(id) { return api("POST", "/api/notifications/" + encodeURIComponent(id) + "/read", {}); }
  function markAllNotificationsRead() { return api("POST", "/api/notifications/read-all", {}); }
  function addReview(review) { return api("POST", "/api/reviews", review); }

  return {
    CATEGORIES, catInfo, BASE_PENALTY, PENALTY_STEP, calcPenalty, REMINDER_WINDOW_DAYS, DOMAIN,
    seed, read, write, uid, todayISO, addDays, fmtDate, daysBetween, getLastError,
    currentUser, requireAuth, isValidDomain, login, register, requestPasswordOtp, resetPassword, updatePhone, updateProfile, logout, getUserById,
    toast, confirmModal, initTheme, toggleTheme,
    mountPublicNav, mountFooter, mountAppShell, unreadCount, NAV_ITEMS,
    getBooks, getBook, createBook, deleteBook, updateBookStatus, getBookContact, getBorrowingContact, avgRating, reviewCount, bookCardHTML, bookCoverHTML, starHTML,
    uiRequestBorrow, respondRequest, requestRenewal, computeDueStatus, returnBook, getUpiPayment, reportUpiPayment, confirmUpiPayment,
    markNotificationRead, markAllNotificationsRead, addReview
  };
})();

document.addEventListener("DOMContentLoaded", function () {
  EEC.seed();
  EEC.initTheme();
});
