package com.metreeca.core;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

final class XtreamTest {

    @Nested final class Loop {

        @Test void testIncludeStartingPoint() {
            assertThat(Xtream.of(0).loop(n -> n < 2 ? Xtream.of(n+1) : Xtream.empty()))
                    .containsExactly(0, 1, 2);
        }

    }

    @Nested final class Scan {

        @Test void testIncludeStartingPoint() {
            assertThat(Xtream.of(0).scan(n -> n < 3
                    ? Xtream.of(entry(Stream.of(n+1), Stream.of(String.valueOf(n))))
                    : Xtream.empty()
            ))
                    .containsExactly("0", "1", "2");
        }

    }

}