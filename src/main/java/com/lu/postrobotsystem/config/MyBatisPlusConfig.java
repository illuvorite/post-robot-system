package com.lu.postrobotsystem.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
@MapperScan("com.lu.postrobotsystem.mapper")
public class MyBatisPlusConfig {

    /**
     * 注册 MyBatis-Plus 拦截器（插件集合）
     * <p>
     * 当前仅添加了分页拦截器 {@link PaginationInnerInterceptor}，
     * 配置为 MySQL 方言。当 Service 层使用 MyBatis-Plus 的 {@code Page<T>} 对象
     * 进行分页查询时，该拦截器会自动在 SQL 语句末尾追加 LIMIT/OFFSET 子句。
     * </p>
     *
     * <p><b>示例：</b>
     * <pre>
     * // Service 层代码
     * Page<User> page = new Page<>(1, 10);
     * userMapper.selectPage(page, null);
     * // 自动生成的 SQL：SELECT * FROM user LIMIT 10 OFFSET 0
     * </pre>
     * </p>
     *
     * @return 配置好的 MybatisPlusInterceptor 实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页拦截器，指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 注册自动填充处理器
     * <p>
     * 自动填充实体类中标注了 {@code @TableField(fill = FieldFill.INSERT)} 或
     * {@code @TableField(fill = FieldFill.INSERT_UPDATE)} 注解的字段。
     * </p>
     *
     * <p><b>填充策略：</b>
     * <ul>
     *   <li>插入操作：自动填充 {@code createTime} 和 {@code updateTime} 为当前时间</li>
     *   <li>更新操作：自动填充 {@code updateTime} 为当前时间</li>
     * </ul>
     * </p>
     *
     * <p><b>调用关系：</b>
     * <ul>
     *   <li>由 MyBatis-Plus 在每次 insert/update 操作时自动回调</li>
     *   <li>对应的实体类字段需要配置 {@code @TableField(fill = FieldFill.INSERT)} 等注解配合使用</li>
     * </ul>
     * </p>
     *
     * @return MetaObjectHandler 的匿名实现类
     */
    @Bean
    public MetaObjectHandler metaObjectHandler() {
        return new MetaObjectHandler() {
            /**
             * 插入操作时的自动填充逻辑
             * <p>为 createTime 和 updateTime 字段设置当前时间</p>
             *
             * @param metaObject MetaObject 对象，封装了实体类的元数据
             */
            @Override
            public void insertFill(MetaObject metaObject) {
                this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
                this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }

            /**
             * 更新操作时的自动填充逻辑
             * <p>为 updateTime 字段更新为当前时间</p>
             *
             * @param metaObject MetaObject 对象，封装了实体类的元数据
             */
            @Override
            public void updateFill(MetaObject metaObject) {
                this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
            }
        };
    }
}
