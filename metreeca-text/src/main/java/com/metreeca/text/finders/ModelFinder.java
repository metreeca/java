/*
 * Copyright © 2020-2022 Metreeca srl
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

package com.metreeca.text.finders;

import com.metreeca.text.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class ModelFinder implements Function<Chunk, Stream<Chunk>> {

	public static final String Entity="ENTITY";
	public static final String Concept="CONCEPT";
	public static final String Action="ACTION";


	//private static final Model demo=upper(type("JJ")); // demonym/language
	//private static final Model head=type("NNP", "NNPS").or(upper(type("NN", "NNS", "VBG")));
	//private static final Model tail=head.or(demo).or(type("CD"));
	//
	//private static final Model block=head.and(tail.star());
	//private static final Model local=demo.star().and(block);
	//private static final Model chain=local.and(type("IN", "CC", ":").and(local).star());
	//private static final Model where=demo.plus().and(head.not());
	//
	//
	//public static ModelFinder EntityFinder() {
	//	return new ModelFinder(Entity,
	//			block.or(local).or(chain).or(where)
	//	);
	//}

	//public static ModelFinder ConceptFinder() {
	//	return new ModelFinder(Concept,
	//			lower(type("JJ", "VBG")).star().and(lower(type("NN", "NNS")).or(type(":")).plus())
	//	);
	//}

	//public static ModelFinder ActionFinder() {
	//	return new ModelFinder(Action,
	//			lower(type("VB*").plus())
	//	);
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String type;
	private final Model<Token> model;


	public ModelFinder(final String type, final Model<Token> model) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		this.type=type;
		this.model=model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	@Override public Stream<Chunk> apply(final Chunk chunk) {

		if ( chunk == null ) {
			throw new NullPointerException("null chunk");
		}

		final List<Token> tokens=chunk.tokens();
		final Collection<Chunk> matches=new ArrayList<>();

		for (int l, i=0, n=tokens.size(); i < n; i+=(l > 0) ? l : 1) {

			l=model.find(tokens.subList(i, n));

			if ( l > 0 ) {
				matches.add(new Chunk(tokens.subList(i, i+l)).type(type));
			}

		}

		return matches.stream();
	}

}
