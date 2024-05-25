# Spring -Worker

## src/main/resources/application properties (for mysql)
```
spring.application.name=Traffic-predict
its.apiKey=${api_key}
its.apiUrl=https://openapi.its.go.kr:9443/trafficInfo
app.feOrigin=${frontend_origin}
node.db.url=jdbc:sqlite:sqlite/daejeon_node_xy.sqlite
geometry.db.url=jdbc:sqlite:sqlite/daejeon_link_with_wgs84.sqlite
batch.size=200

# MYSQL Database
spring.datasource.url=${database_url}
spring.datasource.username=${mysql_username}
spring.datasource.password=${mysql_password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Hibernate
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```
