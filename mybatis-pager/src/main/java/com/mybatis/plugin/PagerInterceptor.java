package com.mybatis.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.RowBounds;

import com.mybatis.plugin.pagination.DialectFactory;
import com.mybatis.plugin.pagination.Pagination;
import com.mybatis.plugin.pagination.utils.CountOptimize;
import com.mybatis.plugin.pagination.utils.JdbcUtils;
import com.mybatis.plugin.pagination.utils.PluginUtils;
import com.mybatis.plugin.pagination.utils.SqlUtils;
import com.mybatis.plugin.pagination.utils.StringUtils;
/**
 * 分页拦截器 拦截StatementHandler类Statement 
 * 方法   prepare(Connection connection, Integer transactionTimeout)
 *      throws SQLException;
 *
 */
@Intercepts({@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class})})
public class PagerInterceptor implements Interceptor{
	

    private static final Log logger = LogFactory.getLog(PagerInterceptor.class);

    /* 溢出总页数，设置第一页 */
    private boolean overflowCurrent = false;
    /* 是否设置动态数据源 设置之后动态获取当前数据源 */
    private boolean dynamicDataSource = false;
    /* Count优化方式 */
    private String optimizeType = "default";
    /* 方言类型 */
    private String dialectType;
    /* 方言实现类 */
    private String dialectClazz;

    /**
     * Physical Pagination Interceptor for all the queries with parameter {@link org.apache.ibatis.session.RowBounds}
     */
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) PluginUtils.realTarget(invocation.getTarget());
        MetaObject metaStatementHandler = SystemMetaObject.forObject(statementHandler);
        // 先判断是不是SELECT操作
        MappedStatement mappedStatement = (MappedStatement) metaStatementHandler.getValue("delegate.mappedStatement");
        if (!SqlCommandType.SELECT.equals(mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }
        RowBounds rowBounds = (RowBounds) metaStatementHandler.getValue("delegate.rowBounds");
        /* 不需要分页的场合 */
        if (rowBounds == null || rowBounds == RowBounds.DEFAULT) {
            return invocation.proceed();
        }
        BoundSql boundSql = (BoundSql) metaStatementHandler.getValue("delegate.boundSql");
        String originalSql = boundSql.getSql();
        Connection connection = (Connection) invocation.getArgs()[0];
        if (isDynamicDataSource()) {
            dialectType = JdbcUtils.getDbType(connection.getMetaData().getURL()).getDb();
        }
        if (rowBounds instanceof Pagination) {
            Pagination page = (Pagination) rowBounds;
            boolean orderBy = true;
            if (page.isSearchCount()) {
                CountOptimize countOptimize = SqlUtils.getCountOptimize(originalSql, optimizeType, dialectType, page.isOptimizeCount());
                orderBy = countOptimize.isOrderBy();
                this.queryTotal(countOptimize.getCountSQL(), mappedStatement, boundSql, page, connection);
                if (page.getTotal() <= 0) {
                    return invocation.proceed();
                }
            }
            String buildSql = SqlUtils.concatOrderBy(originalSql, page, orderBy);
            originalSql = DialectFactory.buildPaginationSql(page, buildSql, dialectType, dialectClazz);
        } else {
            // support physical Pagination for RowBounds
            originalSql = DialectFactory.buildPaginationSql(rowBounds, originalSql, dialectType, dialectClazz);
        }

		/*
         * <p> 禁用内存分页 </p> <p> 内存分页会查询所有结果出来处理（这个很吓人的），如果结果变化频繁这个数据还会不准。</p>
		 */
        metaStatementHandler.setValue("delegate.boundSql.sql", originalSql);
        metaStatementHandler.setValue("delegate.rowBounds.offset", RowBounds.NO_ROW_OFFSET);
        metaStatementHandler.setValue("delegate.rowBounds.limit", RowBounds.NO_ROW_LIMIT);
        return invocation.proceed();
    }

    /**
     * 查询总记录条数
     *
     * @param sql
     * @param mappedStatement
     * @param boundSql
     * @param page
     */
    protected void queryTotal(String sql, MappedStatement mappedStatement, BoundSql boundSql, Pagination page, Connection connection) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            DefaultParameterHandler parameterHandler = new DefaultParameterHandler(mappedStatement, boundSql.getParameterObject(), boundSql);
            parameterHandler.setParameters(statement);
            int total = 0;
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    total = resultSet.getInt(1);
                }
            }
            page.setTotal(total);
            /*
             * 溢出总页数，设置第一页
			 */
            int pages = page.getPages();
            if (overflowCurrent && (page.getCurrent() > pages)) {
                page = new Pagination(1, page.getSize());
                page.setTotal(total);
            }
        } catch (Exception e) {
            logger.error("Error: Method queryTotal execution error !", e);
        }
    }

    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    public void setProperties(Properties prop) {
        String dialectType = prop.getProperty("dialectType");
        String dialectClazz = prop.getProperty("dialectClazz");

        if (StringUtils.isNotEmpty(dialectType)) {
            this.dialectType = dialectType;
        }
        if (StringUtils.isNotEmpty(dialectClazz)) {
            this.dialectClazz = dialectClazz;
        }
    }

    public void setDialectType(String dialectType) {
        this.dialectType = dialectType;
    }

    public void setDialectClazz(String dialectClazz) {
        this.dialectClazz = dialectClazz;
    }

    public void setOverflowCurrent(boolean overflowCurrent) {
        this.overflowCurrent = overflowCurrent;
    }

    public void setOptimizeType(String optimizeType) {
        this.optimizeType = optimizeType;
    }

    public boolean isDynamicDataSource() {
        return dynamicDataSource;
    }

    public void setDynamicDataSource(boolean dynamicDataSource) {
        this.dynamicDataSource = dynamicDataSource;
    }
	

}
