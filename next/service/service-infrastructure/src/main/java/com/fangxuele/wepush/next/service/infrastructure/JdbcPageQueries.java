package com.fangxuele.wepush.next.service.infrastructure;

import com.fangxuele.wepush.next.service.domain.ResourcePageQuery;

import java.util.ArrayList;
import java.util.List;

final class JdbcPageQueries {
    private JdbcPageQueries() { }

    static Query build(String select, String workspaceColumn, String workspaceId,
                       String nameExpression, String statusExpression,
                       String createdExpression, String idExpression,
                       ResourcePageQuery query) {
        StringBuilder sql = new StringBuilder(select).append(" WHERE ")
                .append(workspaceColumn).append(" = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(workspaceId);
        if (query.name() != null) {
            sql.append(" AND LOWER(").append(nameExpression).append(") LIKE ?");
            parameters.add("%" + query.name().toLowerCase() + "%");
        }
        if (query.status() != null) {
            sql.append(" AND ").append(statusExpression).append(" = ?");
            parameters.add(query.status());
        }
        if (query.from() != null) {
            sql.append(" AND ").append(createdExpression).append(" >= ?");
            parameters.add(query.from().toString());
        }
        if (query.to() != null) {
            sql.append(" AND ").append(createdExpression).append(" <= ?");
            parameters.add(query.to().toString());
        }
        if (query.beforeCreatedAt() != null) {
            sql.append(" AND (").append(createdExpression).append(" < ? OR (")
                    .append(createdExpression).append(" = ? AND ").append(idExpression).append(" < ?))");
            parameters.add(query.beforeCreatedAt().toString());
            parameters.add(query.beforeCreatedAt().toString());
            parameters.add(query.beforeId());
        }
        sql.append(" ORDER BY ").append(createdExpression).append(" DESC, ")
                .append(idExpression).append(" DESC LIMIT ?");
        parameters.add(query.limit());
        return new Query(sql.toString(), parameters.toArray());
    }

    record Query(String sql, Object[] parameters) { }
}
