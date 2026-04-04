package com.retailai.service;

import com.retailai.model.Product;
import org.springframework.stereotype.Service;

@Service
public class AIStylistService {

    public String generateAdvice(Product product, String vibe) {

        String category = product.getCategory() != null
                ? product.getCategory().toLowerCase()
                : "";

        String name = product.getItemName();

        // 🔵 FORMAL VIBE
        if ("Formal".equalsIgnoreCase(vibe)) {

            if (category.equals("tops")) {
                return "Build a sharp formal look with this " + name +
                        ". Pair it with tailored trousers, polished shoes, and a structured coat for a refined finish.";
            }

            if (category.equals("bottoms")) {
                return "These create a strong formal foundation. Pair with a crisp shirt, blazer, and dress shoes for a clean silhouette.";
            }

            if (category.equals("shoes")) {
                return "These elevate your formal outfit. Combine with tailored pieces and a structured top for a polished appearance.";
            }

            if (category.equals("outerwear")) {
                return "This adds structure and authority to your outfit. Layer it over formal basics for a complete elevated look.";
            }
        }

        // 🔴 DATE NIGHT VIBE
        if ("Date Night".equalsIgnoreCase(vibe)) {

            if (category.equals("tops")) {
                return "This " + name +
                        " works great for a confident date-night look. Pair with fitted jeans and sleek shoes for a sharp finish.";
            }

            if (category.equals("bottoms")) {
                return "These create a sleek base. Style with a fitted top and bold outerwear to elevate your date-night outfit.";
            }

            if (category.equals("shoes")) {
                return "These shoes anchor your outfit with confidence. Pair with dark denim and a clean top for a strong impression.";
            }

            if (category.equals("outerwear")) {
                return "This piece adds instant edge. Layer it over a fitted outfit to create a bold and confident date-night look.";
            }
        }

        // 🟢 CASUAL (DEFAULT)
        if (category.equals("tops")) {
            return "This " + name +
                    " is a versatile base. Pair it with jeans and sneakers, and add outerwear for a clean layered casual look.";
        }

        if (category.equals("bottoms")) {
            return "These are perfect for everyday styling. Match with a relaxed top and sneakers for an easy casual outfit.";
        }

        if (category.equals("shoes")) {
            return "These sneakers ground your outfit. Pair with denim and a clean top for a balanced casual look.";
        }

        if (category.equals("outerwear")) {
            return "This adds structure to your outfit. Layer it over simple pieces for a refined casual vibe.";
        }

        return "A versatile piece you can build around with balanced layers and confident styling.";
    }
}