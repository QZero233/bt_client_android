package com.nasa.bt.ca;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import com.alibaba.fastjson.JSON;
import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.RSAUtils;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.utils.FileIOUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class CAUtils {

    /**
     * 默认到期时间 2021-01-01 00:00:00
     */
    private static final long END_TIME_DEFAULT=1609430400000L;

    /**
     * 信任的证书公钥，k为公钥hash,v为公钥
     */
    private static Map<String,String> trustedKeyList=new HashMap<>();
    static {
        //TODO PUT

        //TODO 删除测试数据
        addTrustedPubKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA5XXgK2kGOeIg5jagmRbCqYm33kUVtoD+nPRMuQJe/u9gk124PnpNutVj+4Bdq+Dux0kXWx/aKyIXQPWUn76LjsjiR" +
                "NO4eK6fzzqNYjA9EeT71Z+0OuQkbVJIfT7Mj7o+1K0AJZHF7UsjvKUPZSu5AVEXfOykpHu4yLP0wU70I2O1ZtoZM84VIKw0slp9ZZ2S46mKVQMTSYn94EAY7uewn4aLesi" +
                "O2QPbe7RzBCCyjn+VAEK8zvQ5U/I/FFFbMaHNLx7pX/d5s877p6oTv1JioGj2dARiHrwXZmCSDUTz7W2J+JmRKk1q8+GqyLJs2uG7yoGyZF5ql/bTbSF4nGrIdQIDAQAB");
    }

    private static final int CURRENT_CA_VERSION=1;

    private static final String CA_FILE_NAME="caFile.ca";

    public static void addTrustedPubKey(String key){
        trustedKeyList.put(SHA256Utils.getSHA256InHex(key),key);
    }

    public static String caBasicToString(CABasic caBasic){
        return JSON.toJSONString(caBasic);
    }

    public static boolean checkCA(CAObject caObject,String currentIp,String pubKeyReceived){
        if(caObject==null)
            return false;

        //检查基本信息
        CABasic caBasic=caObject.getCaBasic();
        if(caBasic==null)
            return false;

        if(caBasic.getEndTime()<System.currentTimeMillis())
            return false;

        if(!caBasic.getServerIp().equals(currentIp))
            return false;

        if(!SHA256Utils.getSHA256InHex(pubKeyReceived).equalsIgnoreCase(caBasic.getServerPubKeyHashInHex()))
            return false;

        String trustedPubKey=trustedKeyList.get(caObject.getCaBasic().getSignPubKeyHashInHex());
        if(TextUtils.isEmpty(trustedPubKey))
            return false;

        String caStr=caBasicToString(caBasic);
        String caHash=SHA256Utils.getSHA256InHex(caStr);

        RSAUtils rsaUtils=new RSAUtils(new RSAKeySet(trustedPubKey,null));
        String caSignDecrypted=null;
        try{
            caSignDecrypted=rsaUtils.publicDecrypt(caObject.getSign());
        }catch (Exception e){
            return false;
        }

        if(TextUtils.isEmpty(caSignDecrypted))
            return false;

        if(caSignDecrypted.equalsIgnoreCase(caHash))
            return true;
        return false;
    }

    public static CAObject genCA(CABasic caBasic,RSAKeySet signKeySet){
        if(caBasic==null || signKeySet==null)
            return null;

        if(caBasic.getEndTime()<=0)
            caBasic.setEndTime(END_TIME_DEFAULT);

        String signPubKeyHash=SHA256Utils.getSHA256InHex(signKeySet.getPub());
        caBasic.setSignPubKeyHashInHex(signPubKeyHash);

        String signString;
        try{
            signString=new RSAUtils(signKeySet).privateEncrypt(SHA256Utils.getSHA256InHex(caBasicToString(caBasic)));
        }catch (Exception e){
            return null;
        }

        CAObject caObject=new CAObject(caBasic,signString);
        return caObject;
    }

    public static String caObjectToString(CAObject caObject){
        /**
         * 格式
         * 第一行 版本号
         * 第二行 json化的caBasic对象
         * 第三行 签名信息
         *
         * 按以上格式处理完后再base64编码返回
         */

        StringBuffer str=new StringBuffer();
        str.append(CURRENT_CA_VERSION);
        str.append("\n");
        str.append(JSON.toJSONString(caObject.getCaBasic()));
        str.append("\n");
        str.append(caObject.getSign());

        return Base64.encodeToString(str.toString().getBytes(),Base64.NO_WRAP);
    }

    public static CAObject stringToCAObject(String str){
        try{
            String decoded=new String(Base64.decode(str,Base64.NO_WRAP));
            String[] parts=decoded.split("\n");
            int verCode=Integer.parseInt(parts[0]);
            if(verCode==1){
                CABasic caBasic=JSON.parseObject(parts[1],CABasic.class);
                CAObject caObject=new CAObject(caBasic,parts[2]);
                return caObject;
            }else{
                return null;
            }
        }catch (Exception e){
            return null;
        }
    }

    public static boolean writeCAFile(Context context,String caStr){
        return FileIOUtils.writeFile(new File(context.getFilesDir(),CA_FILE_NAME),caStr.getBytes());
    }

    public static String readCAFile(Context context){
        byte[] buf=FileIOUtils.readFile(new File(context.getFilesDir(),CA_FILE_NAME));
        if(buf==null)
            return null;

        return new String(buf);
    }

}
