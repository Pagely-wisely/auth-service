package com.pagely.authservice.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenHasherTest {
    @Test
    @DisplayName("같은 입력은 같은 해시 (결정성)")
    void deterministic() {
        String token = "DXjQz5nNX7Yl2gv7cKbm3HzBp8QmRtV";

        String hash1 = TokenHasher.hash(token);
        String hash2 = TokenHasher.hash(token);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("다른 입력은 다른 해시")
    void uniqueness() {
        String hash1 = TokenHasher.hash("token-a");
        String hash2 = TokenHasher.hash("token-b");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("해시 길이 일정 (Base64URL, 43 chars)")
    void fixedLength() {
        String hashShort = TokenHasher.hash("a");
        String hashLong = TokenHasher.hash("a".repeat(1000));

        // SHA-256 = 32 bytes → Base64URL no padding = 43 chars
        assertThat(hashShort.length()).isEqualTo(43);
        assertThat(hashLong.length()).isEqualTo(43);
    }

    @Test
    @DisplayName("null / blank 는 예외")
    void rejectBlank() {
        assertThatThrownBy(() -> TokenHasher.hash(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenHasher.hash(""))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> TokenHasher.hash("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
