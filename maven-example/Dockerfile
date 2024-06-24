FROM docker.io/amazoncorretto:17.0.11

LABEL author="Bruce Wu"

ENV TZ=Asia/Shanghai
ENV SERVER_PORT 30003
ENV PROJ_HOME "/opt/example"

COPY target/maven-example.jar ${PROJ_HOME}/app.jar

WORKDIR ${PROJ_HOME}

RUN chmod +rwx ./app.jar

ENTRYPOINT ["java","-jar","app.jar"]
