package com.nasa.bt;

import android.util.Log;

import com.nasa.bt.cls.RSAKeySet;
import com.nasa.bt.crypt.RSAUtils;

import org.junit.Test;

public class RSATest {

    public static String pub="MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgGS5C8t7btYAUfpqOeMt3t2Ae/l3s7o3cfD+FgmXERc5ZDrb/rFxuJ4BRrZkAwBfuMVQ5GdFlUwM+oEsDp722lDHhmjn6QzdnFf6a3WNRccj0rrLG+k6UM1gUiepys+SZw627RCEGWZOlr9rlpky9yE9QSILTIUjLVB00hEMLAWl48W4fLmjznd/IsY/PjhpJkR3cQz8As1z8aaIb4UmLIzftL7JVjnbbMRNjsGWpu03lkZdvrmJUAp3QS35N4AyjkfdU+0DWeie//RV0oBcC8Z/unHWRDzlUmWCd0lqA+Kq06/ZK0QhdfBaO+z5dKiiyYjxuEznltcI0PTvH/O7cQIDAQAB";
    public static String pri="MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCAZLkLy3tu1gBR+mo54y3e3YB7+Xezujdx8P4WCZcRFzlkOtv+sXG4ngFGtmQDAF+4xVDkZ0WVTAz6gSwOnvbaUMeGaOfpDN2cV/prdY1FxyPSussb6TpQzWBSJ6nKz5JnDrbtEIQZZk6Wv2uWmTL3IT1BIgtMhSMtUHTSEQwsBaXjxbh8uaPOd38ixj8+OGkmRHdxDPwCzXPxpohvhSYsjN+0vslWOdtsxE2OwZam7TeWRl2+uYlQCndBLfk3gDKOR91T7QNZ6J7/9FXSgFwLxn+6cdZEPOVSZYJ3SWoD4qrTr9krRCF18Fo77Pl0qKLJiPG4TOeW1wjQ9O8f87txAgMBAAECggEAWj/yvvK6ge9VbEp8rtIc5UKH1R1HYgNsg+AlinIVSUMs6WJiT4aFbINMXFtKs71oi6XPNa3OAKLjoJZ51JEy7cTIa5eEJJIRmJtTAIQx2LGhYbhBmw33GuMyaK1Osav0uhJizQLGCGCcr2RAiSyI4aPwvb7jeFdBodGjPCIVjz8owqJUMXEidLLufcie3A6NOqPNDuG/OyVkTblDI6hVgyEH93C4sptzmk+YcUS3qMTHqLJHj/Rp5UbnDYPNZBQrLWPrcWCKkq2qGRBBsoIkvds3eBaQUKLbbONZ488BsndL4OAvkOXYkEBNabwmcGFJ6RUZSYq5sPqvEu92jcY6wQKBgQDe8yO0hmf+H98FV+rYnkqsSo1dIHbKAuZ5wl1413x3a/mwQd/nXlEJQ9JnOSqYHdbq8HU0PDI8T0fQ4UOMnmG3jUxvb/W4FmCdUYDQuYlqtZxjpAsQb6SLjQ8WaiT2CbJHI3Wh49R03rkMtzp0OXmL6L22+RSZaHrMRr6vKcbpWQKBgQCTbTTXWGZ8qnKluRn1mxHHlodlhWcWQlFJwasBh3zb4QrNSPoq2pKUKn6HmOWVYYMsFJC2+exnAaf6vCuarxsBf/H40o48ot+fFTpwP52EH1f8zgUN2BbvoCrPng/HGR64rZZg3M81rcdYZiQihcoyu+C6WgN3i14nRPNUCFqH2QKBgHALcV5ATfMAwWxGCthidNSxgunSbuCHJJz8eU1JvlumAA38jTIRzFgDpbIKoMVh+aiIv95Iglac4VKwYupAjotYj5lRgwPI+zUUyNAc7lqaesX4ozbXZLJdab+yCHE93kKyJ2P8w8EYTZd+XfobCGKvzOgvtZKDV3Nh+mK9dCHBAoGAQBKva4RFDpt5tmLrUF4z7P2UHOBMvFTYRWkBbGm5L0rwSeYUq/lQyUpqsX4XEcROoSLS/mNChkYm/oc3oEPIRe+Yd57zKJFVBSvRuSU8zUZeFEZp1el7lmuTD6bPjqVxP6xZ9gEhoV/EirvhqothJImZEwS6CwOsSP7jjjsPNYkCgYEAw3kPyXM8g3XqzoeI6/DNseGjKd+8Eo7MseE0Tu1J2sr1ZB8L23cNaZ+JUu9z253fC2Z6tT4aYUbfYjQXmX7jnOPRuvp/iy8ukwKHxptj4ZFcMcDw/MsgKVoj8byoeRoJ0dS/BHkoxuvBE+AgXzSID+UPkK93gL7gIe+WN241Xs8=";
    public static String cipherTest="OhDXwmFsarH0rAYTWrYmaAS7IgzPjH6Xyz5EJjTYA0UksfWZSHjATd0jAmLaA1Uxo8YPwmA9hCjrxdFQ7boRj2SULe2E+uDopeRcWfY6zPHdzuDXCg+j9ibTQvRkxJkUtr++efB6r6Zo0bn+ZUt4UZ45MrFMcb6LPgk4A/fqCgzg7P+j7cIQ21OV2ZNy+MgLcjsZSW5TCc7GbTSFhiCSo6meb+K2dypqvfU/uyjSEhu/z1dSJTbrvaOgjWisyo7jrF3PAI7DJ/+uiJFDgLn5eR66ImfV9UArV7Mv+U3wYgPWJR8qEG6SdY/z3zGS8sXFUH94yKhEsWTHasa/7k1Y6Q==";

    @Test
    public void testRSAPubEn() throws Exception{
        RSAUtils rsa=new RSAUtils(new RSAKeySet(pub,pri));

        String clearText="今年下半年，中美合拍的西游记即将正式开机，我继续扮演美猴王孙悟空，我会用美猴王艺术形象努力创造一个正能量的形象，文体两开花，弘扬中华文化，希望大家能多多关注";

        String cipherText=rsa.publicEncrypt(clearText);

        String clear=rsa.privateDecrypt(cipherText);

        Log.d("nasa",cipherText);

        Log.d("nasa",clear);

    }

    @Test
    public void testRSAPriEn() throws Exception{
        RSAUtils rsa=new RSAUtils(new RSAKeySet(pub,pri));

        String clearText="今年下半年，中美合拍的西游记敢问路在何方即将正式开机，我继续扮演美猴王孙悟空，我会用美猴王艺术形象努力创造一个正能量的形象，文体两开花，弘扬中华文化，希望大家能多多关注";

        String cipherText=rsa.privateEncrypt(clearText);

        String clear=rsa.publicDecrypt(cipherText);

        Log.d("nasa",cipherText);

        Log.d("nasa",clear);

    }

}
