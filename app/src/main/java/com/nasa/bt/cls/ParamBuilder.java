package com.nasa.bt.cls;

import java.util.HashMap;
import java.util.Map;

public class ParamBuilder {
    private Map<String,byte[]> params;

    public ParamBuilder() {
        params=new HashMap<>();
    }

    public ParamBuilder putParam(String key,String value){
        if(key!=null && value!=null)
            params.put(key,value.getBytes());
        return this;
    }

    public Map<String,byte[]> build(){
        return this.params;
    }
}
