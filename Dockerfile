# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia arquivos do Maven para baixar dependências com cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 2. Copia todo o código-fonte
COPY src ./src

# 3. Garante que a árvore de diretórios do frontend exista no local padrão do Maven
RUN mkdir -p /app/src/main/frontend/themes/tms && \
    mkdir -p /app/frontend/themes/tms /app/frontend/styles && \
    if [ -d "./src/main/frontend" ]; then \
        cp -r ./src/main/frontend/* /app/frontend/; \
        if [ -d "./src/main/frontend/themes/tms" ]; then \
            cp -r ./src/main/frontend/themes/tms/* /app/frontend/themes/tms/; \
            cp -r ./src/main/frontend/themes/tms/* /app/frontend/styles/; \
        fi \
    fi

# 4. Executa a preparação do frontend do Vaadin explicitamente antes do empacotamento
RUN --mount=type=cache,target=/root/.m2 mvn vaadin:prepare-frontend -Pproduction

# 5. Compila a aplicação e empacota para produção
RUN --mount=type=cache,target=/root/.m2 mvn package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o JAR final gerado do estágio de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]