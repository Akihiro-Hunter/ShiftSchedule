"use strict";
(() => {
  // frontend/ts/modules/confirm.ts
  function initConfirm() {
    document.addEventListener(
      "submit",
      (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;
        const submitter = event.submitter;
        const message = submitter?.dataset.confirm ?? form.dataset.confirm;
        if (message && !window.confirm(message)) {
          event.preventDefault();
          event.stopImmediatePropagation();
        }
      },
      // 他の submit 処理（未保存チェックなど）より先に判定する
      { capture: true }
    );
  }

  // frontend/ts/modules/dirty-guard.ts
  function initDirtyGuard() {
    const forms = Array.from(document.querySelectorAll("form[data-dirty-guard]"));
    if (forms.length === 0) return;
    const initial = new Map(forms.map((form) => [form, snapshot(form)]));
    let submitting = false;
    document.addEventListener("submit", (event) => {
      if (!event.defaultPrevented) submitting = true;
    });
    window.addEventListener("beforeunload", (event) => {
      if (submitting) return;
      const dirty = forms.some((form) => snapshot(form) !== initial.get(form));
      if (dirty) {
        event.preventDefault();
        event.returnValue = "";
      }
    });
  }
  function snapshot(form) {
    const pairs = [];
    new FormData(form).forEach((value, name) => {
      if (typeof value === "string") pairs.push(`${name}=${value}`);
    });
    return pairs.join("&");
  }

  // frontend/ts/modules/hint-fold.ts
  function initHintFold() {
    document.querySelectorAll("details[data-hint-fold]").forEach((details) => {
      const key = `hintFold:${details.dataset.hintFold}`;
      try {
        if (localStorage.getItem(key) === "0") details.open = false;
      } catch {
      }
      details.addEventListener("toggle", () => {
        try {
          localStorage.setItem(key, details.open ? "1" : "0");
        } catch {
        }
      });
    });
  }

  // frontend/ts/modules/paid-leave.ts
  function initPaidLeave() {
    const form = document.querySelector("form[data-worker-total]");
    if (!form) return;
    const render = (toggle) => {
      const leave = toggle.querySelector("[data-leave-input]");
      const label = toggle.querySelector("span");
      if (label) label.textContent = leave?.value === "true" ? "\u5E74" : "\u516C";
    };
    form.addEventListener("contextmenu", (event) => {
      const toggle = event.target.closest("[data-off-toggle]");
      if (!toggle || toggle.closest(".is-locked")) return;
      event.preventDefault();
      const check = toggle.querySelector('input[type="checkbox"]');
      const leave = toggle.querySelector("[data-leave-input]");
      if (!check || !leave) return;
      const becomePaid = !check.checked || leave.value !== "true";
      leave.value = becomePaid ? "true" : "false";
      check.checked = true;
      render(toggle);
      check.dispatchEvent(new Event("change", { bubbles: true }));
    });
    form.addEventListener("change", (event) => {
      const target = event.target;
      if (!(target instanceof HTMLInputElement) || target.type !== "checkbox" || target.checked) return;
      const toggle = target.closest("[data-off-toggle]");
      const leave = toggle?.querySelector("[data-leave-input]");
      if (toggle && leave) {
        leave.value = "false";
        render(toggle);
      }
    });
  }

  // frontend/ts/modules/shift-lock.ts
  var ITEM_SELECTOR = "[data-sortable-item]";
  var LOCKED_CLASS = "is-locked";
  function initShiftLock() {
    const form = document.querySelector("form[data-worker-total]");
    if (!form) return;
    form.addEventListener("click", (event) => {
      const target = event.target;
      const toggle = target.closest("[data-lock-toggle]");
      if (toggle) {
        const item = toggle.closest(ITEM_SELECTOR);
        if (item) setLocked(item, toggle.getAttribute("aria-pressed") !== "true");
        return;
      }
      if (target instanceof HTMLInputElement && target.type === "checkbox" && target.closest(`${ITEM_SELECTOR}.${LOCKED_CLASS}`)) {
        event.preventDefault();
      }
    });
  }
  function setLocked(item, locked) {
    item.classList.toggle(LOCKED_CLASS, locked);
    const toggle = item.querySelector("[data-lock-toggle]");
    if (toggle) {
      toggle.setAttribute("aria-pressed", String(locked));
      toggle.title = locked ? "\u30ED\u30C3\u30AF\u4E2D\uFF08\u62BC\u3059\u3068\u89E3\u9664\uFF09" : "\u30ED\u30C3\u30AF\u3059\u308B\uFF08\u62BC\u3059\u3068\u52E4\u6020\u3092\u5909\u66F4\u4E0D\u53EF\u306B\uFF09";
    }
    const input = item.querySelector("[data-lock-input]");
    if (input) input.value = String(locked);
    item.querySelectorAll(".note-input").forEach((note) => {
      note.readOnly = locked;
    });
  }

  // frontend/ts/modules/side-nav.ts
  var STORAGE_KEY = "managerSideCollapsed";
  var COLLAPSED_CLASS = "side-collapsed";
  var READY_CLASS = "side-ready";
  var root = document.documentElement;
  function initSideNav() {
    const toggle = document.querySelector("[data-side-toggle]");
    if (!toggle) return;
    const apply = (collapsed) => {
      root.classList.toggle(COLLAPSED_CLASS, collapsed);
      toggle.setAttribute("aria-expanded", String(!collapsed));
    };
    apply(read() === "1");
    requestAnimationFrame(() => root.classList.add(READY_CLASS));
    toggle.addEventListener("click", () => {
      const collapsed = !root.classList.contains(COLLAPSED_CLASS);
      apply(collapsed);
      write(collapsed ? "1" : "0");
    });
  }
  function read() {
    try {
      return localStorage.getItem(STORAGE_KEY);
    } catch {
      return null;
    }
  }
  function write(value) {
    try {
      localStorage.setItem(STORAGE_KEY, value);
    } catch {
    }
  }

  // frontend/ts/modules/sortable.ts
  var ITEM_SELECTOR2 = "[data-sortable-item]";
  var HANDLE_SELECTOR = "[data-sortable-handle]";
  var DRAGGING_CLASS = "is-dragging";
  var MOVED_CLASS = "is-moved";
  function initSortable() {
    document.querySelectorAll("[data-sortable]").forEach(setup);
  }
  function setup(container) {
    const items = () => Array.from(container.querySelectorAll(ITEM_SELECTOR2));
    const initial = items();
    let dragging = null;
    const markMoved = () => {
      items().forEach((item, i) => item.classList.toggle(MOVED_CLASS, initial[i] !== item));
    };
    container.addEventListener("dragstart", (event) => {
      const handle = event.target.closest(HANDLE_SELECTOR);
      const item = handle?.closest(ITEM_SELECTOR2);
      if (!item || !event.dataTransfer) return;
      dragging = item;
      event.dataTransfer.effectAllowed = "move";
      event.dataTransfer.setData("text/plain", "");
      const rect = item.getBoundingClientRect();
      event.dataTransfer.setDragImage(item, event.clientX - rect.left, event.clientY - rect.top);
      requestAnimationFrame(() => {
        if (dragging === item) item.classList.add(DRAGGING_CLASS);
      });
    });
    container.addEventListener("dragover", (event) => {
      if (!dragging) return;
      event.preventDefault();
      const target = event.target.closest(ITEM_SELECTOR2);
      if (!target || target === dragging || !container.contains(target)) return;
      const rect = target.getBoundingClientRect();
      const before = event.clientY < rect.top + rect.height / 2;
      target.parentNode?.insertBefore(dragging, before ? target : target.nextSibling);
    });
    container.addEventListener("drop", (event) => {
      if (dragging) event.preventDefault();
    });
    container.addEventListener("dragend", () => {
      dragging?.classList.remove(DRAGGING_CLASS);
      dragging = null;
      markMoved();
    });
    container.addEventListener("keydown", (event) => {
      if (event.key !== "ArrowUp" && event.key !== "ArrowDown") return;
      const handle = event.target.closest(HANDLE_SELECTOR);
      const item = handle?.closest(ITEM_SELECTOR2);
      if (!handle || !item) return;
      event.preventDefault();
      const list = items();
      const index = list.indexOf(item);
      if (event.key === "ArrowUp" && index > 0) {
        item.parentNode?.insertBefore(item, list[index - 1]);
      } else if (event.key === "ArrowDown" && index < list.length - 1) {
        item.parentNode?.insertBefore(item, list[index + 1].nextSibling);
      }
      handle.focus();
      markMoved();
    });
  }

  // frontend/ts/modules/worker-total.ts
  function initWorkerTotal() {
    const form = document.querySelector("form[data-worker-total]");
    if (!form) return;
    const totals = Array.from(form.querySelectorAll("[data-worker-count]"));
    const users = Array.from(form.querySelectorAll("[data-sortable-item]"));
    const isOff = (cell) => cell.querySelector(".mark-locked") !== null || (cell.querySelector('input[type="checkbox"]')?.checked ?? false);
    const maxRunLimit = Number(form.dataset.maxRun ?? "6");
    const updateStreaks = (user) => {
      const cells = Array.from(user.querySelectorAll(".sheet-top [data-day]"));
      const lengths = new Array(cells.length).fill(0);
      let i = 0;
      while (i < cells.length) {
        if (isOff(cells[i])) {
          i++;
          continue;
        }
        let end = i;
        while (end < cells.length && !isOff(cells[end])) end++;
        const length = end - i + (i === 0 ? Number(user.dataset.carryIn ?? "0") : 0);
        for (let j = i; j < end; j++) lengths[j] = length;
        i = end;
      }
      cells.forEach((cell, index) => cell.classList.toggle("is-streak", lengths[index] > maxRunLimit));
      const max = Math.max(0, ...lengths);
      const maxCell = user.querySelector("[data-max-run]");
      if (maxCell) {
        maxCell.textContent = String(max);
        maxCell.classList.toggle("is-streak", max > maxRunLimit);
      }
    };
    const update = () => {
      for (const total of totals) {
        const cells = form.querySelectorAll(`[data-day="${total.dataset.workerCount}"]`);
        total.textContent = String(Array.from(cells).filter((cell) => !isOff(cell)).length);
      }
      for (const user of users) {
        const cells = Array.from(user.querySelectorAll("[data-day]"));
        const off = cells.filter(isOff).length;
        const work = user.querySelector("[data-work-days]");
        const offDays = user.querySelector("[data-off-days]");
        if (work) work.textContent = String(cells.length - off);
        if (offDays) offDays.textContent = String(off);
        updateStreaks(user);
      }
    };
    form.addEventListener("change", (event) => {
      if (event.target instanceof HTMLInputElement && event.target.type === "checkbox") update();
    });
  }

  // frontend/ts/main.ts
  function init() {
    initConfirm();
    initSideNav();
    initDirtyGuard();
    initSortable();
    initWorkerTotal();
    initPaidLeave();
    initShiftLock();
    initHintFold();
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
