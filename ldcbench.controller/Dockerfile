FROM openjdk:8-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY target/lib /usr/src/app/lib
COPY target/ldcbench.controller.jar /usr/src/app

CMD ["java", "-cp", "ldcbench.controller.jar:/usr/src/app/lib/*", "org.hobbit.core.run.ComponentStarter", "org.dice_research.ldcbench.benchmark.BenchmarkController"]
