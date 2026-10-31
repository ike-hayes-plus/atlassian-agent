package atlassian.agent.keygen;

import atlassian.agent.utils.Base64;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Properties;
import java.util.TreeSet;

public final class LicenseHashSigner {
    private LicenseHashSigner() {
    }

    public static String sign(Properties licenseProperties)
            throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Properties canonical = new Properties();
        canonical.putAll(licenseProperties);
        canonical.remove("licenseHash");

        String encodedPayload = Base64.encode(buildCanonicalStore(canonical).getBytes(StandardCharsets.UTF_8));

        PrivateKey privateKey = AgentKeys.getLicenseHashPrivateKey();
        Signature signature = Signature.getInstance("SHA1withDSA");
        signature.initSign(privateKey);
        signature.update(Base64.decode(encodedPayload));
        return Base64.encode(signature.sign());
    }

    /**
     * Same wire format as {@code DefaultPropertiesPersister#store(..., sorted=true)} on {@code SortedProperties}.
     */
    static String buildCanonicalStore(Properties properties) {
        StringBuilder writer = new StringBuilder();
        TreeSet<String> keys = new TreeSet<>();
        for (Object key : properties.keySet()) {
            keys.add(String.valueOf(key));
        }
        for (String key : keys) {
            writer.append(escape(key, true))
                    .append('=')
                    .append(escape(properties.getProperty(key), false))
                    .append('\n');
        }
        return writer.toString();
    }

    /**
     * Matches {@code DefaultPropertiesPersister#escape(String, boolean)}.
     */
    static String escape(String value, boolean isKey) {
        if (value == null) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(value.length() + 8);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case ' ':
                    if (isKey || index == 0) {
                        escaped.append('\\');
                    }
                    escaped.append(' ');
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                default:
                    if ("=: \t\r\n\f#!".indexOf(character) >= 0) {
                        escaped.append('\\');
                    }
                    escaped.append(character);
                    break;
            }
        }
        return escaped.toString();
    }
}
