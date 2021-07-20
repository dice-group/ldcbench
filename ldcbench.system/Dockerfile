FROM openjdk:8-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY target/lib /usr/src/app/lib
COPY log4j.properties /usr/src/app
COPY target/ldcbench.system.jar /usr/src/app

CMD ["java", "-cp", "ldcbench.system.jar:/usr/src/app:/usr/src/app/lib/*", "org.hobbit.core.run.ComponentStarter", "org.dice_research.ldcbench.system.SystemAdapter"]
