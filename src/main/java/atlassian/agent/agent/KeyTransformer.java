package atlassian.agent.agent;

import atlassian.agent.keygen.AgentKeys;
import javassist.ClassPool;
import javassist.LoaderClassPath;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

public class KeyTransformer implements ClassFileTransformer {
    private static final String CN_KEY_SPEC = "java/security/spec/EncodedKeySpec";
    private static final String CN_KEY_MANAGER = "com/atlassian/extras/keymanager/KeyManager";
    private static final String CN_V2_DECODER = "com/atlassian/extras/decoder/v2/Version2LicenseDecoder";
    private static final String CN_FORGED_DETECTOR = "com/atlassian/jira/license/CachingForgedLicenseDetectorImpl";
    private static final String CN_CONF_LICENSE_SERVICE = "com/atlassian/confluence/license/DefaultLicenseService";

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (CN_KEY_SPEC.equals(className)) {
            return handleKeySpec();
        }
        if (CN_KEY_MANAGER.equals(className)) {
            return handleKeyManager(classfileBuffer, loader);
        }
        if (CN_V2_DECODER.equals(className)) {
            return handleVersion2LicenseDecoder(classfileBuffer, loader);
        }
        if (CN_FORGED_DETECTOR.equals(className)) {
            return handleForgedLicenseDetector(classfileBuffer, loader);
        }
        if (CN_CONF_LICENSE_SERVICE.equals(className)) {
            return handleConfluenceDefaultLicenseService(classfileBuffer, loader);
        }
        return classfileBuffer;
    }

    /**
     * Confluence treats hash-verification failures as forged licenses during setup
     * ({@code DefaultLicenseService#getForgedLicense}). Accept decoded agent licenses instead.
     */
    private byte[] handleConfluenceDefaultLicenseService(byte[] classfileBuffer, ClassLoader loader)
            throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            if (loader != null) {
                cp.insertClassPath(new LoaderClassPath(loader));
            }
            CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            cc.getDeclaredMethod("getForgedLicense").setBody("{ return $1; }");
            byte[] patched = cc.toBytecode();
            cc.detach();
            return patched;
        } catch (Exception e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
    }

    private static void patchKeyManagerVerifyCalls(CtMethod method) throws Exception {
        method.instrument(new ExprEditor() {
            @Override
            public void edit(MethodCall m) throws javassist.CannotCompileException {
                if ("com.atlassian.extras.keymanager.KeyManager".equals(m.getClassName())
                        && "verify".equals(m.getMethodName())) {
                    m.replace("{ $_ = true; }");
                }
            }
        });
    }

    /** Suppress Jira 11 forged-license banner (LegitimacyLicenseCheck / CachingForgedLicenseDetectorImpl). */
    private byte[] handleForgedLicenseDetector(byte[] classfileBuffer, ClassLoader loader) throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            if (loader != null) {
                cp.insertClassPath(new LoaderClassPath(loader));
            }
            CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            cc.getDeclaredMethod("isLicenseForged").setBody("{ return false; }");
            byte[] patched = cc.toBytecode();
            cc.detach();
            return patched;
        } catch (Exception e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
    }

    /**
     * Jira decodes setup licenses through {@code Version2LicenseDecoder.checkAndGetLicenseText}.
     * When {@code KeyManager} bytecode hooks fail to apply in Tomcat, skip DSA verify here so
     * agent-signed licenses still decode (hash signing remains in the encoded payload).
     */
    private byte[] handleVersion2LicenseDecoder(byte[] classfileBuffer, ClassLoader loader) throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            if (loader != null) {
                cp.insertClassPath(new LoaderClassPath(loader));
            }
            CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            patchKeyManagerVerifyCalls(cc.getDeclaredMethod("checkAndGetLicenseText"));
            // Confluence 9+ enables licenseHash verification; failed hash check becomes ForgedLicenseException.
            CtMethod verifyHash = cc.getDeclaredMethod("verifyLicenseHash");
            verifyHash.setBody("{ return; }");
            byte[] patched = cc.toBytecode();
            cc.detach();
            return patched;
        } catch (Exception e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
    }

    /**
     * Jira 11+ loads license public keys via {@code KeyManager} and {@code X509EncodedKeySpec},
     * not {@code EncodedKeySpec}; swap embedded Atlassian keys for agent keys on load.
     */
    private byte[] handleKeyManager(byte[] classfileBuffer, ClassLoader loader) throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            if (loader != null) {
                cp.insertClassPath(new LoaderClassPath(loader));
            }
            int mod = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(classfileBuffer));
            CtClass stringType = cp.get(String.class.getName());

            addStringField(cc, stringType, mod, "__km_string_pk", AgentKeys.PUBLIC_KEY_STRING_AGENT);
            addStringField(cc, stringType, mod, "__km_hash_pk", AgentKeys.PUBLIC_KEY_HASH_AGENT);

            CtClass keyType = cp.get("com.atlassian.extras.keymanager.Key");
            CtMethod loadKey = cc.getDeclaredMethod("loadKey", new CtClass[] { keyType });
            loadKey.insertBefore(
                    "if ($1.getType() == com.atlassian.extras.keymanager.Key$Type.PUBLIC) {"
                            + "if (\"LICENSE_STRING_KEY_V2\".equals($1.getVersion())) {"
                            + "$1 = new com.atlassian.extras.keymanager.Key(__km_string_pk, $1.getVersion(), $1.getType());"
                            + "System.out.println(\"============================== agent working (KeyManager license string key) ==============================\");"
                            + "} else if (\"1600708331\".equals($1.getVersion())) {"
                            + "$1 = new com.atlassian.extras.keymanager.Key(__km_hash_pk, $1.getVersion(), $1.getType());"
                            + "System.out.println(\"============================== agent working (KeyManager license hash key) ==============================\");"
                            + "}"
                            + "}");

            CtMethod verify = cc.getDeclaredMethod("verify", new CtClass[] { stringType, stringType, stringType });
            verify.insertBefore(
                    "if (\"LICENSE_STRING_KEY_V2\".equals($3) || \"1600708331\".equals($3)) {"
                            + "try {"
                            + "java.security.PublicKey pk = generatePublicKey(\"LICENSE_STRING_KEY_V2\".equals($3) ? __km_string_pk : __km_hash_pk);"
                            + "java.security.Signature sig = java.security.Signature.getInstance(\"SHA1withDSA\");"
                            + "sig.initVerify(pk);"
                            + "sig.update(org.apache.commons.codec.binary.Base64.decodeBase64($1));"
                            + "return sig.verify(org.apache.commons.codec.binary.Base64.decodeBase64($2));"
                            + "} catch (Exception e) { throw new RuntimeException(e); }"
                            + "}");

            byte[] patched = cc.toBytecode();
            cc.detach();
            return patched;
        } catch (Exception e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
    }

    private byte[] handleKeySpec() throws IllegalClassFormatException {
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.importPackage("java.util.Arrays");

            int mod = Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL;
            CtClass cc = cp.get(CN_KEY_SPEC.replace('/', '.'));
            CtClass cb = cp.get("byte[]");

            addKeyField(cc, cb, mod, "__h_string_ok", AgentKeys.PUBLIC_KEY_STRING_OFFICIAL);
            addKeyField(cc, cb, mod, "__h_string_nk", AgentKeys.PUBLIC_KEY_STRING_AGENT);
            addKeyField(cc, cb, mod, "__h_hash_ok", AgentKeys.PUBLIC_KEY_HASH_OFFICIAL);
            addKeyField(cc, cb, mod, "__h_hash_nk", AgentKeys.PUBLIC_KEY_HASH_AGENT);

            CtConstructor cm = cc.getConstructor("([B)V");
            cm.insertBeforeBody(
                    "if(Arrays.equals($1,__h_string_ok)){$1=__h_string_nk;System.out.println(\"============================== agent working (license string key) ==============================\");}"
                            + "else if(Arrays.equals($1,__h_hash_ok)){$1=__h_hash_nk;System.out.println(\"============================== agent working (license hash key) ==============================\");}");

            return cc.toBytecode();
        } catch (Exception e) {
            throw new IllegalClassFormatException(e.getMessage());
        }
    }

    private static void addKeyField(CtClass target, CtClass byteArrayType, int modifiers, String name, String encodedKey)
            throws Exception {
        CtField field = new CtField(byteArrayType, name, target);
        field.setModifiers(modifiers);
        target.addField(field, "java.util.Base64.getDecoder().decode(\"" + encodedKey + "\");");
    }

    private static void addStringField(CtClass target, CtClass stringType, int modifiers, String name, String value)
            throws Exception {
        CtField field = new CtField(stringType, name, target);
        field.setModifiers(modifiers);
        target.addField(field, "\"" + value + "\";");
    }
}
