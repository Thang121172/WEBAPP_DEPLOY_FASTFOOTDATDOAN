import React, { useState, useMemo, useEffect } from "react";
import { useLocation, useNavigate, Link } from "react-router-dom";
import FormCard from "../components/FormCard";
import api from "../services/http";
import { useAuthContext } from "../context/AuthContext";
import { useLocationContext } from "../context/LocationContext";
import LocationPermission from "../components/LocationPermission";

type SummaryState = {
  subtotal: number;
  delivery_fee: number;
  discount: number;
  total: number;
};

export default function PaymentMethods() {
  const { user, isAuthenticated } = useAuthContext();
  const navigate = useNavigate();
  const routerLocation = useLocation();
  const { address: locationAddress, location: userLocation, getCurrentLocation } = useLocationContext();

  // summary sẽ được truyền từ Cart khi navigate:
  // navigate('/payment', { state: { summary: cartSummary } })
  const summaryFromState = (routerLocation.state as any)?.summary;
  const summary: SummaryState | null = summaryFromState || null;

  // form state
  const [address, setAddress] = useState(
    locationAddress || "123 Lê Lợi, Quận 1, TP.HCM" // Sử dụng địa chỉ từ location nếu có
  );
  const [note, setNote] = useState("");

  // Tự động điền địa chỉ khi có vị trí
  useEffect(() => {
    if (locationAddress && !address) {
      setAddress(locationAddress);
    }
  }, [locationAddress, address]);

  // phương thức thanh toán
  const [paymentMethod, setPaymentMethod] = useState<"cod" | "card">("cod");

  // ui state
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // fallback nếu user vào trực tiếp /payment không có state
  const totals = useMemo(() => {
    if (!summary) {
      return {
        subtotal: 0,
        delivery_fee: 0,
        discount: 0,
        total: 0,
      };
    }
    return summary;
  }, [summary]);

  const handlePlaceOrder = async () => {
    setSubmitting(true);
    setErrorMsg(null);
    setSuccess(false);

    if (!isAuthenticated) {
      alert("Bạn cần đăng nhập trước khi đặt hàng.");
      navigate("/login");
      setSubmitting(false);
      return;
    }

    try {
      // TODO: khi backend sẵn sàng, thay console.log bằng api.post(...)
      // ví dụ:
      // const res = await api.post("/orders/checkout/", {
      //   delivery_address: address,
      //   note,
      //   payment_method: paymentMethod,
      //   total: totals.total,
      //   items: [...]
      // })

      console.log(">> MOCK CHECKOUT <<", {
        user: user?.id,
        address,
        note,
        payment_method: paymentMethod,
        total: totals.total,
      });

      const mockOrderResponse = {
        order_id: 999,
        status: "pending",
        eta_minutes: 25,
      };
      console.log("Đơn hàng tạo mock:", mockOrderResponse);

      setSuccess(true);

      // quay về trang chủ sau khi "đặt hàng"
      setTimeout(() => {
        navigate("/");
      }, 1200);
    } catch (err: any) {
      console.error("Checkout error:", err);
      const apiMsg =
        err?.response?.data?.detail ||
        err?.response?.data?.message ||
        "Không thể tạo đơn hàng.";
      setErrorMsg(apiMsg);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen flex items-start justify-center bg-gray-50 p-4">
      <div className="w-full max-w-2xl space-y-6">
        <FormCard title="Phương thức thanh toán">
          {/* Thông báo trạng thái */}
          {!summary && (
            <div className="text-sm text-yellow-700 bg-yellow-100 border border-yellow-200 rounded-lg p-3 text-center font-medium">
              ⚠ Chưa có thông tin giỏ hàng. Hãy quay lại giỏ hàng để xác nhận.
            </div>
          )}

          {errorMsg && (
            <div className="text-sm text-red-700 bg-red-100 border border-red-200 rounded-lg p-3 font-medium text-center">
              {errorMsg}
            </div>
          )}

          {success && (
            <div className="text-sm text-green-700 bg-green-100 border border-green-200 rounded-lg p-3 font-medium text-center">
              ✅ Đã tạo đơn hàng! Cảm ơn bạn 🥳
            </div>
          )}

          {/* Tóm tắt hoá đơn */}
          <div className="bg-gray-50 rounded-lg border border-gray-200 p-4 text-sm">
            <h3 className="text-gray-800 font-semibold text-base mb-3">
              Tóm tắt thanh toán
            </h3>

            <div className="flex justify-between text-gray-600">
              <span>Tạm tính</span>
              <span className="font-medium">
                {totals.subtotal.toLocaleString("vi-VN")} ₫
              </span>
            </div>

            <div className="flex justify-between text-gray-600">
              <span>Phí giao hàng</span>
              <span className="font-medium text-red-500">
                {totals.delivery_fee.toLocaleString("vi-VN")} ₫
              </span>
            </div>

            <div className="flex justify-between text-gray-600 border-b pb-2">
              <span>Khuyến mãi</span>
              <span className="font-medium text-grabGreen-700">
                - {totals.discount.toLocaleString("vi-VN")} ₫
              </span>
            </div>

            <div className="flex justify-between text-gray-900 text-base font-bold mt-3">
              <span>Tổng thanh toán</span>
              <span className="text-red-600 text-lg">
                {totals.total.toLocaleString("vi-VN")} ₫
              </span>
            </div>
          </div>

          {/* Địa chỉ giao hàng */}
          <div>
            <div className="flex items-center justify-between mb-1">
              <label className="block text-sm font-medium text-gray-700">
                Địa chỉ giao hàng
              </label>
              {!userLocation && (
                <button
                  type="button"
                  onClick={getCurrentLocation}
                  className="text-xs text-grabGreen-700 hover:text-grabGreen-800 font-medium flex items-center space-x-1"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                  </svg>
                  <span>Lấy vị trí hiện tại</span>
                </button>
              )}
            </div>
            <input
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
              placeholder="Nhập địa chỉ nhận hàng"
            />
            {userLocation && (
              <p className="text-xs text-green-600 mt-1 flex items-center space-x-1">
                <svg className="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                </svg>
                <span>Địa chỉ được lấy từ vị trí của bạn</span>
              </p>
            )}
            {!userLocation && (
              <p className="text-[11px] text-gray-400 mt-1">
                Shipper sẽ giao đơn đến địa chỉ này. Bạn có thể nhấn "Lấy vị trí hiện tại" để tự động điền.
              </p>
            )}
          </div>

          {/* Location Permission Prompt (chỉ hiển thị khi chưa có vị trí) */}
          {!userLocation && (
            <div className="mt-4">
              <LocationPermission showOnlyWhenDenied={false} />
            </div>
          )}

          {/* Ghi chú cho cửa hàng */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Ghi chú cho cửa hàng (tuỳ chọn)
            </label>
            <textarea
              value={note}
              onChange={(e) => setNote(e.target.value)}
              className="w-full p-3 border border-gray-300 rounded-lg focus:ring-grabGreen-500 focus:border-grabGreen-500 transition duration-150"
              rows={2}
              placeholder="Ít cay, thêm tương ớt, gọi trước khi giao..."
            />
          </div>

          {/* Phương thức thanh toán */}
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Chọn phương thức thanh toán
            </label>

            <div className="space-y-3">
              {/* COD */}
              <label className="flex items-center justify-between border rounded-lg p-4 cursor-pointer hover:border-grabGreen-500 transition">
                <div className="text-sm">
                  <div className="font-semibold text-gray-800">
                    Tiền mặt khi nhận hàng
                  </div>
                  <div className="text-gray-500">
                    Thanh toán trực tiếp cho tài xế
                  </div>
                </div>
                <input
                  type="radio"
                  name="pmethod"
                  className="accent-grabGreen-700 w-4 h-4"
                  checked={paymentMethod === "cod"}
                  onChange={() => setPaymentMethod("cod")}
                />
              </label>

              {/* CARD */}
              <label className="flex items-center justify-between border rounded-lg p-4 cursor-pointer hover:border-grabGreen-500 transition">
                <div className="text-sm">
                  <div className="font-semibold text-gray-800 flex items-center gap-2">
                    Thẻ / Ví điện tử
                    <span className="text-[10px] bg-yellow-100 text-yellow-700 font-semibold px-2 py-0.5 rounded-full border border-yellow-300">
                      Beta
                    </span>
                  </div>
                  <div className="text-gray-500">
                    Thanh toán trước bằng thẻ đã lưu
                  </div>
                </div>
                <input
                  type="radio"
                  name="pmethod"
                  className="accent-grabGreen-700 w-4 h-4"
                  checked={paymentMethod === "card"}
                  onChange={() => setPaymentMethod("card")}
                />
              </label>
            </div>

            {paymentMethod === "card" && (
              <div className="mt-3 text-right">
                <Link
                  to="/payment/card"
                  className="text-sm font-semibold text-grabGreen-700 hover:text-grabGreen-800 underline"
                >
                  ➜ Thêm / chọn thẻ thanh toán
                </Link>
              </div>
            )}
          </div>

          {/* Nút xác nhận đặt hàng */}
          <button
            disabled={submitting || totals.total <= 0}
            onClick={handlePlaceOrder}
            className={`w-full py-3 rounded-lg font-bold text-white transition duration-200 shadow-md ${
              submitting || totals.total <= 0
                ? "bg-gray-400 cursor-not-allowed"
                : "bg-grabGreen-700 hover:bg-grabGreen-800"
            }`}
          >
            {submitting ? "Đang xử lý..." : "Xác nhận Đặt hàng"}
          </button>

          {/* quay lại giỏ */}
          <div className="text-center text-sm">
            <Link
              to="/cart"
              className="text-grabGreen-700 font-semibold hover:underline"
            >
              ← Quay lại Giỏ hàng
            </Link>
          </div>

          {/* note legal */}
          <div className="text-[11px] text-gray-400 text-center">
            Khi xác nhận, đơn sẽ được gửi tới cửa hàng và shipper.
          </div>
        </FormCard>
      </div>
    </div>
  );
}
