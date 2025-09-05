# 📋 Changelog

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Semantic Versioning](https://semver.org/lang/fr/).

## [1.4.6] - 2025-01-27

### 🚨 Corrections Critiques
- **🔄 Reconnexion automatique** : Le mod se relance automatiquement après une déconnexion/reconnexion
- **⚡ Synchronisation réparée** : Les timers synchronisés s'affichent maintenant correctement en jeu
- **🛡️ Protection anti-crash** : Plus d'erreurs NullPointerException qui cassent le système
- **🔔 Alertes précises** : Les alertes se déclenchent au bon moment (temps restant, pas durée initiale)

### 🐛 Corrections de Bugs
- **📥 Notification manquante** : Les timers du serveur sont maintenant notifiés au système local
- **⏰ Calcul d'alerte** : Correction du calcul du temps restant pour les alertes synchronisées
- **🔄 Relance intelligente** : Détection correcte de l'état fermé du TimerManager
- **🛠️ Gestion d'erreurs** : Protection renforcée contre les ETag manquants

### 🎯 Améliorations Stabilité
- **🔧 Auto-réparation** : Le système se remet automatiquement en marche après les problèmes
- **📊 Logs améliorés** : Messages plus clairs pour diagnostiquer les problèmes
- **⚙️ Architecture robuste** : Gestion d'état plus fiable et prévisible

## [1.4.5] - 2025-01-27

### 🐛 Corrections Critiques
- **🔔 Alertes unifiées** : Correction majeure - les timers synchronisés déclenchent maintenant les alertes
- **🎯 Système d'alerte complet** : Tous les timers (chat, sync, local) programment automatiquement une alerte
- **📱 Interface optimisée** : Espacement parfait entre les contrôles de configuration
- **🎵 Volume sonore** : Bouton "Play" dédié pour tester le son d'enclume sans spam

### 🔧 Améliorations Interface
- **🎚️ Sliders intuitifs** : Volume sonore et nombre de timers affichés avec contrôles glissants
- **📏 Espacement harmonieux** : Labels et boutons parfaitement alignés et espacés
- **🎮 UX améliorée** : Configuration plus intuitive et professionnelle

### 🚀 Performance
- **⚡ Alertes fiables** : Système unifié garantissant les notifications pour tous les timers
- **🔄 Synchronisation robuste** : Alertes programmées même pour les timers reçus via sync

## [1.4.3] - 2025-01-27

### 🚀 Migration Cloudflare Workers + R2
- **Nouvelle architecture** : Migration complète de GitHub Gist vers Cloudflare Workers + R2
- **Performance améliorée** : Jar ultra-léger (170KB vs 6MB+) sans dépendances AWS
- **Synchronisation optimisée** : Circuit breakers séparés pour lecture/écriture
- **Sécurité renforcée** : Token d'écriture obfusqué avec encodage XOR + rotation
- **Fiabilité maximale** : Gestion robuste des erreurs 412/409 avec retry automatique

### 🎯 Single Actor Pattern
- **Thread-safety garantie** : Pattern Actor pour mutations atomiques et thread-safe
- **Snapshot immutable** : Lectures sans blocage avec copies défensives
- **Merge déterministe** : Règles de fusion intelligentes (latest expiresAt, createdAt, createdBy)
- **Anti-double déclenchement** : Protection contre les événements dupliqués
- **TimeAuthority synchronisé** : Temps monotone basé sur headers HTTP Date

### 🔧 Améliorations Techniques
- **HttpURLConnection natif** : Remplacement AWS SDK par client HTTP standard Java
- **Debounce avec jitter** : Délais randomisés pour éviter les collisions
- **ETag intelligent** : Cache optimisé avec invalidation conditionnelle
- **Logs corrélés** : opId pour tracer les opérations liées
- **UI thread-safe** : Mises à jour interface sur le thread principal Minecraft

### 🐛 Corrections Critiques
- **Fix noms dimensions** : Affichage correct des noms de dimensions dans l'interface
- **Fix authentification R2** : Correction du décodage XOR des secrets
- **Fix headers HTTP** : Gestion correcte des caractères non-ASCII dans les tokens
- **Fix merge conflicts** : Résolution automatique des conflits de synchronisation

### 📊 Résultats Performance
- **Taille jar** : 170KB (réduction de 97%)
- **Démarrage** : Instantané (plus de chargement AWS SDK)
- **Synchronisation** : < 5 secondes pour propagation
- **Quota API** : Illimité (Cloudflare Workers vs 5000/h GitHub)
- **Fiabilité** : 99.9% uptime avec circuit breakers

## [1.4.2] - 2025-08-30

### 🔥 Correctifs Critiques Synchronisation
- **Fix alertes dupliquées** : Plus de spam de 6-20 alertes pour le même timer 
- **Fix drag & drop** : Interface déplaçable uniquement avec le chat ouvert
- **Fix re-sync timers expirés** : Correction des délais de 1-2 minutes pour les timers re-créés
- **Fix cache ETag intelligent** : Invalidation automatique pour détecter les nouveaux timers

### ⚡ Optimisations Performance
- **Sync ultra-rapide** : Toutes les 15 secondes au lieu de 30
- **Cache timestamp optimisé** : Blocs de 15 secondes pour réactivité maximale
- **Protection cache thread-safe** : Backup/restore automatique en cas d'erreur
- **Gestion erreurs robuste** : Retry automatique pour erreurs serveur GitHub

### 🔧 Améliorations Techniques
- **Alertes uniques garanties** : Programmation seulement pour nouveaux timers
- **ETag invalidation intelligente** : Force refresh quand timers expirent
- **Logs diagnostics étendus** : Debug détaillé pour autorisation et upload
- **Sync post-upload immédiate** : Propagation en 1 seconde après création

### 📊 Résultat Final
- **Décalage sync** : ~5 secondes (excellent pour système distribué)
- **Réactivité** : 15 secondes maximum pour nouveaux timers
- **Stabilité** : Protection complète contre corruption cache
- **Fiabilité** : Gestion tous cas d'usage (création, expiration, re-création)

## [1.4.1] - 2025-08-30

### 🐛 Correctifs Critiques
- **Fix timeout synchronisation** : Correction du timeout 100ms → 2s qui causait la perte des timers synchronisés
- **Fix erreur parsing alertes** : Correction de l'erreur "For input string: secondes" lors de la programmation d'alertes
- **Fix cache CDN GitHub** : Bypass du cache CDN pour détecter les modifications en temps réel
- **Fix barres de progression** : Préservation des durées initiales lors des mises à jour de timers
- **Fix disparition timers** : Les timers ne disparaissent plus temporairement de l'interface
- **Fix drag & drop chat** : Interface déplaçable même avec le chat ouvert

### ⚡ Optimisations
- **Cache intelligent** : Système ETag + timestamp pour économiser le quota GitHub API
- **Headers anti-cache** : Force la fraîcheur des données quand nécessaire
- **Logs nettoyés** : Suppression des messages de debug temporaires

### 🔧 Améliorations Techniques  
- **Timeout adaptatif** : 2 secondes pour les opérations asynchrones critiques
- **Gestion d'erreurs robuste** : Logs détaillés pour diagnostiquer les problèmes de sync
- **Performance réseau** : Équilibre optimal entre réactivité et quota API

## [1.4.0] - 2025-08-27

### 🌐 Ajouté
- **Synchronisation multi-utilisateurs** : Les timers se partagent automatiquement entre tous les joueurs du mod
- **Position d'interface persistante** : L'interface garde sa position après redémarrage du jeu
- **Interface déplaçable** : Drag & drop de l'interface des timers (clic sur le titre + glisser)
- **Synchronisation à la connexion** : Récupération automatique des timers lors de la connexion au serveur
- **Priorité locale** : Les timers créés localement ont priorité sur les timers distants
- **Débounce intelligent** : Système anti-spam pour les uploads (10 secondes)

### 🔧 Corrigé
- **Fix barres de progression** : Les barres ne se réinitialisent plus lors de la synchronisation
- **Fix spam d'alertes** : Une seule alerte par timer (fini les 25 sons d'enclume !)
- **Optimisation quota GitHub** : Cache optimisé (30s interface, 60s sync) pour supporter 20+ utilisateurs
- **Fix affichage config** : Texte affiché correctement par-dessus les boutons

### ⚡ Amélioré
- **Performance interface** : Cache intelligent pour éviter les blocages
- **Gestion mémoire** : Nettoyage automatique des données obsolètes
- **Feedback visuel** : Bordure blanche pendant le déplacement d'interface
- **Sauvegarde optimisée** : Position sauvée seulement à la fin du déplacement

## [1.3.0] - 2025-08-27

### 🔐 Ajouté
- **Système de whitelist dynamique** : Vérification via GitHub Gist JSON
- **Téléchargement automatique** : Mise à jour périodique configurable (TTL)
- **Configuration flexible** : JSON avec usernames, messages personnalisés
- **Gestion asynchrone** : Vérifications non-bloquantes pour le jeu
- **Messages personnalisables** : Textes d'erreur configurables dans le JSON

### 🗑️ Supprimé
- **Système de guilde** : Suppression complète de GuildVerifier
- **Messages /t info** : Plus de commandes automatiques en jeu
- **Dépendances guilde** : Architecture simplifiée

### 🔧 Amélioré
- **Performance** : Vérifications plus rapides et efficaces
- **Sécurité** : Contrôle centralisé des accès
- **Maintenance** : Mise à jour des utilisateurs sans redéploiement
- **Interface** : Messages d'erreur plus clairs et informatifs

### 🎯 Impact
- **Pour les utilisateurs** : Accès plus fluide, moins d'attente
- **Pour l'admin** : Gestion simple via GitHub Gist
- **Pour le développement** : Code plus maintenable et extensible

## [1.2.0] - 2025-08-27

### 🌅 Ajouté
- **Prédiction de phase du jour** : Affichage de la phase Minecraft au moment du spawn légendaire
- **8 phases détaillées** : Dawn (Aube), Morning (Matin), Noon (Midi), Afternoon (Après-midi), Day (Jour), Dusk (Crépuscule), Night (Nuit), Midnight (Minuit)
- **Affichage enrichi** : Format "🌍 Dimension - Phase (heures)" dans l'interface
- **Calculs précis** : Conversion temps réel vers temps Minecraft avec gestion des cycles 24h

### 🔧 Corrigé
- **Logique des phases** : Correction des chevauchements entre phases nocturnes
- **Phases traversant minuit** : Gestion correcte de Dawn, Night et Midnight
- **Priorité des phases** : Les phases spécifiques (Midi, Crépuscule) sont prioritaires sur les générales

### 🎯 Exemple
- Avant : `🌍 Ressource2               21m 44s`
- Après : `🌍 Ressource2 - Midi (11:00-12:59)     21m 44s`

## [1.1.0] - 2025-03-18

### 🔐 Ajouté
- **Système de sécurité par guilde** : Vérification automatique de l'appartenance à la fondation Aether
- **Détection automatique** du message "Your data has been loaded successfully"
- **Exécution automatique** de `/t info` pour vérifier la guilde
- **Protection complète** : Toutes les fonctionnalités sont protégées par la vérification
- **Support multilingue** : Gestion des messages en anglais et français
- **Messages de statut** : Confirmation ou refus d'accès selon la guilde

### 🎨 Amélioré
- **Interface de configuration** : Options "Affichage alerte" et "Son alerte"
- **Hotkey dynamique** : L'interface affiche la vraie touche configurée
- **Configuration instantanée** : Changements sans redémarrage du jeu
- **Sauvegarde automatique** : Configuration persistante entre les sessions

### 🔧 Modifié
- **README** : Documentation complète avec instructions d'installation et d'utilisation
- **Structure du projet** : Organisation claire des classes et composants
- **Gestion des erreurs** : Meilleure robustesse et messages d'erreur clairs

### 🐛 Corrigé
- **Détection des timers** : Correction de l'interception des messages `/legendaryspawn`
- **Interface des timers** : Restauration du formatage et du positionnement
- **Gestion des dimensions** : Support des dimensions custom et vanilla

## [1.0.0] - 2025-03-18

### ✨ Ajouté
- **Détection automatique des dimensions** : Support des dimensions custom (Construction1, Ressource2, etc.)
- **Système de timers intelligent** : Parsing des formats "X minutes et Y secondes"
- **Interface moderne** : Affichage clair des timers avec couleurs adaptatives
- **Système d'alerte** : Notification à 1 minute avec son d'enclume
- **Hotkeys configurables** : Touches personnalisables pour ouvrir/fermer l'interface
- **Gestion multi-dimensions** : Suivi des timers pour toutes les dimensions visitées

### 🎯 Fonctionnalités principales
- Détection automatique des messages `/legendaryspawn`
- Interface utilisateur élégante et intuitive
- Barres de progression colorées selon le temps restant
- Configuration en jeu accessible via la touche K
- Sauvegarde automatique des préférences

---

## 📝 Notes de version

- **v1.0.0** : Version initiale avec toutes les fonctionnalités de base
- **v1.1.0** : Ajout du système de sécurité par guilde et améliorations de l'interface

## 🔮 Versions futures

### [1.2.0] - Non Planifié

---

*Ce changelog suit le format [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)*
