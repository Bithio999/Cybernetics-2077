"use strict";
(() => {
  if (window.top !== window || location.pathname !== "/" ||
      !document.querySelector('meta[name="application-name"][content="Media Workshop"]')) return;
  document.documentElement.dataset.workshopGxConnector = "1";
  let busy = false;
  document.addEventListener("click", async event => {
    const button = event.target instanceof Element ? event.target.closest("#gx-connect") : null;
    // Page scripts cannot trigger a cookie read with a synthetic DOM event.
    if (!event.isTrusted || !button || busy) return;
    const nonce = button.dataset.gxNonce, token = button.dataset.gxToken;
    delete button.dataset.gxNonce;
    delete button.dataset.gxToken;
    if (!/^[a-f0-9]{32}$/.test(nonce || "") || !/^[A-Za-z0-9_-]{43}$/.test(token || "")) return;
    busy = true;
    let result;
    try {
      const session = await chrome.runtime.sendMessage({type: "media-workshop:gx:cookies"});
      if (!session?.cookies) throw new Error(session?.error || "Reload this page after enabling the Opera GX connector.");
      const response = await fetch("/api/account-cookies", {
        method: "POST", headers: {"X-Workshop-Token": token, "Content-Type": "text/plain;charset=UTF-8"},
        body: session.cookies, credentials: "omit", cache: "no-store",
      });
      session.cookies = "";
      const data = await response.json();
      if (!response.ok) throw new Error(data.error || "Media Workshop could not receive the session.");
      result = {nonce, auth: data.auth};
    } catch (error) {
      result = {nonce, error: error.message || "Connection failed. Reload the app in GX and reconnect."};
    } finally { busy = false; }
    // Only a non-secret session reference returns to the page, never cookies.
    document.dispatchEvent(new CustomEvent("media-workshop:gx:result", {detail: JSON.stringify(result)}));
  });
})();
