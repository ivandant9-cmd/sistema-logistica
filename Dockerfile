# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte
COPY . .

# 3. Cria e garante a pasta styles/ esperada pelo Vaadin no build
RUN mkdir -p /app/frontend/styles && \
    if [ -d "frontend/themes/tms" ]; then \
        cp -r frontend/themes/tms/*.css /app/frontend/styles/ 2>/dev/null || true; \
    fi

# 4. Executa o build de produção do Maven sem o clean para preservar o contexto do Vite
RUN --mount=type=cache,target=/root/.m2 mvn package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]