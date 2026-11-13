package com.customization.oaones.generic.ai;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import weaver.conn.mybatis.MyBatisFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 复用 E9 平台 MyBatis 会话。
 */
public final class AiMyBatis {
    private static volatile SqlSessionFactory factory;

    private AiMyBatis() {
    }

    public static SqlSessionFactory factory() {
        SqlSessionFactory current = factory;
        if (current != null) {
            return current;
        }
        synchronized (AiMyBatis.class) {
            current = factory;
            if (current == null) {
                current = MyBatisFactory.sqlSessionFactory;
                if (current == null) {
                    throw new IllegalStateException("MyBatis 初始化失败，请检查 WEB-INF/config/mapper/oaones/generic/ai");
                }
                factory = current;
            }
        }
        return current;
    }

    public static <M, R> R call(Class<M> type, Function<M, R> work) {
        SqlSession session = factory().openSession(true);
        try {
            return work.apply(session.getMapper(type));
        } finally {
            session.close();
        }
    }

    public static <M> void run(Class<M> type, Consumer<M> work) {
        SqlSession session = factory().openSession(true);
        try {
            work.accept(session.getMapper(type));
        } finally {
            session.close();
        }
    }
}
