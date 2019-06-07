package com.nasa.bt.loop;

import android.os.Message;

import com.nasa.bt.cls.Datagram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageLoop {
    /**
     * 消息循环，有四个优先级别，分别为0123，从小到大优先级递减
     */

    private static Map<String,List<MessageIntent>[]> intents=new HashMap<>();

    public static void addIntent(MessageIntent intent){
        if(intent==null)
            return;
        String identifier=intent.getIdentifier();
        List<MessageIntent>[] lists=intents.get(identifier);
        if(lists==null)
            lists=new List[4];


        if(lists[intent.getLevel()]==null){
            lists[intent.getLevel()]=new ArrayList<>();
        }
        List<MessageIntent> list=lists[intent.getLevel()];
        for(MessageIntent messageIntent:list){
            if(messageIntent!=null && messageIntent.getId().equals(intent.getId())){
                list.remove(messageIntent);
                break;
            }
        }
        list.add(intent);
        lists[intent.getLevel()]=list;
        intents.put(identifier,lists);
    }

    public static void removeIntent(String identifier,String id,int level){
        if(level>=4)
            return;

        List<MessageIntent>[] lists=intents.get(identifier);
        if(lists==null)
            return;

        if(lists[level]==null)
            return;

        for(int i=0;i<lists[level].size();i++){
            if(lists[level].get(i).getId().equals(id)){
                lists[level].remove(i);
                intents.put(identifier,lists);
                break;
            }

        }
    }

    public static void processDatagram(Datagram datagram){
        List<MessageIntent>[] lists=intents.get(datagram.getIdentifier());
        if(lists==null)
            return;

        for(int i=0;i<4;i++){
            if(lists[i]==null)
                continue;

            for(MessageIntent intent:lists[i]){
                Message msg=new Message();
                msg.what=intent.getResponseCode();
                msg.obj=datagram;
                intent.getProHandler().sendMessage(msg);
            }
        }
    }

}
