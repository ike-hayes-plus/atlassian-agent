# Atlassian Agent

Java Agent + 命令行 KeyGen，用于在本地/测试环境配合 Atlassian 产品使用自定义许可。升级产品后一般只需保留 `-javaagent` 并重新生成许可，不必反复改安装包。

**支持**（版本跨度大，以实际为准）：Jira Software / Core / Service Desk、Confluence、Bamboo、Bitbucket、FishEye、Crowd、Crucible，以及 Capture、Training、Portfolio、Questions、Team Calendars 等插件。

---

## 三步上手

### 1. 拿到 jar

- 下载本项目 **Release** 包，或自行编译：

```bash
mvn package -DskipTests
# 产物：target/atlassian-agent-jar-with-dependencies.jar
```

改代码或换 jar 后，**必须**把新 jar 放到 Tomcat 使用的路径并**重启**进程。

### 2. 挂上 Agent

把 jar 放在固定路径，在启动 Tomcat 时加上 `-javaagent`（多产品可共用同一 jar）：

```bash
export CATALINA_OPTS="-javaagent:/path/to/atlassian-agent.jar ${CATALINA_OPTS}"
```

写入 `setenv.sh` / `setenv.bat`，或使用仓库里的 **[docker-compose.example.yml](docker-compose.example.yml)**（Jira / Confluence 11 + 共用 Postgres，含 `-javaagent` 挂载；Jira 与 Confluence **分库分用户**，`docker compose up` 时 **conf-db-init** 会自动创建 `confdb`；复制后改 compose 里的数据库密码即可）。

确认进程参数里能看到 `-javaagent`（`ps aux | grep javaagent`）。stdout 里偶尔会有 `agent working` 之类日志，没有也不代表一定未加载。

### 3. 生成并粘贴许可

**共用同一个 agent jar，但 Jira 与 Confluence 要各跑一条 KeyGen**：`-p` 不同、**Server ID 各用各的安装页上的值**，**不能把 Jira 的 key 贴到 Confluence**（反之亦然）。

粘贴规则：只复制输出里 **`AAAB` 开头、到 `X02…` 结尾** 的多行正文，不要复制 `Your license code` 提示行，粘贴后点 **Next**。

#### Jira Software（11+ 需要 `-d`）

在 **Jira** Setup License 页复制 Server ID，再生成：

```bash
java -jar atlassian-agent.jar \
  -p jira -d \
  -m "you@example.com" -o "Your Org" -n "Your Name" \
  -s "<Jira-Setup-页的-Server-ID>"
```

Docker（**把 `<容器名>` 换成 `docker ps` 里看到的名字**；[docker-compose.example.yml](docker-compose.example.yml) 为 `jira`，本机整套环境常为 `ar-jira`）：

```bash
docker exec -it <容器名> java -jar /opt/atlassian/jira/atlassian-agent.jar \
  -p jira -d \
  -m "you@example.com" -o "Org" -n "Name" \
  -s "<Jira-Setup-页的-Server-ID>"
```

#### Confluence（11+ 用 `-p conf`，KeyGen 默认 Data Center，无需再加 `-d`）

在 **Confluence** Setup License 页复制 Server ID（与 Jira 不同），再生成：

```bash
java -jar atlassian-agent.jar \
  -p conf \
  -m "you@example.com" -o "Your Org" -n "Your Name" \
  -s "<Confluence-Setup-页的-Server-ID>"
```

Docker（example compose 为 `confluence`，本机常为 `ar-confluence`）：

```bash
docker exec -it <容器名> java -jar /opt/atlassian/confluence/atlassian-agent.jar \
  -p conf \
  -m "you@example.com" -o "Org" -n "Name" \
  -s "<Confluence-Setup-页的-Server-ID>"
```

---

## KeyGen 参数

| 选项 | 必填 | 说明 |
|------|------|------|
| `-p` | 是 | 产品代号 |
| `-s` | 是 | Server ID |
| `-m` | 是 | 联系邮箱 |
| `-o` | 是 | 组织名称 |
| `-n` | 否 | 联系人；省略则用邮箱 |
| `-d` | 否 | Data Center 许可；**Jira 11+ 通常必须**；**`-p conf` 时默认已按 DC 生成** |
| `-h` | 否 | 帮助 |

**`-p` 常用取值**：`jira`（Software）、`jc`、`jsd`、`conf`、`bamboo`、`bitbucket`、`crowd`、`fisheye`、`crucible`、`capture`、`training`、`portfolio`、`questions`、`tc`。

参数写在 `java -jar …jar` **之后**。完整选项与长选项名见 `java -jar atlassian-agent.jar -h`。

在 IDE 里跑 KeyGen 时，可用 VM options：`-Datlassian.agent.product=jira`、`-Datlassian.agent.datacenter=true`、`-Datlassian.agent.serverid=…` 等（前缀 `atlassian.agent.`，见 `Usage.java`）。不要把 `--product` 写在 `-jar` 前面。

---

## 常见问题

| 现象 | 处理 |
|------|------|
| *doesn't support Server licenses*（Confluence） | 用 **`-p conf`** 重新生成（已默认 DC）；Jira 则加 **`-d`** |
| *old Cluster license*（Confluence 11） | 勿在许可里手改集群字段；用最新 jar **`-p conf`** 重新生成 |
| `No such container: jira` | 容器名不对；先 `docker ps`，用实际名称（如 `ar-jira` / `ar-confluence`），不是 README 占位符 |
| Confluence *password authentication failed for user confluence* | Postgres 无 `confdb`/用户；`docker compose up` 应跑完 **conf-db-init**，或执行 `scripts/create-confdb.sh`；密码与 compose 中 `CONF_PASSWORD` / `ATL_JDBC_PASSWORD` 一致 |
| Confluence 创建管理员页 *System Error* / *Spring Application context has not been set* | `confdb` **无表**但向导已到管理员步（compose 的 `ATL_JDBC_*` 会预写库连接，若跳过中间步则未跑 schema）。停 Confluence → 备份并清空 `./data/confluence` → 从 http://127.0.0.1:18090/ **按向导顺序**走完（许可 → 部署/集群 → 选空站点等）；提交管理员前在 Postgres 执行 `\dt` 确认已有表。勿直接打开 `/setup/setupadministrator-start.action` |
| invalid license / 无法确认 | 核对 Server ID、Jira 是否 `-d`、agent jar 是否最新且已重启、许可是否完整粘贴 |
| Session Expired | 安装未完成前无管理员账号；用 Setup 地址继续（如 `/secure/SetupLicense!default.jspa`），不要只去 `/login.jsp` |
| validateLicense 403 | 需在 Setup 页面正常点 Next（带 `atl_token`），不要用裸 curl 测 |
| Gadget feed URL 健康检查失败 | Docker 使用 `18080:8080` 时常见；Base URL 用 `http://127.0.0.1:18080/`，本机仪表盘正常可忽略 |
| 产品许可证不合法（顶栏 / Confluence 安装向导） | Jira 11 `Forged` 顶栏、Confluence `DefaultLicenseService` / `licenseHash` 校验；需 **最新 agent jar** 并重启，确认 `-javaagent` 已加载 |

---

## 申明

- 仅供个人学习研究，**不得用于商业用途**；商业使用请向 [Atlassian](https://www.atlassian.com) 购买正版。
- 本项目采用 [GPL-3.0](LICENSE) 开源。
- `src/main/resources/dsa*.pem` 为 **内置测试用签名密钥**（与 agent 配套），仅供本地/学习环境，勿当作生产机密管理。
