package test;

import com.finlay.scaffold.Application;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author: Finlay
 * @ClassName: ApplicationTest
 * @Description:
 * @date: 2019-12-25 3:16 PM
 */
@RunWith(SpringRunner.class)
//@ActiveProfiles("uat")
@SpringBootTest(classes = Application.class)// 指定spring-boot的启动类
public class ApplicationTest {
}