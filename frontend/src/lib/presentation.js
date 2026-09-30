export function categoryImage(name = '') {
  const text = name.toLocaleLowerCase('vi');
  const pairs = [['điện thoại','phone'],['laptop','laptop'],['máy tính bảng','tablet'],['màn hình','monitor'],['tai nghe','headphones'],['bàn phím','keyboard'],['chuột','mouse']];
  return '/tech/' + (pairs.find(([key]) => text.includes(key))?.[1] || 'accessory') + '.svg';
}
