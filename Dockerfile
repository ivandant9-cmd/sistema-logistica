# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências (com cache)
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 2. Copia todo o contexto do projeto
COPY . .

# 3. Garante que a pasta styles exigida pelo build do Vaadin exista na raiz espelhando o tema tms
RUN mkdir -p /app/frontend/styles && \
    if [ -d "./src/main/frontend/themes/tms" ]; then \
        cp -r ./src/main/frontend/themes/tms/* /app/frontend/styles/; \
    fi

# 4. Executa o build de produção do Maven/Vaadin gerando o pacote final
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o JAR gerado no estágio anterior para a imagem final de execução
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]