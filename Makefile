IMAGE_BASE = git.project-hobbit.eu:4567/ldcbench/ldcbench/

test:
	mvn verify

publish: images push-images push-hobbit

images:
	mvn -DskipTests package

push-images:
	# mvn -DskipTests deploy
	docker push $(IMAGE_BASE)ldcbench.controller
	docker push $(IMAGE_BASE)ldcbench.data-generator
	docker push $(IMAGE_BASE)ldcbench.eval-module
	docker push $(IMAGE_BASE)ldcbench.system
	docker push $(IMAGE_BASE)ldcbench.empty-server
	docker push $(IMAGE_BASE)ldcbench.http-node
	docker push $(IMAGE_BASE)ldcbench.ckan-node
	docker push $(IMAGE_BASE)ldcbench.sparql-node
	docker push $(IMAGE_BASE)ldcbench.rdfa-gen
	docker push $(IMAGE_BASE)ldcbench.html-embd-node
	docker push $(IMAGE_BASE)ldcbench.jsonld-gen

add-hobbit-remote:
	git remote |grep hobbit ||git remote --verbose add hobbit https://git.project-hobbit.eu/ldcbench/ldcbench

push-hobbit: add-hobbit-remote
	git push --verbose hobbit master:master

test-benchmark:
	mvn -DfailIfNoTests=false -Dtest=BenchmarkTest#executeBenchmark test

test-benchmark-dockerized:
	mvn -DskipTestPhase -DfailIfNoTests=false -Dtest=BenchmarkIT#executeBenchmark verify
