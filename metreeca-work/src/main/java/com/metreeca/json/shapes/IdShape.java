package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import java.net.URI;

public final class IdShape extends ComparableShape<URI> {

	public static Shape id() {
		return new IdShape();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private IdShape() { }


	@Override public <V> V accept(final Visitor<V> visitor) {

		if ( visitor == null ) {
			throw new NullPointerException("null visitor");
		}

		return visitor.visit(this);
	}

}
