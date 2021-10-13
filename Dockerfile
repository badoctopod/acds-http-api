FROM openjdk:11
RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app
COPY resources/public ./resources/public
COPY target/acds-http-api.jar deploy/heroku/config.edn ./
CMD java -Dconf=config.edn -jar acds-http-api.jar