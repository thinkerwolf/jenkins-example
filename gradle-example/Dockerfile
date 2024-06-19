FROM docker.io/amazoncorretto:17.0.11

LABEL author="Bruce Wu"

ENV TZ=Asia/Shanghai
ENV SERVER_PORT 3090
ENV PROJ_HOME "/opt/example"

COPY target/*-SNAPSHOT.jar ${PROJ_HOME}/app.jar

WORKDIR ${PROJ_HOME}

RUN chmod +rwx ./devops-auth.jar

ENTRYPOINT ["java","-jar","app.jar"]
