package org.aburavov.urlshortener.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    @Test
    void shouldGenerateSevenLowercaseChars() {
        ShortCodeGenerator generator = new ShortCodeGenerator();

        String code = generator.generate();

        assertThat(code).hasSize(7).matches("[0-9a-z]+");
        assertThat(generator.generate()).isNotEqualTo(code);
    }
}
