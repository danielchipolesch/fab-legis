# ── Estágio de build: compila o JAR com Maven ─────────────────────────────────
FROM eclipse-temurin:25-jdk-alpine AS build
RUN apk add --no-cache maven
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B -q
COPY src ./src
RUN mvn package -DskipTests -B -q

# ── Estágio de runtime: apenas o JRE ──────────────────────────────────────────
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
# Fixa o timezone padrão da JVM em America/Sao_Paulo — sem isso, converter uma
# data pura (LocalDate, sem hora) para Timestamp usa o timezone padrão do
# container (UTC), e exibir esse Timestamp de volta em America/Sao_Paulo pode
# cruzar a meia-noite e mostrar o dia errado.
ENV TZ=America/Sao_Paulo
ENTRYPOINT ["java", "-jar", "app.jar"]
