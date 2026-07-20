-- =====================================================
-- V1: 初始化核心业务表
-- 说明：9张核心表，无物理外键
-- =====================================================

-- 1. 商品表
CREATE TABLE product (
    id              BIGINT          NOT NULL COMMENT '商品ID（Snowflake）',
    name            VARCHAR(200)    NOT NULL COMMENT '商品名称',
    description     TEXT            NULL     COMMENT '商品描述',
    tags            VARCHAR(500)    NULL     COMMENT '标签（逗号分隔，如"文创,热门,安徽"）',
    price           DECIMAL(10,2)   NOT NULL COMMENT '售价',
    original_price  DECIMAL(10,2)   NULL     COMMENT '原价',
    image_url       VARCHAR(500)    NULL     COMMENT '商品图片URL',
    robot_graspable TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '是否支持机器人抓取展示 0-否 1-是',
    display_point   VARCHAR(100)    NULL     COMMENT '陈列点位编号',
    status          TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态 0-下架 1-上架',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2. 库存表
CREATE TABLE inventory (
    id                  BIGINT          NOT NULL COMMENT '库存ID（Snowflake）',
    product_id          BIGINT          NOT NULL COMMENT '关联商品ID',
    real_stock          INT             NOT NULL DEFAULT 0 COMMENT '实时库存',
    locked_stock        INT             NOT NULL DEFAULT 0 COMMENT '锁定库存（下单未支付时锁定）',
    low_stock_threshold INT             NOT NULL DEFAULT 10 COMMENT '低库存告警阈值',
    sample_status       VARCHAR(20)     NOT NULL DEFAULT 'NORMAL' COMMENT '样品状态 NORMAL-正常 MISSING-缺失 DISPLACED-错位',
    mismatch_flag       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '账实不一致标记 0-一致 1-异常',
    vision_inspect_time DATETIME        NULL     COMMENT '最近视觉巡检时间',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

-- 3. 订单表
CREATE TABLE orders (
    id                BIGINT          NOT NULL COMMENT '订单ID（Snowflake）',
    order_no          VARCHAR(64)     NOT NULL COMMENT '订单号',
    total_amount      DECIMAL(10,2)   NOT NULL COMMENT '订单总金额',
    postage           DECIMAL(10,2)   NULL     COMMENT '邮资费用',
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING_PAY' COMMENT '订单状态 PENDING_PAY/PAYING/PAID/FAILED/CANCELLED/TIMEOUT/MANUAL_REQUIRED',
    mail_no           VARCHAR(20)     NULL     COMMENT '关联邮件号码',
    transaction_id    VARCHAR(64)     NULL     COMMENT '邮政交易流水号',
    payment_flow_no   VARCHAR(64)     NULL     COMMENT '支付流水号',
    qr_code_url       VARCHAR(500)    NULL     COMMENT '收款二维码链接',
    pay_query_no      VARCHAR(64)     NULL     COMMENT '支付查询流水号',
    remark            VARCHAR(500)    NULL     COMMENT '备注/失败原因',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 4. 订单明细表
CREATE TABLE order_item (
    id              BIGINT          NOT NULL COMMENT '明细ID（Snowflake）',
    order_id        BIGINT          NOT NULL COMMENT '关联订单ID',
    product_id      BIGINT          NOT NULL COMMENT '关联商品ID',
    product_name    VARCHAR(200)    NOT NULL COMMENT '商品名称（下单快照）',
    product_price   DECIMAL(10,2)   NOT NULL COMMENT '商品单价（下单快照）',
    quantity        INT             NOT NULL DEFAULT 1 COMMENT '购买数量',
    subtotal        DECIMAL(10,2)   NOT NULL COMMENT '小计金额',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 5. 支付表
CREATE TABLE payment (
    id                BIGINT          NOT NULL COMMENT '支付ID（Snowflake）',
    order_id          BIGINT          NOT NULL COMMENT '关联订单ID',
    payment_flow_no   VARCHAR(64)     NOT NULL COMMENT '支付流水号',
    pay_query_no      VARCHAR(64)     NOT NULL COMMENT '查询流水号',
    qr_code_url       VARCHAR(500)    NULL     COMMENT '二维码链接',
    platform_flow_no  VARCHAR(64)     NULL     COMMENT '平台流水号（邮政返回）',
    status            VARCHAR(10)     NOT NULL DEFAULT 'PAYING' COMMENT '支付状态 PAYING/SUCCESS/FAILED/REFUNDED/PARTIAL_REFUND',
    amount            DECIMAL(10,2)   NOT NULL COMMENT '支付金额',
    paid_time         DATETIME        NULL     COMMENT '支付完成时间',
    create_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted        TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_flow_no (payment_flow_no),
    KEY idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付表';

-- 6. 任务表
CREATE TABLE task (
    id                  BIGINT          NOT NULL COMMENT '任务ID（Snowflake）',
    task_no             VARCHAR(64)     NOT NULL COMMENT '任务编号',
    task_type           VARCHAR(30)     NOT NULL COMMENT '任务类型 NAVIGATION/GRASP/DISPLAY/EXPLAIN/SETTLEMENT/INVENTORY_CHECK/PATROL/OTHER',
    status              VARCHAR(20)     NOT NULL DEFAULT 'CREATED' COMMENT '任务状态 CREATED/QUEUED/RUNNING/PAUSED/SUCCEEDED/FAILED/CANCELLED/MANUAL_REQUIRED',
    priority            TINYINT(1)      NOT NULL DEFAULT 5 COMMENT '优先级 1-10（1最高）',
    dependency_task_no  VARCHAR(64)     NULL     COMMENT '依赖任务编号',
    timeout_seconds     INT             NOT NULL DEFAULT 300 COMMENT '超时阈值（秒）',
    retry_count         INT             NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    max_retry           INT             NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    input_params        TEXT            NULL     COMMENT '任务输入参数（JSON）',
    output_result       TEXT            NULL     COMMENT '任务输出结果（JSON）',
    fail_reason         VARCHAR(500)    NULL     COMMENT '失败原因',
    started_time        DATETIME        NULL     COMMENT '开始执行时间',
    completed_time      DATETIME        NULL     COMMENT '完成时间',
    duration_ms         BIGINT          NULL     COMMENT '耗时（毫秒）',
    create_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted          TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_task_no (task_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

-- 7. 告警表
CREATE TABLE alert (
    id              BIGINT          NOT NULL COMMENT '告警ID（Snowflake）',
    alert_type      VARCHAR(30)     NOT NULL COMMENT '告警类型 LOW_STOCK/STOCK_DISCREPANCY/SAMPLE_MISSING/TASK_FAILURE/PAYMENT_TIMEOUT/NETWORK_DOWN/SYSTEM_ERROR',
    alert_level     VARCHAR(10)     NOT NULL DEFAULT 'WARNING' COMMENT '告警级别 INFO/WARNING/CRITICAL',
    source          VARCHAR(50)     NOT NULL COMMENT '告警来源',
    source_id       VARCHAR(64)     NULL     COMMENT '来源ID',
    message         VARCHAR(500)    NOT NULL COMMENT '告警描述',
    status          VARCHAR(15)     NOT NULL DEFAULT 'UNRESOLVED' COMMENT '处理状态 UNRESOLVED/PROCESSING/RESOLVED',
    handler         VARCHAR(50)     NULL     COMMENT '处理人',
    handle_time     DATETIME        NULL     COMMENT '处理时间',
    handle_note     VARCHAR(500)    NULL     COMMENT '处理备注',
    resolved_time   DATETIME        NULL     COMMENT '解决时间',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警表';

-- 8. 用户表
CREATE TABLE user (
    id          BIGINT          NOT NULL COMMENT '用户ID（Snowflake）',
    username    VARCHAR(50)     NOT NULL COMMENT '登录账号',
    password    VARCHAR(255)    NOT NULL COMMENT '密码（BCrypt加密）',
    real_name   VARCHAR(50)     NULL     COMMENT '真实姓名',
    phone       VARCHAR(20)     NULL     COMMENT '手机号',
    email       VARCHAR(100)    NULL     COMMENT '邮箱',
    role        VARCHAR(20)     NOT NULL DEFAULT 'OPERATOR' COMMENT '角色 ADMIN/OPERATOR/MAINTAINER',
    status      TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-启用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT(1)      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-正常 1-删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 9. 审计日志表
CREATE TABLE audit_log (
    id              BIGINT          NOT NULL COMMENT '审计日志ID（Snowflake）',
    operator        VARCHAR(50)     NOT NULL COMMENT '操作人用户名',
    operation_type  VARCHAR(30)     NOT NULL COMMENT '操作类型 LOGIN/LOGOUT/ORDER_CREATE/ORDER_PAY/ORDER_CANCEL/PRODUCT_UPDATE/INVENTORY_ADJUST/TASK_MANUAL/POSTAL_API_CALL/PERMISSION_CHANGE/AUDIT_EXPORT/CONFIG_CHANGE',
    target_type     VARCHAR(30)     NOT NULL COMMENT '操作对象类型 ORDER/PRODUCT/INVENTORY/TASK/USER/ALERT/SYSTEM',
    target_id       VARCHAR(64)     NULL     COMMENT '操作对象ID',
    result          VARCHAR(10)     NOT NULL COMMENT '操作结果 SUCCESS/FAIL',
    detail          VARCHAR(500)    NULL     COMMENT '操作详情',
    trace_id        VARCHAR(64)     NULL     COMMENT '关联流水号/追踪ID',
    ip_address      VARCHAR(50)     NULL     COMMENT '请求IP',
    create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_operator (operator),
    KEY idx_operation_type (operation_type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';
