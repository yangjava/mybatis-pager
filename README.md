## mybatis-pager用于使用Mybatis分页

快速使用


1.添加pom文件依赖



<dependency>

	<groupId>com.mybaits.plugin</groupId>	
	<artifactId>mybatis-pager</artifactId>
	<version>1.0.0</version>
	
</dependency>



2.修改sqlMapConfig.xml配置文件



			<plugins>
		<plugin interceptor="com.mybatis.plugin.PagerInterceptor">
			<property name="dialectType" value="mysql" />
		</plugin>
		
			</plugins>
	



至此,配置工作就算大功告成了,接下来通过一个简单的例子来感受一下它的使用.



1.新建一个User表

public class User {

    private Integer id;

	private String username;

	private String password;

	private Date birthday;

	private String sex;

	private String address;
	
	......
	}
	
	

2.新建Dao层接口


public interface UserMapper {
	
	List<User> selectMyPage(RowBounds rowBounds);
}
	
	
3.新建Mapper配置文件



		<select id="selectMyPage" resultMap="BaseResultMap">

    select 
    <include refid="Base_Column_List" />
    from user
	
		 </select>


	
4.测试分页功能	
	
	@Test
	public void testAllPager() throws Exception {
		// mybatis配置文件
		String resource = "SqlMapConfig.xml";
		// 得到配置文件流
		InputStream inputStream = Resources.getResourceAsStream(resource);
		// 创建会话工厂，传入mybatis配置文件的信息
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
				.build(inputStream);

		// 通过工厂得到SqlSession
		SqlSession sqlSession = sqlSessionFactory.openSession();
		// 通过Mapper 加载SQL信息
		UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
		Page<User> page = new Page<User>(0, 3);
		// RowBounds rowBounds=new RowBounds(0, 3);
		List<User> user = userMapper.selectMyPage(page);
		page.setRecords(user);
		System.out.println(page.getRecords().size());
		System.out.println(page.getTotal());
		// 释放资源
		sqlSession.close();

	}
	
5.测试输出结果

DEBUG com.mybaits.plugin.test.UserMapper.selectMyPage 
- ==>  Preparing: SELECT COUNT(1) FROM ( select id, username, password, birthday, sex, address from user ) TOTAL 
DEBUG com.mybaits.plugin.test.UserMapper.selectMyPage
 - ==> Parameters: 
DEBUG com.mybaits.plugin.test.UserMapper.selectMyPage 
- ==>  Preparing: select id, username, password, birthday, sex, address from user LIMIT 0,3 
DEBUG com.mybaits.plugin.test.UserMapper.selectMyPage 
- ==> Parameters: 
DEBUG com.mybaits.plugin.test.UserMapper.selectMyPage 
- <==      Total: 3

3</br>
6
	
	
	
	
	
	
	
