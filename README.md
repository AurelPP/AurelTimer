# ⏰ Aurel Timer - Mod Minecraft Fabric

Un mod simple et efficace pour tracker les timers de spawn légendaires sur Cobblestory.

## ✨ Fonctionnalités

- **Détecte automatiquement** le nom de la dimension actuelle (Construction1, Ressource2, etc.)
- **Détection automatique** des messages `/legendaryspawn`

- **Interface claire** avec couleurs selon le temps restant
- **Mise à jour en temps réel** des countdowns
- **Alerte à 1 minute** avant expiration du timer
- **Message dans le chat** : "⚠ SPAWN DE LÉGENDAIRE DANS 1 MINUTE EN [DIMENSION] ⚠"
- **Son d'enclume Minecraft** pour alerter le joueur

- **Hotkey configurable** pour ouvrir/fermer l'interface
- **Interface de configuration** accessible via la touche K de base

## 🔐 **Système de sécurité par guilde**

### **Vérification automatique**
- **Exécution automatique** de `/t info` pour vérifier l'appartenance
- **Vérification unique** : une seule fois par session de jeu
- **Accès restreint** : seuls les membres de la guilde **Aether** peuvent utiliser le mod

### **Messages de statut**
- **✅ Autorisé** : "Aurel Timer : Appartenance à la guilde Aether vérifiée"
- **❌ Refusé** : "Aurel Timer : Vous n'appartenez pas à la guilde Aether, accès refusé"
- **⏳ En cours** : "Vérification de guilde en cours..." (si pas encore vérifié)

### **Protection complète**
- **Interface des timers** : bloquée si non autorisé
- **Interface de configuration** : bloquée si non autorisé
- **Détection des timers** : désactivée si non autorisé
- **Toutes les fonctionnalités** : protégées par la vérification

## 🚀 Installation

### **Prérequis**
- Minecraft 1.21.1
- Fabric Loader
- Fabric API 0.131.3+

### **Étapes d'installation**
1. **Téléchargez** le fichier `.jar` depuis les releases
2. **Placez-le** dans le dossier `mods`
3. **Lancez** le jeu avec Fabric
4. **Attendez** la vérification automatique de guilde

## 🎯 Utilisation

### **Première connexion**
1. **Connectez-vous** au serveur Cobblestory
3. **Le mod exécute automatiquement** `/t info`
4. **Vérification** de l'appartenance à la guilde Aether
5. **Accès accordé/refusé** selon le résultat

### **Commandes de base**
- **`/legendaryspawn`** : Ajoute le timer du légendaire de la dimension actuelle dans l'interface des timers
- **Touche L** (par défaut) : Ouvre/ferme l'interface des timers
- **Touche K** (par défaut) : Ouvre l'interface de configuration

### **Comment ça marche**
1. **Téléportez-vous** dans une dimension (ex: `/home construction1`)
2. **Lancez** `/legendaryspawn` pour démarrer le timer
3. **Surveillez** l'interface pour voir le countdown
4. **Recevez** l'alerte à 1 minute restante
5. **Répétez** dans d'autres dimensions

### **Interface des timers**
- **Titre** : "⏰ Timers Légendaires"
- **Instructions** : "Appuie sur [TOUCHE] pour fermer"
- **Liste des timers** : Dimension + temps restant
- **Couleurs** : Vert (>5min) → Orange (1-5min) → Rouge (<1min)

## 🔧 Configuration

### **Interface de configuration (Touche K)**
- **Affichage alerte** : Chat ou Non
- **Son alerte** : Oui ou Non
- **Bouton "Terminé"** pour sauvegarder

### **Fichier de configuration**
- **Emplacement** : `config/aurel-timer-config.json`
- **Format** : JSON automatique
- **Sauvegarde** : Instantanée à chaque changement

### **Exemple de configuration**
```json
{
  "alertDisplay": "CHAT",
  "soundEnabled": "YES"
}
```

## 🎨 Personnalisation

### **Hotkeys personnalisables**
- **Ouvrir/fermer interface** : Configurable dans Options → Contrôles
- **Ouvrir configuration** : Touche K (fixe)
- **Interface intelligente** : Affiche toujours la vraie touche configurée

### **Couleurs et style**
- **Fond** : Noir semi-transparent
- **Bordures** : Vert
- **Titre** : Blanc
- **Texte** : Gris clair
- **Timers** : Jaune → Orange → Rouge

## 🐛 Dépannage

### **Problèmes courants**
- **Interface ne s'ouvre pas** : Vérifiez la hotkey dans Options → Contrôles
- **Timers ne se créent pas** : Assurez-vous d'être dans une dimension et d'utiliser `/legendaryspawn`
- **Alertes ne fonctionnent pas** : Vérifiez la configuration avec la touche K
- **Accès refusé** : Vérifiez que vous appartenez à la guilde Aether

### **Vérification de guilde**
- **Message "Vérification en cours"** : Attendez la fin de la vérification
- **Message "Accès refusé"** : Vous n'appartenez pas à la guilde Aether
- **Aucun message** : Vérifiez que le mod est bien installé et activé

## 🔄 Versions

### **v1.1.0 - Version actuelle**
- ✅ Détection automatique des dimensions
- ✅ Système de timers complet
- ✅ Alertes à 1 minute avec son
- ✅ Interface de configuration
- ✅ Hotkeys configurables
- ✅ Interface moderne et responsive
- 🔐 **NOUVEAU : Système de sécurité par guilde Aether**

## 🤝 Contribution

Ce mod est développé pour la fondation Aether. N'hésitez pas à :
- **Signaler des bugs**
- **Proposer des améliorations**
- **Partager vos retours d'expérience**

## 📜 Licence

Droits réservés.

---

**Développé avec ❤️ pour la fondation Aether par Aurel**

*Un mod sécurisé et efficace pour tracker vos timers légendaires !*

