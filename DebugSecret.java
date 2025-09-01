// ✅ DEBUG SECRET KEY ENCODING

public class DebugSecret {
    public static void main(String[] args) {
        String accessKey = "7fae5a0c23d8a65c132f7ab5625c63e1";
        String secretKey = "3e36fdc0daa2f9d8d7a38df4210ae43166bdb94f2a268b8c255066efd910d617";
        
        System.out.println("=== SECRET KEY DEBUGGING ===");
        System.out.println("Secret Key original: " + secretKey);
        System.out.println("Secret Key length: " + secretKey.length());
        
        // GÉNÉRATION DU BON ARRAY
        System.out.println("\n=== GENERATION ARRAY SECRET ===");
        printArrayForSecret(secretKey, "AurelR2Secret2024");
        
        // ARRAY ACTUEL DANS R2CONFIG
        int[] currentArray = {
            78, 59, 49, 100, 126, 1, 99, 53, 74, 31, 76, 47, 103, 94, 70, 27, 73, 40, 34, 102, 45, 65, 90, 109, 8, 61, 51, 70, 44, 194, 223, 147, 184, 186, 193, 178, 133, 141, 204, 197, 228, 222, 186, 142, 217, 165, 176, 164, 177, 140, 193, 188, 188, 135, 219, 198, 248, 221, 194, 180, 212, 190, 174, 191
        };
        
        // TEST DÉCODAGE ARRAY ACTUEL
        System.out.println("\n=== TEST DÉCODAGE ARRAY ACTUEL ===");
        String decoded = decodeSecret(currentArray, "AurelR2Secret2024");
        System.out.println("Décodé actuel: " + decoded);
        System.out.println("Length décodé: " + decoded.length());
        System.out.println("Match avec original: " + secretKey.equals(decoded));
        
        if (!secretKey.equals(decoded)) {
            System.out.println("\n❌ PROBLÈME D'ENCODAGE !");
            System.out.println("Original : " + secretKey.substring(0, 20) + "...");
            System.out.println("Décodé   : " + decoded.substring(0, Math.min(20, decoded.length())) + "...");
        }
    }
    
    static void printArrayForSecret(String text, String key) {
        System.out.print("private static final int[] ENCODED_SECRET_KEY = {\n    ");
        for (int i = 0; i < text.length(); i++) {
            int keyChar = key.charAt(i % key.length());
            int encoded = text.charAt(i) ^ keyChar ^ (42 + i * 3);
            System.out.print(encoded);
            if (i < text.length() - 1) {
                System.out.print(", ");
                if ((i + 1) % 16 == 0) {
                    System.out.print("\n    ");
                }
            }
        }
        System.out.println("\n};");
    }
    
    static String decodeSecret(int[] encoded, String key) {
        StringBuilder result = new StringBuilder();
        
        try {
            for (int i = 0; i < encoded.length; i++) {
                int keyChar = key.charAt(i % key.length());
                int decoded = encoded[i] ^ keyChar ^ (42 + i * 3);
                result.append((char) decoded);
            }
            return result.toString();
        } catch (Exception e) {
            System.err.println("Erreur décodage: " + e.getMessage());
            return "";
        }
    }
}

