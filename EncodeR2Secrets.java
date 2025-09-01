// ✅ UTILITAIRE TEMPORAIRE POUR ENCODER LES SECRETS R2
// Même logique que le token GitHub

public class EncodeR2Secrets {
    public static void main(String[] args) {
        String accessKey = "7fae5a0c23d8a65c132f7ab5625c63e1";
        String secretKey = "3e36fdc0daa2f9d8d7a38df4210ae43166bdb94f2a268b8c255066efd910d617";
        
        System.out.println("// Access Key encoded:");
        printEncoded(accessKey, "AurelR2Access2024");
        
        System.out.println("\n// Secret Key encoded:");  
        printEncoded(secretKey, "AurelR2Secret2024");
    }
    
    static void printEncoded(String text, String key) {
        System.out.print("{ ");
        for (int i = 0; i < text.length(); i++) {
            int keyChar = key.charAt(i % key.length());
            int encoded = text.charAt(i) ^ keyChar ^ (42 + i * 3);
            System.out.print(encoded);
            if (i < text.length() - 1) System.out.print(", ");
        }
        System.out.println(" }");
    }
}
