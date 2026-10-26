# Build context needs to be services/ as Maven needs the parent pom as well
# as sibling poms.
# From services/:
#   docker build --build-arg SERVICE=court-service   --build-arg PORT=8081 -t court-service:latest .
#   docker build --build-arg SERVICE=member-service  --build-arg PORT=8082 -t member-service:latest .
#   docker build --build-arg SERVICE=booking-service --build-arg PORT=8083 -t booking-service:latest .

FROM maven:3.9-eclipse-temurin-25 AS build
ARG SERVICE
WORKDIR /build

#  Copy parent pom and all child poms first (for dependency caching)
COPY pom.xml .
COPY court-service/pom.xml court-service/
COPY booking-service/pom.xml booking-service/
COPY member-service/pom.xml member-service/

# Download dependencies (cached layer if poms havent changed)
RUN mvn -B dependency:go-offline -pl ${SERVICE} -am

# Copy source and build
COPY ["${SERVICE:?Pass --build-arg SERVICE=<name>-service}/src", "${SERVICE}/src"]
RUN mvn -B package -pl ${SERVICE} -am -DskipTests -Dcheckstyle.skip=true

# Extract stage: split the fat jar into layers
FROM eclipse-temurin:25-jre AS extract
ARG SERVICE
WORKDIR /extracted

COPY --from=build /build/${SERVICE}/target/${SERVICE}.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination layers

# Run stage
FROM eclipse-temurin:25-jre
ARG PORT
RUN test -n "$PORT" || (echo "Pass --build-arg PORT=<port>" && exit 1)
WORKDIR /app

RUN useradd -r -s /sbin/nologin -u 1001 app
USER app

COPY --from=extract /extracted/layers/dependencies/ ./
COPY --from=extract /extracted/layers/spring-boot-loader/ ./
COPY --from=extract /extracted/layers/snapshot-dependencies/ ./
COPY --from=extract /extracted/layers/application/ ./

EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "application.jar"]
