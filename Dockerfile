FROM alpine/git
WORKDIR /app
RUN git clone https://github.com/RaffaeleDragone/raffaele_dragone_adc_2020.git

FROM maven:3.5-jdk-8-alpine
WORKDIR /app
COPY --from=0 /app/ /app
RUN mvn package

FROM java:openjdk-8
WORKDIR /root/app
ENV MASTERIP=127.0.0.1
ENV ID=0
ENV SHOWGUI=yes
COPY --from=1 /app/target/AnonymousChat-1.0-jar-with-dependencies.jar /root/app
COPY --from=1 /app/images /root/app/images
COPY --from=1 /app/log_structure /root/app

CMD /usr/bin/java -jar AnonymousChat-1.0-jar-with-dependencies.jar -m $MASTERIP -id $ID -showgui $SHOWGUI
