package atlassian.agent.keygen;

import java.util.Date;
import java.util.Properties;
import java.util.TreeSet;

public final class LicensePlaintext {
    private LicensePlaintext() {
    }

    public static String format(Date headerTimestamp, Properties properties) {
        StringBuilder sb = new StringBuilder("#");
        sb.append(headerTimestamp.toString());

        TreeSet<String> keys = new TreeSet<>();
        for (Object key : properties.keySet()) {
            keys.add(String.valueOf(key));
        }
        for (String key : keys) {
            sb.append('\n');
            sb.append(key);
            sb.append('=');
            sb.append(properties.getProperty(key));
        }
        return sb.toString();
    }
}
