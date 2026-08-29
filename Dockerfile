# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências isoladamente para aproveitar o cache
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline

# 2. Copia todo o código-fonte do projeto
COPY . .

# 3. Garante que os diretórios do frontend existam no formato esperado pelo plugin do Vaadin
RUN mkdir -p /app/frontend/styles /app/frontend/themes/tms && \
    if [ -d "./src/main/frontend/themes/tms" ]; then \
        cp -r ./src/main/frontend/themes/tms/* /app/frontend/styles/; \
        cp -r ./src/main/frontend/themes/tms/* /app/frontend/themes/tms/; \
    fi

# 4. Executa a preparação do frontend separadamente para baixar o Node/npm e resolver o tema Lumo
RUN --mount=type=cache,target=/root/.m2 mvn vaadin:prepare-frontend -Pproduction

# 5. Executa o build e empacotamento final de produção
RUN --mount=type=cache,target=/root/.m2 mvn package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copia apenas o JAR gerado para a imagem final limpa
COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]