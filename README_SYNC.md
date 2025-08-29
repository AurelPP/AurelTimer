# 🚀 Synchronisation des Timers - Guide de Configuration

## 📋 **Structure JSON de Synchronisation**

Voici la structure exacte du JSON hébergé sur GitHub Gist pour synchroniser les timers entre utilisateurs :

```json
{
  "version": "1.0.0",
  "last_updated": "2025-01-26T15:30:00Z",
  "ttl_minutes": 3,
  "settings": {
    "auto_cleanup_expired": true,
    "max_timers_per_dimension": 1,
    "sync_enabled": true
  },
  "timers": {
    "Ressource1": {
      "expires_at": "2025-01-26T16:45:33Z",
      "created_by": "Ruael",
      "created_at": "2025-01-26T16:30:00Z", 
      "initial_duration_seconds": 933,
      "predicted_phase": "afternoon",
      "predicted_phase_display": "Après-midi (13h - 17h)"
    },
    "Construction2": {
      "expires_at": "2025-01-26T17:12:15Z",
      "created_by": "PlayerX",
      "created_at": "2025-01-26T16:50:45Z",
      "initial_duration_seconds": 1290,
      "predicted_phase": "dusk", 
      "predicted_phase_display": "Crépuscule (18h - 18h)"
    }
  },
  "whitelist": {
    "players": ["Ruael", "PlayerX", "TestPlayer"],
    "last_whitelist_check": "2025-01-26T15:25:00Z"
  },
  "stats": {
    "total_timers_created": 156,
    "active_users_24h": 12,
    "most_tracked_dimension": "Ressource1"
  }
}
```

## 🔧 **Configuration Requise**

### **Dans TimerSyncManager.java :**

Remplacez les URLs de placeholder par les vraies :

```java
private static final String SYNC_URL = "https://gist.githubusercontent.com/AurelPP/VOTRE_GIST_ID/raw/timer_sync.json";
private static final String UPDATE_URL = "https://api.github.com/gists/VOTRE_GIST_ID";
```

### **Token GitHub (pour les updates) :**

1. Aller sur GitHub → Settings → Developer settings → Personal access tokens
2. Créer un token avec permission "gist"
3. L'ajouter dans TimerSyncManager pour l'upload

## 🚀 **Fonctionnement**

### **🕐 Logique de Timestamp :**

- **Timer créé** : `expires_at = now() + duration` → **UNE SEULE UPDATE**
- **Côté client** : Calcul temps restant = `expires_at - now()` → **TEMPS RÉEL**
- **Auto-cleanup** : Timers expirés supprimés automatiquement

### **⚡ Performance :**

- **TTL de 3 minutes** : Re-download seulement si nécessaire
- **ETag support** : Pas de download si pas de changements
- **Cache local** : Fusion des timers locaux + distants

### **🔧 Configuration Utilisateur :**

- **Interface de config** : Touche K → Option "Synchronisation: Oui/Non"
- **Status en temps réel** : Affiché dans l'interface de config
- **Fallback graceful** : Continue de fonctionner si sync désactivée

### **🛡️ Sécurité :**

- **Whitelist intégrée** : Seuls les joueurs autorisés peuvent sync
- **Validation côté client** : Vérification avant chaque opération

## 📊 **Avantages de cette Architecture :**

✅ **Pas de serveur** : GitHub Gist comme backend gratuit  
✅ **Performance** : Quasi-zero API calls grâce aux timestamps  
✅ **Temps réel** : Décompte fluide côté client  
✅ **Conflict resolution** : Last-writer-wins  
✅ **Backward compatible** : Fonctionne sans sync  

## 🧪 **Test Multi-Utilisateurs**

1. **Utilisateur A** : Fait `/legendaryspawn` → Crée timer
2. **JSON mis à jour** avec `expires_at` fixe
3. **Utilisateur B** : Se connecte → Voit le timer automatiquement
4. **Les deux voient** le décompte en temps réel

La synchronisation est maintenant prête ! 🎯
