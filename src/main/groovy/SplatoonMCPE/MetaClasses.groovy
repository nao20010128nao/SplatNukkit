package SplatoonMCPE


/**
 * Created by nao on 2017/05/03.
 */
class MetaClasses {
    static boolean hasDone=false
    static Map<Object,Map<String,Object>> extras=[:]
    static void init(){
        if(hasDone)return
        ExpandoMetaClass.enableGlobally()
        Object.metaClass.setProperty={k,v->
            def mapForObject={Object o->
                if(!extras.containsKey(o)){
                    extras[o]=[:]
                }
                extras[o]
            }
            mapForObject(delegate)[k]=v
        }
        hasDone=true
    }
}
