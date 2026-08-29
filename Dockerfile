# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte
COPY . .

# 3. Garante que os diretórios de temas existam tanto na raiz quanto em src/main para satisfazer o Vaadin
RUN mkdir -p /app/frontend/themes/tms /app/src/main/frontend/themes/tms /app/frontend/styles && \
    if [ -d "./src/main/frontend/themes/tms" ]; then \
        cp -r ./src/main/frontend/themes/tms/* /app/frontend/themes/tms/; \
        cp -r ./src/main/frontend/themes/tms/* /app/src/main/frontend/themes/tms/; \
        cp -r ./src/main/frontend/themes/tms/* /app/frontend/styles/; \
    fi

# 4. Executa o build de produção do Maven
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]