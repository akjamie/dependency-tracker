package org.akj.test.tracker.infrastructure.config;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class CredentialEncryptor {
    @Bean(name = "jasyptStringEncryptor")
    public StringEncryptor stringEncryptor(
            @Value("${jasypt.encryptor.password:}") String key,
            @Value("${jasypt.encryptor.key-path}") String keyPath)
            throws IOException {
        log.info("Initializing Jasypt StringEncryptor");
        Path path = Paths.get(keyPath);
        if (StringUtils.isNotBlank(key)) {
            return createEncryptor(key);
        } else {
            return createEncryptor(Files.readAllLines(path).get(0));
        }
    }

    private StringEncryptor createEncryptor(String key) {
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(key);

        config.setKeyObtentionIterations(1000);
        config.setPoolSize(1);

        config.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");

        config.setIvGeneratorClassName("org.jasypt.iv.RandomIvGenerator");
        config.setStringOutputType("base64");

        PooledPBEStringEncryptor stringEncryptor = new PooledPBEStringEncryptor();
        stringEncryptor.setConfig(config);

        return stringEncryptor;
    }
}
