[![Java CI with Maven](https://github.com/dice-group/orca/actions/workflows/maven.yml/badge.svg)](https://github.com/dice-group/orca/actions/workflows/maven.yml)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6030450cade64e259f69dddbd4a17c14)](https://www.codacy.com/app/MichaelRoeder/ldcbench)

<img src="orca.png" alt="" height="215">

# Benchmark for Data Web Crawlers

**OR**CA is a **C**rawler **A**nalysis benchmark for Data Web Crawlers which runs on [the HOBBIT platform](http://project-hobbit.eu/).
Currently, the following types of data nodes are available:
- RDF data served in various formats over HTTP (dump and dereferencing variants)
- RDFa data embedded in HTML, based on the [RDFa Test Suite](https://github.com/rdfa/rdfa.github.io)
- SPARQL endpoints, based on [Virtuoso](https://virtuoso.openlinksw.com/)
- [CKAN](https://ckan.org/) instances

## License

This project is licensed under the
GNU Affero General Public License v3.0.
For the full license text, see [LICENSE](../LICENSE).

## Source code

- Permanent URL: [http://w3id.org/dice-research/orca/code](http://w3id.org/dice-research/orca/code)
- GitHub: [https://github.com/dice-group/orca](https://github.com/dice-group/orca)

## Documentation

- [Developer information](developer.md)
- [Adding crawlers](adding-crawlers.md)
- [Adding KPIs](adding-KPIs.md)
- [Forking this project](forking.md)

## Crawlers

* **[LDSpider](https://github.com/dice-group/ldcbench-ldspider-adapter)**
* **[Squirrel](https://github.com/dice-group/ldcbench-squirrel-adapter)**
* [How to integrate a crawler with ORCA](adding-crawlers.md)

## Parameters

| Parameter | Description | Ontology resources
| --- | --- | ---
| Number of nodes | The number of nodes in the synthetic graph. | [orca:numberOfNodes](http://w3id.org/dice-research/orca/ontology#numberOfNodes)
| Average node degree | The average degree of the nodes in the generated graph. | [orca:averageNodeGraphDegree](http://w3id.org/dice-research/orca/ontology#averageNodeGraphDegree)
| RDF dataset size | Average number of triples of the generated RDF graphs. | [orca:averageTriplesPerNode](http://w3id.org/dice-research/orca/ontology#averageTriplesPerNode)
| Average resource degree | The average degree of the resources in the RDF graphs. | [orca:averageRdfGraphDegree](http://w3id.org/dice-research/orca/ontology#averageRdfGraphDegree)
| Node type amounts | For each node type, the user can define the proportion of nodes that should have this type. | [orca:httpDumpNodeWeight](http://w3id.org/dice-research/orca/ontology#httpDumpNodeWeight) [orca:dereferencingHttpNodeWeight](http://w3id.org/dice-research/orca/ontology#dereferencingHttpNodeWeight) [orca:sparqlNodeWeight](http://w3id.org/dice-research/orca/ontology#sparqlNodeWeight) [orca:ckanNodeWeight](http://w3id.org/dice-research/orca/ontology#ckanNodeWeight)
| Dump file serialisations | For each available dump file serialisation, a boolean flag can be set. | [orca:useNtDumps](http://w3id.org/dice-research/orca/ontology#useNtDumps) [orca:useN3Dumps](http://w3id.org/dice-research/orca/ontology#useN3Dumps) [orca:useRdfXmlDumps](http://w3id.org/dice-research/orca/ontology#useRdfXmlDumps) [orca:useTurtleDumps](http://w3id.org/dice-research/orca/ontology#useTurtleDumps)
| Dump file compression ratio | Proportion of dump files that are compressed. | [orca:httpDumpNodeCompressedRatio](http://w3id.org/dice-research/orca/ontology#httpDumpNodeCompressedRatio)
| Average ratio of disallowed resources | Proportion of resources that are generated within a node and marked as disallowed for crawling. | [orca:averageDisallowedRatio](http://w3id.org/dice-research/orca/ontology#averageDisallowedRatio)
| Average crawl delay | The crawl delay of the node's `robots.txt` file. | [orca:averageCrawlDelay](http://w3id.org/dice-research/orca/ontology#averageCrawlDelay)
| Seed | A seed value for initialising random number generators is used to ensure the repeatability of experiments. | [orca:seed](http://w3id.org/dice-research/orca/ontology#seed)

## Key performance indicators

| KPI | Description | Ontology resources
| --- | --- | ---
| Recall | Number of true positives divided by the number of checked triples. | [orca:microRecall](http://w3id.org/dice-research/orca/ontology#microRecall) [orca:macroRecall](http://w3id.org/dice-research/orca/ontology#macroRecall)
| Runtime | The time it takes from starting the crawling process to termination. | [orca:runtime](http://w3id.org/dice-research/orca/ontology#runtime)
| Requested disallowed resources | The number of forbidden resources crawled by the crawler, divided by the number of all resources forbidden by the `robots.txt` file. | [orca:ratioOfRequestedDisallowedResources](http://w3id.org/dice-research/orca/ontology#ratioOfRequestedDisallowedResources)
| Crawl delay fulfilment | The average measured delay between the requests received by a single node divided by the delay defined in the `robots.txt` file. If the measure is below 1.0 the crawler does not strictly follow the delay instruction. | [orca:minAverageCrawlDelayFulfillment](http://w3id.org/dice-research/orca/ontology#minAverageCrawlDelayFulfillment) [orca:maxAverageCrawlDelayFulfillment](http://w3id.org/dice-research/orca/ontology#maxAverageCrawlDelayFulfillment) [orca:macroAverageCrawlDelayFulfillment](http://w3id.org/dice-research/orca/ontology#macroAverageCrawlDelayFulfillment)
| Consumed hardware resources | The RAM and CPU consumption of the benchmarked crawler. | [orca:totalCpuUsage](http://w3id.org/dice-research/orca/ontology#totalCpuUsage) [orca:averageDiskUsage](http://w3id.org/dice-research/orca/ontology#averageDiskUsage) [orca:averageMemoryUsage](http://w3id.org/dice-research/orca/ontology#averageMemoryUsage)
| Triples over time | The number of triples in the sink over time. | [orca:tripleCountOverTime](http://w3id.org/dice-research/orca/ontology#tripleCountOverTime)

## Maintenance

This project is maintained by the [Data Science Group](https://w3id.org/dice-research) at [Paderborn University](https://upb.de/) within its role as a member of the special group 7 of task force 6 of the BDVA.

## Citation

ORCA has been accepted by the IEEE International Conference on Semantic Computing (ICSC). [The paper](https://papers.dice-research.org/2021/ICSC2021_ORCA/ORCA_public.pdf) should be cited as follows:

```BibTeX
@InProceedings{roeder2021orca,
  author    = {Michael Röder and Geraldo de Souza Jr. and Denis Kuchelev and Abdelmoneim Amer Desouki and Axel-Cyrille Ngonga Ngomo},
  booktitle = {Proceedings of the 15th IEEE International Conference on Semantic Computing (ICSC)},
  title     = {ORCA – a Benchmark for Data Web Crawlers},
  year      = {2021},
  pages     = {62-69},
  publisher = {IEEE Computer Society},
  keywords  = {dice raki daikiri opal limbo sys:relevantFor:limbo sys:relevantFor:opal group_aksw roeder ngonga kuchelev gsjunior},
  url       = {https://papers.dice-research.org/2021/ICSC2021_ORCA/ORCA_public.pdf},
}
```
