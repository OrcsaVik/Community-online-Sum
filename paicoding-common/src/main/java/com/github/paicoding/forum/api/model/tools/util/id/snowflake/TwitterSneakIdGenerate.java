package com.github.paicoding.forum.core.util.id.snowflake;

/**
 * @author Vik
 * @date 2026-01-22
 * @description 使用现有Twitter Snowflake算法，但是将时间戳部分从41位减少到32位，
 * 以支持更长的时间范围。
 */
//TOOD 现在留空机制
public class TwitterSneakIdGenerate implements IdGenerator{



    @Override
    public Long nextId() {
        return 0L;
    }
}
