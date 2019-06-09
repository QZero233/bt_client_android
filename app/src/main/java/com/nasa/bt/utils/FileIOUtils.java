package com.nasa.bt.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileIOUtils {

    /**
     * 写入文件
     * @param file 文件
     * @param content 内容
     * @return 是否成功
     */
    public static boolean writeFile(File file,byte[] content){
        try{
            FileOutputStream fos=new FileOutputStream(file);
            fos.write(content);
            fos.flush();
            fos.close();
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 读取文件
     * @param file 文件
     * @return 读到的内容，失败返回null
     */
    public static byte[] readFile(File file){
        try {
            FileInputStream fis=new FileInputStream(file);
            byte[] buf=new byte[(int) file.length()];
            fis.read(buf);
            fis.close();
            return buf;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

}
