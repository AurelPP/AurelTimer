# 📋 Changelog

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Semantic Versioning](https://semver.org/lang/fr/).

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
