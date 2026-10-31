package atlassian.agent.keygen;

import atlassian.agent.license.LicenseProperty;
import atlassian.agent.utils.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Properties;
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

public class Encoder {
    private static final PrivateKey LICENSE_STRING_PRIVATE_KEY = AgentKeys.getLicenseStringPrivateKey();

    public static String encode(LicenseProperty property)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Properties properties = property.toProperties();
        properties.setProperty("keyVersion", AgentKeys.LICENSE_HASH_KEY_VERSION);
        properties.setProperty("licenseHash", LicenseHashSigner.sign(properties));
        String licenseText = LicensePlaintext.format(property.getGenerationDate(), properties);
        return encode(licenseText);
    }

    public static String encode(String licenseText) throws IOException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] licenseData = zipText(licenseText.getBytes());
        byte[] text = new byte[licenseData.length + 5];
        text[0] = 13;   // license prefix
        text[1] = 14;
        text[2] = 12;
        text[3] = 10;
        text[4] = 15;
        System.arraycopy(licenseData, 0, text, 5, licenseData.length);

        Signature signature = Signature.getInstance("SHA1withDSA");
        signature.initSign(LICENSE_STRING_PRIVATE_KEY);
        signature.update(text);

        return packLicense(text, signature.sign());
    }

    private static byte[] zipText(byte[] licenseText) throws IOException {
        int len;
        byte[] buff = new byte[64];
        ByteArrayInputStream in = new ByteArrayInputStream(licenseText);
        DeflaterInputStream deflater = new DeflaterInputStream(in, new Deflater());
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            while ((len = deflater.read(buff)) > 0) {
                out.write(buff, 0, len);
            }

            return out.toByteArray();
        } finally {
            out.close();
            deflater.close();
            in.close();
        }
    }

    private static String packLicense(byte[] text, byte[] hash) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DataOutputStream dOut = new DataOutputStream(out);
        dOut.writeInt(text.length);
        dOut.write(text);
        dOut.write(hash);

        String result = Base64.encode(out.toByteArray()).trim();
        return split(result + "X02" + Integer.toString(result.length(), 31));
    }

    private static String split(String licenseData) {
        if (licenseData == null || licenseData.length() <= 0) {
            return licenseData;
        }

        char[] chars = licenseData.toCharArray();
        StringBuilder buf = new StringBuilder(chars.length + chars.length / 76);

        for (int i = 0; i < chars.length; ++i) {
            buf.append(chars[i]);
            if (i > 0 && i % 76 == 0) {
                buf.append('\n');
            }
        }

        return buf.toString();
    }
}
