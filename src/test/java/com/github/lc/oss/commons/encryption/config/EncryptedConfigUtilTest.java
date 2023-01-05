package com.github.lc.oss.commons.encryption.config;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.github.lc.oss.commons.encoding.Encodings;
import com.github.lc.oss.commons.testing.AbstractMockTest;
import com.github.lc.oss.commons.util.IoTools;

public class EncryptedConfigUtilTest extends AbstractMockTest {
    private enum TestKeys implements ConfigKey {
        Key;

        @Override
        public Class<?> type() {
            return String.class;
        }
    }

    private static class TestConfig extends EncryptedConfig {
        public TestConfig() {
            super(TestKeys.class);
        }
    }

    private static final String ENV_VAR = "ITS_A_SECRET";

    private static String tempDir = null;

    private String getTempDir() {
        if (EncryptedConfigUtilTest.tempDir == null) {
            EncryptedConfigUtilTest.tempDir = System.getProperty("java.io.tmpdir").replace("\\", "/");
            if (!EncryptedConfigUtilTest.tempDir.endsWith("/")) {
                EncryptedConfigUtilTest.tempDir += "/";
            }
        }
        return EncryptedConfigUtilTest.tempDir;
    }

    private String getKeyPath() {
        return this.getTempDir() + "encrupted-config-test.key";
    }

    private String getConfigPath() {
        return this.getTempDir() + "encrupted-config-test.cfg";
    }

    @AfterEach
    public void cleanup() {
        File f = new File(this.getKeyPath());
        f.delete();

        f = new File(this.getConfigPath());
        f.delete();

        this.setEnvVar(EncryptedConfigUtilTest.ENV_VAR, null);
    }

    @Test
    public void test_main_badArgument() {
        this.setEnvVar(EncryptedConfigUtilTest.ENV_VAR, "shhh...");

        try {
            EncryptedConfigUtil.main(new String[] { "junk" });
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }

        try {
            EncryptedConfigUtil.main(new String[] { "--config-env-var", EncryptedConfigUtilTest.ENV_VAR });
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }

        try {
            EncryptedConfigUtil.main(new String[] { "--key-path", "path", "--config-env-var", EncryptedConfigUtilTest.ENV_VAR });
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }
    }

    @Test
    public void test_read_badArguments() {
        try {
            EncryptedConfigUtil.read(null, null, null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }

        try {
            EncryptedConfigUtil.read("a", null, null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }

        try {
            EncryptedConfigUtil.read("a", "b", null);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("All arguments are required.", ex.getMessage());
        }
    }

    @Test
    public void test_badPayload() {
        final String clear = "not-json";
        final String encoded = Encodings.Base64.encode(clear);
        this.setEnvVar(EncryptedConfigUtilTest.ENV_VAR, encoded);

        IoTools.writeToFile("pass", this.getKeyPath());

        EncryptedConfigUtil.main(new String[] { //
                "--config-env-var", EncryptedConfigUtilTest.ENV_VAR, //
                "--key-path", this.getKeyPath(), //
                "--config-path", this.getConfigPath() //
        });

        String encrypted = new String(IoTools.readFile(this.getConfigPath()), StandardCharsets.UTF_8);
        Assertions.assertNotEquals(clear, encrypted);
        Assertions.assertNotEquals(encoded, encrypted);

        try {
            EncryptedConfigUtil.read(this.getKeyPath(), this.getConfigPath(), TestConfig.class);
            Assertions.fail("Expected exception");
        } catch (RuntimeException ex) {
            Assertions.assertEquals("Error reading secure config", ex.getMessage());
        }
    }

    @Test
    public void test_valid() {
        final String clear = "{\"" + TestKeys.Key.name() + "\":\"value\"}";
        final String encoded = Encodings.Base64.encode(clear);
        this.setEnvVar(EncryptedConfigUtilTest.ENV_VAR, encoded);

        IoTools.writeToFile("pass", this.getKeyPath());

        EncryptedConfigUtil.main(new String[] { //
                "--config-env-var", EncryptedConfigUtilTest.ENV_VAR, //
                "--key-path", this.getKeyPath(), //
                "--config-path", this.getConfigPath() //
        });

        String encrypted = new String(IoTools.readFile(this.getConfigPath()), StandardCharsets.UTF_8);
        Assertions.assertNotEquals(clear, encrypted);
        Assertions.assertNotEquals(encoded, encrypted);

        TestConfig config = EncryptedConfigUtil.read(this.getKeyPath(), this.getConfigPath(), TestConfig.class);
        Object result = config.get(TestKeys.Key);
        Assertions.assertEquals("value", result);
    }

    /**
     * DANGER: Don't call this method unless you know _exactly_ what you are doing
     * any why you need to call it!
     */
    @SuppressWarnings("unchecked")
    protected void setEnvVar(String key, String value) {
        try {
            /* Windows */
            Object instance = null;
            Class<?> envClass = Class.forName("java.lang.ProcessEnvironment");
            Assertions.assertNotNull(envClass);
            Field field = this.findField("theCaseInsensitiveEnvironment", envClass);
            if (field == null) {
                /* Everyone else */
                field = this.findField("theUnmodifiableEnvironment", envClass);
                boolean accessible = field.canAccess(instance);
                if (!accessible) {
                    field.setAccessible(true);
                }
                instance = field.get(null);
                if (!accessible) {
                    field.setAccessible(false);
                }
                field = this.findField("m", instance.getClass());
            }

            boolean accessible = field.canAccess(instance);
            if (!accessible) {
                field.setAccessible(true);
            }

            Map<String, String> envMap = (Map<String, String>) field.get(instance);
            if (value == null) {
                envMap.remove(key);
            } else {
                envMap.put(key, value);
            }

            if (!accessible) {
                field.setAccessible(false);
            }

        } catch (SecurityException | IllegalArgumentException | IllegalAccessException | ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
