package com.metreeca.rest.codecs;

import java.io.OutputStream;
import java.util.function.Consumer;

@FunctionalInterface interface Output extends Consumer<OutputStream> { }
