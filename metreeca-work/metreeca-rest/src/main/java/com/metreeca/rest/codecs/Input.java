package com.metreeca.rest.codecs;

import java.io.InputStream;
import java.util.function.Supplier;

@FunctionalInterface interface Input extends Supplier<InputStream> { }
