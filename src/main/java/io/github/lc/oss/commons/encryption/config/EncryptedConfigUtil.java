package io.github.lc.oss.commons.encryption.config;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.lc.oss.commons.encoding.Encodings;
import io.github.lc.oss.commons.encryption.Ciphers;
import io.github.lc.oss.commons.encryption.ephemeral.FileBackedCipher;
import io.github.lc.oss.commons.util.IoTools;

public class EncryptedConfigUtil {
    private EncryptedConfigUtil() {
    }

    public static void main(String[] args) {
        String configEnv = null;
        String keyPath = null;
        String configPath = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--config-env-var":
                    i++;
                    configEnv = System.getenv(args[i]);
                    break;
                case "--key-path":
                    i++;
                    keyPath = args[i];
                    break;
                case "--config-path":
                    i++;
                    configPath = args[i];
                    break;
                default:
                    System.err.println(String.format("Unknown argument '%s'", args[i]));
                    break;
            }
        }

        if (EncryptedConfigUtil.isAnyBlank(configEnv, keyPath, configPath)) {
            throw new RuntimeException("All arguments are required.");
        }

        FileBackedCipher cipher = new FileBackedCipher(keyPath);
        IoTools.writeToFile(cipher.encrypt(Encodings.Base64.decode(configEnv), Ciphers.AES256), configPath);
    }

    public static <T extends EncryptedConfig> T read(String keyPath, String configPath, Class<T> clazz) {
        if (EncryptedConfigUtil.isAnyBlank(keyPath, configPath) || clazz == null) {
            throw new RuntimeException("All arguments are required.");
        }

        FileBackedCipher cipher = new FileBackedCipher(keyPath);

        try {
            String json = cipher.decryptString(new String(IoTools.readFile(configPath), StandardCharsets.UTF_8), Ciphers.AES256);
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Error reading secure config", ex);
        }
    }

    private static boolean isAnyBlank(String... strings) {
        for (String s : strings) {
            if (EncryptedConfigUtil.isBlank(s)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        if (s == null) {
            return true;
        }

        return s.trim().equals("");
    }
}
