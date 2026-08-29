# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 1. Copia o pom.xml e baixa as dependências
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B

# 2. Copia todo o código-fonte do projeto
COPY . .

# 3. Garante o mapeamento de temas de forma segura apenas se os arquivos existirem
RUN mkdir -p src/main/frontend/themes/tms frontend/themes/tms && \
    if [ "$(ls -A src/main/frontend/themes/tms 2>/dev/null)" ]; then \
        cp -r src/main/frontend/themes/tms/* frontend/themes/tms/; \
    elif [ "$(ls -A frontend/themes/tms 2>/dev/null)" ]; then \
        cp -r frontend/themes/tms/* src/main/frontend/themes/tms/; \
    fi

# 4. Executa o build de produção do Maven
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Pproduction -DskipTests

# Estágio de Execução
FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java", "-Xms128m", "-Xmx384m", "-Dvaadin.productionMode=true", "-Dserver.port=10000", "-jar", "app.jar"]