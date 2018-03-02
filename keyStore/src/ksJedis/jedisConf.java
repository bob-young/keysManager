package ksJedis;

public class jedisConf {
	public String ip=null;
	public int port=0;
	public String password=null;
	public jedisConf(String string, int i, String pwd) {
		// TODO Auto-generated constructor stub
		this.ip=string;this.port=i;this.password=pwd;
	}
}