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

package com.metreeca.xml.actions;

import com.metreeca.core.Xtream;
import com.metreeca.rest.Message;
import com.metreeca.rest.Request;
import com.metreeca.rest.actions.*;
import com.metreeca.rest.processors.Regex;
import com.metreeca.xml.formats.HTMLFormat;
import com.metreeca.xml.processors.XPath;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.xml.formats.HTMLFormat.html;

import static java.lang.Runtime.getRuntime;

/**
 * Site crawling.
 *
 * <p>Maps site root URLs to streams of URLs for HTML site pages.</p>
 */
public final class Crawl implements Function<String, Stream<String>> {

    // !!! inline after linking context to threads in the execution service

    private final Function<String, Optional<Request>> head=new Query(request -> request.method(HEAD));
    private final Function<String, Optional<Request>> get=new Query();

    private final Function<Message<?>, Optional<Document>> parse=new Parse<>(html()); // !!! support xhtml


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // !!! honour robots.txt
    // !!! session state


    private int threads;

    private Fetch fetch=new Fetch();

    private Function<? super Node, Optional<Node>> focus=Optional::of;
    private BiPredicate<String, String> prune=(root, link) -> true;


    /**
     * Configures the number of concurrent requests (defaults to the number of processors)
     *
     * @param threads the maximum number of concurrent resource fetches; equivalent to the number of system processors if
     *                equal to zero
     *
     * @return this action
     *
     * @throws IllegalArgumentException if {@code threads} is negative
     */
    public Crawl threads(final int threads) {

        if ( threads < 0 ) {
            throw new IllegalArgumentException("negative thread count");
        }

        this.threads=threads;

        return this;
    }

    /**
     * Configures the fetch action (defaults to {@link Fetch}.
     *
     * @param fetch the action used to fetch pages
     *
     * @return this action
     *
     * @throws NullPointerException if {@code fetch} is null
     */
    public Crawl fetch(final Fetch fetch) {

        if ( fetch == null ) {
            throw new NullPointerException("null fetch");
        }

        this.fetch=fetch;

        return this;
    }

    /**
     * Configures the content focus action (defaults to the identity function).
     *
     * @param focus a function taking as argument an element and returning an optional partial/restructured focus
     *              element, if one was identified, or an empty optional, otherwise
     *
     * @return this action
     *
     * @throws NullPointerException if {@code focus} is null
     */
    public Crawl focus(final Function<? super Node, Optional<Node>> focus) {

        if ( focus == null ) {
            throw new NullPointerException("null focus");
        }

        this.focus=focus;

        return this;
    }

    /**
     * Configures the prune action (defaults to always pass).
     *
     * @param prune a bi-predicate taking as arguments the site root URL and a link URL and returning {@code true} if the
     *              link targets a site page or {@code false} otherwise
     *
     * @return this action
     *
     * @throws NullPointerException if {@code prune} is null
     */
    public Crawl prune(final BiPredicate<String, String> prune) {

        if ( prune == null ) {
            throw new NullPointerException("null prune");
        }

        this.prune=prune;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Crawls a site.
     *
     * @param root the root URL of the site to be crawled
     *
     * @return a stream of links to nested HTML pages reachable from the root {@code root}; empty if {@code root} is null
     * or empty
     */
    @Override public Stream<String> apply(final String root) {
        return root == null || root.isEmpty() ? Stream.empty() : new Crawler(root).crawl();
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final class Crawler {

        private final String root;
        private final Map<String, Boolean> pages=new ConcurrentHashMap<>();

        private final Phaser phaser=new Phaser(); // !!! handle 65k limit with tiered phasers
        private final ExecutorService executor=Executors.newFixedThreadPool(
                threads > 0 ? threads : getRuntime().availableProcessors()
                // !!! custom thread factory for linking context
        );


        private Crawler(final String root) {
            this.root=root;
        }


        private Stream<String> crawl() {
            try {

                phaser.register();

                crawl(root);

                phaser.arriveAndAwaitAdvance();

                return pages
                        .entrySet().stream()
                        .filter(Map.Entry::getValue)
                        .map(Map.Entry::getKey);


            } finally {

                executor.shutdown();

            }
        }


        private void crawl(final String page) {
            if ( pages.putIfAbsent(page, false) == null ) { // mark as pending

                phaser.register();

                executor.execute(() -> {
                    try {

                        Xtream

                                .of(page)

                                .filter(link -> Xtream.of(link)

                                        .optMap(head)
                                        .optMap(fetch)

                                        .anyMatch(response -> response
                                                .header("Content-Type")
                                                .filter(HTMLFormat.MIMEPattern.asPredicate())
                                                .isPresent()
                                        )

                                )

                                .optMap(get)
                                .optMap(fetch)

                                .optMap(parse)
                                .optMap(focus)

                                .peek(node -> pages.put(page, true)) // successfully processed

                                .map(XPath::new).flatMap(xpath -> xpath.links("//html:a/@href"))

                                .map(Regex::new).map(regex -> regex.replace("#.*$", "")) // remove anchor
                                .map(Regex::new).map(regex -> regex.replace("\\?.*$", "")) // remove query // !!! ?

                                .filter(link -> { // keep only nested resources
                                    try {

                                        final URI origin=new URI(root).normalize();
                                        final URI target=new URI(link).normalize();

                                        return !origin.relativize(target).equals(target);

                                    } catch ( final URISyntaxException e ) {

                                        return false;

                                    }
                                })

                                .filter(link -> prune.test(root, link))

                                .forEach(this::crawl);

                    } finally {

                        phaser.arrive();

                    }
                });

            }
        }

    }

}
