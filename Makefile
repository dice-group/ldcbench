IMAGE_BASE = git.project-hobbit.eu:4567/ldcbench/ldcbench/

publish: images push-images push-hobbit

images:
	mvn -DskipTests clean package
	mvn -DfailIfNoTests=false -Dtest=BenchmarkTest#buildImages test
	docker build --tag $(IMAGE_BASE)simple-http-node ldcbench.http-node/

push-images:
	docker push $(IMAGE_BASE)benchmark-controller
	docker push $(IMAGE_BASE)datagen
	docker push $(IMAGE_BASE)eval-module
	docker push $(IMAGE_BASE)system-adapter
	docker push $(IMAGE_BASE)simple-http-node

add-hobbit-remote:
	git remote |grep hobbit ||git remote --verbose add hobbit https://git.project-hobbit.eu/ldcbench/ldcbench

push-hobbit: add-hobbit-remote
	git push --verbose hobbit master:master

test-benchmark:
	mvn -DfailIfNoTests=false -Dtest=BenchmarkTest#checkHealth test
