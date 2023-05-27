package utils;

import org.apache.commons.codec.digest.DigestUtils;

public class Utils {
    public static String calculateHA1(String username, String realm, String password) {
        return DigestUtils.md5Hex(username + ":" + realm + ":" + password);
    }
}
