package ksJedis;

public class jedisConf {
	public String ip=null;
	public int port=0;
	public String password=null;
	public int db_num=1;
	public jedisConf(String string, int i, String pwd,int db) {
		// TODO Auto-generated constructor stub
		this.ip=string;this.port=i;this.password=pwd;this.db_num=db;
	}
}