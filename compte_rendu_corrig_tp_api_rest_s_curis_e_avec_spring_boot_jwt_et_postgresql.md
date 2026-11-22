# Compte rendu — TP API REST Sécurisée avec Spring Boot, JWT et PostgreSQL

* **Établissement :** Université Cheikh Ahmadoul Khadim (UCAK) — Département Informatique (L3)
* **Projet :** `tp-api-rest` (Spring Boot 3.4.3, Java 21, Maven, PostgreSQL)
* **Date :** 22 novembre 2026
* **Etudiant: Mohamed Mbacke L3 Développement d'Applications Réparties (DAR)

---

## 1. Présentation du projet

L'objectif de ce travail pratique est de concevoir et de développer une API REST JSON sécurisée permettant de gérer des **utilisateurs** et des **produits**, avec authentification par jeton **JWT** et contrôle d'accès par rôles (`USER` et `ADMIN`), exposée via **Swagger UI**.

| Composant | Choix retenu |
| :--- | :--- |
| **Langage / Runtime** | Java 21 |
| **Framework** | Spring Boot 3.4.3 (`spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`) |
| **Base de données** | PostgreSQL — rôle `tp_api` (non superutilisateur, sans droit de création de bases/rôles) |
| **Authentification** | `jjwt` `0.12.6` — JWT signé en `HS256` |
| **Hachage des mots de passe** | BCrypt (géré par Spring Security) |
| **Documentation** | Springdoc OpenAPI `2.8.5` / Swagger UI |
| **Identifiants** | UUID |

---

## 2. Choix techniques

### 2.1 Architecture sans état (`Stateless`)
Aucune session HTTP n'est créée (`SessionCreationPolicy.STATELESS`), et ni le formulaire de connexion Spring ni HTTP Basic ne sont activés. Le jeton JWT est transmis exclusivement via l'en-tête `Authorization: Bearer <token>`. La protection CSRF est désactivée car l'API n'utilise aucun cookie d'authentification.

### 2.2 Filtre JWT et contrôle dynamique des droits
Un filtre d'authentification JWT intercepte chaque requête, vérifie la signature, l'algorithme et l'expiration du jeton, puis charge le compte correspondant en base à partir du sujet (`sub`) du jeton. Un compte supprimé entraîne `401`, et un compte rétrogradé perd immédiatement ses droits `ADMIN` (`403`) dès la requête suivante.

**Limite retenue pour le TP :** aucune liste noire de jetons n'est maintenue. Un jeton déjà émis reste valide jusqu'à son expiration (`900` secondes par défaut dans le profil `tp`) tant que le compte existe en base.

### 2.3 Validation des données et normalisation
Les règles de validation sont appliquées aux créations et modifications (ainsi qu'aux bornes et cas invalides) via `ValidationUtils` et les services métier :
* **Noms (utilisateur et produit) :** obligatoires, non vides ni composés uniquement d'espaces, longueur entre 2 et 100 caractères après suppression des espaces périphériques ; la valeur nettoyée est enregistrée.
* **Email :** normalisé (`trim()` et mise en minuscules indépendante de la locale), format valide, longueur maximale de 254 caractères ; unicité vérifiée dans le service et garantie par contrainte unique en base (`409` en cas de doublon normalisé ; conservation de son propre email autorisée lors d'une modification).
* **Mot de passe :** 8 à 72 caractères Unicode avant hachage BCrypt, contenu et espaces conservés sans modification ; jamais re-haché ; conservé si omis lors d'une modification administrative.
* **Rôle :** énumération `USER` ou `ADMIN` stockée sous forme de chaîne ; `USER` attribué automatiquement à l'inscription publique (présence de `role` refusée en `400`).
* **Description produit :** facultative, au plus 1 000 caractères (`1 001` caractères refusés en `400` sans troncature).
* **Prix produit :** `BigDecimal` obligatoire, strictement positif, au plus 2 décimales, sans notation scientifique et sans arrondi des valeurs comportant plus de 2 décimales (`1.230` refusé en `400`).

### 2.4 Initialisation du premier `ADMIN`
Un profil `bootstrap` (désactivé par défaut et activé localement en complément de `tp` sur une seule instance) lit les variables temporaires `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL` et `BOOTSTRAP_ADMIN_PASSWORD` pour créer le premier `ADMIN` uniquement si aucun `ADMIN` n'existe déjà. Une fois l'opération effectuée, l'application est arrêtée, les variables et le profil `bootstrap` sont retirés, et le service redémarre avec `tp` seul.

---

## 3. Récapitulatif des règles et bornes de validation

| Champ | Règle | Cas invalides attendus (`400`) | Cas valides et aux limites (`2xx`) |
| :--- | :--- | :--- | :--- |
| `name` (Utilisateur / Produit) | Obligatoire, non vide / non espaces, 2 à 100 caractères après `trim()` | Absent, `null`, vide, espaces seuls, 1 caractère, 101 caractères | 2 caractères, 100 caractères (ex. `Clavier` après nettoyage) |
| `email` | Obligatoire, format valide, $\le$ 254 caractères, normalisé (`trim` + minuscules), unique | Absent, `null`, format invalide, 255 caractères | Format valide (254 caractères max), doublon normalisé $\rightarrow$ `409` |
| `password` (création) | Obligatoire, 8 à 72 caractères Unicode, contenu/espaces conservés | Absent, `null`, 7 caractères, 73 caractères | 8 caractères, 72 caractères, espaces conservés |
| `password` (`PUT` utilisateur) | Optionnel ; si fourni, non `null` et 8 à 72 caractères | `null` explicite, 7 caractères, 73 caractères | Absent (hachage existant conservé), 8 à 72 caractères |
| `role` (inscription publique) | Interdit à l'inscription publique (serveur attribue `USER`) | Champ `role` présent dans la requête | Absence de `role` (rôle `USER` attribué) |
| `role` (`POST` / `PUT` admin) | `USER` ou `ADMIN` | Absent ou `null` (sur création/PUT admin), valeur hors énumération | `USER`, `ADMIN` |
| `description` (Produit) | Facultative, $\le$ 1 000 caractères, sans troncature | 1 001 caractères | Absente, `null`, vide, 1 000 caractères |
| `price` (Produit) | `BigDecimal` obligatoire, $> 0$, $\le$ 2 décimales, pas de notation scientifique ni d'arrondi | Absent, `null`, `0`, négatif, `1.230`, `1.234`, notation scientifique (`1e2`) | `0.01`, `1`, `12.34`, `999.99` |

---

## 4. Codes HTTP et gestion centralisée des erreurs

Toutes les erreurs retournent un objet JSON structuré (`status`, `message`, `errors` le cas échéant, `path`, `timestamp`), sans exposer de secret, mot de passe, hachage, jeton ou détail interne :

| Statut HTTP | Situation traitée |
| :--- | :--- |
| **`400 Bad Request`** | Donnée invalide, rôle inconnu ou interdit à l'inscription, identifiant mal formé, corps JSON mal formé ou illisible |
| **`401 Unauthorized`** | Authentification absente, invalide ou expirée sur une route protégée, identifiants de connexion incorrects (même message générique), ou compte supprimé |
| **`403 Forbidden`** | Compte authentifié ne disposant pas du rôle requis (`USER` tentant une opération réservée à `ADMIN`) |
| **`404 Not Found`** | Produit ou utilisateur inexistant pour un appelant autorisé |
| **`409 Conflict`** | Adresse email normalisée déjà utilisée (à l'inscription, à la création ou à la modification) |

---

## 5. Persistance PostgreSQL et résultats des tests

* **Persistance entre redémarrages :** dans le profil `tp`, `spring.jpa.hibernate.ddl-auto=update` permet de conserver les données existantes entre deux démarrages de l'application (contrairement à `create` ou `create-drop`). Hors du profil `tp`, la valeur est `validate`.
* **Base dédiée aux tests :** les tests d'intégration s'exécutent sur une base de test séparée (`tp_rest_test`) appartenant au rôle `tp_api`, isolée de la base `tp_rest`.
* **Exécution de `mvn test` :** **42 tests exécutés, 0 échec, 0 erreur** (`27` tests unitaires dans `ValidationUtilsTest` et `15` tests d'intégration dans `ApiSecurityAndCrudIntegrationTest`). Les violations de contrainte d'unicité déclenchées intentionnellement lors du test de conflit d'email confirment le retour d'un statut `409` sans fuite d'information sensible.

---

## 6. Parcours de démonstration Swagger UI

Avec le profil `tp` actif, Swagger UI est accessible sans authentification à l'adresse `http://localhost:8080/swagger-ui/index.html` (et `/swagger-ui.html`) :

1. **Inscription publique (`201`) :** appeler `POST /api/auth/register` avec un nom, un email et un mot de passe ; vérifier que le rôle retourné est `USER` et qu'aucun mot de passe ni hachage n'apparaît dans la réponse.
2. **Connexion et autorisation Bearer :** appeler `POST /api/auth/login` avec les identifiants du compte `USER`, copier la valeur `accessToken`, cliquer sur **Authorize** et coller uniquement le jeton (sans préfixe `Bearer` ni guillemets).
3. **Vérification des droits `USER` :** consulter les produits (`200`), puis vérifier que la création de produit (`POST /api/products`) et la consultation des utilisateurs (`GET /api/users`) retournent `403`.
4. **Vérification sans jeton (`401`) :** retirer l'autorisation dans Swagger UI et vérifier qu'une route protégée (ex. `GET /api/products`) retourne `401`.
5. **Parcours `ADMIN` :** se connecter avec le compte `ADMIN`, mettre à jour le jeton dans **Authorize**, puis effectuer la création (`201`), la consultation (`200`), la modification (`200`) et la suppression (`204`) d'un produit ainsi que la gestion d'un compte utilisateur.
6. **Erreurs `400` et `409` :** avec un compte autorisé, vérifier un refus de validation en `400` (ex. `price: 1.230` ou nom trop court) et un conflit d'email normalisé en `409`.
7. **Hygiène des preuves :** aucun mot de passe, hachage BCrypt, jeton JWT ou export réseau brut non masqué n'est conservé dans les captures ou notes de démonstration.

---

## 7. Conclusion

L'API `tp-api-rest` répond aux exigences fonctionnelles et de sécurité du TP : authentification JWT `HS256` sans session serveur, contrôle d'accès `USER` / `ADMIN` vérifié dynamiquement en base, validation rigoureuse des données et gestion unifiée des erreurs `400` / `401` / `403` / `404` / `409`, persistance PostgreSQL via `tp_api`, documentation Swagger UI et validation par la suite de tests automatisés.
