package com.sneakerstore.dto.response;

import com.sneakerstore.dto.response.ProductVariantDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String productCode;
    private String productName;
    private Long brandId;
    private String brandName;
    private Long categoryId;
    private String categoryName;
    private String material;
    private String description;
    private String thumbnailUrl;
    private List<String> imageUrls;
    private List<ProductImageDto> images;
    private BigDecimal importPrice;
    private BigDecimal salePrice;
    private String status;
    private List<ProductVariantDto> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageDto {
        private Long id;
        private String imageUrl;
        private Long colorId;
        private Integer isPrimary;
        private Integer displayOrder;
    }
}

