/*
 * Copyright © 2013-2022 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.jsonld.codecs;

import com.metreeca.link.Shape;
import com.metreeca.link.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.json.JSONAssert.assertThat;
import static com.metreeca.link.Values.*;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Map.entry;

import static javax.json.Json.*;
import static javax.json.JsonValue.EMPTY_JSON_OBJECT;

final class JSONLDEncoderTest {

    private static final String base="http://example.com/";

    private static final IRI focus=iri(base);

    private final IRI w=iri(base, "w");
    private final IRI x=iri(base, "x");
    private final IRI y=iri(base, "y");
    private final IRI z=iri(base, "z");


    private JsonObject encode(
            final IRI focus, final Shape shape, final Statement... model
    ) {
        return encode(focus, shape, emptyMap(), model);
    }

    private JsonObject encode(
            final IRI focus, final Shape shape, final Map<String, String> keywords, final Statement... model
    ) {
        return new JSONLDEncoder(focus, shape, keywords, false).encode(asList(model));
    }


    @Nested final class Values {

        private JsonValue encode(final Value value) {
            return new JSONLDEncoder(focus, Field.field(RDF.VALUE, Shape.optional()), emptyMap(), false)
                    .encode(singleton(statement(focus, RDF.VALUE, value))) // wrap value inside root object
                    .get("value"); // then unwrap
        }


        @Test void testBNode() {
            assertThat(encode(bnode())).isEqualTo(EMPTY_JSON_OBJECT);
        }


        @Test void testIRIInternal() {
            assertThat(encode(iri(base, "/x"))).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .build());
        }

        @Test void testIRIExternal() {
            assertThat(encode(iri("http://example.net/"))).isEqualTo(createObjectBuilder()
                    .add("@id", "http://example.net/")
            );
        }


        @Test void testBoolean() {
            assertThat(encode(True)).isEqualTo(JsonValue.TRUE);
            assertThat(encode(False)).isEqualTo(JsonValue.FALSE);
        }

        @Test void testString() {
            assertThat(encode(literal("string"))).isEqualTo(createValue("string"));
        }

        @Test void testInteger() {
            assertThat(encode(literal(1))).isEqualTo(createValue(1));
        }

        @Test void testDecimal() {
            assertThat(encode(literal(1.0))).isEqualTo(createValue(1.0));
        }

        @Test void testInt() {
            assertThat(encode(literal(1, true))).isEqualTo(createObjectBuilder()
                    .add("@value", createValue("1"))
                    .add("@type", createValue(XSD.INT.stringValue()))
            );
        }

        @Test void testDouble() {
            assertThat(encode(literal(1.0, true))).isEqualTo(createObjectBuilder()
                    .add("@value", createValue("1.0"))
                    .add("@type", createValue(XSD.DOUBLE.stringValue()))
            );
        }


        @Test void testTyped() {
            assertThat(encode(literal("2019-04-03", XSD.DATE))).isEqualTo(createObjectBuilder()
                    .add("@value", createValue("2019-04-03"))
                    .add("@type", createValue(XSD.DATE.stringValue()))
            );
        }

        @Test void testTagged() {
            assertThat(encode(literal("value", "en"))).isEqualTo(createObjectBuilder()
                    .add("@value", createValue("value"))
                    .add("@language", createValue("en"))
            );
        }

    }

    @Nested final class Focus {

        @Test void testIgnoreNoNFocusNodes() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and()),

                    statement(x, RDF.VALUE, literal("x")),
                    statement(y, RDF.VALUE, literal("y"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createArrayBuilder()
                            .add("x")
                    )
            );
        }

        @Test void testHandleUnknownFocusNode() {
            assertThat(encode(z,

                    Field.field(RDF.VALUE, And.and()),

                    statement(x, RDF.VALUE, literal("x")),
                    statement(y, RDF.VALUE, literal("y"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/z")
            );
        }

    }

    @Nested final class References {

        @Test void testExpandSharedTrees() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.repeatable(),
                            Field.field(RDF.VALUE, Shape.required())
                    )),

                    statement(x, RDF.VALUE, w),
                    statement(x, RDF.VALUE, y),

                    statement(w, RDF.VALUE, z),
                    statement(y, RDF.VALUE, z)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createArrayBuilder()

                            .add(createObjectBuilder()
                                    .add("@id", "/w")
                                    .add("value", createObjectBuilder()
                                            .add("@id", "/z")
                                    )
                            )

                            .add(createObjectBuilder()
                                    .add("@id", "/y")
                                    .add("value", createObjectBuilder()
                                            .add("@id", "/z")
                                    )
                            )

                    )
            );
        }

        @Test void testHandleNamedLoops() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(),
                            Field.field(RDF.VALUE, Shape.required())
                    )),

                    statement(x, RDF.VALUE, y),
                    statement(y, RDF.VALUE, x)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "/y")
                            .add("value", createObjectBuilder()
                                    .add("@id", "/x")
                            )
                    )
            );
        }

        @Test void testHandleBlankLoops() {

            final BNode a=bnode("a");
            final BNode b=bnode("b");

            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(),
                            Field.field(RDF.VALUE, And.and(Shape.required(),
                                    Field.field(RDF.VALUE, Shape.required())
                            ))
                    )),

                    statement(x, RDF.VALUE, a),
                    statement(a, RDF.VALUE, b),
                    statement(b, RDF.VALUE, a)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "_:a")
                            .add("value", createObjectBuilder()
                                    .add("value", createObjectBuilder()
                                            .add("@id", "_:a")
                                    )
                            )
                    )
            );
        }


        @Test void testBNodeWithBackLinkToProvedResource() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(),
                            Field.field(RDF.VALUE, And.and(Shape.required(), Datatype.datatype(ResourceType)))
                    )),

                    statement(x, RDF.VALUE, y),
                    statement(y, RDF.VALUE, x)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "/y")
                            .add("value", "/x")
                    )
            );
        }

    }

    @Nested final class Aliases {

        @Test void testAliasDirectField() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Shape.required()),

                    statement(x, RDF.VALUE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "/y")
                    )
            );
        }

        @Test void testAliasInverseField() {
            assertThat(encode(x,

                    Field.field(inverse(RDF.VALUE), Shape.required()),

                    statement(y, RDF.VALUE, x)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("valueOf", "/y")
            );
        }

        @Test void testAliasUserLabelledField() {
            assertThat(encode(x,

                    Field.field("alias", RDF.VALUE, Shape.required()),

                    statement(x, RDF.VALUE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("alias", createObjectBuilder()
                            .add("@id", "/y")
                    )
            );
        }

        @Test void testAliasNestedField() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(),
                            Field.field("alias", RDF.VALUE, Shape.required())
                    )),

                    statement(x, RDF.VALUE, y),
                    statement(y, RDF.VALUE, z)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "/y")
                            .add("alias", createObjectBuilder()
                                    .add("@id", "/z")
                            )
                    )
            );
        }

        @Test void testRejectAliasClashes() {
            assertThatThrownBy(() -> encode(x,

                    And.and(
                            Field.field(RDF.VALUE),
                            Field.field(iri(base, "value"))
                    )

            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test void testRejectReservedAliases() {
            assertThatThrownBy(() -> {
                encode(x,

                        Field.field("@id", RDF.VALUE)

                );
            }).isInstanceOf(IllegalArgumentException.class);
        }

    }

    @Nested final class IRIs {

        private final IRI container=iri(base, "/container/");


        @Test void testRootRelativizeProvedIRIs() {
            assertThat(encode(container,

                    Field.field(RDF.VALUE, Datatype.datatype(IRIType)),

                    statement(container, RDF.VALUE, iri(base, "/container/x")),
                    statement(container, RDF.VALUE, iri(base, "/container/y"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/container/")
                    .add("value", createArrayBuilder()
                            .add("/container/x")
                            .add("/container/y")
                    )
            );
        }

        @Test void testRelativizeProvedIRIBackReferences() {
            assertThat(encode(container,

                    Field.field(RDF.VALUE, And.and(Shape.required(), Datatype.datatype(IRIType))),

                    statement(container, RDF.VALUE, container)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/container/")
                    .add("value", "/container/")
            );
        }

    }

    @Nested final class Shapes {

        @Test void testOmitMissingValues() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Shape.optional())

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
            );
        }

        @Test void testOmitEmptyArrays() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and())

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
            );
        }


        @Test void testCompactProvedScalarValue() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, MaxCount.maxCount(1)),

                    statement(x, RDF.VALUE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("@id", "/y")
                    )
            );
        }

        @Test void testCompactProvedLeafIRI() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(), Datatype.datatype(IRIType))),

                    statement(x, RDF.VALUE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", "/y")
            );
        }

        @Test void testCompactProvedTypedLiteral() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, And.and(Shape.required(), Datatype.datatype(XSD.DATE))),

                    statement(x, RDF.VALUE, literal("2019-04-03", XSD.DATE))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", "2019-04-03")
            );
        }


        @Test void testCompactProvedTaggedValues() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Datatype.datatype(RDF.LANGSTRING)),

                    statement(x, RDF.VALUE, literal("one", "en")),
                    statement(x, RDF.VALUE, literal("two", "en")),
                    statement(x, RDF.VALUE, literal("uno", "it"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("en", createArrayBuilder().add("one").add("two"))
                            .add("it", createArrayBuilder().add("uno"))
                    )
            );
        }

        @Test void testCompactProvedLocalizedValues() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Localized.localized()),

                    statement(x, RDF.VALUE, literal("one", "en")),
                    statement(x, RDF.VALUE, literal("uno", "it"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createObjectBuilder()
                            .add("en", createValue("one"))
                            .add("it", createValue("uno"))
                    )
            );
        }

        @Test void testCompactProvedTaggedValuesWithKnownLanguage() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Lang.lang("en")),

                    statement(x, RDF.VALUE, literal("one", "en")),
                    statement(x, RDF.VALUE, literal("two", "en"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createArrayBuilder()
                            .add("one")
                            .add("two")
                    )
            );
        }

        @Test void testCompactProvedLocalizedValuesWithKnownLanguage() {
            assertThat(encode(x,

                    Field.field(RDF.VALUE, Localized.localized(), Lang.lang("en")),

                    statement(x, RDF.VALUE, literal("one", "en"))

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("value", createValue("one"))
            );
        }


        @Test void testConsiderDisjunctiveDefinitions() {
            assertThat(encode(x,

                    Or.or(
                            Field.field(RDF.FIRST, Shape.required()),
                            Field.field(RDF.REST, Shape.required())
                    ),

                    statement(x, RDF.FIRST, y),
                    statement(x, RDF.REST, z)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("first", createObjectBuilder()
                            .add("@id", "/y")
                    )
                    .add("rest", createObjectBuilder()
                            .add("@id", "/z")
                    )
            );
        }

    }

    @Nested final class Keywords {

        @Test void testHandleType() {
            assertThat(encode(x,

                    Field.field(RDF.TYPE, Shape.required(), Datatype.datatype(IRIType)),

                    statement(x, RDF.TYPE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("@id", "/x")
                    .add("@type", "/y")
            );
        }

        @Test void testHandleAliasedType() {
            assertThat(encode(x,

                    Field.field(RDF.TYPE, Shape.required(), Datatype.datatype(IRIType)),
                    Map.ofEntries(
                            entry("@id", "id"),
                            entry(
                                    "@type", "type")
                    ),

                    statement(x, RDF.TYPE, y)

            )).isEqualTo(createObjectBuilder()
                    .add("id", "/x")
                    .add("type", "/y")
            );
        }

        @Test void testHandleKeywordAliases() {
            assertThat(encode(x,

                    Field.field(RDF.FIRST),

                    Map.ofEntries(
                            entry("@id", "id"),
                            entry("@value", "value"),
                            entry("@type", "type"),
                            entry("@language", "language")
                    ),

                    statement(x, RDF.FIRST, literal("string", "en")),
                    statement(x, RDF.FIRST, literal("2020-09-10", XSD.DATE)))

            ).isEqualTo(createObjectBuilder()
                    .add("id", "/x")
                    .add("first", createArrayBuilder() // keyword alias overrides field alias
                            .add(createObjectBuilder()
                                    .add("value", "string")
                                    .add("language", "en")
                            )
                            .add(createObjectBuilder()
                                    .add("value", "2020-09-10")
                                    .add("type", XSD.DATE.stringValue())
                            )
                    )
            );
        }

    }

    @Nested final class Context {

        private JsonObject encode(final Shape shape) {
            return new JSONLDEncoder(x, shape, emptyMap(), true)
                    .encode(emptyList())
                    .getJsonObject("@context");
        }


        @Test void testDirectFields() {
            assertThat(encode(Field.field(RDF.VALUE)))
                    .hasField("value", RDF.VALUE.stringValue());

        }

        @Test void testInverseFields() {
            assertThat(encode(Field.field(inverse(RDF.VALUE))))
                    .hasField("valueOf", createObjectBuilder()
                            .add("@reverse", RDF.VALUE.stringValue())
                    );

        }

        @Test void testProvedIRIs() {
            assertThat(encode(Field.field(RDF.VALUE, Datatype.datatype(IRIType))))
                    .hasField("value", createObjectBuilder()
                            .add("@id", RDF.VALUE.stringValue())
                            .add("@type", "@id")
                    );
        }

        @Test void testKnownDatatype() {
            assertThat(encode(Field.field(RDF.VALUE, Datatype.datatype(RDF.FIRST))))
                    .hasField("value", createObjectBuilder()
                            .add("@id", RDF.VALUE.stringValue())
                            .add("@type", RDF.FIRST.stringValue())
                    );
        }

        @Test void testProvedTagged() {
            assertThat(encode(Field.field(RDF.VALUE, Localized.localized())))
                    .hasField("value", createObjectBuilder()
                            .add("@id", RDF.VALUE.stringValue())
                            .add("@container", "@language")
                    );
        }

        @Test void testKnownLanguage() {
            assertThat(encode(Field.field(RDF.VALUE, Lang.lang("en"))))
                    .hasField("value", createObjectBuilder()
                            .add("@id", RDF.VALUE.stringValue())
                            .add("@language", "en")
                    );
        }

    }

}