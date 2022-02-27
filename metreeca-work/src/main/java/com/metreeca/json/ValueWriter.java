package com.metreeca.json;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

import static java.util.stream.IntStream.iterate;

final class ValueWriter extends Value.Visitor<Void> {

    private static final int Indent=4;


    private static final ThreadLocal<DecimalFormat> DecimalFormat=ThreadLocal.withInitial(() -> new DecimalFormat(
            "0.0#########", DecimalFormatSymbols.getInstance(Locale.ROOT)
    ));

    private static final ThreadLocal<DecimalFormat> FloatingFormat=ThreadLocal.withInitial(() -> new DecimalFormat(
            "0.0#########E0", DecimalFormatSymbols.getInstance(Locale.ROOT)
    ));


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Writer writer;

    private int depth;


    ValueWriter(final Writer writer) {
        this.writer=writer;
    }


    void write(final Value value) {
        value.accept(this);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Void visit(final Map<String, Value> object) {

        final Iterator<? extends Entry<String, Value>> fields=object.entrySet().iterator();

        write("{");

        iterate(0, n -> fields.hasNext(), n -> n+1).forEachOrdered(n -> {

            final Entry<String, Value> entry=fields.next();

            write(n == 0 ? "\n" : ",\n");

            indent(++depth);
            quote(entry.getKey());
            write(": ");
            write(entry.getValue());

            --depth;

        });

        if ( !object.isEmpty() ) {
            write("\n");
            indent(depth);
        }

        write("}");

        return null;
    }

    @Override public Void visit(final List<Value> array) {

        final Iterator<Value> items=array.iterator();

        write("[");

        iterate(0, n -> items.hasNext(), n -> n+1).forEachOrdered(n -> {

            write(n == 0 ? "\n" : ",\n");

            indent(++depth);
            write(items.next());

            --depth;

        });

        if ( !array.isEmpty() ) {
            write("\n");
            indent(depth);
        }

        write("]");

        return null;
    }


    @Override public Void visit(final URI id) {
        return quote(id.toString());
    }

    @Override public Void visit(final LocalDateTime datetime) {
        return quote(DateTimeFormatter.ISO_DATE_TIME.format(datetime)+"Z");
    }

    @Override public Void visit(final LocalDate date) {
        return quote(DateTimeFormatter.ISO_DATE.format(date)+"Z");
    }

    @Override public Void visit(final LocalTime time) {
        return quote(DateTimeFormatter.ISO_TIME.format(time)+"Z");
    }

    @Override public Void visit(final NavigableMap<Locale, List<String>> localized) {
        throw new UnsupportedOperationException(";(  be implemented"); // !!!
    }


    @Override public Void visit(final String string) {
        return quote(string);
    }

    @Override public Void visit(final BigInteger integer) {
        return write(integer.toString());
    }

    @Override public Void visit(final BigDecimal decimal) {
        return write(DecimalFormat.get().format(decimal));
    }

    @Override public Void visit(final Double floating) {
        return write(FloatingFormat.get().format(floating));
    }

    @Override public Void visit(final Boolean bool) {
        return write(bool ? "true" : "false");
    }

    @Override public Void visit(final Void nil) {
        return write("null");
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Void quote(final String string) {
        try {

            writer.write('"');

            for (int i=0, l=string.length(); i < l; ++i) {

                final char c=string.charAt(i);

                switch ( c ) {

                    case '"':

                        writer.write("\\\"");
                        break;

                    case '\\':

                        writer.write("\\\\");
                        break;

                    case '\b':

                        writer.write("\\b");
                        break;

                    case '\f':

                        writer.write("\\f");
                        break;

                    case '\n':

                        writer.write("\\n");
                        break;

                    case '\r':

                        writer.write("\\r");
                        break;

                    case '\t':

                        writer.write("\\t");
                        break;

                    default:

                        if ( Character.isISOControl(c) ) {
                            writer.write(String.format("\\u%04X", (int)c));
                        } else {
                            writer.write(c);
                        }

                        break;

                }
            }

            writer.write('"');

            return null;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    private Void write(final String literal) {
        try {

            writer.write(literal);

            return null;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

    private Void indent(final int depth) {
        try {

            for (int n=depth*Indent; n > 0; --n) {
                writer.write(' ');
            }

            return null;

        } catch ( final IOException e ) {
            throw new UncheckedIOException(e);
        }
    }

}
