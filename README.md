# Spring -Worker

## src/main/resources/application properties
### for mysql
```
spring.application.name=Traffic-predict
its.apiKey=${api_key}
its.apiUrl=https://openapi.its.go.kr:9443/trafficInfo
app.feOrigin=${frontend_origin}
node.db.url=jdbc:sqlite:sqlite/daejeon_node_xy.sqlite
geometry.db.url=jdbc:sqlite:sqlite/daejeon_link_with_wgs84.sqlite

# MYSQL Database
spring.datasource.url=jdbc:mysql://${endpoint}:3306/${database_name}
spring.datasource.username=${mysql_username}
spring.datasource.password=${mysql_password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
```
