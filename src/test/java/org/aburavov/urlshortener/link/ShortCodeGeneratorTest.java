package org.aburavov.urlshortener.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShortCodeGeneratorTest {

    private final ShortCodeGenerator generator = new ShortCodeGenerator(new LinkProperties("http://localhost:8080", 7));

    @Test
    void generatesCodeOfConfiguredLengthFromBase62Alphabet() {
        String code = generator.generate();

        assertThat(code).hasSize(7).matches("[0-9A-Za-z]+");
    }

    @Test
    void generatesDifferentCodes() {
        Set<String> codes = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            codes.add(generator.generate());
        }

        assertThat(codes).hasSize(1000);
    }
}
