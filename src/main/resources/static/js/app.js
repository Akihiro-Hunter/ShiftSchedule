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

  // frontend/ts/modules/request-counter.ts
  var DAY_SELECTOR = ".req-day";
  var UNANSWERED_CLASS = "is-unanswered";
  function initRequestCounter() {
    const form = document.querySelector("form[data-request-form]");
    if (!form) return;
    const counter = document.querySelector("[data-unanswered-count]");
    const days = Array.from(form.querySelectorAll(DAY_SELECTOR));
    const update = () => {
      let unanswered = 0;
      for (const day of days) {
        const answered = day.querySelector('input[type="radio"]:checked') !== null;
        day.classList.toggle(UNANSWERED_CLASS, !answered);
        if (!answered) unanswered++;
      }
      if (counter) {
        counter.textContent = `\u672A\u5165\u529B ${unanswered}\u65E5`;
        counter.hidden = unanswered === 0;
      }
    };
    form.addEventListener("change", update);
    update();
  }

  // frontend/ts/main.ts
  function init() {
    initConfirm();
    initDirtyGuard();
    initRequestCounter();
  }
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
