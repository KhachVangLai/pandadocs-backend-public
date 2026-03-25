package com.pandadocs.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayOSCreatePaymentRequest {
    private Long orderCode;          // Mã đơn hàng unique
    private Integer amount;          // Số tiền (VND)
    private String description;      // Mô tả đơn hàng
    private String buyerName;        // Tên người mua (optional)
    private String buyerEmail;       // Email người mua (optional)
    private String buyerPhone;       // SĐT người mua (optional)
    private String buyerAddress;     // Địa chỉ (optional)
    private String returnUrl;        // URL redirect sau khi thanh toán thành công
    private String cancelUrl;        // URL redirect khi user hủy thanh toán
    private List<Item> items;        // Danh sách items
    private String signature;        // Chữ ký để verify request

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Item {
        private String name;
        private Integer quantity;
        private Integer price;
    }
}
