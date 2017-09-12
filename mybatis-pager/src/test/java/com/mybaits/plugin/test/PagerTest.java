package com.mybaits.plugin.test;

import java.io.InputStream;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import com.mybatis.plugin.pagination.Page;

public class PagerTest {
//    @Test
//	public void testAll() throws Exception{
//        // mybatis配置文件
//        String resource = "SqlMapConfig.xml";
//        // 得到配置文件流
//        InputStream inputStream =  Resources.getResourceAsStream(resource);
//        //创建会话工厂，传入mybatis配置文件的信息
//        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
//
//        // 通过工厂得到SqlSession
//        SqlSession sqlSession = sqlSessionFactory.openSession();
//        //通过Mapper 加载SQL信息
//        UserMapper userMapper=sqlSession.getMapper(UserMapper.class);
//        List<User> user=userMapper.findAllUser();
//        System.out.println(user.size());
//        // 释放资源
//        sqlSession.close();
//		
//	}
    
    @Test
	public void testAllPager() throws Exception{
        // mybatis配置文件
        String resource = "SqlMapConfig.xml";
        // 得到配置文件流
        InputStream inputStream =  Resources.getResourceAsStream(resource);
        //创建会话工厂，传入mybatis配置文件的信息
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        // 通过工厂得到SqlSession
        SqlSession sqlSession = sqlSessionFactory.openSession();
        //通过Mapper 加载SQL信息
        UserMapper userMapper=sqlSession.getMapper(UserMapper.class);
        Page page=new Page(0, 3);
//        RowBounds rowBounds=new RowBounds(0, 3);
        List<User> user=userMapper.selectMyPage(page);
        page.setRecords(user);
        System.out.println(page);
        System.out.println(user.size());
        // 释放资源
        sqlSession.close();
		
	}
}
