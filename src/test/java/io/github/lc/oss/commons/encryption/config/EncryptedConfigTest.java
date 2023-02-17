package io.github.lc.oss.commons.encryption.config;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.lc.oss.commons.encryption.config.EncryptedConfig.User;
import io.github.lc.oss.commons.testing.AbstractMockTest;

public class EncryptedConfigTest extends AbstractMockTest {
    private enum TestKeys implements ConfigKey {
        Key1,
        Key2;

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

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void test_getset() {
        EncryptedConfig config = new TestConfig();

        Assertions.assertNull(config.get(TestKeys.Key1));
        config.set(TestKeys.Key1.name(), "a");
        Assertions.assertEquals("a", config.get(TestKeys.Key1));

        Assertions.assertNull(config.get(TestKeys.Key2));
        Map<String, String> map = new HashMap<>();
        map.put("username", "name");
        map.put("password", "pass");
        config.set(TestKeys.Key2, map);
        User user = (User) config.get(TestKeys.Key2);
        Assertions.assertNotNull(user);
        Assertions.assertEquals("name", user.getUsername());
        Assertions.assertEquals("pass", user.getPassword());

        config.set(TestKeys.Key2, "b");
        Assertions.assertEquals("b", config.get(TestKeys.Key2));

        config.set("junk", "doesn't matter");
        Map<ConfigKey, Object> innermap = this.getField("map", config);
        Assertions.assertNotNull(innermap);
        Assertions.assertNull(innermap.get("junk"));
    }

    @Test
    public void test_mapValues_extra() {
        EncryptedConfig config = new TestConfig();

        Map<String, String> map = new HashMap<>();
        map.put("username", "user");
        map.put("password", "name");
        map.put("extra", "value");
        config.set(TestKeys.Key1, map);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) config.get(TestKeys.Key1);
        Assertions.assertFalse(result instanceof User);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("user", result.get("username"));
        Assertions.assertEquals("name", result.get("password"));
        Assertions.assertEquals("value", result.get("extra"));
    }

    @Test
    public void test_mapValues_missingUser() {
        EncryptedConfig config = new TestConfig();

        Map<String, String> map = new HashMap<>();
        map.put("password", "name");
        map.put("extra", "value");
        config.set(TestKeys.Key1, map);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) config.get(TestKeys.Key1);
        Assertions.assertFalse(result instanceof User);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("name", result.get("password"));
        Assertions.assertEquals("value", result.get("extra"));
    }

    @Test
    public void test_mapValues_missingPassword() {
        EncryptedConfig config = new TestConfig();

        Map<String, String> map = new HashMap<>();
        map.put("username", "user");
        map.put("extra", "value");
        config.set(TestKeys.Key1, map);
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) config.get(TestKeys.Key1);
        Assertions.assertFalse(result instanceof User);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("user", result.get("username"));
        Assertions.assertEquals("value", result.get("extra"));
    }
}
