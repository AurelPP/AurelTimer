// ✅ GÉNÉRATION FINALE SECRET KEY CORRECTE

public class FixSecretFinal {
    public static void main(String[] args) {
        String correctSecret = "3e36fdc0daa2f9d8d7a38df4210ae43166bdb94f2a268b8c255066efd910d617";
        
        System.out.println("Secret attendue: " + correctSecret);
        System.out.println("Length: " + correctSecret.length());
        System.out.println("Format hex: " + correctSecret.matches("[a-f0-9]{64}"));
        
        System.out.println("\n=== GÉNÉRATION ARRAY CORRECT ===");
        generateCorrectArray(correctSecret, "AurelR2Secret2024");
        
        System.out.println("\n=== TEST DÉCODAGE ===");
        int[] correctArray = encodeToArray(correctSecret, "AurelR2Secret2024");
        String decoded = decodeFromArray(correctArray, "AurelR2Secret2024");
        
        System.out.println("Décodé: " + decoded);
        System.out.println("Match: " + correctSecret.equals(decoded));
        System.out.println("Format hex décodé: " + decoded.matches("[a-f0-9]{64}"));
    }
    
    static void generateCorrectArray(String text, String key) {
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
    
    static int[] encodeToArray(String text, String key) {
        int[] result = new int[text.length()];
        for (int i = 0; i < text.length(); i++) {
            int keyChar = key.charAt(i % key.length());
            result[i] = text.charAt(i) ^ keyChar ^ (42 + i * 3);
        }
        return result;
    }
    
    static String decodeFromArray(int[] encoded, String key) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < encoded.length; i++) {
            int keyChar = key.charAt(i % key.length());
            int decoded = encoded[i] ^ keyChar ^ (42 + i * 3);
            result.append((char) decoded);
        }
        return result.toString();
    }
}

