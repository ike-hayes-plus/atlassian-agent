package atlassian.agent.utils;

public class Base64 {
    public static byte[] decode(String val) {
        return java.util.Base64.getDecoder().decode(val);
    }

    public static String encode(byte[] data) {
        return java.util.Base64.getEncoder().encodeToString(data);
    }
}
