package org.aburavov.urlshortener.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ShortCodeGeneratorTest {

    @Test
    void shouldGenerateSevenBase62Chars() {
        var generator = new ShortCodeGenerator();

        var code = generator.generate();

        assertThat(code).hasSize(7).matches("[0-9A-Za-z]+");
        assertThat(generator.generate()).isNotEqualTo(code);
    }
}
