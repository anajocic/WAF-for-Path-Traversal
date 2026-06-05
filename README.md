# WAF — Zastita od Path Traversal napada

Web Application Firewall implementiran kao Spring Boot filter koji stiti od
path traversal napada kroz dva vektora: JWT `kid` parametar i file upload.

Projekat sadrzi dvije aplikacije:
- **vulnerable-app** (port `8088`) — ista aplikacija BEZ zastite, za demonstraciju napada
- **protected-app** (port `8089`) — ista aplikacija SA WAF zastitom

---

## Arhitektura

```
Klijent
  |
  |--► vulnerable-app:8088  (bez filtera)
  |         └── FileUploadController  ← direktno, bez provjere
  |
  └──► protected-app:8089
            └── PathTraversalFilter  ← WAF sloj (provjera JWT kid + multipart filename)
                      └── FileUploadController  ← drugi sloj provjere (WafRules)
```

### Kljucne klase

| Klasa | Lokacija | Uloga |
|-------|----------|-------|
| `PathTraversalFilter` | `protected-app/.../waf/` | Servlet filter — presrece svaki request |
| `WafRules` | `protected-app/.../waf/` | Staticka pravila validacije (whitelist + blacklist) |
| `FileUploadController` | obje apps, `.../api/` | Upload endpoint, u protected verziji poziva WafRules |
| `SecurityConfiguration` | obje apps, `.../security/` | Spring Security config, u protected verziji registruje filter |

---

## Napadacki vektori

### 1. JWT `kid` Path Traversal

JWT token ima `kid` (Key ID) polje u headeru koje server koristi za pronalazak
kljuca kojim verifikuje potpis. Napadac moze postaviti `kid` na putanju van
dozvoljenog direktorijuma:

```json
// Legitimni JWT header
{ "alg": "HS256", "kid": "key1" }

// Zlonamjerni JWT header
{ "alg": "HS256", "kid": "../../../application.properties" }
{ "alg": "HS256", "kid": "../../keys/key2" }
```

**Posljedica:** Server cita sadrzaj proizvoljnog fajla kao kljuc za verifikaciju.

**WAF odbrana:** `PathTraversalFilter` izvlaci `kid` iz JWT headera i provjerava
ga — dozvoljava samo vrijednost `key1`.

---

### 2. File Upload Path Traversal

Endpoint `/upload` prima multipart fajl. Ranjiva implementacija koristi
`getOriginalFilename()` direktno, bez sanitizacije:

```java
// RANJIVO
String filename = file.getOriginalFilename();  // npr. "../../../secret.txt"
Path target = uploadDir.resolve(filename);     // izlazi van uploads/
Files.copy(file.getInputStream(), target, ...);
```

Napadac moze uploadovati fajl sa nazivom `../secret.txt` i prepisati fajlove
van `uploads/` direktorijuma.

**WAF odbrana:** Dva sloja provjere u protected-app:
1. `PathTraversalFilter` — presrece multipart request i provjerava naziv fajla
2. `FileUploadController` — poziva `WafRules.isSafeFilename()` kao dodatnu provjeru

---

## WAF — kako funkcionise

### PathTraversalFilter

Filter se izvrsava na svakom HTTP requestu (`OncePerRequestFilter`).

**Tok izvrsavanja:**

```
Request primljen
      |
      v
Ima li Authorization: Bearer header?
   Da -> Izvuci kid iz JWT headera
      -> Je li kid siguran i na whitelisti?
         Ne -> HTTP 403 Forbidden
         Da -> nastavi
      |
      v
Je li request multipart/form-data?
   Da -> Izvuci filename iz multipart dijela
      -> WafRules.isSafeFilename(filename)
         Ne -> HTTP 403 Forbidden
         Da -> nastavi
      |
      v
filterChain.doFilter() — request prolazi do controllera
```

### WafRules

Dvije metode validacije:

**`isValidPath(String path)`** — za JWT `kid` provjeru
- Whitelist: samo `[a-zA-Z0-9-]{1,64}` (bez tacke, slash, backslash)
- Blacklist: `..`, `%2e%2e`, `%252e`, `%c0%ae`, `/`, `\`, hex encodinzi

**`isSafeFilename(String filename)`** — za file upload provjeru
- Blacklist: sve varijante path traversal encodinga (find(), ne matches())
- Whitelist ekstenzija: samo `.txt`, `.jpg`, `.jpeg`, `.png`, `.pdf`
- Format: `[a-zA-Z0-9_-]{1,100}.(txt|jpg|jpeg|png|pdf)`

### WAF pravila — tabela

| Payload | Tehnika | JWT kid | File Upload |
|---------|---------|---------|-------------|
| `../secret.txt` | Klasicni traversal | BLOKIRAN | BLOKIRAN |
| `..%2Fsecret.txt` | URL encoding | BLOKIRAN | BLOKIRAN |
| `..%252Fsecret.txt` | Dvostruki URL encoding | BLOKIRAN | BLOKIRAN |
| `....//secret.txt` | Duplikacija | BLOKIRAN | BLOKIRAN |
| `%c0%aesecret.txt` | Unicode overlong | BLOKIRAN | BLOKIRAN |
| `..\secret.txt` | Windows backslash | BLOKIRAN | BLOKIRAN |
| `key1` | Dozvoljena vrijednost | DOZVOLJEN | — |
| `test.txt` | Normalan upload | — | DOZVOLJEN |

---

## Konfiguracija

### Portovi

Svaka aplikacija se pokrecé na zasebnom portu. Izmjeniti u
`application.properties` ako je potrebno:

```properties
# vulnerable-app/src/main/resources/application.properties
server.port=8088

# protected-app/src/main/resources/application.properties
server.port=8089
```

### Baza podataka

Obje aplikacije koriste MySQL. Konfiguracija u `application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3308/websec?useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=rootpassword
```

Kreirati bazu prije pokretanja:
```sql
CREATE DATABASE websec;
```

### JWT konfiguracija

```properties
websec.jwt-secret=qyz9dgGxtfp28GN5mSpgGydyjMFVCg8LOqwrn+mnYvI=
```

### Upload ogranicenja

```properties
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

### WAF bijela lista kljuceva

Dozvoljeni `kid` vrijednosti se kontrolisu u `PathTraversalFilter.java`:

```java
private static final String ALLOWED_FILE = "key1";
```

Za dodavanje novih kljuceva — prosiriti logiku u `isPathSafeAndWhitelisted()`.

---

## Pokretanje

### Preduslovi

- Java 17+
- Maven 3.8+
- Docker Desktop (za MySQL kontejner)

### 1. Pokretanje baze podataka (Docker)

Baza se pokrece kao Docker kontejner. **Ovo treba uraditi prije pokretanja aplikacija.**

**Prvi put — kreiranje volumena i pokretanje kontejnera:**

```bash
docker run -d \
  --name websec-mysql \
  -e MYSQL_ROOT_PASSWORD=rootpassword \
  -e MYSQL_DATABASE=websec \
  -p 3308:3306 \
  -v websec-mysql-data:/var/lib/mysql \
  mysql:8.0
```

> `-v websec-mysql-data:/var/lib/mysql` kreira named volume koji cuva podatke i izmedju restartova.

**Svaki naredni put — samo pokrenuti vec postojeci kontejner:**

```bash
docker start websec-mysql
```

**Provjera da li kontejner radi:**

```bash
docker ps
```

Sacekati nekoliko sekundi da MySQL bude spreman, pa tek onda pokrenuti aplikacije.

### 2. Pokretanje aplikacija

---

## Testni slucajevi (curl)

Svi testovi se mogu pokrenuti iz terminala nakon sto su obje aplikacije pokrenute.

### File Upload testovi

**Priprema test fajla:**

```powershell
echo "test sadrzaj" > test.txt
```

#### Ranjiva aplikacija (port 8088)

```powershell
# Normalan upload — ocekivani odgovor: 200 OK
curl.exe -X POST http://localhost:8088/upload -F "file=@test.txt;filename=test.txt"

# Path traversal (direktni) — ranjiva app DOZVOLJAVA
curl.exe -X POST http://localhost:8088/upload -F "file=@test.txt;filename=../malicious.txt"

# Path traversal (URL encoding) — ranjiva app DOZVOLJAVA
curl.exe -X POST http://localhost:8088/upload -F "file=@test.txt;filename=..%2Fmalicious.txt"
```

#### Zasticena aplikacija (port 8089)

```powershell
# Normalan upload — ocekivani odgovor: 200 OK
curl.exe -X POST http://localhost:8089/upload -F "file=@test.txt;filename=test.txt"

# Path traversal (direktni) — WAF BLOKIRA, ocekivano: 403 Forbidden
curl.exe -X POST http://localhost:8089/upload -F "file=@test.txt;filename=../malicious.txt"

# Path traversal (URL encoding) — WAF BLOKIRA, ocekivano: 403 Forbidden
curl.exe -X POST http://localhost:8089/upload -F "file=@test.txt;filename=..%2Fmalicious.txt"
```

---

### JWT `kid` Path Traversal i URL/Query parametar testovi

Token koji se koristi u primjerima ispod:

```
eyJraWQiOiJrZXkxIiwiYWxnIjoiSFMyNTYifQ.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOjEsInN1YiI6Im1hamFAbWFqYS5jb20iLCJpYXQiOjE3ODA2NzIzMzZ9.j0mCBbw9GqoM_upOs17d7lVLCYmpwGku3UKXormu6JU
```

#### Ranjiva aplikacija (port 8088)

```powershell
# Path traversal u URL putanji — ranjiva app DOZVOLJAVA
curl.exe -H "Authorization: Bearer eyJraWQiOiJrZXkxIiwiYWxnIjoiSFMyNTYifQ.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOjEsInN1YiI6Im1hamFAbWFqYS5jb20iLCJpYXQiOjE3ODA2NzIzMzZ9.j0mCBbw9GqoM_upOs17d7lVLCYmpwGku3UKXormu6JU" "http://localhost:8088/etc/passwd"

# Path traversal u query parametru — ranjiva app DOZVOLJAVA
curl.exe -H "Authorization: Bearer eyJraWQiOiJrZXkxIiwiYWxnIjoiSFMyNTYifQ.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOjEsInN1YiI6Im1hamFAbWFqYS5jb20iLCJpYXQiOjE3ODA2NzIzMzZ9.j0mCBbw9GqoM_upOs17d7lVLCYmpwGku3UKXormu6JU" "http://localhost:8088/download?file=../../etc/passwd"
```

#### Zasticena aplikacija (port 8089)

```powershell
# Path traversal u URL putanji — WAF BLOKIRA, ocekivano: 403 Forbidden
curl.exe -H "Authorization: Bearer eyJraWQiOiJrZXkxIiwiYWxnIjoiSFMyNTYifQ.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOjEsInN1YiI6Im1hamFAbWFqYS5jb20iLCJpYXQiOjE3ODA2NzIzMzZ9.j0mCBbw9GqoM_upOs17d7lVLCYmpwGku3UKXormu6JU" "http://localhost:8089/etc/passwd"

# Path traversal u query parametru — WAF BLOKIRA, ocekivano: 403 Forbidden
curl.exe -H "Authorization: Bearer eyJraWQiOiJrZXkxIiwiYWxnIjoiSFMyNTYifQ.eyJ0b2tlblR5cGUiOiJBQ0NFU1MiLCJ1c2VySWQiOjEsInN1YiI6Im1hamFAbWFqYS5jb20iLCJpYXQiOjE3ODA2NzIzMzZ9.j0mCBbw9GqoM_upOs17d7lVLCYmpwGku3UKXormu6JU" "http://localhost:8089/download?file=../../etc/passwd"
```

---

## Struktura projekta

```
Path_Traversal_WAF/
├── vulnerable-app/          # Spring Boot app bez WAF zastite
│   ├── src/main/java/.../
│   │   ├── api/
│   │   │   └── FileUploadController.java   # ranjivi endpoint
│   │   └── security/
│   │       └── SecurityConfiguration.java
│   └── uploads/             # direktorijum za uploadovane fajlove
│
├── protected-app/           # Spring Boot app sa WAF zastitom
│   ├── src/main/java/.../
│   │   ├── api/
│   │   │   └── FileUploadController.java   # zasticeni endpoint
│   │   ├── waf/
│   │   │   ├── PathTraversalFilter.java    # WAF filter
│   │   │   └── WafRules.java               # pravila validacije
│   │   └── security/
│   │       └── SecurityConfiguration.java  # registruje filter
│   └── uploads/             # direktorijum za uploadovane fajlove
│
└── payloads/
    ├── path-traversal-payloads.txt   # lista napadackih payload-a
    └── test_waf.py                   # automatizovani testovi
```