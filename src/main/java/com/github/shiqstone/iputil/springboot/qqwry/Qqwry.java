package com.github.shiqstone.iputil.springboot.qqwry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Thread safe. A Qqwry instance can share amount threads.
 *
 * <pre>
 * Usage:
 * {@code
 * Qqwry qqwry = new Qqwry(); // load qqwry.dat from classpath
 *
 * String myIP = "127.0.0.1";
 * IpZone ipzone = qqwry.findIP(myIP);
 * IpLocation ipLocation = qqwry.getLocation(myIP);
 * System.out.printf("%s, %s", ipzone.getMainInfo(), ipzone.getSubInfo());
 * // IANA, 保留地址用于本地回送
 * }
 * </pre>
 */
@Component
public class Qqwry {

	private static class QIndex {
		public final long minIP;
		public final long maxIP;
		public final int recordOffset;

		public QIndex(final long minIP, final long maxIP, final int recordOffset) {
			this.minIP = minIP;
			this.maxIP = maxIP;
			this.recordOffset = recordOffset;
		}
	}

	private static class QString {
		public final String string;
		/** length including the \0 end byte */
		public final int length;

		public QString(final String string, final int length) {
			this.string = string;
			this.length = length;
		}
	}

	private static final int INDEX_RECORD_LENGTH = 7;
	private static final byte REDIRECT_MODE_1 = 0x01;
	private static final byte REDIRECT_MODE_2 = 0x02;
	private static final byte STRING_END = '\0';

	private final byte[] data;
	private final long indexHead;
	private final long indexTail;

	/**
	 * Create Qqwry by loading qqwry.dat from classpath.
	 *
	 * @throws IOException if encounter error while reading qqwry.dat
	 */
	public Qqwry() throws IOException {
		final InputStream in = Qqwry.class.getClassLoader().getResourceAsStream("qqwry.dat");
		final ByteArrayOutputStream out = new ByteArrayOutputStream(10 * 1024 * 1024); // 10MB
		final byte[] buffer = new byte[4096];
		while (true) {
			final int r = in.read(buffer);
			if (r == -1) {
				break;
			}
			out.write(buffer, 0, r);
		}
		data = out.toByteArray();
		indexHead = readLong32(0);
		indexTail = readLong32(4);
	}

	/**
	 * Create Qqwry with provided qqwry.dat data.
	 *
	 * @param data
	 *            fully read data from a qqwry.dat file.
	 */
	// public Qqwry(final byte[] data) {
	// 	this.data = data;
	// 	indexHead = readLong32(0);
	// 	indexTail = readLong32(4);
	// }

	/**
	 * Create Qqwry from a path to qqwry.dat file.
	 *
	 * @param file path to qqwry.dat
	 * @throws IOException if encounter error while reading from the given file.
	 */
	// public Qqwry(final Path file) throws IOException {
	// 	this(Files.readAllBytes(file));
	// }

	public IpZone findIP(final String ip) {
		final long ipNum = toNumericIP(ip);
		final QIndex idx = searchIndex(ipNum);
		if (idx == null) {
			return new IpZone(ip);
		}
		return readIP(ip, idx);
	}

	private long getMiddleOffset(final long begin, final long end) {
		long records = (end - begin) / INDEX_RECORD_LENGTH;
		records >>= 1;
		if (records == 0) {
			records = 1;
		}
		return begin + (records * INDEX_RECORD_LENGTH);
	}

	private QIndex readIndex(final int offset) {
		final long min = readLong32(offset);
		final int record = readInt24(offset + 4);
		final long max = readLong32(record);
		return new QIndex(min, max, record);
	}

	private int readInt24(final int offset) {
		int v = data[offset] & 0xFF;
		v |= ((data[offset + 1] << 8) & 0xFF00);
		v |= ((data[offset + 2] << 16) & 0xFF0000);
		return v;
	}

    /**
     * qqwry.dat 格式说明
     * 一. 文件头，共8字节
     *    1. 第一个起始IP的绝对偏移， 4字节
     *    2. 最后一个起始IP的绝对偏移， 4字节
     * 二. "结束地址/国家/区域"记录区
     *     四字节ip地址后跟的每一条记录分成两个部分
     *     1. 国家记录
     *     2. 地区记录
     *     但是地区记录是不一定有的。而且国家记录和地区记录都有两种形式
     *     1. 以0结束的字符串
     *     2. 4个字节，一个字节可能为0x1或0x2
     *        a. 为0x1时，表示在绝对偏移后还跟着一个区域的记录，注意是绝对偏移之后，而不是这四个字节之后
     *        b. 为0x2时，表示在绝对偏移后没有区域记录不管为0x1还是0x2，后三个字节都是实际国家名的文件内绝对偏移
     *   如果是地区记录，0x1和0x2的含义不明，但是如果出现这两个字节，也肯定是跟着3个字节偏移，如果不是则为0结尾字符串
     * 三. "起始地址/结束地址偏移"记录区
     *     1. 每条记录7字节，按照起始地址从小到大排列
     *        a. 起始IP地址，4字节
     *        b. 结束ip地址的绝对偏移，3字节
     *
     * 注意，这个文件里的ip地址和所有的偏移量均采用little-endian格式，而java是采用big-endian格式的，要注意转换
    **/
	private IpZone readIP(final String ip, final QIndex idx) {
		final int pos = idx.recordOffset + 4; // skip ip
		final byte mode = data[pos];
		final IpZone z = new IpZone(ip);
		if (mode == REDIRECT_MODE_1) {
			final int offset = readInt24(pos + 1);
			if (data[offset] == REDIRECT_MODE_2) {
				readMode2(z, offset);
			} else {
				final QString mainInfo = readString(offset);
				final String subInfo = readSubInfo(offset + mainInfo.length);
				z.setMainInfo(mainInfo.string);
				z.setSubInfo(subInfo);
			}
		} else if (mode == REDIRECT_MODE_2) {
			readMode2(z, pos);
		} else {
			final QString mainInfo = readString(pos);
			final String subInfo = readSubInfo(pos + mainInfo.length);
			z.setMainInfo(mainInfo.string);
			z.setSubInfo(subInfo);
		}
		return z;
	}

	private long readLong32(final int offset) {
		long v = data[offset] & 0xFFL;
		v |= (data[offset + 1] << 8L) & 0xFF00L;
		v |= ((data[offset + 2] << 16L) & 0xFF0000L);
		v |= ((data[offset + 3] << 24L) & 0xFF000000L);
		return v;
	}

	private void readMode2(final IpZone z, final int offset) {
		final int mainInfoOffset = readInt24(offset + 1);
		final String main = readString(mainInfoOffset).string;
		final String sub = readSubInfo(offset + 4);
		z.setMainInfo(main);
		z.setSubInfo(sub);
	}

	private QString readString(final int offset) {
		int i = 0;
		final byte[] buf = new byte[128];
		for (;; i++) {
			final byte b = data[offset + i];
			if (STRING_END == b) {
				break;
			}
			buf[i] = b;
		}
		try {
			return new QString(new String(buf, 0, i, "GB18030"), i + 1);
		} catch (final UnsupportedEncodingException e) {
			return new QString("", 0);
		}
	}

	private String readSubInfo(final int offset) {
		final byte b = data[offset];
		if ((b == REDIRECT_MODE_1) || (b == REDIRECT_MODE_2)) {
			final int areaOffset = readInt24(offset + 1);
			if (areaOffset == 0) {
				return "";
			} else {
				return readString(areaOffset).string;
			}
		} else {
			return readString(offset).string;
		}
	}

	private QIndex searchIndex(final long ip) {
		long head = indexHead;
		long tail = indexTail;
		while (tail > head) {
			final long cur = getMiddleOffset(head, tail);
			final QIndex idx = readIndex((int) cur);
			if ((ip >= idx.minIP) && (ip <= idx.maxIP)) {
				return idx;
			}
			if ((cur == head) || (cur == tail)) {
				return idx;
			}
			if (ip < idx.minIP) {
				tail = cur;
			} else if (ip > idx.maxIP) {
				head = cur;
			} else {
				return idx;
			}
		}
		return null;
	}

	private long toNumericIP(final String s) {
		final String[] parts = s.split("\\.");
		if (parts.length != 4) {
			throw new IllegalArgumentException("ip=" + s);
		}
		long n = Long.parseLong(parts[0]) << 24L;
		n += Long.parseLong(parts[1]) << 16L;
		n += Long.parseLong(parts[2]) << 8L;
		n += Long.parseLong(parts[3]);
		return n;
	}

	private List<String> ispList = Arrays.asList(
        "联通",
        "移动",
        "铁通",
        "电信",
        "长城",
        "鹏博士"
	);

    private List<String> municipality = Arrays.asList(
        "北京",
        "天津",
        "重庆",
        "上海"
	);

    private List<String> provinces = Arrays.asList(
        "北京",
        "天津",
        "重庆",
        "上海",
        "河北",
        "山西",
        "辽宁",
        "吉林",
        "黑龙江",
        "江苏",
        "浙江",
        "安徽",
        "福建",
        "江西",
        "山东",
        "河南",
        "湖北",
        "湖南",
        "广东",
        "海南",
        "四川",
        "贵州",
        "云南",
        "陕西",
        "甘肃",
        "青海",
        "台湾",
        "内蒙古",
        "广西",
        "宁夏",
        "新疆",
        "西藏",
        "香港",
        "澳门"
    );

    private String sepProv = "省";
    private String sepCity = "市";
	private List<String> seqCities = Arrays.asList("市", "自治州", "州", "地区", "盟");
    private String sepCounty = "县";
	private List<String> seqCounties = Arrays.asList("县", "区", "旗");
    private String sepDistrict = "区";

	public IpLocation getLocation(final String ip) {
		IpZone ipZone = findIP(ip);
		IpLocation ipLocation = new IpLocation(ip);
		boolean isChina = false;
		int provPos = ipZone.getMainInfo().indexOf(sepProv);
		if( provPos != -1) {
			isChina = true;
			ipLocation.setProvince(ipZone.getMainInfo().substring(0, provPos));
			String tmpArea = ipZone.getMainInfo().substring(provPos+1);
			for( String sc : seqCities) {
				int cityPos = tmpArea.indexOf(sc);
				if(cityPos != -1) {
					ipLocation.setCity(tmpArea.substring(0, cityPos));
					break;
				}
			}
			// int cityPos = tmpArea.indexOf(sepCity);
			// if(cityPos != -1) {
			// 	ipLocation.setCity(tmpArea.substring(0, cityPos));
			// 	// tmpArea = tmpArea.substring(cityPos + 1);

			// 	// int countyPos = tmpArea.indexOf(sepCity);
			// 	// if(countyPos != -1) {
			// 	// } else {
			// 	// 	int districtPos = tmpArea.indexOf(sepDistrict);
			// 	// 	if(districtPos != -1) {

			// 	// 	}
			// 	// }
			// }
			if(ipLocation.getCity().isEmpty()) {
				for( String sc : seqCounties) {
					int countyPos = tmpArea.indexOf(sc);
					if(countyPos != -1) {
						ipLocation.setCity(tmpArea.substring(0, countyPos));
						break;
					}
				}
			}
		} else {
			int idx = 0;
			for (String prov : this.provinces) {
				if(ipZone.getMainInfo().indexOf(prov) != -1) {
					isChina = true;
					ipLocation.setProvince(prov);
					if (idx < 4) {
						ipLocation.setCity(prov);
					} else {
						String tmpArea = ipZone.getMainInfo().substring(prov.length());
						tmpArea.replaceAll("(自治区|特别行政区)", "");
						for( String sc : seqCities) {
							int cityPos = tmpArea.indexOf(sc);
							if(cityPos != -1) {
								ipLocation.setCity(tmpArea.substring(0, cityPos));
								break;
							}
						}
						if(ipLocation.getCity().isEmpty()) {
							for( String sc : seqCounties) {
								int countyPos = tmpArea.indexOf(sc);
								if(countyPos != -1) {
									ipLocation.setCity(tmpArea.substring(0, countyPos));
									break;
								}
							}
						}
					}
					break;
				}
				idx++;
			}
		}
		
		if(isChina) {
			ipLocation.setCountry("中国");
		} else {
			ipLocation.setCountry(ipZone.getMainInfo());
		}
		ipLocation.setIsp(getIsp(ipZone.getSubInfo()));
		return ipLocation;
	}

	private String getIsp(String subinfo) {
		String res = "";
		for (String isp : this.ispList) {
			if(subinfo.indexOf(isp) != -1) {
				res = isp;
				break;
			}
		}
		return res;
	}
}

