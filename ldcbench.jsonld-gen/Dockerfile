FROM maven

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

ADD target/lib /usr/src/app/lib
ADD target/ldcbench.jsonld-gen.jar /usr/src/app

CMD ["java", "-cp", "ldcbench.jsonld-gen.jar:/usr/src/app/lib/*", "org.hobbit.core.run.ComponentStarter", "org.dice_research.ldcbench.jsonld.gen.JsonLDDataGenerator"]
