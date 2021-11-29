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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.github.shiqstone.iputil.springboot.qqwry.IpLocation;
import com.github.shiqstone.iputil.springboot.qqwry.IpZone;
import com.github.shiqstone.iputil.springboot.qqwry.Qqwry;

public class Qqwry_Test {

	private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss S");

	public static void main(String args[]) throws IOException {

		Qqwry qqwry = new Qqwry(); // load qqwry.dat from classpath

		String myIP = "54.151.155.9";
		IpZone ipzone = qqwry.findIP(myIP);
		System.out.printf("%s : %s, %s", df.format(new Date()), ipzone.getMainInfo(), ipzone.getSubInfo());

		IpLocation ipLocation = qqwry.getLocation(myIP);
		System.out.println(ipLocation);
	}

}
