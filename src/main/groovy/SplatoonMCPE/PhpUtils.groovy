package SplatoonMCPE

import java.text.SimpleDateFormat

/**
 * Created by nao on 2017/05/04.
 */
class PhpUtils {
    static String date(String fmt,Calendar c=Calendar.instance){
        def sdf = new SimpleDateFormat(fmt)
        return sdf.format(c.time)
    }
    static String strPad(str,int len){
        str="$str"
        if(len<str.length()){
            return str
        }else{
            return ' '*(len-str.length())+str
        }
    }
}
