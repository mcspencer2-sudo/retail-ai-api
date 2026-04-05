package com.retailai.service;

import com.retailai.model.Product;
import org.springframework.stereotype.Service;

@Service
public class AIStylistService {

    public String generateAdvice(Product product, String vibe) {
        String category = normalize(product.getCategory());
        String item = product.getItemName();

        return switch (vibe.toLowerCase()) {

            case "casual" -> casualAdvice(category, item);
            case "formal" -> formalAdvice(category, item);
            case "date night" -> dateNightAdvice(category, item);
            case "streetwear" -> streetwearAdvice(category, item);

            default -> defaultAdvice(item);
        };
    }

    private String casualAdvice(String category, String item) {
        return switch (category) {
            case "tops" -> "This " + item + " is a versatile everyday piece. Pair it with relaxed denim and clean sneakers for an effortless casual look.";
            case "bottoms" -> "These " + item + " work perfectly with a simple tee or hoodie. Add sneakers to keep it laid-back and comfortable.";
            case "shoes" -> "These " + item + " are perfect for daily wear. Pair them with jeans and a basic top for a clean casual outfit.";
            case "outerwear" -> "This " + item + " layers well over simple outfits. Combine with jeans and sneakers for a clean, casual vibe.";
            default -> defaultAdvice(item);
        };
    }

    private String formalAdvice(String category, String item) {
        return switch (category) {
            case "tops" -> "This " + item + " creates a polished base. Pair it with tailored trousers and dress shoes for a refined formal look.";
            case "bottoms" -> "These " + item + " elevate your outfit. Combine with a button-up shirt and sleek shoes for a sharp appearance.";
            case "shoes" -> "These " + item + " complete a formal outfit. Pair with structured pieces like trousers and a fitted top.";
            case "outerwear" -> "This " + item + " adds structure and sophistication. Layer over a clean formal outfit for a finished look.";
            default -> defaultAdvice(item);
        };
    }

    private String dateNightAdvice(String category, String item) {
        return switch (category) {
            case "tops" -> "This " + item + " is perfect for a stylish night out. Pair it with fitted bottoms and standout shoes to elevate your look.";
            case "bottoms" -> "These " + item + " create a sleek silhouette. Combine with a sharp top and bold footwear for a date-ready outfit.";
            case "shoes" -> "These " + item + " add personality to your look. Pair with fitted pieces for a confident and stylish vibe.";
            case "outerwear" -> "This " + item + " adds an edge to your outfit. Layer over a clean base to stand out on your night out.";
            default -> defaultAdvice(item);
        };
    }

    private String streetwearAdvice(String category, String item) {
        return switch (category) {
            case "tops" -> "This " + item + " works great in a streetwear fit. Pair with oversized bottoms and statement sneakers.";
            case "bottoms" -> "These " + item + " fit perfectly into a streetwear aesthetic. Combine with a graphic tee and bold sneakers.";
            case "shoes" -> "These " + item + " are the centerpiece of a streetwear outfit. Keep the rest of the outfit clean to let them stand out.";
            case "outerwear" -> "This " + item + " adds depth to your streetwear look. Layer it over a hoodie or tee for a strong finish.";
            default -> defaultAdvice(item);
        };
    }

    private String defaultAdvice(String item) {
        return "This " + item + " is a strong foundation piece. Pair it with complementary items to build a complete outfit.";
    }

    private String normalize(String category) {
        if (category == null) return "";
        return category.trim().toLowerCase();
    }
}