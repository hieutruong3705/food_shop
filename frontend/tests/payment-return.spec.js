import { expect, test } from "@playwright/test";

test("VNPay return waits for server payment confirmation", async ({ page }) => {
  // The page polls the signed callback itself, so a payer whose session was lost in the
  // redirect still sees the result; nothing here may need an authenticated endpoint.
  let polls = 0;
  await page.route("**/api/v1/orders/123/payment-status", (route) => route.abort());
  await page.route("**/api/v1/payments/vnpay/return?**", (route) => {
    polls += 1;
    route.fulfill({
      json: {
        valid: true,
        orderId: 123,
        responseCode: "00",
        transactionStatus: "00",
        amount: 1990000,
        bankCode: "NCB",
        transactionNo: "14123456",
        orderStatus: polls === 1 ? "PENDING" : "CONFIRMED",
        paymentStatus: polls === 1 ? "UNPAID" : "PAID",
        paymentReference: polls === 1 ? null : "14123456",
        refundRequired: false,
        message: "OK",
      },
    });
  });

  await page.goto("/payment-return?vnp_TxnRef=123&vnp_ResponseCode=00&vnp_TransactionStatus=00");
  await expect(page.getByText("VNPay báo giao dịch thành công")).toBeVisible();
  await expect(page.getByText("Đang chờ xác nhận từ VNPay")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Thanh toán đã được xác nhận" })).toBeVisible({
    timeout: 8000,
  });
  await expect(page.getByRole("link", { name: "Xem đơn" })).toHaveAttribute("href", "/orders/123");
});

test("VNPay return shows invalid signature result", async ({ page }) => {
  await page.route("**/api/v1/payments/vnpay/return?**", (route) =>
    route.fulfill({
      json: { valid: false },
    }),
  );

  await page.goto("/payment-return?vnp_TxnRef=123&vnp_ResponseCode=00");
  await expect(page.getByText("Không xác thực được kết quả thanh toán")).toBeVisible();
  await expect(page.getByRole("link", { name: "Về đơn hàng" })).toHaveAttribute("href", "/orders");
});
