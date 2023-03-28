/*
 * Copyright © 2013-2023 Metreeca srl. All rights reserved.
 */

package com.metreeca.open.actions;

import com.metreeca.core.Xtream;
import com.metreeca.core.actions.Fill;
import com.metreeca.core.actions.Limit;
import com.metreeca.http.actions.*;
import com.metreeca.json.codecs.JSON;
import com.metreeca.rdf.Frame;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.function.Function;

import javax.json.*;

import static com.metreeca.rdf.Frame.frame;
import static com.metreeca.rdf.Values.iri;


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
