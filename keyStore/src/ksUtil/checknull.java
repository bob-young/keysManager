package ksUtil;

public class checknull {
	public static boolean check(Object[] o){
		for(int i=0;i<o.length;i++){
			if(o[i]!=null)
				continue;
			else
				return true;
		}
		return false;
	}
}
