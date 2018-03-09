package keyStore;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	public String ks_tableName;
	private char mode='r';
	//db config
	private Jedis ksJedis=null;
	private String IP="127.0.0.1";
	private int Port=6379;
	private String password="bob";
	private int dbId=0;
	private Map<String, List<byte[]>> ks_map=null;
	private List<byte[]> result_list=null;
	private LinkedList<byte[]> key_list=null;
	final static int TOTAL_KEYS=7;
	
	//class

	//func
//constructor
	public keyStore(String tabName,char mode,jedisConf jdsconf){
		this.ks_tableName=tabName;
		if(jdsconf.ip==null){
			System.out.print("void ip for redis\n");
		}
		if(jdsconf.port==0){
			System.out.print("unsafe port for redis\n");
		}
		this.IP=jdsconf.ip;
		this.Port=jdsconf.port;
		this.password=jdsconf.password;
		this.dbId=jdsconf.db_num;
		
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
	//fast mode for database level tablename=dbname
	//generate a key for mode
	public int genKeyFast(int mode){
		da_handle dh = new da_handle();
		DaToSHCA dats=new DaToSHCA();
		if(!dats.DA_OpenHsmServer(dh,"192.168.0.178",6006)){
			return -2;
		}
		byte[] tmp_bytes=dats.DA_QinGenCipherKey(dh,mode);
		for(int i=0;i<tmp_bytes.length;i++){
			System.out.printf("0x%02x",tmp_bytes[i]);
		}
		System.out.println("");
		try{
			this.ks_dbConnect();
			this.ksJedis.set(this.ks_tableName+"-"+mode,SerializationUtil.su_BytestoString(tmp_bytes));
			this.ks_dbDisconnect();
		}catch(Exception e){
			System.out.println("write redis failed!");
			e.printStackTrace();
		}
		return 0;
	}
	//generate 8 keys for a name
	public int genKeyFastAll(){
		da_handle dh = new da_handle();
		DaToSHCA dats=new DaToSHCA();
		if(!dats.DA_OpenHsmServer(dh,"192.168.0.178",6006)){
			return -2;
		}
		
		this.ks_dbConnect();
		for(int j=0;j<8;j++){
			
			byte[] tmp_bytes=dats.DA_QinGenCipherKey(dh,j+1);
			for(int i=0;i<tmp_bytes.length;i++){
				System.out.printf("0x%02x",tmp_bytes[i]);
			}
			System.out.println("");
			try{
				
				this.ksJedis.set(this.ks_tableName+"-"+(j+1),SerializationUtil.su_BytestoString(tmp_bytes));
				
			}catch(Exception e){
				System.out.println("write redis failed!");
				this.ks_dbDisconnect();
				e.printStackTrace();
			}
			this.ks_dbDisconnect();
		}
		return 0;
	}
	
	public List<byte[]> getKeyFast(){
		List<byte[]> ret_byte=new ArrayList<byte[]>();
		this.ks_dbConnect();
		for(int i =0;i<8;i++){
			String tmp=this.ksJedis.get(this.ks_tableName+"-"+(i+1));
			ret_byte.add(SerializationUtil.su_StringtoBytes(tmp));
		}
		this.ks_dbDisconnect();
		return ret_byte;
	}
	
/*
	public List<byte[]> getKeyFast(){
		this.ks_dbConnect();
		Set<String> keySet=this.ksJedis.keys(this.ks_tableName+"-*");

		String[] atmp=new String[keySet.size()];
		int i=0;
		for(String value: keySet){
            atmp[i]=value;
            i++;
        }
		
		List<String> tmp=this.ksJedis.mget(atmp);
		this.ks_dbDisconnect();
		if(null==tmp||tmp.size()==0){
			System.out.println("read empty password list,but still returned!");
			return null;
		}
		
		List<byte[]> ret_byte=new ArrayList<byte[]>();
		for(i=0;i<tmp.size();i++){
			//System.out.println(tmp.get(i));
			ret_byte.add(SerializationUtil.su_StringtoBytes(tmp.get(i)));
		}
		
		return ret_byte;
	}
	*/
	//---------table level------------------
	//input:int mode:	the encrypt mode for this table
	//return:the key for this mode
	public byte[] genKey(int mode){
		da_handle dh = new da_handle();
		DaToSHCA dats=new DaToSHCA();
		byte[] tmp_bytes=dats.DA_QinGenCipherKey(dh,mode);
		//sync this table 
		List<byte[]> a=this.getAllKeys();
		//add key for this mode in to keys`table
		if(a.get(mode-1)==new byte[1]){
			System.out.println("add a new key for mode :"+mode);
		}else{
			System.out.println("change the key for mode	:"+mode);
		}
		a.set(mode-1, tmp_bytes);
		//sync
		Map<String, String> tmp_map=new HashMap<String, String>();
		
		for(int i=0;i<TOTAL_KEYS;i++){
			List<byte[]> tmp_list=new ArrayList<byte[]>();;
			byte[] i_bytes=a.get(i);
			tmp_list.add(i_bytes);
			tmp_map.put(String.valueOf(i),
			SerializationUtil.su_BytestoString(SerializationUtil.serialize(tmp_list)));
		}
		ks_dbConnect();
		ks_dbWrite(tmp_map);
		ks_dbDisconnect();
		
		return tmp_bytes;
	}
	
	//----------------col level-----------
	//input:secColID:	the index of the cols
	//		encMode:	the encryption mode of the cols
	//return 0 success
	//return -1/-2/-3 failed

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
			ks_dbConnect();
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
	
	//input :mode number
	//return :the key for the mode (0 means no key)
	public byte[] getKeysForMode(int mode){
		if(mode<1 && mode>TOTAL_KEYS){
			System.out.println("recieve wrong mode number!");
			return null;
		}else{
			return this.getAllKeys().get(mode-1);
		}
		
	}
	//return :all the 7 keys in list 
	//if no key for this mode ,gives 0

	public List<byte[]> getAllKeys(){
		this.key_list=new LinkedList();
		ks_dbConnect();
		for(int i=0;i<TOTAL_KEYS;i++){
			List<byte[]> tmp=ks_dbRead(i);
			if(tmp==null){
				this.key_list.push(new byte[1]);
			}else{
				this.key_list.push(tmp.get(0));
			}
		}
		ks_dbDisconnect();
		return this.key_list;
	}
	//input:	index:the index of the col
	//return:	the key list
	public List<byte[]> getKeys(int index){
		ks_dbConnect();
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
	private boolean ks_dbConnect(){
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
			tmp=this.ksJedis.hmget(this.ks_tableName,String.valueOf(index)).get(0);
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
		String status=this.ksJedis.hmset(this.ks_tableName,hashmap);
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
		ks_dbConnect();
		ks_dbWrite(map1);
		ks_dbDisconnect();
	}
	public void test2(int x){
		System.out.print("\n");
		ks_dbConnect();
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


