-- =====================================================
-- V3: 订单与支付模块增强
-- 说明：补充订单乐观锁、状态变更日志、支付流水记录
-- =====================================================

-- 1. orders 表增加字段（乐观锁版本号 + 用户关联）
ALTER TABLE orders ADD COLUMN user_id         BIGINT       NULL COMMENT '用户ID' AFTER order_no;
ALTER TABLE orders ADD COLUMN version         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号' AFTER remark;
ALTER TABLE orders ADD KEY idx_user_id (user_id);
ALTER TABLE orders ADD KEY idx_status (status);
ALTER TABLE orders ADD KEY idx_create_time (create_time);

-- 2. 订单状态变更日志表
CREATE TABLE order_status_log (
    id              BIGINT       NOT NULL COMMENT '日志ID（Snowflake）',
    order_id        BIGINT       NOT NULL COMMENT '关联订单ID',
    order_no        VARCHAR(64)  NOT NULL COMMENT '订单号（冗余，便于独立查询）',
    from_status     VARCHAR(20)  NULL     COMMENT '变更前状态',
    to_status       VARCHAR(20)  NOT NULL COMMENT '变更后状态',
    operator        VARCHAR(50)  NULL     COMMENT '操作人（系统触发时为SYSTEM）',
    operation_type  VARCHAR(30)  NOT NULL COMMENT '操作类型 ORDER_CREATE/ORDER_PAY/PAYMENT_CALLBACK/ORDER_CANCEL/ORDER_TIMEOUT/MANUAL_PROCESS',
    remark          VARCHAR(500) NULL     COMMENT '变更备注/原因',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单状态变更日志表';

-- 3. 支付流水记录表（支付信息安全：不存敏感数据，仅存流水标识和时间）
CREATE TABLE payment_flow_record (
    id                BIGINT       NOT NULL COMMENT '记录ID（Snowflake）',
    order_id          BIGINT       NOT NULL COMMENT '关联订单ID',
    order_no          VARCHAR(64)  NOT NULL COMMENT '订单号（冗余，便于独立查询）',
    payment_flow_no   VARCHAR(64)  NOT NULL COMMENT '支付流水号',
    flow_type         VARCHAR(20)  NOT NULL COMMENT '流水类型 QR_REQUEST-二维码请求 CALLBACK-支付回调 QUERY-支付查询',
    req_data_digest   VARCHAR(255) NULL     COMMENT '请求数据摘要（仅存非敏感字段JSON，无卡号/签名原文）',
    resp_data_digest  VARCHAR(255) NULL     COMMENT '响应数据摘要（仅存非敏感字段JSON，无卡号/签名原文）',
    source_ip         VARCHAR(50)  NULL     COMMENT '请求来源IP',
    trace_id          VARCHAR(64)  NULL     COMMENT '追踪ID',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id),
    KEY idx_order_no (order_no),
    KEY idx_payment_flow_no (payment_flow_no),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水记录表（仅存非敏感信息）';
