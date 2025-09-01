import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * ✅ TEST RAPIDE CONFIGURATION R2 (standalone)
 * 
 * Test simple qui ne nécessite pas Minecraft/Fabric
 */
public class TestR2Config {
    
    // Configuration copiée depuis R2Config
    private static final String WORKER_TIMERS_URL = "https://aureltimer-sync.aure-perreyprillo.workers.dev/timer_sync.json";
    private static final String R2_ACCOUNT_ID = "54cd4124b871d620069df5881c7537c0";
    private static final String R2_ENDPOINT = "https://" + R2_ACCOUNT_ID + ".r2.cloudflarestorage.com";
    
    // Secrets encodés
    private static final int[] ENCODED_ACCESS_KEY = {
        92, 62, 35, 51, 111, 10, 62, 29, 19, 21, 73, 0, 92, 85, 81, 6, 
        95, 47, 39, 119, 52, 100, 92, 104, 5, 36, 46, 125, 59, 193, 211, 134
    };
    
    private static final int[] ENCODED_SECRET_KEY = {
        78, 59, 49, 100, 126, 1, 99, 53, 74, 31, 76, 47, 103, 94, 70, 27, 
        73, 40, 34, 102, 45, 65, 90, 109, 8, 61, 51, 70, 44, 194, 223, 147, 
        184, 186, 193, 178, 133, 141, 204, 197, 228, 222, 186, 142, 217, 165, 176, 164
    };
    
    public static void main(String[] args) {
        System.out.println("🧪 === TEST CONFIGURATION R2 + WORKER ===");
        
        boolean allTestsPassed = true;
        
        // Test 1: Décodage secrets
        allTestsPassed &= testSecretDecoding();
        
        // Test 2: Worker GET
        allTestsPassed &= testWorkerGet();
        
        // Résultat final
        if (allTestsPassed) {
            System.out.println("✅ 🎉 TESTS RÉUSSIS ! Configuration R2 semble OK !");
            System.out.println("\n📋 PROCHAINES ÉTAPES :");
            System.out.println("  1. Le Worker répond correctement");
            System.out.println("  2. Les secrets se décodent bien");
            System.out.println("  3. On peut maintenant créer R2Client pour tester le PUT");
        } else {
            System.out.println("❌ 🚨 CERTAINS TESTS ONT ÉCHOUÉ !");
        }
    }
    
    /**
     * Test décodage des secrets
     */
    private static boolean testSecretDecoding() {
        System.out.println("🔧 Test 1: Décodage secrets...");
        
        try {
            String accessKey = decodeSecret(ENCODED_ACCESS_KEY, "AurelR2Access2024");
            String secretKey = decodeSecret(ENCODED_SECRET_KEY, "AurelR2Secret2024");
            
            System.out.println("  📋 Access Key: " + accessKey.substring(0, Math.min(8, accessKey.length())) + "... (longueur: " + accessKey.length() + ")");
            System.out.println("  📋 Secret Key: " + secretKey.substring(0, Math.min(8, secretKey.length())) + "... (longueur: " + secretKey.length() + ")");
            System.out.println("  📋 Endpoint: " + R2_ENDPOINT);
            
            if (accessKey.length() > 10 && secretKey.length() > 20) {
                System.out.println("✅ Test 1 RÉUSSI: Secrets décodés correctement");
                return true;
            } else {
                System.out.println("❌ Test 1 ÉCHOUÉ: Secrets trop courts");
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ Test 1 ÉCHOUÉ: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test Worker GET
     */
    private static boolean testWorkerGet() {
        System.out.println("🌐 Test 2: Worker GET...");
        
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
            
            System.out.println("  📥 GET " + WORKER_TIMERS_URL);
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WORKER_TIMERS_URL))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            System.out.println("  📨 Status: " + response.statusCode());
            
            // Afficher quelques headers importants
            response.headers().firstValue("ETag").ifPresent(etag -> 
                System.out.println("  📨 ETag: " + etag));
            response.headers().firstValue("Cache-Control").ifPresent(cc -> 
                System.out.println("  📨 Cache-Control: " + cc));
            response.headers().firstValue("Content-Type").ifPresent(ct -> 
                System.out.println("  📨 Content-Type: " + ct));
            
            if (response.statusCode() == 200) {
                String body = response.body();
                System.out.println("  📨 Body size: " + body.length() + " chars");
                System.out.println("  📨 Body preview: " + body.substring(0, Math.min(200, body.length())).replace("\n", "\\n") + "...");
                
                // Vérifier qu'on a un JSON valide
                if (body.contains("{") && body.contains("}")) {
                    System.out.println("✅ Test 2 RÉUSSI: Worker GET OK, JSON reçu");
                    return true;
                } else {
                    System.out.println("❌ Test 2 ÉCHOUÉ: Réponse ne semble pas être du JSON");
                    return false;
                }
                
            } else {
                System.out.println("❌ Test 2 ÉCHOUÉ: Status " + response.statusCode());
                return false;
            }
            
        } catch (Exception e) {
            System.out.println("❌ Test 2 ÉCHOUÉ: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Décodage des secrets (copié depuis R2Config)
     */
    private static String decodeSecret(int[] encoded, String key) {
        StringBuilder result = new StringBuilder();
        
        try {
            for (int i = 0; i < encoded.length && encoded[i] != 0; i++) {
                int keyChar = key.charAt(i % key.length());
                int decoded = encoded[i] ^ keyChar ^ (42 + i * 3);
                if (decoded != 0) {
                    result.append((char) decoded);
                }
            }
            return result.toString();
        } catch (Exception e) {
            System.err.println("Erreur décodage: " + e.getMessage());
            return "";
        }
    }
}

