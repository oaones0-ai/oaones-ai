# 部署手册

```text
版权声明：本项目已开源，但版权属于个人所有，禁止转卖。如有问题请联系https://www.oaones.com/contract%20me
```

## 1. 前言

```text
OA中实现AI网关功能。
```

## 2. 前后端架构

### 2.1 前端文件

```text
前端文件集中在 `ecology/oaones` 下，使用jsp进行页面渲染。
前端组件依赖https://github.com/oaones0-ai/oaones-scripts.git，请先下载前端组件。
```

### 2.2 后端文件

```text
后端文件集中在 `ecology/src or classbean/com/customization/oaones/` 下，使用ssm架构，通过mybatis的xml进行数据库交互。
```

### 2.3 其他文件

```text
其他为配置文件，用于泛微标准安全协议等的通行等。
项目中所有文件理论上应全部为新增文件，需要修改的文件在下方单独列出。如覆盖时提示冲突，说明与原有项目文件冲突，不可使用本开源项目。需要定制二开。
```

## 3. 功能说明


```text
实现AI网关统一管理的功能，接入openai协议大模型。
```


## 4. 使用文档

### 4.1 文件覆盖

```text
将文件下载后，直接覆盖到 `ecology` 目录，重启resin。
```

### 4.2 Sql执行

```text
请将项目下的data/「database」/oaones/下文件，按照顺序，进行执行。
```

### 4.3 配置oa

```text
logjint.properties最后增加内容，不然日志报错：
log4j.logger.mylog=INFO,mylog
log4j.appender.mylog=org.apache.log4j.DailyRollingFileAppender
log4j.appender.mylog.DatePattern='_'yyyyMMdd'.log'
log4j.appender.mylog.File=@mylog/mylog.log
log4j.appender.mylog.layout=org.apache.log4j.PatternLayout
log4j.appender.mylog.layout.ConversionPattern=%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [Thread:%t] %m%n
log4j.additivity.mylog=false
```
### 4.4 配置WEB-INF/resin-web-post.xml
```text
没有则新增文件，仅限resin中间件，其他配置请咨询AI
```
### 4.5 菜单URL设置

```text
主菜单：\oaones\ai\page\admin.jsp
```
