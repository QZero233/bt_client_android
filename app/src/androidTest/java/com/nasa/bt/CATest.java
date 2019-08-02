package com.nasa.bt;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import com.nasa.bt.ca.CABasic;
import com.nasa.bt.ca.CAObject;
import com.nasa.bt.ca.CAUtils;
import com.nasa.bt.crypt.AppKeyStore;
import com.nasa.bt.crypt.SHA256Utils;
import com.nasa.bt.log.AppLogConfigurator;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class CATest {

    public static final String SERVER_PUB_KEY="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAl25SnsKTpQxsJCWpS9eKO2aAlgcfUXc3YK3S5QHNwptxM5GUvYilUjrLvcoaaQsfoxuc5JeBhAKAkRhtAsIis6/4sSsLJuOKMCE8wotkkgF6QJRW8SUnYS/MdFfgdPg11Hc+wZnUSycv4GBfykuW89tKxFK8xYKhLSaJHWPAJbGEvtR0G2ixOGrfSKFNIX8tytCfIzTO31ZCfdMyMp5dnbEwbLC/SRqCdJ4T2stVRjJ/C545NHdKsmAhvuMEffrk6vJRbpqqw65QTK6pHxwcM9YPPqmQ9lBUzI6d6aNxBqiUcTiRwIqltStooDI6VTZx6zUQ66Dhdl0O+l2R2hf/lQIDAQAB";
    private AppKeyStore keyStore;

    private static final Logger log= AppLogConfigurator.getLogger();

    @Before
    public void initKeyUtils(){
        Context context=InstrumentationRegistry.getTargetContext();
        keyStore=AppKeyStore.getInstance();
        keyStore.initKeyStore(context);
    }


    @Test
    public void testVerifyCA(){
        String caStr="MQp7ImVuZFRpbWUiOjE2MDk0MzA0MDAwMDAsInNlcnZlcklwIjoiMTAuMC4yLjIiLCJzZXJ2ZXJQdWJLZXlIYXNoSW5IZXgiOiIyYzo3MDplMToyYjo3YTowNjo0NjpmOToyMjo3OTpmNDoyNzpjNzpiMzo4ZTo3MzozNDpkODplNTozODo5YzpmZjoxNjo3YToxZDpjMzowZTo3MzpmODoyNjpiNjo4MyIsInNpZ25QdWJLZXlIYXNoSW5IZXgiOiJlYTowYzo2ZjphYjozMDphODplNjo5ODoxNDo2Mjo4ZDoyZTpiNTpiNToyNTo0MTpiMDoxMzo1ZjpjZDpmNzoxMzoyZDo3ZDo4Yzo0NjpkZDozZDo0MjpkOTowNDozMyJ9CmN1cDdFZTZPMTZQd09lWmR6T1lzNWhTYjZMZW1ScjBGUkIxYVVSMkVNcFRTUGlkbWU2NGtTRUZPMlJPS1JCMkc1WUFETUxpdk0yYTl0YUZ2c1VvbHZWZTQ0dTF2emRDY0RrRXc2MzEwT0w0SjlyT2E4OUlCei9ObHVYMTlpQmt6aUV4ZWJxelEwQ2pZQldFVi9mZEdFeUtBaTB6dWZ2ckR1cWJ3NEZrUzFDcFlRLzZ3dFZ5TXo3bmo1bXN0Z1FPTG5tK0ZDVmRYSjViVDR6bVNxWWNsdWpJa1dlcFRKdDVid21yY2s2VFNGbVVHQzd3SHZ6ZXBWczhRVU1DYzlCaWNsMmRIUFRxNWoxR0ovVytpSjVVc0hlcHYwQTJMUGJYVnV5R2N4OVlSZjNnQ3pNMEZCVjZzS0t3YjEwS1J4ZytYQjBsSFRzNFRlM0JRa1M0UWpaUTNEZz09";
        CAObject caObject=CAUtils.stringToCAObject(caStr);
        log.info(caObject);

        boolean result=CAUtils.checkCA(caObject,"10.0.2.2","key1");
        log.info(result);
    }

}
