FROM openjdk:8-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY target/lib /usr/src/app/lib
COPY log4j.properties /usr/src/app
COPY target/ldcbench.eval-module.jar /usr/src/app

CMD ["java", "-cp", "ldcbench.eval-module.jar:/usr/src/app:/usr/src/app/lib/*", "org.hobbit.core.run.ComponentStarter", "org.dice_research.ldcbench.benchmark.EvalModule"]
