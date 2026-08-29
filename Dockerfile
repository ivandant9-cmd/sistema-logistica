# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte do projeto
COPY . .

# 3. Garante que a estrutura src/main/frontend exija os arquivos que o pom.xml aponta
RUN mkdir -p src/main/frontend/styles && \
    if [ -d "frontend/styles" ]; then \
        cp -r frontend/styles/* src/main/frontend/styles/; \
    elif [ -d "styles" ]; then \
        cp -r styles/* src/main/frontend/styles/; \
    fi && \
    if [ -d "frontend/themes" ]; then \
        cp -r frontend/themes src/main/frontend/; \
    fi

# 4. Executa o build de produção do Maven de forma limpa
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]