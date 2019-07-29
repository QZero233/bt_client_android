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
        addTrustedPubKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAuV+h2abIjwhmnpllk/6sFnhXUJSPqOonZueRauZ+Tqbeclli5idCPPFJTWJQCHfXfA2fRRDdHi53ey9R/2vrZDxeZmihkMAxTWfaYHUnWJq533GEsCGmVaz4Tio5s4SMmOFFUy9FEZsdL3sYvUpcRILWsbot0F0u/pIb/lEMFH+McSUJMh4fqaOllVa4z0zdMuqpnrEOSMyf+LUJk5CHHQ/ZmcyBaDC2KfaPJRcMngLKIC3I6b475v+9rukGT7590hm1IzmOnqJJh1Px53efKVxcyZprSU7Dp/0qO/iLCfGeVu1uRLETBEcrICCcczN/QBLFKa/Djj3RcPcR1rbeFwIDAQAB");
        addTrustedPubKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA058iwYIciqocqUOTPWWsH196gyYb2tKopf64KxI0dfy2fwIosI4CGWFjeHOMuzg4GMjDr+OgqH8f923293JmNx1mIFgKI67jQD0C2g3sjOtFxsa1JAmzsjt4O0w/I7pnj9xGzCB9qvebd4pCP2thjVQHMrTqBXVd5XrbzLVkICWMFzqPwIzraAkifM90O7bnadsSydrrO94Uqv2l/u7zT1+N0Q2JBQR4RsHzitOaQzAWCtAqLpyGkP1KLmWyDyGTTQgRy2GNl3QIVS11sTejNffw8J7J2YtcMSsD3xPMm9Gmf6UVNZKLyC16r/8ezC8hFy69EoAxSOxKDT9IC8MdSwIDAQAB");
        addTrustedPubKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx//aGTdBlbjcSKWuwR+zFf64jsexl+Vf8oXJmAwL65bsQv91ow6uHCYJyva9sVjQ3BRjw4F3RDIDEIZWr+b1ZlUrODgw4xNVgU/lC00RQO9mP7ny3BJgMTwe9aeA5yVlUHuDmWYbBcegHh5arHD/0XjwBEaR4Wg0CKigzEp9v3wPGR6R8Z9fRBCRimLvOBhbQOCTVIPDZvUq85dIex3DmyfOTusPBPWSV81AOTcvHP9vu/aCd/j/MvUDACtr/lXnKx9ou4Y7MBOKA1bRMTSmkTsyuQYWSv4OE/SlE5Oluuf2ogcNpWuE7ZUkeFfd1beARWtNd0W63EILYZGR5kubqwIDAQAB");
        addTrustedPubKey("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAn7K5htRFWMhuL1xdB7FARbikymhniPWprdqTSh29876kcoNEdMlxOhvHLpISkkUWdn6wsCal+qGGULZ97zWm0HVdM7SFVT3i67VA0LHX9it/R6CDK/tRa+oA41DY9aNyEfi0tIvdCpRqZiD85JyrI5LREGCU+Qsgdvat2KSOY5ZryLhP72EozsCTaGx7d+oUeJLI5TXw+cvCI7bO1VolIeCOTzFN0ntQqGgyy0WI8/XFjqOLqWfQgx6UdXj2YLpPGvJNBTBfqrZDjptuCP9w/FvbolNYWL4OjXK0Y0Zp5/4mijcPbUlxK+O5gZKkqaWEBwa5CUe9av7EjJK3mZL4uwIDAQAB");
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
