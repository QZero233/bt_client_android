package com.nasa.bt;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.upgrade.UpgradeStatus;

import org.junit.Test;

public class UpgradeJSONTest {

    @Test
    public void genJSONString(){
        UpgradeStatus upgradeStatus=new UpgradeStatus(8,"theta","更新","http://www.baidu.com/");
        System.out.println(JSON.toJSONString(upgradeStatus));
    }

}
