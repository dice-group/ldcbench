FROM openjdk:8-alpine

ARG CLASS=org.dice_research.ldcbench.benchmark.LemmingBasedBenchmarkController
ARG JAR_FILE

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

# Copy data
COPY data /usr/src/app/data

COPY target/lib /usr/src/app/lib
COPY log4j.properties /usr/src/app
COPY target/${JAR_FILE} /usr/src/app

ENTRYPOINT ["java", "-cp", "${JAR_FILE}:/usr/src/app:/usr/src/app/lib/*", "org.hobbit.core.run.ComponentStarter", "${CLASS}"]
