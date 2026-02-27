package com.babjo.deliverycommerce.global.common.logging;

/**
 * P6Spy 쿼리 로그 포맷터
 *
 * P6Spy는 JDBC 레벨에서 쿼리를 가로채는 라이브러리입니다.
 * 기본 출력은 전체 쿼리가 한 줄로 나와 가독성이 떨어지기 때문에,
 * MessageFormattingStrategy 인터페이스를 구현하여 Hibernate처럼 줄 나눔이 적용된 포맷으로 출력합니다.
 *
 * spy.properties의 logMessageFormat에 해당 클래스를 지정해야 적용됩니다.
 * ex) logMessageFormat=com.babjo.deliverycommerce.global.common.logging.P6spyFormatter
 *
 * DDL (create, alter, drop) → Hibernate DDL 포맷 적용
 * DML (select, insert, update, delete) → Hibernate BASIC 포맷 적용
 */

import com.p6spy.engine.logging.Category;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import org.hibernate.engine.jdbc.internal.FormatStyle;

public class P6spyFormatter implements MessageFormattingStrategy{

    @Override
    public String formatMessage(int connectionId, String now, long elapsed,
                                String category, String prepared, String sql, String url) {
        // sql이 없으면 빈 문자열 반환 (불필요한 로그 제거)
        if (sql == null || sql.trim().isEmpty()) {
            return "";
        }

        // DDL (create, alter 등) / DML (select, insert 등) 구분해서 포맷팅
        String formattedSql = formatSql(category, sql);

        return String.format("\n[p6spy] %s | %dms\n%s", category, elapsed, formattedSql);
    }

    private String formatSql(String category, String sql) {
        if (Category.STATEMENT.getName().equals(category)) {
            String trimmed = sql.trim().toLowerCase();

            // DDL 포맷
            if (trimmed.startsWith("create") || trimmed.startsWith("alter")
                    || trimmed.startsWith("drop") || trimmed.startsWith("comment")) {
                return FormatStyle.DDL.getFormatter().format(sql);
            }

            // DML 포맷 (select, insert, update, delete)
            return FormatStyle.BASIC.getFormatter().format(sql);
        }

        return sql;
    }
}