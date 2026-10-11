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

  // frontend/ts/modules/sortable.ts
  var ITEM_SELECTOR = "[data-sortable-item]";
  var HANDLE_SELECTOR = "[data-sortable-handle]";
  var DRAGGING_CLASS = "is-dragging";
  var MOVED_CLASS = "is-moved";
  function initSortable() {
    document.querySelectorAll("[data-sortable]").forEach(setup);
  }
  function setup(container) {
    const items = () => Array.from(container.querySelectorAll(ITEM_SELECTOR));
    const initial = items();
    let dragging = null;
    const markMoved = () => {
      items().forEach((item, i) => item.classList.toggle(MOVED_CLASS, initial[i] !== item));
    };
    container.addEventListener("dragstart", (event) => {
      const handle = event.target.closest(HANDLE_SELECTOR);
      const item = handle?.closest(ITEM_SELECTOR);
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
      const target = event.target.closest(ITEM_SELECTOR);
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
      const item = handle?.closest(ITEM_SELECTOR);
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
      }
    };
    form.addEventListener("change", (event) => {
      if (event.target instanceof HTMLInputElement && event.target.type === "checkbox") update();
    });
  }

  // frontend/ts/main.ts
  function init() {
    initConfirm();
    initDirtyGuard();
    initSortable();
    initWorkerTotal();
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
