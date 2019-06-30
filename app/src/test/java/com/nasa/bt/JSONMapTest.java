package com.nasa.bt;

import com.alibaba.fastjson.JSON;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class JSONMapTest {

    @Test
    public void mapTest(){
        Map<String,String> param=new HashMap<>();
        param.put("key1","value1");
        param.put("key2","value2");

        String json= JSON.toJSONString(param);
        System.out.println(json);
        param= (Map<String, String>) JSON.parse(json);
        System.out.println(param);
    }

}
