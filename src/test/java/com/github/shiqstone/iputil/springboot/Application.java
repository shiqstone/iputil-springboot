/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.shiqstone.iputil.springboot;

import java.io.IOException;

import javax.annotation.PostConstruct;

import com.github.shiqstone.iputil.springboot.qqwry.Qqwry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan({"com.github.shiqstone.iputil_springboot.qqwry"})
public class Application {
	
	@Autowired
	Qqwry qqwry;
	
	@PostConstruct
	public void test() throws IOException {
		
		long startTime = System.currentTimeMillis();
		System.out.println(qqwry.findIP("61.94.43.82"));
		System.out.println(qqwry.findIP("223.153.86.201"));
		System.out.println(qqwry.findIP("127.0.0.1"));

		System.out.println(qqwry.getLocation("61.94.43.82"));
		System.out.println(qqwry.getLocation("223.153.86.201"));
		System.out.println("Time :" + (System.currentTimeMillis() - startTime));
	}
	
	
	public static void main(String[] args) throws Exception {
		SpringApplication.run(Application.class, args);
	}

}
