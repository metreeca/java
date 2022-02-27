package com.metreeca.json;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.*;

import static com.metreeca.json.Value.*;

import static java.lang.String.format;


final class ValueReader {

    private static final char ObjectStart='{';
    private static final char ObjectEnd='}';

    private static final char ArrayStart='[';
    private static final char ArrayEnd=']';

    private static final char Colon=':';
    private static final char Comma=',';

    private static final char String='"';
    private static final char Integer='#';
    private static final char Decimal='.';
    private static final char Floating='^';
    private static final char True='t';
    private static final char False='f';
    private static final char Null='n';

    private static final char EOF=0;


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Reader reader;


    ValueReader(final Reader reader) {
        this.reader=reader;
    }


    Value read() throws IOException, ParseException {
        return new Parser(new Scanner(reader)).parse();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static final class Scanner {

        private final Reader reader;
        private final StringBuilder token=new StringBuilder(256);

        private final char[] buffer=new char[1024];

        private int size;
        private int next;

        private int offset;
        private int line;
        private int col;

        private boolean cr;


        private Scanner(final Reader reader) { this.reader=reader; }


        private char scan() throws IOException, ParseException {

            token.setLength(0);

            while ( true ) {
                switch ( read() ) {

                    case ObjectStart:
                    case ObjectEnd:
                    case ArrayStart:
                    case ArrayEnd:
                    case Colon:
                    case Comma:

                        return peek();

                    case String:

                        return string();

                    case '-':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':

                        return number();

                    case 't':

                        return literal("true");

                    case 'f':

                        return literal("false");

                    case 'n':

                        return literal("null");

                    case EOF:

                        return EOF;

                    case ' ':
                    case '\t':
                    case '\r':
                    case '\n':

                        break;

                    default:

                        return error();

                }
            }
        }


        private String token() {
            return token.toString();
        }

        private int offset() {
            return offset;
        }

        private int line() {
            return line+1;
        }

        private int col() {
            return col+1;
        }


        private char string() throws IOException, ParseException {
            while ( true ) {
                switch ( read() ) {

                    case '\\':

                        escape();

                        break;

                    case '"':

                        return String;

                    case 0:

                        return error();

                    default:

                        append();

                        break;

                }
            }
        }

        private char number() throws IOException, ParseException {

            char type=integer();

            if ( read() == '.' ) { type=decimal(); } else { back(); }

            if ( Character.toLowerCase(read()) == 'e' ) { type=floating(); } else { back(); }

            return type;
        }

        private char integer() throws IOException, ParseException {

            final char c=peek();

            if ( c == '-' ) {
                append();
            } else {
                back();
            }

            digit();

            if ( peek() != '0' ) {
                digits();
            }

            return Integer;
        }

        private char decimal() throws IOException, ParseException {

            append();

            digit();
            digits();

            return Decimal;
        }

        private char floating() throws IOException, ParseException {

            append();

            final char c=read();

            if ( c == '-' || c == '+' ) {
                append();
            } else {
                back();
            }

            digit();
            digits();

            return Floating;
        }

        private char literal(final String literal) throws IOException, ParseException {

            if ( literal.charAt(0) != peek() ) {
                return error();
            }

            for (int i=1; i < literal.length(); ++i) {
                if ( read() != literal.charAt(i) ) { return error(); }
            }

            return literal.charAt(0);
        }


        private void append() {
            token.append(peek());
        }

        private void escape() throws IOException, ParseException {
            switch ( read() ) {

                case '"':

                    token.append('"');

                    break;

                case '/':

                    token.append('/');

                    break;

                case '\\':

                    token.append('\\');

                    break;

                case 'b':

                    token.append('\b');

                    break;

                case 'f':

                    token.append('\f');

                    break;

                case 'n':

                    token.append('\n');

                    break;

                case 'r':

                    token.append('\r');

                    break;

                case 't':

                    token.append('\t');

                    break;

                case 'u':

                    token.append((char)(
                            Character.digit(read(), 16)<<12
                                    |Character.digit(read(), 16)<<8
                                    |Character.digit(read(), 16)<<4
                                    |Character.digit(read(), 16)
                    ));

                    break;

                case 'U':

                    token.append(Character.toChars(
                            Character.digit(read(), 16)<<28
                                    |Character.digit(read(), 16)<<24
                                    |Character.digit(read(), 16)<<20
                                    |Character.digit(read(), 16)<<16
                                    |Character.digit(read(), 16)<<12
                                    |Character.digit(read(), 16)<<8
                                    |Character.digit(read(), 16)<<4
                                    |Character.digit(read(), 16)
                    ));

                    break;

                default:

                    error();

                    break;

            }
        }

        private void digit() throws IOException, ParseException {

            final char c=read();

            if ( Character.isDigit(c) ) {

                token.append(c);

            } else {

                back();
                error();

            }
        }

        private void digits() throws IOException {
            while ( true ) {

                final char c=read();

                if ( Character.isDigit(c) ) {

                    token.append(c);

                } else {

                    back();

                    return;

                }
            }
        }


        private char peek() {
            return next > 0 ? buffer[next-1] : EOF;
        }

        private char read() throws IOException {

            if ( next >= size ) {
                if ( (size=reader.read(buffer)) >= 0 ) {
                    next=0;
                } else {
                    return 0;
                }
            }

            final char c=buffer[next++];

            offset++;

            if ( !cr && c == '\n' || (cr=(c == '\r')) ) {
                line++;
                col=0;
            }

            return c;
        }

        private char back() {
            return next > 0 ? buffer[--next] : EOF;
        }


        private <V> V error() throws ParseException {
            throw new ParseException(peek() == EOF
                    ? format("(%d,%d) unexpected end of file", line(), col())
                    : format("(%d,%d) unexpected character <%c>", line(), col(), peek()),
                    offset()
            );
        }

    }

    private static final class Parser {

        private final Scanner scanner;


        private Parser(final Scanner scanner) {
            this.scanner=scanner;
        }


        private Value parse() throws IOException, ParseException {

            final Value value=value();

            return value != null && scanner.scan() == EOF ? value : error();
        }


        private Value value() throws IOException, ParseException {
            switch ( scanner.scan() ) {

                case ObjectStart:

                    return object();

                case ArrayStart:

                    return array();

                case String:

                    return string(scanner.token());

                case Integer:

                    return integer(new BigInteger(scanner.token()));

                case Decimal:

                    return decimal(new BigDecimal(scanner.token()));

                case Floating:

                    return floating(Double.parseDouble(scanner.token()));

                case True:

                    return bool(true);

                case False:

                    return bool(false);

                case Null:

                    return nil();

                default:

                    return null;

            }
        }

        private Value object() throws IOException, ParseException {

            Map<String, Value> fields=null;

            while ( true ) {

                if ( fields == null ) { // first field

                    switch ( scanner.scan() ) {

                        case String:

                            final String label=scanner.token();

                            if ( scanner.scan() != Colon ) { return error(); }

                            final Value value=value();

                            if ( value == null ) { return error(); }

                            (fields=new LinkedHashMap<>()).put(label, value);

                            break;

                        case ObjectEnd:

                            return Value.object(Map.of());

                        default:

                            return error();

                    }

                } else { // other fields

                    switch ( scanner.scan() ) {

                        case Comma:

                            if ( scanner.scan() != String ) { return error(); }

                            final String label=scanner.token();

                            if ( scanner.scan() != Colon ) { return error(); }

                            final Value value=value();

                            if ( value == null ) { return error(); }

                            fields.put(label, value);

                            break;

                        case ObjectEnd:

                            return Value.object(fields);

                        default:

                            return error();

                    }

                }

            }
        }

        private Value array() throws IOException, ParseException {

            List<Value> values=null;

            while ( true ) {

                if ( values == null ) { // first value

                    final Value value=value();

                    if ( value != null ) {

                        (values=new ArrayList<>()).add(value);

                    } else if ( scanner.peek() == ArrayEnd ) {

                        return Value.array(List.of());

                    } else {

                        return error();

                    }

                } else { // other values

                    switch ( scanner.scan() ) {

                        case Comma:

                            final Value value=value();

                            if ( value != null ) {

                                values.add(value);

                            } else {

                                return error();

                            }

                            break;

                        case ArrayEnd:

                            return Value.array(values);

                        default:

                            return error();

                    }

                }

            }
        }


        private <V> V error() throws ParseException {
            return scanner.error();
        }

    }

}
