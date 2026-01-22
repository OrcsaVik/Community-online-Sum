# paicoding-forum

## 项目描述

一个基于 **Java** 和 **Spring Boot** 构建的高可用、高扩展性的论坛应用。该项目为在线讨论提供平台，并结合多种现代技术以提升性能、扩展性和功能丰富度。

## 功能特性

- **核心 Web 功能**：基于 Spring Boot，支持 Web 服务的快速开发与部署，涵盖用户交互、评论、登录、通知和聊天等功能
- **用户管理**：完整的用户注册、认证（包含 JWT）以及用户资料管理
- **论坛内容管理**：支持论坛主题、帖子和回复的创建、查询、更新与删除（CRUD）
- **数据库集成**：使用 Mybatis-Plus 与 MySQL 进行高效、灵活的数据访问
- **云存储**：无缝集成阿里云 OSS，用于文件上传与存储
- **搜索能力**：基于 Elasticsearch，实现快速且高相关性的内容搜索
- **API 文档**：通过 Knife4j 自动生成接口文档，便于接口理解与测试
- **图像处理**：支持图片处理能力，可能包含 WebP 支持，并使用 OpenCV 进行高级图像处理
- **PDF 生成**：使用 iTextPDF 生成 PDF 文档
- **数据序列化**：使用 Kryo 实现高效对象序列化
- **工具类支持**：大量使用 Guava、Hutool 及 Apache Commons 提供通用能力
- **监控与指标**：集成 Spring Boot Actuator 与 Prometheus，用于应用监控与性能指标采集
- **数据库迁移**：使用 Liquibase 管理数据库结构变更
- **缓存与实时能力**：通过 Redisson 集成 Redis，支持缓存及潜在的实时功能
- **流量控制**：使用 Sentinel 实现稳定的流控与限流能力
- **AI / ML 集成**：集成火山引擎 SDK，并接入豆包、阿里、讯飞等 AI 服务，用于增强型 AI 功能
- **支付集成**：支持微信支付
- **性能测试**：引入 JMH 进行微基准测试与性能分析
- **消息系统**：使用 RabbitMQ 实现异步消息处理

## 模块说明

该项目采用 **Maven 多模块结构**：

- **`paicoding-common`**：基础公共模块，提供全项目共享的核心组件与工具类，包括权限控制注解（`@Permission`）、敏感数据处理注解（`@SensitiveField`）、基础数据模型及通用辅助类，是其他模块的基础依赖
- **`paicoding-service`**：业务核心模块，封装论坛系统的核心业务逻辑，负责数据访问（Mybatis-Plus、MySQL）、外部服务集成（阿里云 OSS、Elasticsearch、豆包 / 阿里 / 讯飞 AI 服务）、用户认证与鉴权，以及评论、文章、通知等核心功能，同时集成 RabbitMQ 消息能力
- **`paicoding-web`**：主 Web 应用模块，作为系统入口，对外提供 RESTful API（并可能包含页面渲染），处理用户管理、评论、登录、通知、聊天等 HTTP 请求，并通过 `paicoding-service` 执行业务逻辑，结合 `paicoding-ui` 提供前端资源支持

## 快速开始

### 环境要求

- Java 8 或更高版本
- Maven 3.x
- 支持的数据库（如 MySQL）
- Redis 实例（用于缓存及相关功能）
- Elasticsearch 实例（用于搜索功能）
- 阿里云 OSS 账号（用于云存储）
- RabbitMQ 实例（用于消息通信）
- AI 服务配置（豆包、阿里、讯飞，可选）

### 构建项目

在项目根目录执行：

```bash
mvn clean install
```

### 运行应用

进入 `paicoding-web` 目录并执行：

```bash
cd paicoding-web
mvn spring-boot:run
```

## 文档说明

更多文档及架构说明请参考：

- [AI 指南](https://chatgpt.com/c/Ai-guidance.md)
- [动态数据源](https://chatgpt.com/c/DynamicDB.md)

------

