# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Baixa dependências e insumos do Vaadin com cache de Maven
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 2. Copia o código-fonte
COPY src ./src

# 3. Compila a aplicação e o frontend do Vaadin (apenas uma execução)
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o JAR final gerado
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=${PORT:-8080}", "-jar", "app.jar"]