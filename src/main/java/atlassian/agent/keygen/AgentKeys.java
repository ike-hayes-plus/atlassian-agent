package atlassian.agent.keygen;

import atlassian.agent.utils.Base64;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public final class AgentKeys {
    public static final String LICENSE_STRING_KEY_VERSION = "LICENSE_STRING_KEY_V2";
    public static final String LICENSE_HASH_KEY_VERSION = "1600708331";

    /** {@code PublicKeys.LICENSE_STRING_KEY_V2} in atlassian-extras-key-manager. */
    public static final String PUBLIC_KEY_STRING_OFFICIAL =
            "MIIBuDCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYUAAoGBAIvfweZvmGo5otwawI3no7Udanxal3hX2haw962KL/nHQrnC4FG2PvUFf34OecSK1KtHDPQoSQ+DHrfdf6vKUJphw0Kn3gXm4LS8VK/LrY7on/wh2iUobS2XlhuIqEc5mLAUu9Hd+1qxsQkQ50d0lzKrnDqPsM0WA9htkdJJw2nS";

    /** {@code PublicKeys.LICENSE_HASH_KEY_1600708331}. */
    public static final String PUBLIC_KEY_HASH_OFFICIAL =
            "MIIBtzCCASwGByqGSM44BAEwggEfAoGBAP1/U4EddRIpUt9KnC7s5Of2EbdSPO9EAMMeP4C2USZpRV1AIlH7WT2NWPq/xfW6MPbLm1Vs14E7gB00b/JmYLdrmVClpJ+f6AR7ECLCT7up1/63xhv4O1fnxqimFQ8E+4P208UewwI1VBNaFpEy9nXzrith1yrv8iIDGZ3RSAHHAhUAl2BQjxUjC8yykrmCouuEC/BYHPUCgYEA9+GghdabPd7LvKtcNrhXuXmUr7v6OuqC+VdMCz0HgmdRWVeOutRZT+ZxBxCBgLRJFnEj6EwoFhO3zwkyjMim4TwWeotUfI0o4KOuHiuzpnWRbqN/C/ohNWLx+2J6ASQ7zKTxvqhRkImog9/hWuWfBpKLZl6Ae1UlZAFMO/7PSSoDgYQAAoGALZHuJwQzgGnYm/X9BkMcewYQnWjMIGWHd9Yom5Qw7cVIdiZkqpiSzSKurO/WAHHLN31obg7NgGkitWUysECRE3zuJVbKGhx9xjVMnP6z5SwI89vB7Gn7UWxoCvT0JZgcMyQobXeVBtM9J3EgzkdDx/+Dck7uz/l1y+HDNdRzW00=";

    public static final String PUBLIC_KEY_STRING_AGENT = loadResource("dsa_public.pem");
    public static final String PUBLIC_KEY_HASH_AGENT = loadResource("dsa_hash_public.pem");

    private static final PrivateKey LICENSE_STRING_PRIVATE_KEY = loadPrivateKey(loadResource("dsa_private.pem"));
    private static final PrivateKey LICENSE_HASH_PRIVATE_KEY = loadPrivateKey(loadResource("dsa_hash_private.pem"));

    private AgentKeys() {
    }

    public static PrivateKey getLicenseStringPrivateKey() {
        return LICENSE_STRING_PRIVATE_KEY;
    }

    public static PrivateKey getLicenseHashPrivateKey() {
        return LICENSE_HASH_PRIVATE_KEY;
    }

    private static PrivateKey loadPrivateKey(String encoded) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("DSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decode(stripWhitespace(encoded))));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    static String loadResource(String resourceName) {
        try (InputStream in = AgentKeys.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IOException("Missing classpath resource: " + resourceName);
            }
            java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return stripWhitespace(new String(buffer.toByteArray(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static String stripWhitespace(String value) {
        return value.replaceAll("\\s", "");
    }
}
