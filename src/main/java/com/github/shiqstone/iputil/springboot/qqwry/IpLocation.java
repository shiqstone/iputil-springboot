package com.github.shiqstone.iputil.springboot.qqwry;

public class IpLocation {
    private final String ip;
    private String country = "";
    private String province = "";
    private String city = "";
    private String isp = "";

    public IpLocation(String ip) {
        this.ip = ip;
    }

    public String getIp() {
        return ip;
    }
    
    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
    
    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    @Override
	public String toString() {
		return new StringBuilder(country).append(province).append(city).append(isp).toString();
	}
}

