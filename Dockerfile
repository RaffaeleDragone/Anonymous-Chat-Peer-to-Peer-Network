#FROM alpine/git
#WORKDIR /app
#RUN git clone https://github.com/spagnuolocarmine/p2ppublishsubscribe.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY / /app
RUN mvn package

#FROM openjdk:8-jre-alpine
FROM java:openjdk-8
WORKDIR /app
ENV MASTERIP=127.0.0.1
ENV ID=0
ENV SHOWGUI=yes
COPY --from=0 /app/target/AnonymousChat-1.0-jar-with-dependencies.jar /app

CMD /usr/bin/java -jar AnonymousChat-1.0-jar-with-dependencies.jar -m $MASTERIP -id $ID -showgui $SHOWGUI
