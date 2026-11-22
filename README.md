# TP API REST Sécurisée avec Spring Boot, JWT et PostgreSQL — UCAK

API REST JSON permettant de gérer des **utilisateurs** et des **produits**, sécurisée par authentification **JWT (HS256)** et contrôle d'accès par rôles (`USER` et `ADMIN`), avec documentation interactive **Swagger UI**.

---

## 1. Technologies et versions

| Composant | Version / Technologie |
| :--- | :--- |
| **Java** | 21 |
| **Spring Boot** | 3.4.3 |
| **Maven** | 3.9+ |
| **Spring Web** | `spring-boot-starter-web` (3.4.3) |
| **Spring Security** | `spring-boot-starter-security` (3.4.3) |
| **Spring Data JPA** | `spring-boot-starter-data-jpa` (3.4.3) |
| **Bean Validation** | `spring-boot-starter-validation` (3.4.3) |
| **PostgreSQL Driver** | `org.postgresql:postgresql` (42.7.x via parent Spring Boot) |
| **JWT** | `io.jsonwebtoken:jjwt-*` (`0.12.6`) |
| **Documentation OpenAPI** | `org.springdoc:springdoc-openapi-starter-webmvc-ui` (`2.8.5`) |

---

## 2. Préparation de la base de données PostgreSQL

Connectez-vous avec un compte d'administration PostgreSQL (`postgres`) et exécutez les commandes suivantes (ou le script `sql/init_postgres.sql` après modification du mot de passe) :

```sql
CREATE ROLE tp_api WITH LOGIN PASSWORD '<mot_de_passe_local_a_definir>' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE DATABASE tp_rest OWNER tp_api;

\c tp_rest
GRANT ALL PRIVILEGES ON SCHEMA public TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO tp_api;
```

> **Sécurité** : le rôle `tp_api` n'est ni superutilisateur ni autorisé à créer d'autres bases ou rôles. L'application ne se connecte jamais avec le compte `postgres`.

---

## 3. Configuration des variables d'environnement

Copiez `.env.example` vers `.env` (ignoré par Git via `.gitignore`) et renseignez vos valeurs locales :

```bash
cp .env.example .env
```

### Génération de la clé secrète JWT (`JWT_SECRET_BASE64`)
Générez une clé aléatoire d'au moins 32 octets encodée en Base64 :

```bash
openssl rand -base64 32
```

### Chargement des variables et démarrage
Spring Boot ne lit pas automatiquement les fichiers `.env`. Chargez-les dans votre shell avant de démarrer l'application :

```bash
set -a
source .env
set +a

mvn spring-boot:run
```

---

## 4. Gestion du schéma JPA (`ddl-auto=update`)

Dans le profil `tp` (`src/main/resources/application-tp.yml`), la propriété `spring.jpa.hibernate.ddl-auto=update` est activée :
* **Conservation des données** : contrairement à `create` ou `create-drop` qui suppriment et recréent les tables à chaque démarrage, `update` conserve les tables et les enregistrements existants entre deux redémarrages.
* **Limites** : hors du profil `tp`, la valeur par défaut est `validate`. En environnement de production, `update` ne remplace pas un outil de migration versionné (Flyway/Liquibase) car il ne supprime pas les colonnes obsolètes et ne modifie pas toujours les contraintes existantes.

---

## 5. Initialisation du premier `ADMIN` et création d'un `USER` de test

### Étape A : Initialiser le premier `ADMIN` (profil `bootstrap`)
1. Définissez temporairement les variables d'environnement d'initialisation :
   ```bash
   export SPRING_PROFILES_ACTIVE=tp,bootstrap
   export BOOTSTRAP_ADMIN_NAME="Administrateur UCAK"
   export BOOTSTRAP_ADMIN_EMAIL="admin.tp@example.com"
   export BOOTSTRAP_ADMIN_PASSWORD="<mot_de_passe_admin_local>"
   ```
2. Démarrez l'application une seule fois :
   ```bash
   mvn spring-boot:run
   ```
3. Une fois le message `[BOOTSTRAP] Premier compte ADMIN créé avec succès` affiché, **arrêtez l'application** (`Ctrl+C`), supprimez les variables temporaires et revenez au profil `tp` seul :
   ```bash
   unset BOOTSTRAP_ADMIN_NAME BOOTSTRAP_ADMIN_EMAIL BOOTSTRAP_ADMIN_PASSWORD
   export SPRING_PROFILES_ACTIVE=tp
   mvn spring-boot:run
   ```

### Étape B : Créer un compte `USER` de test
Via `curl` ou Swagger UI (`POST /api/auth/register`) :

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Etudiant Test",
    "email": "etudiant.test@example.com",
    "password": "<mot_de_passe_user_local>"
  }'
```

---

## 6. Choix et limites de sécurité

* **Architecture sans état (Stateless)** : `SessionCreationPolicy.STATELESS`, sans formulaire de connexion ni HTTP Basic.
* **Désactivation de CSRF** : l'API n'utilise aucun cookie de session ou d'authentification ; le jeton JWT est transmis exclusivement via l'en-tête HTTP `Authorization: Bearer <token>`, éliminant le risque d'attaque CSRF via navigateur.
* **Vérification dynamique en base** : à chaque requête authentifiée, le filtre JWT recharge l'utilisateur en base à partir du sujet (`sub`) du jeton. Un compte supprimé reçoit immédiatement `401` et un compte rétrogradé perd immédiatement ses droits `ADMIN` (`403`).
* **Limites de révocation** : conformément au périmètre du TP, aucune liste noire de jetons n'est maintenue. Un jeton déjà émis reste valide jusqu'à son expiration (`900` secondes par défaut dans le profil `tp`) tant que le compte existe toujours en base.

---

## 7. Documentation Swagger UI

Avec le profil `tp` actif, accédez à la documentation interactive sans authentification préalable :
* **Swagger UI** : `http://localhost:8080/swagger-ui.html`
* **Spécification OpenAPI JSON** : `http://localhost:8080/v3/api-docs`

Pour appeler les routes protégées depuis Swagger UI, cliquez sur **Authorize** et collez uniquement le jeton `accessToken` retourné par `POST /api/auth/login` (sans le préfixe `Bearer`).

---

## 8. Exécution des tests

### Prérequis

Créez une base de test dédiée appartenant au rôle `tp_api` :

```sql
CREATE DATABASE tp_rest_test OWNER tp_api;

\c tp_rest_test
GRANT ALL PRIVILEGES ON SCHEMA public TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO tp_api;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO tp_api;
```

La configuration de test (`src/test/resources/application-test.yml`) utilise `ddl-auto: create-drop` : les tables sont créées au démarrage et supprimées à la fin, sans impacter la base de développement.

### Lancement

```bash
mvn test
```

### Résultats attendus

```
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

La suite comprend :
* **27 tests unitaires** (`ValidationUtilsTest`) — validation des noms, emails, mots de passe, prix et descriptions, y compris les cas aux bornes (1/2/100/101 caractères, 72/73 caractères pour les mots de passe, prix à 2/3 décimales).
* **15 tests d'intégration** (`ApiSecurityAndCrudIntegrationTest`) — authentification JWT, CRUD complet des produits et utilisateurs, contrôle des rôles (USER / ADMIN), gestion des erreurs (400, 401, 403, 404, 409), validation stricte des prix via `JsonParser` (détection de `1.230` à 3 décimales et de la notation scientifique), et accès public à Swagger.

---

## 9. Notes d'implémentation

* **Validation stricte des prix** : le champ `price` utilise un `JsonDeserializer` custom (`StrictPriceDeserializer`) qui accède au texte brut du token JSON via `JsonParser.getText()`. Cela permet de détecter `1.230` (3 décimales, rejeté) et `10.00` (2 décimales, accepté) sans que Jackson normalise la représentation avant la vérification.
* **Hachage BCrypt préservé** : un `PUT /api/users/{id}` sans champ `password` conserve le hash existant. Un `PUT` avec `"password": null` retourne explicitement `400`.
* **Révocation dynamique des rôles** : le filtre JWT (`JwtAuthenticationFilter`) recharge le compte en base à chaque requête. La modification ou suppression d'un compte prend effet immédiatement sans attendre l'expiration du jeton.
# tp-api-rest
