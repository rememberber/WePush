FROM node:24-alpine AS web-build
RUN npm install --global pnpm@11.22.0
WORKDIR /src/ui
COPY ui/package.json ui/pnpm-lock.yaml ui/pnpm-workspace.yaml ui/tsconfig.base.json ./
COPY ui/apps ./apps
COPY ui/packages ./packages
RUN pnpm install --frozen-lockfile && pnpm --filter @wepush-next/web build

FROM eclipse-temurin:21-jdk AS java-build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY core core
COPY providers providers
COPY agent agent
COPY service service
COPY sdk sdk
COPY tests tests
COPY distribution distribution
RUN ./mvnw -q -pl service/service-app -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 wepush \
    && useradd --system --uid 10001 --gid 10001 --home /var/lib/wepush-next --shell /usr/sbin/nologin wepush
WORKDIR /opt/wepush-next
COPY --from=java-build /src/service/service-app/target/wepush-next-service.jar ./wepush-next-service.jar
COPY --from=web-build /src/ui/apps/web/dist ./web
RUN mkdir -p /var/lib/wepush-next/tmp && chown -R wepush:wepush /var/lib/wepush-next
USER 10001
ENV WEPUSH_WEB_ROOT=file:/opt/wepush-next/web/ WEPUSH_JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.io.tmpdir=/var/lib/wepush-next/tmp"
EXPOSE 18990 19090
ENTRYPOINT ["sh", "-c", "exec java $WEPUSH_JAVA_OPTS -jar /opt/wepush-next/wepush-next-service.jar"]
