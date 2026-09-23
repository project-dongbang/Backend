package com.dongbang.finance.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountCipherTest {
    private final AccountCipher cipher = new AccountCipher("test-key-that-is-long-enough-and-not-a-real-secret");

    @Test
    void encryptsAndDecryptsAccountNumber() {
        String encrypted = cipher.encrypt("3333-12-3456789");

        assertThat(encrypted).doesNotContain("3333-12-3456789");
        assertThat(cipher.decrypt(encrypted)).isEqualTo("3333-12-3456789");
        assertThat(cipher.encrypt("3333-12-3456789")).isNotEqualTo(encrypted);
    }
}
