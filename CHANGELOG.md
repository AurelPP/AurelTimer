# 📋 Changelog

Toutes les modifications notables de ce projet seront documentées dans ce fichier.

Le format est basé sur [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/),
et ce projet adhère au [Semantic Versioning](https://semver.org/lang/fr/).

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

### [1.2.0] - Planifié
- **Notifications avancées** : Options de notification personnalisables
- **Statistiques** : Historique des timers et statistiques d'utilisation
- **Export/Import** : Sauvegarde et partage des configurations

### [1.3.0] - Planifié
- **Support multi-serveurs** : Configuration par serveur
- **API publique** : Interface pour d'autres mods
- **Thèmes visuels** : Personnalisation de l'apparence

---

*Ce changelog suit le format [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/)*
