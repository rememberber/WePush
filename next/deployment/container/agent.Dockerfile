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
RUN ./mvnw -q -pl agent/agent-app -am -DskipTests package

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10002 wepush-agent \
    && useradd --system --uid 10002 --gid 10002 --home /var/lib/wepush-next-agent --shell /usr/sbin/nologin wepush-agent
WORKDIR /opt/wepush-next
COPY --from=java-build /src/agent/agent-app/target/wepush-next-agent.jar ./wepush-next-agent.jar
RUN mkdir -p /var/lib/wepush-next-agent/plugins/active /var/lib/wepush-next-agent/tmp && chown -R wepush-agent:wepush-agent /var/lib/wepush-next-agent
USER 10002
ENV WEPUSH_AGENT_IDENTITY_PATH=/var/lib/wepush-next-agent/identity.json WEPUSH_AGENT_STATE_PATH=/var/lib/wepush-next-agent/journal.json WEPUSH_AGENT_EVENT_OUTBOX_PATH=/var/lib/wepush-next-agent/event-outbox WEPUSH_AGENT_COMPLETION_OUTBOX_PATH=/var/lib/wepush-next-agent/completion-outbox WEPUSH_PLUGIN_ACTIVE_PATH=/var/lib/wepush-next-agent/plugins/active WEPUSH_JAVA_OPTS="-XX:MaxRAMPercentage=75 -Djava.io.tmpdir=/var/lib/wepush-next-agent/tmp"
ENTRYPOINT ["sh", "-c", "exec java $WEPUSH_JAVA_OPTS -jar /opt/wepush-next/wepush-next-agent.jar"]
