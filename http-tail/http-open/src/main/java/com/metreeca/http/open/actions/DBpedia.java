/*
 * Copyright © 2013-2023 Metreeca srl
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

package com.metreeca.http.open.actions;

import com.metreeca.http.actions.*;
import com.metreeca.http.json.formats.JSON;
import com.metreeca.http.rdf.Frame;
import com.metreeca.http.work.Xtream;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.util.function.Function;

import static com.metreeca.http.rdf.Frame.frame;
import static com.metreeca.http.rdf.Values.iri;


/**
 * DBpedia Lookup search.
 *
 * @see <a href="https://github.com/dbpedia/lookup">DBpedia Lookup</a>
 */
public final class DBpedia implements Function<String, Xtream<Frame>> {

    private static final Limit<String> limit=new Limit<>(2);


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Xtream<Frame> apply(final String query) {
        return Xtream.of(limit.apply(query))

                .flatMap(new Fill<>()

                        .model("http://lookup.dbpedia.org/api/search/KeywordSearch"
                                +"?QueryString=%{query}"
                                +"&QueryClass"
                                +"&MaxHits=10"
                        )

                        .value("query", query)

                )

                .optMap(new Query(request -> request.header("Accept", JSON.MIME)))
                .optMap(new Fetch())
                .optMap(new Parse<>(new JSON()))

                .flatMap(response -> response.asJsonObject().getJsonArray("results").stream()
                        .map(JsonValue::asJsonObject)
                        .map(this::result)
                );
    }


    private Frame result(final JsonObject result) {
        return frame(iri(result.getString("uri")))
                .string(RDFS.LABEL, string(result.get("label")))
                .string(RDFS.COMMENT, string(result.get("description")))
                .values(RDF.TYPE, result.getJsonArray("classes").stream().map(clazz ->
                        iri(clazz.asJsonObject().getString("uri"))
                ));
    }

    private String string(final JsonValue value) {
        return value instanceof JsonString ? ((JsonString)value).getString() : null;
    }

}
