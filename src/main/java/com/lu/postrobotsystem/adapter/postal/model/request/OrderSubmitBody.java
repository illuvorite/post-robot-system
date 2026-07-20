package com.lu.postrobotsystem.adapter.postal.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 收寄订单提交请求体（SessionBody）
 * <p>
 * 入参含机构/员工/寄件人/收件人信息、业务产品、邮件条码、重量、件数、资费、包装物列表、内件信息列表等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSubmitBody {

    /** 机构编号 */
    private String orgCode;

    /** 机构名称 */
    private String orgName;

    /** 员工工号 */
    private String staffCode;

    /** 员工姓名 */
    private String staffName;

    /** 寄件人信息 */
    private SenderInfo sender;

    /** 收件人信息 */
    private RecipientInfo recipient;

    /** 业务产品代码 */
    private String productCode;

    /** 业务产品名称 */
    private String productName;

    /** 邮件条码（号码） */
    private String mailNo;

    /** 重量（克） */
    private Integer weight;

    /** 件数 */
    private Integer pieceCount;

    /** 总资费（分） */
    private BigDecimal totalPostage;

    /** 包装物列表 */
    private List<PackageItem> packages;

    /** 内件信息列表 */
    private List<InnerItem> innerItems;

    /** 备注 */
    private String remark;

    /** 寄件人信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenderInfo {
        /** 姓名 */
        private String name;
        /** 手机号 */
        private String phone;
        /** 固定电话 */
        private String tel;
        /** 省份 */
        private String province;
        /** 城市 */
        private String city;
        /** 区县 */
        private String district;
        /** 详细地址 */
        private String address;
        /** 邮编 */
        private String zipCode;
    }

    /** 收件人信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecipientInfo {
        /** 姓名 */
        private String name;
        /** 手机号 */
        private String phone;
        /** 固定电话 */
        private String tel;
        /** 省份 */
        private String province;
        /** 城市 */
        private String city;
        /** 区县 */
        private String district;
        /** 详细地址 */
        private String address;
        /** 邮编 */
        private String zipCode;
    }

    /** 包装物 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageItem {
        /** 包装物代码 */
        private String code;
        /** 包装物名称 */
        private String name;
        /** 数量 */
        private Integer quantity;
        /** 单价（分） */
        private BigDecimal unitPrice;
    }

    /** 内件信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InnerItem {
        /** 内件名称 */
        private String name;
        /** 内件数量 */
        private Integer quantity;
        /** 内件备注 */
        private String remark;
    }

    /** 资费明细 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostageDetail {
        /** 资费项目代码 */
        private String itemCode;
        /** 资费项目名称 */
        private String itemName;
        /** 资费金额（分） */
        private BigDecimal amount;
    }
}
