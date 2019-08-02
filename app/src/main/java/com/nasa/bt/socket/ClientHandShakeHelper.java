package com.nasa.bt.socket;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.nasa.bt.SettingsActivity;
import com.nasa.bt.ca.CAObject;
import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.cls.Datagram;
import com.nasa.bt.cls.ParamBuilder;
import com.nasa.bt.crypt.KeyUtils;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.data.dao.CADao;
import com.nasa.bt.data.entity.TrustedRemotePublicKeyEntity;
import com.nasa.bt.log.AppLogConfigurator;
import com.nasa.bt.loop.MessageLoopUtils;
import com.nasa.bt.loop.SendDatagramUtils;
import com.nasa.bt.utils.LocalSettingsUtils;

import org.apache.log4j.Logger;

import java.util.Map;

public class ClientHandShakeHelper{

    private Context context;
    private SocketIOHelper helper;

    private String currentIp;

    private static final Logger log= AppLogConfigurator.getLogger();

    public ClientHandShakeHelper(Context context, SocketIOHelper helper, String currentIp) {
        this.context = context;
        this.helper = helper;
        this.currentIp = currentIp;
    }

    private String getNeed(Map<String,String> needParam){
        String pubKeyHash=needParam.get("keyHash");
        String need="";
        if(TextUtils.isEmpty(pubKeyHash)){
            need+=SocketIOHelper.NEED_PUB_KEY+",";
        }else{
            CADao caDao=new CADao(context);
            TrustedRemotePublicKeyEntity trustedRemotePublicKeyEntity=caDao.getTrustedRemoteKey(currentIp);
            if(trustedRemotePublicKeyEntity!=null && trustedRemotePublicKeyEntity.getPublicKeyHash().equals(pubKeyHash)){
                helper.initRSACryptModule(trustedRemotePublicKeyEntity.getPublicKey(),KeyUtils.read().getPri());
                return "";
            }else{
                need+=SocketIOHelper.NEED_PUB_KEY+",";
            }
        }

        if(LocalSettingsUtils.readBoolean(context,LocalSettingsUtils.FIELD_FORCE_CA))
            need+=SocketIOHelper.NEED_CA;
        return need;
    }

    private ParamBuilder prepareHandShakeParam(String need){
        ParamBuilder result=new ParamBuilder();
        if(need.contains(SocketIOHelper.NEED_PUB_KEY)){
            result.putParam(SocketIOHelper.NEED_PUB_KEY, KeyUtils.read().getPub());
        }
        if(need.contains(SocketIOHelper.NEED_CA)){
            String caStr= CAUtils.readCAFile();
            result.putParam(SocketIOHelper.NEED_CA,caStr);
        }

        return result;
    }

    private boolean checkHandShakeParam(Map<String,String> params, String myNeed){
        /**
         * 如果有问题就返回false，没问题就跳过
         */
        String dstPubKey=params.get(SocketIOHelper.NEED_PUB_KEY);
        if(myNeed.contains(SocketIOHelper.NEED_PUB_KEY)){
            if(TextUtils.isEmpty(dstPubKey)){
                log.error("对方公钥为空");
                return false;
            }

            helper.initRSACryptModule(dstPubKey,KeyUtils.read().getPri());
        }
        if(myNeed.contains(SocketIOHelper.NEED_CA)){
            String ca=params.get(SocketIOHelper.NEED_CA);
            if(TextUtils.isEmpty(ca)){
                log.error("证书为空");
                return false;
            }

            CAObject caObject=CAUtils.stringToCAObject(ca);
            if(!CAUtils.checkCA(caObject,currentIp,dstPubKey)){
                return false;
            }

        }

        return true;
    }

    public boolean doHandShake(){
        String feedback= Datagram.HANDSHAKE_FEEDBACK_SUCCESS;
        /**
         * 0.发送需求参数
         * 1.发送需求
         * 2.获取需求
         * 3.发送对方需要的
         * 4.接收自己需要的
         * 5.反馈握手信息（如 成功 证书错误 等）
         */
        ParamBuilder needParam=new ParamBuilder();
        needParam.putParam("name",LocalSettingsUtils.read(context,LocalSettingsUtils.FIELD_NAME));
        needParam.putParam("keyHash", SHA256Utils.getSHA256InHex(KeyUtils.read().getPub()));
        Datagram datagramNeedParamSend=new Datagram(Datagram.IDENTIFIER_NONE,needParam.build());
        if(!helper.writeOsNotEncrypt(datagramNeedParamSend)){
            log.error("发送需求参数失败");
            return false;
        }

        Map<String,String> needParamServer=helper.readIsNotEncrypted().getParamsAsString();


        String myNeed=getNeed(needParamServer);
        ParamBuilder paramBuilderNeedSend=new ParamBuilder().putParam("need",myNeed);
        Datagram datagramNeedSend=new Datagram(Datagram.IDENTIFIER_NONE,paramBuilderNeedSend.build());
        if(!helper.writeOsNotEncrypt(datagramNeedSend)){
            log.error("发送需求失败");
            return false;
        }

        String dstNeed;
        dstNeed=helper.readIsNotEncrypted().getParamsAsString().get("need");
        if(dstNeed==null){
            log.error("读取对方需求失败");
            return false;
        }

        ParamBuilder handShakeParam=prepareHandShakeParam(dstNeed);
        Datagram datagramHandShakeParam=new Datagram(Datagram.IDENTIFIER_NONE,handShakeParam.build());
        if(!helper.writeOsNotEncrypt(datagramHandShakeParam)){
            log.error("发送握手参数失败");
            return false;
        }

        Map<String,String> params;
        if((params=helper.readIsNotEncrypted().getParamsAsString())==null){
            log.error("读取对方握手参数失败");
            return false;
        }

        if(!checkHandShakeParam(params,myNeed)){
            log.error("参数检查失败");

            Intent intent=new Intent(context, SettingsActivity.class);
            intent.putExtra("event",2);
            intent.putExtra("ip",currentIp);
            intent.putExtra(SocketIOHelper.NEED_PUB_KEY, params.get(SocketIOHelper.NEED_PUB_KEY));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            stopConnection();

            return false;
        }else{
            CADao caDao=new CADao(context);
            caDao.addTrustedRemoteKey(new TrustedRemotePublicKeyEntity(currentIp,helper.getRSACryptModuleDstKey()));
        }

        Datagram datagramFeedbackSend=new Datagram(Datagram.IDENTIFIER_NONE,new ParamBuilder().putParam("feedback",feedback).build());
        if(!helper.writeOsNotEncrypt(datagramFeedbackSend)){
            log.error("发送反馈失败");
            return false;
        }

        return readHandShakeFeedback();
    }

    private boolean readHandShakeFeedback(){
        Datagram datagram=helper.readIsNotEncrypted();
        String feedback=datagram.getParamsAsString().get("feedback");
        if(TextUtils.isEmpty(feedback))
            return false;

        if(feedback.equalsIgnoreCase(Datagram.HANDSHAKE_FEEDBACK_SUCCESS)){
            return true;
        }else if(feedback.equalsIgnoreCase(Datagram.HANDSHAKE_FEEDBACK_CA_WRONG)){
            stopConnection();
            Intent intent=new Intent(context, SettingsActivity.class);
            intent.putExtra("event",1);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);

            return false;
        }
        return false;
    }

    private void stopConnection(){
        MessageLoopUtils.sendLocalDatagram(SendDatagramUtils.INBOX_IDENTIFIER_DISCONNECTED);
    }

}
