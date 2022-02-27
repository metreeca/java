package com.metreeca.json.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.ObjectShape.*;
import static com.metreeca.json.shapes.StringShape.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;


final class ObjectShapeTest {

    // !!! handle @id/@type
    // !!! handle local IRI

    @Test void testGuessLabelFromIRI() {

        final Field field=field("http://example.com/terms#value", string());

        assertThat(((ObjectShape)object(field, reverse(field))).fields().keySet())
                .containsExactly("value", "valueOf");

    }

    @Test void testReportClashingLabels() {

        final Field field=field("http://example.com/terms#value", string());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> object(field, field));
    }

}
