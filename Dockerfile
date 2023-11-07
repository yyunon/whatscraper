FROM openjdk:17

RUN mkdir /app
COPY ./src ./pom.xml ./run.sh  /app/
WORKDIR /app

ENTRYPOINT [ "." "/app/run.sh" ]