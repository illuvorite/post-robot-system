-- =====================================================
-- V2: 插入示例数据
-- 说明：为所有核心表添加初始示例数据
-- 默认密码（用户表）：123456（BCrypt 加密）
-- =====================================================

-- 1. 用户表（admin 由 AdminInitializer 自动创建，此处仅添加业务用户）
INSERT INTO user (id, username, password, real_name, phone, email, role, status, is_deleted) VALUES (1800000000000000001, 'zhangsan', '$2a$10$00SzSHg9KhvgM8g9FdgKa.KlVucw1OotsINf7f3DCBJ5LnwcahC3i', '张三', '13800001001', 'zhangsan@example.com', 'OPERATOR', 1, 0);
INSERT INTO user (id, username, password, real_name, phone, email, role, status, is_deleted) VALUES (1800000000000000002, 'lisi', '$2a$10$00SzSHg9KhvgM8g9FdgKa.KlVucw1OotsINf7f3DCBJ5LnwcahC3i', '李四', '13800001002', 'lisi@example.com', 'OPERATOR', 1, 0);
INSERT INTO user (id, username, password, real_name, phone, email, role, status, is_deleted) VALUES (1800000000000000003, 'wangwu', '$2a$10$00SzSHg9KhvgM8g9FdgKa.KlVucw1OotsINf7f3DCBJ5LnwcahC3i', '王五', '13800001003', 'wangwu@example.com', 'MAINTAINER', 1, 0);

-- 2. 商品表（status=1 上架 / 0 下架）
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000101, '安徽文创明信片套装', '精选安徽地标建筑手绘明信片，含黄山、徽派建筑等主题，每套12张。', '文创,热门,安徽', 29.90, 39.90, '/images/postcard-set.jpg', 1, 'A-01', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000102, '黄山风景纪念册', '黄山四季风光高清摄影集，硬壳精装，共64页。', '热门,安徽,纪念品', 59.00, 79.00, '/images/huangshan-book.jpg', 1, 'A-02', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000103, '机器人主题冰箱贴', '智能机器人造型冰箱贴，磁吸设计，一套6款。', '文创,热门', 15.00, 18.00, '/images/robot-magnet.jpg', 1, 'B-01', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000104, '合肥科技馆限定徽章', '合肥科技馆开馆纪念徽章，限量发售。', '限定,纪念品,安徽', 25.00, NULL, '/images/hefei-badge.jpg', 0, 'B-02', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000105, '智能机器人挂件', '迷你机器人钥匙扣挂件，环保硅胶材质。', '文创,热门', 39.90, 49.90, '/images/robot-keychain.jpg', 1, 'C-01', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000106, '徽派建筑书签套装', '以徽派建筑马头墙、花窗等元素设计的金属书签，一套4枚。', '文创,安徽', 22.00, 28.00, '/images/bookmark-set.jpg', 1, 'C-02', 1, 0);
INSERT INTO product (id, name, description, tags, price, original_price, image_url, robot_graspable, display_point, status, is_deleted) VALUES (1800000000000000107, '机器人明信片', '科幻风格机器人主题明信片，单张售卖。', '文创,热门', 5.90, NULL, '/images/robot-postcard.jpg', 1, 'D-01', 1, 0);

-- 3. 库存表（与商品一一对应）
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000201, 1800000000000000101, 100, 0, 10, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000202, 1800000000000000102, 50, 0, 5, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000203, 1800000000000000103, 200, 0, 20, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000204, 1800000000000000104, 80, 2, 10, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000205, 1800000000000000105, 60, 0, 10, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000206, 1800000000000000106, 150, 0, 15, 'NORMAL', 0, 0);
INSERT INTO inventory (id, product_id, real_stock, locked_stock, low_stock_threshold, sample_status, mismatch_flag, is_deleted) VALUES (1800000000000000207, 1800000000000000107, 300, 5, 30, 'NORMAL', 0, 0);

-- 4. 订单表
INSERT INTO orders (id, order_no, total_amount, postage, status, mail_no, transaction_id, is_deleted) VALUES (1800000000000000301, 'ORD202607010001', 50.80, 0.00, 'PAID', 'MAIL001001', 'TRANS202607010001', 0);
INSERT INTO orders (id, order_no, total_amount, postage, status, mail_no, transaction_id, is_deleted) VALUES (1800000000000000302, 'ORD202607010002', 59.00, 5.00, 'PAID', 'MAIL001002', 'TRANS202607010002', 0);
INSERT INTO orders (id, order_no, total_amount, postage, status, remark, is_deleted) VALUES (1800000000000000303, 'ORD202607020001', 15.00, 0.00, 'CANCELLED', '用户主动取消', 0);
INSERT INTO orders (id, order_no, total_amount, postage, status, qr_code_url, is_deleted) VALUES (1800000000000000304, 'ORD202607030001', 47.00, 0.00, 'PENDING_PAY', '/qrcode/order-304.png', 0);

-- 5. 订单明细表
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000401, 1800000000000000301, 1800000000000000101, '安徽文创明信片套装', 29.90, 1, 29.90, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000402, 1800000000000000301, 1800000000000000107, '机器人明信片', 5.90, 1, 5.90, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000403, 1800000000000000301, 1800000000000000103, '机器人主题冰箱贴', 15.00, 1, 15.00, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000404, 1800000000000000302, 1800000000000000102, '黄山风景纪念册', 59.00, 1, 59.00, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000405, 1800000000000000303, 1800000000000000103, '机器人主题冰箱贴', 15.00, 1, 15.00, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000406, 1800000000000000304, 1800000000000000106, '徽派建筑书签套装', 22.00, 1, 22.00, 0);
INSERT INTO order_item (id, order_id, product_id, product_name, product_price, quantity, subtotal, is_deleted) VALUES (1800000000000000407, 1800000000000000304, 1800000000000000104, '合肥科技馆限定徽章', 25.00, 1, 25.00, 0);

-- 6. 支付表
INSERT INTO payment (id, order_id, payment_flow_no, pay_query_no, platform_flow_no, status, amount, paid_time, is_deleted) VALUES (1800000000000000501, 1800000000000000301, 'PAY202607010001', 'QUERY202607010001', 'PLAT202607010001', 'SUCCESS', 50.80, '2026-07-01 10:05:00', 0);
INSERT INTO payment (id, order_id, payment_flow_no, pay_query_no, platform_flow_no, status, amount, paid_time, is_deleted) VALUES (1800000000000000502, 1800000000000000302, 'PAY202607010002', 'QUERY202607010002', 'PLAT202607010002', 'SUCCESS', 64.00, '2026-07-01 14:30:00', 0);

-- 7. 任务表
INSERT INTO task (id, task_no, task_type, status, priority, timeout_seconds, retry_count, max_retry, input_params, output_result, started_time, completed_time, duration_ms, is_deleted) VALUES (1800000000000000601, 'TASK20260701001', 'PATROL', 'SUCCEEDED', 3, 300, 0, 3, '{"startPoint":"A-01","endPoint":"D-01"}', '{"checkedCount":7,"issueCount":0}', '2026-07-01 09:00:00', '2026-07-01 09:05:30', 330000, 0);
INSERT INTO task (id, task_no, task_type, status, priority, timeout_seconds, retry_count, max_retry, input_params, output_result, started_time, completed_time, duration_ms, is_deleted) VALUES (1800000000000000602, 'TASK20260701002', 'NAVIGATION', 'SUCCEEDED', 5, 120, 0, 3, '{"target":"C-02"}', '{"arrived":true,"duration":45}', '2026-07-01 09:10:00', '2026-07-01 09:10:45', 45000, 0);
INSERT INTO task (id, task_no, task_type, status, priority, timeout_seconds, retry_count, max_retry, input_params, output_result, started_time, completed_time, duration_ms, is_deleted) VALUES (1800000000000000603, 'TASK20260702001', 'GRASP', 'FAILED', 1, 60, 2, 3, '{"productId":1800000000000000104,"position":"B-02"}', '{"failReason":"grasp_position_offset"}', '2026-07-02 10:00:00', '2026-07-02 10:01:30', 90000, 0);
INSERT INTO task (id, task_no, task_type, status, priority, timeout_seconds, retry_count, max_retry, input_params, started_time, is_deleted) VALUES (1800000000000000604, 'TASK20260703001', 'INVENTORY_CHECK', 'RUNNING', 4, 600, 0, 3, '{"zone":"A"}', '2026-07-03 08:00:00', 0);

-- 8. 告警表
INSERT INTO alert (id, alert_type, alert_level, source, source_id, message, status, handler, handle_note, is_deleted) VALUES (1800000000000000701, 'LOW_STOCK', 'WARNING', 'inventory', '1800000000000000202', '商品"黄山风景纪念册"库存低于阈值（当前: 50, 阈值: 5）', 'RESOLVED', 'zhangsan', '已安排补货', 0);
INSERT INTO alert (id, alert_type, alert_level, source, source_id, message, status, handler, handle_note, is_deleted) VALUES (1800000000000000702, 'TASK_FAILURE', 'CRITICAL', 'task', '1800000000000000603', '抓取任务 TASK20260702001 执行失败：抓取位置偏移', 'PROCESSING', 'wangwu', '正在校准机械臂', 0);
INSERT INTO alert (id, alert_type, alert_level, source, source_id, message, status, is_deleted) VALUES (1800000000000000703, 'STOCK_DISCREPANCY', 'WARNING', 'inventory', '1800000000000000204', '商品"合肥科技馆限定徽章"库存账实不符', 'UNRESOLVED', 0);

-- 9. 审计日志表
INSERT INTO audit_log (id, operator, operation_type, target_type, target_id, result, detail, trace_id, ip_address) VALUES (1800000000000000801, 'zhangsan', 'LOGIN', 'USER', '1800000000000000001', 'SUCCESS', '用户登录', 'TRACE001', '192.168.1.100');
INSERT INTO audit_log (id, operator, operation_type, target_type, target_id, result, detail, trace_id, ip_address) VALUES (1800000000000000802, 'zhangsan', 'ORDER_CREATE', 'ORDER', '1800000000000000301', 'SUCCESS', '创建订单 ORD202607010001', 'TRACE002', '192.168.1.100');
INSERT INTO audit_log (id, operator, operation_type, target_type, target_id, result, detail, trace_id, ip_address) VALUES (1800000000000000803, 'lisi', 'PRODUCT_UPDATE', 'PRODUCT', '1800000000000000101', 'SUCCESS', '更新商品价格 39.90 -> 29.90', 'TRACE003', '192.168.1.101');
INSERT INTO audit_log (id, operator, operation_type, target_type, target_id, result, detail, trace_id, ip_address) VALUES (1800000000000000804, 'wangwu', 'TASK_MANUAL', 'TASK', '1800000000000000603', 'SUCCESS', '手动重试抓取任务', 'TRACE004', '192.168.1.102');
