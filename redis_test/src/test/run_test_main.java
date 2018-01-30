package test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import keyStore.keyStore;
import ksJedis.jedisConf;





public class run_test_main {
	public static void main(String args[]){
		jedisConf jconf=new jedisConf("127.0.0.1",6379,"bob");
		keyStore ks = new keyStore("tab21",'r',jconf);
		//ks.pingTest();
		//System.out.print(ks.ks_tableNmae);
		//showMap(ks.getKeyMap());
		List<byte[]> tmp = ks.getKeys(0);
		for(int i=0;i<tmp.size();i++){
			for(int j=0;j<tmp.get(i).length;j++){
				System.out.print(tmp.get(i)[j]+" ");
			}
			System.out.print("\n");
		}
	}


public static void showMap(Map<String, List> map){
	Iterator iter = map.entrySet().iterator();
	while (iter.hasNext()) {
		Map.Entry entry = (Map.Entry) iter.next();
		Object key = entry.getKey();
		System.out.println(key);
		Object val = entry.getValue();
		showList((List<byte[]>) val);
		}
	}
public static void showList(List<byte[]> list){
	if (list==null){
		return;
	}
	for(int i = 0;i<list.size();i++){
		for(int j = 0;j<list.get(i).length;j++){
			System.out.print("\t"+list.get(i)[j]);
		}
		System.out.print("\n");
	}
	}
}
