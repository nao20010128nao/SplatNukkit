package SplatoonMCPE


/**
 * Created by nao on 2017/05/03.
 */
class MetaClasses {
    private static boolean hasDone=false
    private static Map<Object,Map<String,Object>> extras=[:]
    static void init(){
        if(hasDone)return
        ExpandoMetaClass.enableGlobally()
        // workaround for fuck'in feature from php:
        //  set value to undefined variable
        def mapForObject={Object o->
            if(!extras.containsKey(o)){
                extras[o]=[:]
            }
            extras[o]
        }
        Object.metaClass.setProperty={String k,Object v->
            mapForObject(delegate)[k]=v
        }
        Object.metaClass.setProperty={k->
            def ext=mapForObject(delegate)
            ext?ext[k]:null
        }
        hasDone=true
    }
}
