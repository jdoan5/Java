# =============================================================================
# Stage 5 — containerising the Bookmark API
#
# A MULTI-STAGE build. Stage one ("build") has the whole JDK + Maven toolchain;
# stage two ("run") keeps only a JRE and the finished jar. Only the last stage
# ships, so the image stays small and the build tools never reach production.
#
# Nothing needs to be installed locally: the builder image brings its own Maven.
# =============================================================================

# -----------------------------------------------------------------------------
# Stage 1: build the jar
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copy the POM alone first. Docker caches each layer, so as long as the POM is
# unchanged this dependency download is reused and rebuilds only recompile code.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Now the sources, which change far more often than the dependency list.
COPY src ./src
RUN mvn -B clean package -DskipTests

# -----------------------------------------------------------------------------
# Stage 2: the runtime image
# -----------------------------------------------------------------------------
# Alpine keeps the runtime image a few hundred MB smaller than the Ubuntu-based
# tag. Smaller image = faster pulls on every pod start and less to patch.
FROM eclipse-temurin:24-jre-alpine AS run
WORKDIR /app

# Run as a non-root user. If the container is ever compromised, the attacker
# lands as an unprivileged account instead of root. The UID/GID are pinned to
# 1000 deliberately — see the USER note below.
RUN addgroup -g 1000 -S spring && adduser -u 1000 -S spring -G spring

# --chown sets ownership DURING the copy. A separate "RUN chown" would rewrite
# every file into a second layer, silently doubling the jar's weight in the image.
COPY --from=build --chown=spring:spring /build/target/*.jar app.jar

# USER must be NUMERIC, not the name "spring". With securityContext.runAsNonRoot
# Kubernetes has to prove the user isn't root before starting the container, and
# it does not read /etc/passwd from the image — a named user is unverifiable and
# the pod fails with CreateContainerConfigError:
#   "has runAsNonRoot and image has non-numeric user (spring)"
USER 1000

EXPOSE 8080

# MaxRAMPercentage makes the JVM size its heap from the container's memory
# limit rather than the host's RAM — without it the JVM can overshoot the
# cgroup limit and get OOM-killed by Kubernetes.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Default the image to the k8s profile (in-memory H2). The default profile from
# application.yml writes H2 to ./data, which UID 1000 cannot create inside /app —
# so without this a plain `docker run` of this image would crash on startup.
# The Deployment sets the same variable explicitly; this just makes the image
# runnable on its own.
ENV SPRING_PROFILES_ACTIVE=k8s

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
