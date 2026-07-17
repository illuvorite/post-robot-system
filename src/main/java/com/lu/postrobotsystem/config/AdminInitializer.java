package com.lu.postrobotsystem.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lu.postrobotsystem.mapper.UserMapper;
import com.lu.postrobotsystem.model.entity.User;
import com.lu.postrobotsystem.model.enums.UserRoleEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 管理员初始化器
 * <p>
 * 实现了 {@link ApplicationRunner} 接口，在 Spring Boot 应用启动完成后自动执行。
 * 用于检查数据库中是否存在管理员账号，如果不存在则创建默认管理员。
 * 确保系统首次部署后可以直接使用管理员账号登录。
 * </p>
 *
 * <p><b>执行时机：</b>
 * Spring 容器初始化完成后 -> {@link PostRobotSystemApplication#main} 启动 ->
 * {@code AdminInitializer.run()} 被自动调用。
 * </p>
 *
 * <p><b>默认管理员信息：</b>
 * <ul>
 *   <li>用户名：admin</li>
 *   <li>密码：admin123（BCrypt 加密存储）</li>
 *   <li>角色：ADMIN</li>
 * </ul>
 * </p>
 *
 * <p><b>幂等性：</b>
 * 如果数据库中已存在管理员角色且未删除的记录，则跳过初始化操作，
 * 保证多次启动不会重复创建。
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    /** 用户数据访问层 Mapper，用于操作 user 表 */
    private final UserMapper userMapper;

    /**
     * 应用启动后的回调方法
     * <p>
     * 在 Spring 容器完全初始化后执行，检查管理员账号是否存在，
     * 不存在则创建默认管理员 admin/admin123。
     * </p>
     *
     * @param args 应用启动时传入的参数（来自 {@code main()} 方法）
     */
    @Override
    public void run(ApplicationArguments args) {
        // 查询当前数据库中管理员角色的数量
        // 条件：角色为 ADMIN 且 is_deleted = 0（未逻辑删除）
        long adminCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, UserRoleEnum.ADMIN)  // 筛选管理员角色
                        .eq(User::getIsDeleted, 0)            // 筛选未删除的记录
        );

        // 如果管理员数量为 0，说明是首次部署，需要创建默认管理员
        if (adminCount == 0) {
            // 构建管理员实体对象
            User admin = new User()
                    .setUsername("admin")                    // 设置默认用户名
                    .setRealName("系统管理员")               // 设置真实姓名
                    .setPassword(BCrypt.hashpw("admin123")) // 使用 BCrypt 对密码进行加密
                    .setRole(UserRoleEnum.ADMIN)            // 设置角色为管理员
                    .setStatus(1);                          // 设置状态为启用（1=启用，0=禁用）
            // 将管理员记录插入数据库
            userMapper.insert(admin);
            // 记录初始化日志，通知运维人员默认账号信息
            log.info("默认管理员账号已创建: admin / admin123");
        } else {
            // 管理员已存在，无需重复创建
            log.info("管理员账号已存在，跳过初始化");
        }
    }
}
