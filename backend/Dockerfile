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
# Carlito é metricamente compatível com Calibri (mesmas larguras de glifo) e,
# ao contrário dela, é livremente redistribuível -- por isso o PDF é gerado
# com Carlito registrada sob o nome "Calibri" (ver fop-config.xml), casando
# com a fonte já usada no editor/prévia em tela (WysiwygEditor.vue).
RUN apk add --no-cache font-carlito
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
# Fixa o timezone padrão da JVM em America/Sao_Paulo — sem isso, converter uma
# data pura (LocalDate, sem hora) para Timestamp usa o timezone padrão do
# container (UTC), e exibir esse Timestamp de volta em America/Sao_Paulo pode
# cruzar a meia-noite e mostrar o dia errado.
ENV TZ=America/Sao_Paulo
# O heap padrão da JVM é 25% da memória do contêiner (num limite de 2 GB, só 512 MB), pouco para o Apache FOP gerando PDF. 65%
# deixa o restante para a memória fora do heap (metaspace, threads, buffers) e para o sistema. Só vale com limite de memória no
# contêiner; para trocar, defina JAVA_TOOL_OPTIONS no ambiente (ver "Infraestrutura" em docs/infraestrutura.md).
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65"
ENTRYPOINT ["java", "-jar", "app.jar"]
