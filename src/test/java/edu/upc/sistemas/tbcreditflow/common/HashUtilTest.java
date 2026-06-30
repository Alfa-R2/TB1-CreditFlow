package edu.upc.sistemas.tbcreditflow.common;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    void sha256_stringAndBytes_returnSameLowercaseHex() {
        String expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

        assertThat(HashUtil.sha256("abc")).isEqualTo(expected);
        assertThat(HashUtil.sha256("abc".getBytes(StandardCharsets.UTF_8))).isEqualTo(expected);
    }

    @Test
    void sha256_emptyContent_returnsSha256OfEmptyArray() {
        assertThat(HashUtil.sha256(new byte[0]))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb924"
                        + "27ae41e4649b934ca495991b7852b855");
    }
}
