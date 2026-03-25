# 第一阶段：构建
FROM maven:3.9.6-amazoncorretto-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# 第二阶段：运行
FROM amazoncorretto:17
WORKDIR /app

# 安装 shadow-utils 才有 groupadd/useradd
RUN yum install -y shadow-utils && yum clean all && \
    groupadd --system appgroup && \
    useradd --system --gid appgroup appuser

COPY --from=builder /app/target/*.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 8088

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]