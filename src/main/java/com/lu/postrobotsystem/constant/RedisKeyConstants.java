package com.lu.postrobotsystem.constant;

/**
 * Redis 键常量定义类
 * <p>
 * 集中管理系统内所有 Redis 缓存键的命名规范，确保键名统一、可维护。
 * 所有键均采用 "{业务域}:{用途}:" 的命名格式，便于在 Redis 中按前缀搜索和管理。
 * </p>
 *
 * <p><b>键命名约定：</b>
 * <pre>
 *   login:token:xxxx       -- 登录会话相关
 *   stock:available:xxxx   -- 库存业务相关
 * </pre>
 * </p>
 *
 * <p><b>调用关系：</b>
 * <ul>
 *   <li>{@link com.lu.postrobotsystem.config.JwtAuthenticationFilter} -- 使用 TOKEN 和 BLACKLIST 常量检查认证状态</li>
 *   <li>{@code AuthService} / {@code LoginService} -- 在登录、登出、刷新令牌时使用</li>
 *   <li>{@code StockService} -- 在库存相关操作时使用 STOCK 系列常量</li>
 * </ul>
 * </p>
 *
 * <p><b>设计说明：</b>
 * 类被声明为 final 且构造器私有，防止被继承或实例化，
 * 仅作为常量的静态持有者。
 * </p>
 *
 * @author lu
 * @since 1.0.0
 */
public final class RedisKeyConstants {

    /**
     * 私有构造器，防止实例化
     * <p>常量类不允许被实例化，调用构造器将抛出 {@link UnsupportedOperationException}。</p>
     */
    private RedisKeyConstants() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    // ==================== 登录认证相关 ====================

    /**
     * accessToken 会话信息键前缀
     * <p>完整键：{@code login:token:<token值>}，值为用户会话信息。</p>
     */
    public static final String LOGIN_TOKEN_KEY = "login:token:";

    /**
     * refreshToken 映射键前缀
     * <p>完整键：{@code login:refresh:<refreshToken值>}，用于刷新令牌时的验证。</p>
     */
    public static final String LOGIN_REFRESH_KEY = "login:refresh:";

    /**
     * 用户 ID 映射当前 token 的键前缀
     * <p>完整键：{@code login:user:<用户ID>}，值为当前用户使用的 token，用于踢人下线等场景。</p>
     */
    public static final String LOGIN_USER_KEY = "login:user:";

    /**
     * Token 黑名单键前缀
     * <p>完整键：{@code login:blacklist:<token值>}，存储已登出但未过期的 token。
     * 当用户主动登出时，将 token 加入黑名单，在过期前无法再次使用。</p>
     */
    public static final String LOGIN_BLACKLIST_KEY = "login:blacklist:";

    /** ==================== 库存相关 ==================== */

    /**
     * 商品可用库存 Hash 键前缀
     * <p>完整键：{@code stock:available:<商品ID>}，使用 Hash 结构，field 为商品 ID，value 为可用数量。</p>
     */
    public static final String STOCK_AVAILABLE = "stock:available:";

    /**
     * 商品锁定库存 Hash 键前缀
     * <p>完整键：{@code stock:locked:<商品ID>}，使用 Hash 结构，field 为商品 ID，value 为已锁定数量。
     * 锁定库存表示被订单占用但尚未扣减的数量。</p>
     */
    public static final String STOCK_LOCKED = "stock:locked:";

    /**
     * 分布式锁键前缀
     * <p>完整键：{@code stock:lock:<商品ID>}，用于库存扣减时的分布式锁，防止并发超卖。</p>
     */
    public static final String STOCK_LOCK_KEY = "stock:lock:";
}
