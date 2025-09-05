# 🕒 AurelTimer - Mod de Synchronisation des Légendaires

## 🎯 À quoi ça sert ?

AurelTimer est un mod exclusif à la fondation Aether qui gère les timers de spawn des Pokémon légendaires avec une interface claire et des alertes automatiques.

## ⚡ Comment ça marche ?

### 🎮 Utilisation simple
Quand tu lances `/legendaryspawn` dans une dimension, le mod enregistre automatiquement le timer et l'associe à la bonne dimension.

### 📊 Interface intuitive  
Appuie sur **L** pour voir tous tes timers actifs avec le temps restant exact et la phase du jour prédite (Midi, Crépuscule, etc.).

### 🔔 Alertes intelligentes
Reçois automatiquement une notification 1 minute avant chaque spawn avec un son d'enclume pour ne jamais rater un légendaire. **Nouveau v1.4.7** : Couleurs de phase et opacité personnalisables !

### 🕐 Prédiction des phases
Le mod calcule automatiquement à quelle phase du jour le légendaire va spawn (Dawn, Noon, Night, etc.) pour t'aider à optimiser tes captures.

### 🌐 Synchronisation automatique
Tous les timers se partagent entre les joueurs du mod ! Si quelqu'un crée un timer, tu le vois automatiquement. **Nouveau v1.4.7** : Interface colorée et opacité ajustable pour une meilleure visibilité !

### 🖱️ Interface déplaçable
Clique et glisse le titre de l'interface pour la positionner où tu veux. La position est sauvegardée !

## 🎛️ Personnalisation

### Configuration rapide
Appuie sur **K** pour accéder aux options :
- **Affichage alerte** : Chat / Non  
- **Son alerte** : Oui / Non
- **Synchronisation** : Oui / Non
- **🎵 Volume sonore** : Contrôle le volume du son d'enclume (0-100%)
- **📊 Timers affichés** : Nombre de timers visibles dans l'interface (1-6)

### 🎚️ Contrôles avancés
- **Slider volume** : Ajuste le volume du son d'alerte avec un contrôle glissant
- **Bouton Play** : Teste le son d'enclume à tout moment
- **Slider timers** : Choisis combien de timers afficher (interface s'adapte automatiquement)

### Configuration instantanée
Tous les changements s'appliquent immédiatement sans redémarrage !

## 🚀 Installation

### Prérequis
- **Minecraft** : 1.20.1
- **Fabric Loader** : 0.14.21+  
- **Fabric API** : 0.84.0+

### Étapes
1. Télécharge la dernière version depuis [Releases](../../releases)
2. Place le fichier `.jar` dans ton dossier `mods/`
3. Lance Minecraft avec Fabric
4. Appuie sur **L** en jeu pour tester !

**🌐 La synchronisation est automatique et fonctionne immédiatement !**

## 🌟 Avantages

✅ **Interface propre** qui ne pollue pas ton écran  
✅ **Alertes fiables** pour ne jamais rater un spawn  
✅ **Prédiction des phases** pour optimiser selon l'heure Minecraft  
✅ **Gestion multi-dimensions** automatique  
✅ **Synchronisation temps réel** avec les autres joueurs  
✅ **Position personnalisable** et sauvegardée  

## 🕐 Phases du Jour Reconnues

- **🌅 Dawn** (Aube) : 05:00 - 05:59
- **🌄 Morning** (Matin) : 06:00 - 10:59  
- **☀️ Noon** (Midi) : 11:00 - 12:59
- **🌤️ Afternoon** (Après-midi) : 13:00 - 17:59
- **🌞 Day** (Jour) : 06:00 - 17:59
- **🌆 Dusk** (Crépuscule) : 18:00 - 18:59
- **🌙 Night** (Nuit) : 19:00 - 04:59
- **🌃 Midnight** (Minuit) : 23:00 - 00:59

## 🎮 Raccourcis

- **L** : Ouvrir/fermer l'interface des timers
- **K** : Ouvrir la configuration
- **Clic + Glisser** : Déplacer l'interface

## 🆕 Nouveautés v1.4.3

🚀 **Migration Cloudflare** : Architecture ultra-performante avec Workers + R2  
⚡ **Jar ultra-léger** : 170KB (réduction de 97%) sans dépendances lourdes  
🔒 **Sécurité renforcée** : Token obfusqué et circuit breakers intelligents  
🎯 **Single Actor Pattern** : Thread-safety garantie et merge déterministe  
🌐 **Synchronisation optimisée** : < 5 secondes de propagation, quota illimité  
🐛 **Corrections critiques** : Noms de dimensions, authentification, headers HTTP

## 🆕 Nouveautés v1.4

🌐 **Synchronisation multi-joueurs** : Partage automatique des timers  
🖱️ **Interface déplaçable** : Drag & drop pour positionner l'interface  
💾 **Position persistante** : Ta position d'interface est sauvegardée  
🔄 **Sync à la connexion** : Récupération automatique des timers  
🐛 **Corrections importantes** : Fini les bugs de barres de progression et le spam d'alertes  
🚀 **v1.4.6** : Reconnexion automatique, synchronisation réparée, alertes précises  
🎨 **v1.4.7** : Couleurs de phase, opacité d'interface, personnalisation avancée

## 📝 Historique

Voir [CHANGELOG.md](CHANGELOG.md) pour l'historique complet des versions.

## 📄 Licence

Tous droits réservés. Usage exclusif pour la Fondation Aether.