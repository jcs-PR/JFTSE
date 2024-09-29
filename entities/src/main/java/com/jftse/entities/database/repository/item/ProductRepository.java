package com.jftse.entities.database.repository.item;

import com.jftse.entities.database.model.item.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Product findProductByProductIndex(int productIndex);
    List<Product> findProductByItem0AndCategoryAndEnabledIsTrue(int itemIndex, String category);
    List<Product> findProductsByItem0AndCategory(int itemIndex, String category);

    @Query(value = "SELECT p.productIndex FROM Product p WHERE p.category = :category AND p.item0 IN :itemIndexList")
    List<Integer> findAllProductIndexesByCategoryAndItemIndexList(String category, List<Integer> itemIndexList);

    List<Product> findProductsByNameAndCategory(String name, String category);

    @Query(value = "SELECT p.price0 FROM Product p WHERE p.item0 = :itemIndex AND p.category = :category")
    List<Integer> getItemSellPriceByItemIndexAndCategory(@Param("itemIndex") Integer itemIndex, @Param("category") String category);

    List<Product> findProductsByProductIndexIn(List<Integer> productIndexList);

    List<Product> findAllByCategoryAndEnabled(String category, Boolean enabled, Pageable pageable);

    List<Product> findAllByCategoryAndEnabledAndPriceType(String category, Boolean enabled, String priceType, Pageable pageable);

    List<Product> findAllByCategoryAndEnabledAndItem0In(String category, Boolean enabled, List<Integer> item0, Pageable pageable);

    List<Product> findAllByCategoryAndEnabledAndItem0InAndItem1Not(String category, Boolean enabled, List<Integer> item0, Integer item1, Pageable pageable);

    List<Product> findAllByCategoryAndEnabledAndItem0InAndItem1Is(String category, Boolean enabled, List<Integer> item0, Integer item1, Pageable pageable);

    List<Product> findAllByPrice0Between(Integer minPrice, Integer maxPrice, Pageable pageable);

    long countProductsByCategoryAndEnabled(String category, Boolean enabled);

    long countProductsByCategoryAndEnabledAndPriceType(String category, Boolean enabled, String priceType);

    long countProductsByCategoryAndEnabledAndItem0In(String category, Boolean enabled, List<Integer> item0);

    long countProductsByCategoryAndEnabledAndItem0InAndItem1Not(String category, Boolean enabled, List<Integer> item0, Integer item1);

    long countProductsByCategoryAndEnabledAndItem0InAndItem1Is(String category, Boolean enabled, List<Integer> item0, Integer item1);
}
