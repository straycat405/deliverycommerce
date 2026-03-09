package com.babjo.deliverycommerce.domain.product.entity;

import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // 생성자를 통해서 값 변경을 목적으로 접근하는 메시지들 차단
@AllArgsConstructor
@Builder
@Table(name = "p_product")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "product_id")
    private UUID productId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
     private Store store;

    @Column(name = "product_category", nullable = false, length = 10)
    private String productCategory;

    @Column(name = "name", nullable = false, length = 10)
    private String name;

    @Column(name = "price", nullable = false)
    private Integer price;

    @Column(name = "description", nullable = true, columnDefinition = "TEXT")   // 255 부족할 경우 TEXT로 수정 예정
    private String description;

    @Column(name = "product_hide", nullable = false)
    @Builder.Default    // Lombok 기능, default 값 설정
    private Boolean productHide = false;

    @Column(name = "is_use_ai", nullable = false)
    @Builder.Default
    private Boolean useAiDescription = false;

    // domain method
    public void update(String name, Integer price, String description, String productCategory) {
        this.name = name;
        this.price = price;
        this.description = description;
        this.productCategory = productCategory;
    }

    public void updateDescription(String description) {
        this.description = description;
        this.useAiDescription = true;
    }

    public void hide() {
        this.productHide = true;
    }

    public void show() {
        this.productHide = false;
    }

    public Boolean isProductHide() {
        if(this.productHide) {
            return true;
        }
        return false;
    }
}
