PREFIX owl: <http://www.w3.org/2002/07/owl#>
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>

prefix lucene: <http://www.ontotext.com/connectors/lucene#>
prefix index: <http://www.ontotext.com/connectors/lucene/instance#>
prefix rank: <http://www.ontotext.com/owlim/RDFRank#>

select * where {

	values ?a {
		'Europe'
	}

	[a index:entities; lucene:query ?a; lucene:entities ?s].

    ?s lucene:score ?w.
	?s rank:hasRDFRank5 ?r

	optional { ?s rdfs:label ?l filter (lang(?l) in ('', 'en', 'it') )}
	optional { ?s rdfs:comment ?c filter (lang(?c) in ('', 'en', 'it') )}
    
    optional { ?s owl:sameAs ?e }

} order by ?a desc(?r)