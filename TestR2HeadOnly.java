// ✅ TEST R2 HEAD SEULEMENT (sans PUT)

import java.net.URI;

public class TestR2HeadOnly {
    public static void main(String[] args) {
        System.out.println("🧪 TEST R2 HEAD SEULEMENT");
        
        try {
            // Utiliser AWS CLI pour test
            System.out.println("💡 Pour tester manuellement :");
            System.out.println("aws s3 ls s3://aureltimer/ --endpoint-url https://54cd4124b871d620069df5881c7537c0.r2.cloudflarestorage.com");
            System.out.println("aws s3api head-object --bucket aureltimer --key timer_sync.json --endpoint-url https://54cd4124b871d620069df5881c7537c0.r2.cloudflarestorage.com");
            
            System.out.println("\n🔍 Vérifications :");
            System.out.println("1. Les clés R2 ont-elles les permissions Object:Read et Object:Write ?");
            System.out.println("2. Le bucket 'aureltimer' existe-t-il ?");
            System.out.println("3. L'objet 'timer_sync.json' existe-t-il ?");
            System.out.println("4. Les clés sont-elles dans la bonne Account ID ?");
            
            System.out.println("\n🎯 HYPOTHÈSES :");
            System.out.println("- L'Access Key et Secret Key se décodent correctement maintenant");
            System.out.println("- Le problème vient probablement des permissions R2");
            System.out.println("- Ou d'une incompatibilité région/endpoint");
            
            System.out.println("\n🔧 TESTS À FAIRE :");
            System.out.println("1. Vérifier les permissions dans Cloudflare Dashboard");
            System.out.println("2. Tester avec un bucket public en lecture");
            System.out.println("3. Essayer region 'us-east-1' au lieu de 'auto'");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

