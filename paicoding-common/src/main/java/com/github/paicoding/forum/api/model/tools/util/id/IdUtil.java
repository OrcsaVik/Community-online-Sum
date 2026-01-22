package com.github.paicoding.forum.core.util.id;

import com.github.paicoding.forum.api.model.enums.pay.ThirdPayWayEnum;
import com.github.paicoding.forum.core.util.CompressUtil;
import com.github.paicoding.forum.core.util.DateUtil;
import com.github.paicoding.forum.core.util.id.snowflake.SnowflakeProducer;
import com.github.paicoding.forum.core.util.id.snowflake.TwitterSneakIdGenerate;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author YiHui
 * @date 2023/8/30
 */
public class IdUtil {
    /**
     * 默认的id生成器
     */
    public static SnowflakeProducer DEFAULT_ID_PRODUCER = new SnowflakeProducer(new TwitterSneakIdGenerate());

    private static AtomicLong INCR = new AtomicLong((int) (Math.random() * 500));
    private static long lastTime = 0;


    /**
     * 生成全局id
     *
     * @return
     */
    public static Long genId() {
        return DEFAULT_ID_PRODUCER.genId();
    }

    /**
     * 生成字符串格式全局id
     *
     * @return
     */
    public static String genStrId() {
        return CompressUtil.int2str(genId());
    }


    /**
     * 生成支付的唯一code
     * 简化的规则：payWay前缀 + 年月日+时分秒
     *
     * @return
     */
    public static String genPayCode(ThirdPayWayEnum payWay, Long id) {
        long now = System.currentTimeMillis();
        if (DateUtil.skipDay(lastTime, now)) {
            lastTime = now;
            INCR.set((int) (Math.random() * 500));
        }
        return payWay.getPrefix() + String.format("%06d", INCR.addAndGet(1)) + "-" + id;
    }

    /**
     * 根据payCode 解析获取 payId
     *
     * @param code
     * @return
     */
    public static Long getPayIdFromPayCode(String code) {
        String[] str = StringUtils.split(code, "-");
        return Long.valueOf(str[str.length - 1]);
    }


}
