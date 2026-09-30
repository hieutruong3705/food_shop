const base = import.meta.env.VITE_API_BASE_URL || "/api/v1";
let csrfPromise;
export function clearCsrf() {
  csrfPromise = undefined;
}
async function csrf() {
  if (!csrfPromise)
    csrfPromise = fetch(base + "/auth/csrf", { credentials: "include" })
      .then(async (r) => {
        if (!r.ok) throw new Error("Không thể tạo phiên bảo mật");
        return r.json();
      })
      .catch((e) => {
        clearCsrf();
        throw e;
      });
  return csrfPromise;
}
export async function api(path, options = {}) {
  const method = (options.method || "GET").toUpperCase(),
    token = ["GET", "HEAD", "OPTIONS"].includes(method) ? null : await csrf();
  const headers = {
    ...(options.body instanceof FormData
      ? {}
      : { "Content-Type": "application/json" }),
    ...options.headers,
    ...(token ? { [token.headerName]: token.token } : {}),
  };
  const response = await fetch(path.startsWith('/api/') ? path : base + path, {
    ...options,
    method,
    headers,
    credentials: "include",
  });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    if ([401, 403].includes(response.status)) clearCsrf();
    const error = new Error(body?.message || "Không thể kết nối máy chủ");
    error.status = response.status;
    error.details = body?.details;
    if (body?.details && Object.keys(body.details).length)
      error.message +=
        ": " +
        Object.entries(body.details)
          .map(([k, v]) => k + " " + v)
          .join(", ");
    if (response.status === 401)
      window.dispatchEvent(new Event("session-expired"));
    throw error;
  }
  return body;
}
export async function download(path, name) {
  const r = await fetch(base + path, { credentials: "include" });
  if (!r.ok) throw new Error("Không thể tải tệp");
  const url = URL.createObjectURL(await r.blob());
  const a = document.createElement("a");
  a.href = url;
  a.download = name;
  a.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
