# iputil_springboot

IpUtil for Spring Boot with QQWry

### 说明


 > 基于 qqwry 的 IpUtil for SpringBoot 实现

1. 最新IP数据下载地址： http://www.cz88.net/

### Maven

``` xml
<dependency>
	<groupId>com.github.shiqstone</groupId>
	<artifactId>iputil-springboot</artifactId>
	<version>${project.version}</version>
</dependency>
```

### Sample

```java


import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.github.shiqstone.iputil_springboot.qqwry.Qqwry;

@SpringBootApplication
@ComponentScan({"com.github.shiqstone.iputil_springboot.qqwry"})
public class Application {
	
	@Autowired
	Qqwry qqwry;
	
	@PostConstruct
	public void test() throws IOException {
		
		System.out.println(qqwry.findIP("127.0.0.1"));

		System.out.println(qqwry.getLocation("127.0.0.1"));
		
	}
	
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

}

```
