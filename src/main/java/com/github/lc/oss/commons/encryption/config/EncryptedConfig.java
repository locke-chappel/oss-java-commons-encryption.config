package com.github.lc.oss.commons.encryption.config;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.github.lc.oss.commons.util.TypedEnumCache;

public abstract class EncryptedConfig {
    public static class User {
        private final String username;
        private final String password;

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return this.username;
        }

        public String getPassword() {
            return this.password;
        }
    }

    private final TypedEnumCache<?, ConfigKey> keyCache;
    private final Map<ConfigKey, Object> map = new HashMap<>();

    public <T extends Enum<T>> EncryptedConfig(Class<T> keyType) {
        this.keyCache = new TypedEnumCache<>(keyType);
    }

    @JsonAnySetter
    public void set(String key, Object value) {
        if (!this.keyCache.hasName(key)) {
            this.logError(String.format("Ignoring unknown key '%s'.", key));
            return;
        }
        this.set(this.keyCache.byName(key), value);
    }

    @SuppressWarnings("unchecked")
    public void set(ConfigKey key, Object value) {
        if (value instanceof Map) {
            Map<String, String> map = (Map<String, String>) value;
            if (map.containsKey("username") && map.containsKey("password") && map.size() == 2) {
                this.map.put(key, new User(map.get("username"), map.get("password")));
            } else {
                this.map.put(key, value);
            }
        } else {
            this.map.put(key, value);
        }
    }

    public Object get(ConfigKey key) {
        return this.map.get(key);
    }

    protected void logError(String message) {
    }
}
