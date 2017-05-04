package SplatoonMCPE

import java.text.SimpleDateFormat

/**
 * Created by nao on 2017/05/04.
 */
class PhpUtils {
    static String date(String fmt){
        def c = Calendar.instance
        def sdf = new SimpleDateFormat(fmt)
        return sdf.format(c.time)
    }
}
