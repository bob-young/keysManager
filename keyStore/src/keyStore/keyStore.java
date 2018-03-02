package keyStore;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ksJedis.jedisConf;
import ksUtil.SerializationUtil;
import redis.clients.jedis.Jedis; 
import com.datech.DaToSHCA.*;
//error note:
//	-1:input error
//	-2:da connection error
//	-3:redis connection error

public class keyStore {

	//var
	public String ks_tableNmae;
	private char mode='r';
	//db config
	private Jedis ksJedis=null;
	private String IP="127.0.0.1";
	private int Port=6379;
	private String password="bob";
	private int dbId=1;
	private Map<String, List<byte[]>> ks_map=null;
	private List<byte[]> result_list=null;
	
	
	//class

	//func
//constructor
	public keyStore(String tabName,char mode,jedisConf jdsconf){
		this.ks_tableNmae=tabName;
		if(jdsconf.ip==null){
			System.out.print("void ip for redis\n");
		}
		if(jdsconf.port==0){
			System.out.print("unsafe port for redis\n");
		}
		this.IP=jdsconf.ip;
		this.Port=jdsconf.port;
		this.password=jdsconf.password;
		switch (mode){
			case 'r':read_constructor();
						break;
			case 'w':write_constructor();
						break;
			default: modeErr(mode);
		}
	}
	
	
//public	
	public int pingTest(){

		try{
	      //check whether server is running or not 
		System.out.println("Server is running: "+this.ksJedis.ping());
		}catch(Exception e){
			e.printStackTrace();
			return -1;
		}
		return 0;
	}
	
	//input:secColID:	the index of the cols
	//		encMode:	the encryption mode of the cols
	//return 0 success
	//return -1/-2/-3 failed
	public int genKey(String[] secColName,int[] encMode){
		try{
			
		}catch(Exception e){
			return -1;
		}
		
		return 0;
	}
	
	public int genKey(int[] secColId,int[] encMode){
		try{
			//send to machine
			da_handle dh = new da_handle();
			DaToSHCA dats=new DaToSHCA();
			if(!dats.DA_OpenHsmServer(dh,"192.168.0.178",6006)){
				return -2;
			}
			Map<String, String> tmp_map=new HashMap<String, String>();
			
			for(int i=0;i<secColId.length;i++){
				List<byte[]> tmp_list=new ArrayList<byte[]>();
				System.out.println("\nDA in:"+encMode[i]);
				byte[] tmp_bytes=dats.DA_QinGenCipherKey(dh,encMode[i]);
				for(int ii =0;ii<tmp_bytes.length;ii++)
					System.out.print(" "+tmp_bytes[ii]);
				tmp_list.add(tmp_bytes);
				tmp_map.put(String.valueOf(secColId[i]),
					SerializationUtil.su_BytestoString(SerializationUtil.serialize(tmp_list)));
			}
			ks_dbConnent();
			ks_dbWrite(tmp_map);
			ks_dbDisconnect();
			
		}catch(Exception e){
			System.out.println("fail to generate keys ");
			return -1;
		}
		
		return 0;
	}
	
	
	public Map<String,List<byte[]>> getKeyMap(){
		return ks_map;
	}
	
	//input:	index:the index of the col
	//return:	the key list
	public List<byte[]> getKeys(int index){
		ks_dbConnent();
		this.result_list=ks_dbRead(index);
		ks_dbDisconnect();
		return this.result_list;
	}

//private
	private void modeErr(char mode){
		System.out.println("no "+mode+" operation\n\t"
				+"mode:w for generating new enckeys\n\t"
				+"mode:r for getting existed enckeys");
	}
	
	private void write_constructor(){
		this.mode='w';
	}
	
	private void read_constructor(){
		this.mode='r';
	}
	
	private boolean checkMode(char m){
		return this.mode==m?true:false;
	}
	
	//database operations
	public boolean ks_dbSelect(int i){
		try{
			this.ksJedis.select(i);
			return true;
		}catch (Exception e){
			e.printStackTrace();
			return false;
		}
	}
	private boolean ks_dbConnent(){
		try{
			this.ksJedis=new Jedis(this.IP,this.Port);
			this.ksJedis.auth(this.password);
		}catch(Exception e){
			return false;
		}
		return true;
	}
	
	private boolean ks_dbDisconnect(){
		try{
			System.out.println(this.ksJedis.quit());
		}catch(Exception e){
			System.out.println("failed to drop the connection!");
			return false;
		}
		System.out.println("drop the connection successfully!");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	private List<byte[]> ks_dbRead(int index){
		String tmp=null;
		if(!checkMode('r')){
			System.out.println("permission denied!\n"
					+ "read redis failed:"
					+ "mode error");
			return null;
		}
		
		try{
			tmp=this.ksJedis.hmget(this.ks_tableNmae,String.valueOf(index)).get(0);
			if(null==tmp||tmp.length()==0){
				System.out.println("read empty password list,but still returned!");
				return null;
			}
			//this.result_list=(List<byte[]>) SerializationUtil.deserialize(SerializationUtil.su_StringtoBytes(tmp));
		}catch(Exception e){
			System.out.println("read redis failed!");
			e.printStackTrace();			
		}
		return (List<byte[]>) SerializationUtil.deserialize(SerializationUtil.su_StringtoBytes(tmp));
	}

	private boolean ks_dbWrite(Map<String,String> hashmap){
		if(!checkMode('w')){
			System.out.println("permission denied!\n"
					+ "write redis failed:"
					+ "mode error");
			return false;
		}
			
		
		try{
		String status=this.ksJedis.hmset(this.ks_tableNmae,hashmap);
		}catch(Exception e){
			System.out.println("write redis failed!");
			e.printStackTrace();
		}
		return true;
	}
	
	//test-----------test part----------
	public void test1(){
		Map<String, String> map1=new HashMap<String, String>();
		List<byte[]> list1=new ArrayList<byte[]>();
		byte[] b1={1,2,3,4,5,6,7,8};
		byte[] b2={25,-128,127,0,0,1,1,1};
		byte[] b3={2,-1,11,1,1,1,1,1};
		list1.add(b1);
		list1.add(b2);
		map1.put("0",SerializationUtil.su_BytestoString(SerializationUtil.serialize(list1)));
		map1.put("1","");
		list1.add(b3);
		map1.put("2",SerializationUtil.su_BytestoString(SerializationUtil.serialize(list1)));
		ks_dbConnent();
		ks_dbWrite(map1);
		ks_dbDisconnect();
	}
	public void test2(int x){
		System.out.print("\n");
		ks_dbConnent();
		ks_dbRead(x);
		ks_dbDisconnect();
		if(this.result_list==null){
			System.out.println("null result");
			return;
		}

		for(int i=0;i<this.result_list.size();i++){
			for(int j=0;j<this.result_list.get(i).length;j++){
				System.out.print(this.result_list.get(i)[j]+" ");
			}
			System.out.print("\n");
		}
	}
}


