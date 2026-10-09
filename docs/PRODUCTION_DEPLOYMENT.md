# Production Deployment — MySQL + Apache Tomcat (Windows)

This guide takes the project from this repository to a **running web
application backed by a real MySQL database on Apache Tomcat**.

Two scripts automate everything:

| Script | What it does |
|--------|--------------|
| `scripts/init-mysql.ps1` | Creates the `java_quiz_platform` database and imports `database/schema.sql` + `database/sample_data.sql` |
| `scripts/deploy-tomcat.ps1` | Builds the production WAR and deploys it into your Tomcat |

---

## 0. Prerequisites — what YOU need to install (one time)

| # | Software | Where |
|---|----------|-------|
| 1 | **Java JDK 17** (or 21) | Already installed ✔ (`C:\Program Files\Java\jdk-17`) |
| 2 | **Apache Maven** | Already installed ✔ |
| 3 | **MySQL Server 8.x** | https://dev.mysql.com/downloads/installer/ — choose *MySQL Server* + *MySQL Workbench* |
| 4 | **Apache Tomcat 9** | https://tomcat.apache.org/download-90.cgi → *Windows Service Installer* or the 64-bit Windows zip |

> Tip: during MySQL setup, set the **root password** and tick *"Add MySQL to PATH"*
> (so `mysql` works in any terminal). If you already installed it without PATH,
> the `-MySqlBin` switch handles that.

---

## 1. Load the database

Open **PowerShell** in the project folder and run (pick the line that matches your setup):

```powershell
# mysql is on PATH — you will be asked for the password
powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 -User root

# mysql is NOT on PATH — point to its bin folder
powershell -ExecutionPolicy Bypass -File scripts\init-mysql.ps1 `
    -MySqlBin "C:\Program Files\MySQL\MySQL Server 8.0\bin" -User root
```

Expected output at the end:

```
users=3   quizzes=4   questions=33   options=132   results=2
```

---

## 2. Point the app at YOUR database password

Edit **`src/main/resources/config.properties`**:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/java_quiz_platform?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
db.user=root
db.password=YOUR_MYSQL_PASSWORD      # <-- change this
```

> This file is packaged into the WAR, so it is baked in at build time. Change it
> **before** step 3.

---

## 3. Build + deploy to Tomcat

```powershell
powershell -ExecutionPolicy Bypass -File scripts\deploy-tomcat.ps1 `
    -TomcatHome "C:\Program Files\Apache Software Foundation\Tomcat 9.0"
```

Then start Tomcat and open:

- **http://localhost:8080/java-quiz-platform**

Default logins (also shown on the login page):

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@quiz.com` | `admin123` |
| Quiz Creator | `creator@quiz.com` | `creator123` |
| Participant | `student@quiz.com` | `student123` |

---

## 4. (Optional) Run the Swing desktop app

The desktop GUI uses the **same** MySQL database. From the project root:

```powershell
mvn compile exec:java
```

---

## Manual alternative (no scripts)

If you prefer to do it by hand:

```powershell
# 1) database
mysql -u root -p < database\schema.sql
mysql -u root -p java_quiz_platform < database\sample_data.sql

# 2) build WAR
mvn clean package

# 3) copy WAR into Tomcat
copy target\java-quiz-platform.war "C:\Program Files\Apache Software Foundation\Tomcat 9.0\webapps\"

# 4) start Tomcat
"C:\Program Files\Apache Software Foundation\Tomcat 9.0\bin\startup.bat"
```

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `mysql` not recognized | Add `C:\Program Files\MySQL\MySQL Server 8.0\bin` to PATH, or use `-MySqlBin`. |
| `Access denied for user 'root'` | Wrong `db.password` in `config.properties` — match your MySQL root password. |
| `Communications link failure` / port 3306 | MySQL server not running — start it (Windows Services → MySQL). |
| `Unknown database 'java_quiz_platform'` | You skipped step 1 — run `scripts\init-mysql.ps1`. |
| `Address already in use: 8080` | Another app uses port 8080. Change Tomcat's port in `conf\server.xml` (8080 → 8081). |
| 404 at `/java-quiz-platform` | WAR didn't deploy — check `Tomcat\logs\catalina.*.log`. |
| Blank page / driver error | Confirm `mysql-connector-j` is inside `WEB-INF\lib` (Maven adds it automatically). |
