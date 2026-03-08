package com.crawlerApi.integrations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class IntegrationConfigEncryptionTest {

    @Test
    void encryptDecryptReturnsOriginalValueWhenKeyIsConfigured() {
        IntegrationConfigEncryption encryption = new IntegrationConfigEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionKey", "0123456789abcdef");

        String plain = "{\"token\":\"secret\"}";
        String cipher = encryption.encrypt(plain);

        assertNotEquals(plain, cipher);
        assertEquals(plain, encryption.decrypt(cipher));
    }

    @Test
    void encryptAndDecryptArePassThroughWhenKeyIsMissing() {
        IntegrationConfigEncryption encryption = new IntegrationConfigEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionKey", "");

        String plain = "plain-text";
        assertEquals(plain, encryption.encrypt(plain));
        assertEquals(plain, encryption.decrypt(plain));
    }

    @Test
    void encryptAndDecryptHandleNullValues() {
        IntegrationConfigEncryption encryption = new IntegrationConfigEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionKey", "0123456789abcdef");

        assertNull(encryption.encrypt(null));
        assertNull(encryption.decrypt(null));
    }

    @Test
    void encryptSkipsWhenKeyTooShort() {
        IntegrationConfigEncryption encryption = new IntegrationConfigEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionKey", "short");

        String plain = "abc";
        assertEquals(plain, encryption.encrypt(plain));
        assertEquals(plain, encryption.decrypt(plain));
    }

    @Test
    void decryptReturnsOriginalValueWhenCipherIsInvalid() {
        IntegrationConfigEncryption encryption = new IntegrationConfigEncryption();
        ReflectionTestUtils.setField(encryption, "encryptionKey", "0123456789abcdef");

        String invalidCipher = "not-base64";
        assertEquals(invalidCipher, encryption.decrypt(invalidCipher));
    }
}
