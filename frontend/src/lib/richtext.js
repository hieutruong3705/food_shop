// Mô tả sản phẩm do admin nhập có thể chứa HTML đơn giản (đoạn văn, danh sách, ảnh).
// Nội dung được làm sạch trước khi render: chỉ giữ thẻ và thuộc tính trong danh sách cho phép,
// chặn script, sự kiện on*, iframe và mọi src/href không phải /uploads, https hay dữ liệu an toàn.
const TAGS = new Set(['P', 'BR', 'STRONG', 'B', 'EM', 'I', 'UL', 'OL', 'LI', 'H3', 'H4', 'IMG', 'FIGURE', 'FIGCAPTION', 'SPAN', 'DIV', 'A']);
const ATTRS = { IMG: ['src', 'alt'], A: ['href'] };

function safeUrl(value) {
  const v = String(value || '').trim();
  return v.startsWith('/uploads/') || v.startsWith('/tech/') || v.startsWith('https://') ? v : '';
}

export function sanitizeHtml(input) {
  if (!input) return '';
  const host = document.createElement('div');
  host.innerHTML = String(input);
  const walk = (node) => {
    [...node.children].forEach((el) => {
      if (!TAGS.has(el.tagName)) { el.replaceWith(...el.childNodes); return; }
      [...el.attributes].forEach((a) => {
        const allowed = ATTRS[el.tagName] || [];
        if (!allowed.includes(a.name)) el.removeAttribute(a.name);
        else if ((a.name === 'src' || a.name === 'href') && !safeUrl(a.value)) el.removeAttribute(a.name);
      });
      if (el.tagName === 'IMG' && !el.getAttribute('src')) { el.remove(); return; }
      if (el.tagName === 'IMG') { el.setAttribute('loading', 'lazy'); }
      if (el.tagName === 'A') { el.setAttribute('rel', 'nofollow noopener'); el.setAttribute('target', '_blank'); }
      walk(el);
    });
  };
  walk(host);
  return host.innerHTML;
}

/** Mô tả cũ là văn bản thuần; chỉ render HTML khi nội dung thực sự có thẻ. */
export function looksLikeHtml(value) {
  return /<(p|div|ul|ol|li|h3|h4|img|strong|em|br)\b/i.test(String(value || ''));
}
