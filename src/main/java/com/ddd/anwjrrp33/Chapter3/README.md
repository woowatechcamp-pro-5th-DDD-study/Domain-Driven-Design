# 애그리거트

## 애그리거트
* 상위 수준 개념을 이용해서 전체 모델을 정리하면 전반적인 관계를 이해하는데 도움이 된다.
<br/><img src="./그림 3.1.png">
* 상위 모델에 대한 이해없이 상위 수준에서 개념을 파악할려면 오래 걸리며 많은코드를 보고 도메인 전문가와 더 많은 대화를 나눠야 비로소 상위 수준 모델 간의 관계가 이해되기 시작한다.
    * 도메인 객체 모델이 복잡하면 개별 구성요소 위주로 모델을 이해하게 되고 전반적인 구조나 큰 수준에서 도메인 간의 관계를 파악하기 어려워진다.
    * 코드를 변경하고 확장하는 것이 어려워진다는 것을 의미한다.
    * 당장 돌아가는 코드를 추가할 수는 있지만 이런 방법은 장기적으로 코드를 더 수정하기 어렵게 만든다.
<br/><img src="./그림 3.2.png">
* 복잡한 도메인을 이해하고 관리하기 쉬운 단위로 만들려면 상위 수준에서 모델을 조망할 수 있는 방법이 필요한데 그 방법이 `애그리거트`다.
    * 모델을 이해하는데 도움을 주며 일관성을 관리하는 기준이 된다.
    * 복잡한 도메인을 단순한 구조로 만들어주고 도메인 기능을 확장하고 변경하는데 필요한 노력(개발시간)도 줄어든다.
* 애그리거트의 경계를 설정할 때 기본이 되는 것은 도메인 규칙과 요구사항이다.
    * 도메인 규칙에 따라 함께 생성되는 구성요소는 한 애그리거트에 속할 가능성이 높다.
    * `A가 B를 갖는다` 라는 요구사항이 존재해도 한 애그리거트는 아니며 상품과 리뷰는 함께 생성되지도, 변경되지도 않기 때문에 한 애그리거트에 속하지 않는다.
* 처음 도메인 모델을 만들면 큰 애그리거트로 보이는 것들이 많지만 도메인에 대한 경험이 생기고 도메인 규칙을 제대로 이해할수록 애그리거트 실제 크기는 줄어든다.
    * 다수의 애그리거트가 한 개의 엔티티만 갖는 경우가 많고 두개 이상의 엔티티로 구성되는 애그리거트는 드물다.

## 애그리거트 루트
* 애그리거트는 여러객체를 구성되기 때문에 한 객체만 상태가 정상이면 안되며 도메인 규칙을 지키려면 애그리거트에 속한 모든 객체가 정상 상태를 가져야한다.
    * 주문이라는 애그리거트가 존재하면 OrderLine이 변경되면 Order의 totlaAmounts도 다시 계산해서 총 금액이 맞아야 한다.
* 애그리거트에 속한 모든 객체가 일관된 상태를 유지하려면 애그리거트 전체를 관리할 주체가 필요한데 이 책을 지는 것이 바로 애그리거트의 `루트 엔티티`다.
    * 애그리거트에 속한 객체는 애그리거트 루트 엔티티에 직접 또는 간접적으로 속하게 된다.

### 도메인 규칙과 일관성
* 애그리거트 루트가 단순히 애그리거트에 속한 객체를 포함하는 것으로 끝나는 것은 아니며 애그리거트 루트의 핵심 역할은 애그리거트의 일관성이 깨지지 않도록 하는 것이다.
    * 애그리거트 루트는 애그리거트가 제공해야 할 도메인 기능을 구현한다.
    * 애그리거트가 배송지 변경, 상품 변경과 같은 기능을 제공하고 애그리거트 루트인 Order가 이 기능을 구현한 메서드를 제공해준다.
* 애그리거트 루트가 제공하는 메서드는 도메인 규칙에 따라 애그리거트에 속한 객체의 일관성이 꺠지지 않도록 구현해야 한다.
    * 배송이 시작되기 전까지만 배송지 정보를 변경할 수 있다는 규칙이 있다면 루트인 Order의 changeShippingInfo() 메서드는 이 규칙에 따라 배송 시작 여부를 확인하고 규칙을 충족할 때만 배송지 정보를 변경해야한다.
* 애그리거트 외부에서 애그리거트에 속한 객체를 직접 변경하면 안된다.
    * 애그리거트 루트가 강제하는 규칙을 적용할 수 없어 모델의 일관성을 꺠는 원인이 된다.
* 불필요한 중복을 피하고 애그리거트 루트를 통해서만 도메인 로직을 구현하게 만들려면 도메인 모델에 대해서 두 가지를 습관적으로 적용해야한다.
    * 단순히 필드를 변경하는 set 메서드를 공개(public) 범위로 만들지 않는다.
        * 공개 set메서드로 인해서 도메인의 의미나 의도를 표현하지 못하고 도메인 로직을 도메인 객체가 아닌 응용 영역이나 표현 영역으로 분산시켜서 로직이 한 곳에 응집되지 않아서 코드를 유지 보수할 때에도 분석하고 수정하는데 많은 시간이 필요하다.
    * 밸류 타입은 불변으로 구현한다.
        * 밸류 타입을 불변이면 값을 변경할 수 없고 외부에서 밸류 객체의 상태를 변경할 수 없어서 일관성이 깨질 가능성이 줄어든다. 변경하는 방법은 새로운 밸류 객체를 애그리거트 루트가 제공하는 메서드를 통해서 값을 변경해야한다.

### 애그리거트 루트의 기능 구현
* 애그리거트 루트는 애그리거트 내부의 다른 객체를 조합해서 기능을 완성한다.
    * Order은 총금액을 구하기 위해서 OrderLine 목록을 사용한다.
    * Member는 암호 변경을 위해서 Password 객체에 암호가 일치하는지 확인한다.
* 애그리거트 루트가 구성요소의 상태만 참조하는 것은 아니며 기능 실행을 위임한다.
    * Order의 changeOrderLines() 메서드는 orderLines 필드에 상태 변경을 위임하는 방식으로 구현하는데 get을 제공하게 되면 외부에서 기능을 실행할 수 있게되므로 버그를 만든다. 이런 버그가 생기지 않도록 외부에서 OrderLine 목록이 변경할 수 없도록 OrderLines를 불변으로 구현한다.
    * 불변으로 구현이 불가능하다면 패키지나 protected 범위로 한정해서 실행을 제한한다.

### 트랙잭션 범위
* 트랜잭션의 범위는 작으면 작을수록 좋다.
    * 한 트랙잭션이 한 개의 테이블을 수정하는 것과 세 개의 테이블을 수정하는 것을 비교하면 성능에서 차이가 발생한다.
    * 잠금 대상이 많아진다는 것은 동시에 처리할 수 있는 트랜잭션 개수가 줄어든다는 것을 의미하고 전체적인 성능(처리량)을 떨어뜨린다.
* 한 트랙잭션에선 한 애그리거트만 수정해야한다.
    * 애그리거트 냉부에서 다른 애그리거트의 상태를 변경하는 기능을 실행하면 안된다.
    * 애그리거트는 최대한 서로 독립적이여야하고 한 애그리거트가 다른 애그리거트의 기능에 의존하면 결합도가 높아져 향후 수정 비용이 증가한다.
    * 부득이하게 한 트랜잭션으로 두 개 이상의 애그리거트를 수정해야한다면 다른 애그리거트를 직접 수정하지말고 응용 서비스에서 두 애그리거트를 수정하도록 구현한다.
* 도메인 이벤트를 사용하면 한 트랙잭션에서 한 개의 애그리거트를 수정하면서도 동기나 비동기로 다른 애그리거트의 상태를 변경하는 코드를 작성할 수 있다.
* 한 트랙잭션에서 한 개의 애그리거트를 변경하는 것을 권장하지만 예외의 경우엔 두개 이상의 애그리거트를 변경하는 것을 고려할 수 있다.
    * 팀 표준: 팀이나 조직의 표준에 따라 사용자 유스케이스와 관련된 응용 서비스의 기능을 한 트랜잭션으로 실행해야 하는 경우가 있다.
    * 기술 제약: 기술적으로 이벤트 방식을 도입할 수 없는 경우 한 트랜잭션에서 다수의 애그리거트를 수정해서 일관성을 처리해야 한다.
    * UI 구현의 편리: 운영자의 편리함을 위해 주문 목록 화면에서 여러 주문의 상태를 한 번에 변경하고 싶을 것이다. 이 경우 한 트랜잭션에서 여러 주문 애그리거트의 상태를 변경해야 한다.

## 리포지터리와 애그리거트
* 애그리거트는 개념상 완전한 한 개의 도메인 모델을 표현하므로 객체의 영속성을 처리하는 리포지터리는 애그리거트 단위로 존자한다.
* 리포지터리는 보통 두 메서드를 기본으로 제공한다.
    * save: 애그리거트 저장
    * findById: ID로 애그리거트를 구함
* 필요에 따라 다양한 조건으로 애그리거트를 검색하는 메서드나 애그리거트를 삭제하는 메서드를 추가할 수 있다.
* 어떤 기술을 이용해서 리포지터리를 구현하느냐에 따라 애그리거트의 구현도 영향을 받는다.
    * JPA를 사용하면 데이터베이스 관계형 모델에 객체 도메인 모델을 맞춰야 할 때가 있는데 DB 테이블 구조에 맞게 모델을 변경해야하고 이 경우 밸류 타입인 도메인 모델을 @Component가 아닌 @Entity를 이용해야 할 수도 있다.
* 애그리거트는 개념적으로 하나이므로 리포지터리는 애그리거트 전체를 저장소에 영속화해야한다.
    * Order라는 애그리거트와 관련된 테이블이 세 개라면 Order 애그리거트를 저장할 때 애그리거트 루트와 매핑되는 테이블뿐만 아니라 애그리거트에 속한 모든 구성요소에 매핑된 테이블에 데이터를 저장해야 한다.
* 애그리거트를 구하는 리포지터리 메서드는 완전한 애그리거트를 제공해야 한다.
    * 리포지티러가 완전한 애그리거트를 제공하지 않으면 필드나 값이 올바르지 않아 기능을 실행하는 도중에 NPE와 같은 문제가 발생할 수 있다.
* 저장소로 RDBMS, NoSQL 사용하는데 애그리거트를 영속화할 저장소로 무엇을 사용하든지 간에 애그리거트의 상태가 변경되면 모든 변경을 원자적으로 저장소에 반영해야 한다.

## ID를 이용한 애그리거트 참조
* 한 객체가 다른 객체를 참조하는 것처럼 애그리거트도 다른 애그리거트를 참조한다.
* 애그리거트 관리 주체는 애그리거트 루트이므로 애그리거트에서 다른 애그리거트를 참조한다는 것은 다른 애그리거트의 루트를 참조한다는 것과 같다.
<br/><img src="./그림 3.6.png">
* 필드를 이용한 다른 애그리거트 참조는 개발자에게 구현의 편리함을 제공한다.
    * JPA에선 @ManyToOne, @OneToOne과 같은 애너테이션을 이용해서 연관된 객체를 로딩하는 기능을 제공해 쉽게 참조가 가능하다.
```
order.getOrderer().getMember().getId()
```
* 필드를 이용한 애그리거트 참조는 편한 탬색 오용, 성능에 대한 고민, 확장 어려움과 같은 문제를 야기할 수 있다.
    * 한 애그리거트가 관리하는 범위는 자기 자신이여야 하는데 애그리거트 내부에서 다른 애그리거트 객체에 직접 접근하면 코드 구현의 편리함 때문에 사용하게되면 애그리거트 간 의존 결합도를 높여서 추후 애그리거트의 변경을 어렵게 만든다.
    * JPA를 사용해서 애그리거트를 직접 참조하면 지연(Lazy), 즉시(Eager) 로딩의 두 가지 방식이 존재하는데 다양한 경우의 수를 고려해서 연관 매핑과 JPQL/Criteria 쿼리의 로딩 전략을 결정해야 한다.
        * 지연(Lazy): 애그리거트의 상태를 변경하는 기능을 실행하는 경우에는 불필요한 객체를 함께 로딩할 필요가 없어서 지연 로딩이 유리하다.
        * 즉시(Eager): 연관된 객체의 데이터를 함께 화면에 보여줘야 하면 즉시 로딩이 조회 성능에 유리하다.
    * 부하를 분산하기 위해서 하위 도메인 별로 시스템을 분리하면 서로다른 DB를 사용하게 될 수도 있는데 그로 인해서 JPA와 같은 단일 기술을 사용할 수 없게 된다. 이런 문제들을 완화하기 위해서 ID를 이용한 다른 애그리거트를 참조하는 방식으로 모델의 복잡도를 낮춰주고 의존을 제거하므로 응집도를 높여준다. 또한 구현 복잡도도 낮아지면 로딩전략을 신경쓸 필요가 없어져서 애그리거트가 필요한 경우 응용 서비스에서 ID로 로딩하면 된다.
        * 다른 애그리거트를 수정하는 문제를 근원적으로 방지할 수 있다.
        * 애그리거트별로 구현 기술을 다르게 사용할 수 있다.

### ID를 이용한 참조와 조회 성능
* 다른 애그리거트를 ID로 참조하면 참조하는데 여러 애그리거트를 읽을 때 조회 속도에 문제가 될 수 있다.
    * ID를 이용한 애그리거트 참조는 지연로딩과 같은 효과를 만들고 N + 1 조회 문제가 발생하게 되고 많은 쿼리를 실행하기 때문에 전체 조회 속도가 느려진다.
    * 이로 인해서 즉시로딩으로 바꾸게 된다면 객체 참조 방식으로 다시 돌아오게 되는데 ID 참조 방식을 사용하면서 N + 1 조회 문제를 발생하지 않도록 조회 전용 쿼리를 사용한다.
    * 데이터 조회를 위한 별도 DAO를 만들고 DAO의 조회 메서드에서 조인을 이용해 한 번의 쿼리롤 필요한 데이터를 로딩한다.
* 아래 쿼리는 JPA를 이용해서 특정 사용자의 주문 내역을 보여주기 위한 코드로 JPQL을 사용해서 Order, Member, Product 애그리거트를 조인을 조회해 한번에 로딩한다.
    * 쿼리가 복잡하거나 SQL 특화 기능을 사용해야한다면 조회 부분만 마이바티스와 같은 기술을 구현할 수 있다.
```
@Repository
public class JpaOrderViewDao implements OrderViewDao {
    @PersistenceContext
    private EntityManager em;
    
    @Override
    public List<OrderView> selectByOrderer(String ordererld) {
        String selectQuery =
            "select new com.myshop.order.application.dto.OrderView(o, m, p) "+
            "from Order o join o.orderLines ol, Member m, Product p " +
            'V/here o.orderer.memberld .id = :ordererld "+
            "and o.orderer.memberld = m.id "+
            "and index(ol) = 0 " +
            "and ol.productld = p.id "+
            "order by o.number.number desc";
        TypedQuery<OrderView> query =
            em.createQuery(selectQuery, OrderView.class);
        query.setParameterCbrdererld", ordererld);
        return query.getResultList();
    }
}
```
* 애그리거트마다 서로 다른 저장소를 사용하면 한 번의 쿼리로 관련 애그리거트를 조회할 수 없다.
    * 조회 성능을 높이기 위해 캐시를 적용하거나 조회 전용 저장소를 따로 구성한다.
        * 코드가 복잡해진다.
        * 시스템 처리량을 높일 수 있다.
        * 한 대의 DB 장비로 대응할 수 없는 수준의 트래픽이 발생하면 캐시나 조회 전용 저장소는 필수로 선택하는 기법

## 애그리거트 간 집합 연관
* 애그리거트 간 1-N과 M-N 연관
    * 1-N 연관
        * 컬렉션을 이용한 연관으로 카테고리와 상품 간의 연관이 대표적이다.
        * 애그리거트 간 1-N 관계는 Set과 같은 컬렉을 이요해서 표현할 수있다.
            ```
            public class Category {
                private Set<Product> products; // 다른 애그리거트에 대한 1-N 연관
            }
            ```
        * 보통 목록 관련은 페이징을 처리하는ㄷ 1-N 연관을 이용해서 구현하면 실제 테이블의 데이터가 많다면 코드를 실행할 때마다 실행속도가 급격히 느려져 성능에 심각한 문제를 일으킨다.
            ```
            public class Category {
                private Set<Product> products;

                public List<Product> getProducts(int page, int size) {
                    List<Product> sortedProducts = sortById(products);
                    return sortedProducts.subList((page - 1) * size, page * size);
                }
            }
            ```
        * 개념적으로 1-N 연관이 있더라도 성능 문제 때문에 애그리거트 간의 1-N 연관을 실제 구현에 반영하지 않는다.
        * 카테고리에 속한 상품을 구할 필요가 없다면 상품 입장에서 자신이 속한 카테고리를 N-1로 연관지어서 구하면 된다. 또한 그 연관을 이용해서 특정 카테고리에 속한 상품 목록을 구하면 된다.
            ```
                public class Product {
                    private Categoryld categoryld;
                }
            ```
            ```
            public class ProductListService {
                public Page<Product> getProductOf Category (Long categoryld, int page) int size) {
                    Category category = categoryRepository.findByld(categoryld);
                    checkcategory(category);
                    List<Product> products = productRepository.findByCategoryldCcategory.getldO, page, size);
                    int totalCount = productRepository.countsByCategoryld(category.getld());
                    return new Page(page, size, totalCount, products);
                }
            }
            ```
    * M-N 연관
        * M-N 연관은 개념적으로 양쪽 애그리거트에 컬렉션으로 연관을 만드는데 상품이 여러 카테고리에 속할 수 있다면 상품과 카테고리는 M-N 연관을 맺는다.
        * M-N 연관도 실제 요구사항을 고려하여 M-N 연관을 구현에 포함시킬지 결졍해야 한다.
        * 보통 특정 카테고리에 속한 상품 목록을 보여줄 때 목록 화면에서 각 상품이 속한 카테고리를 상품 정보에 표시하지 않고 상품 상세 화면에서만 필요 하기 때문에 집합 연관이 필요하지 않다. 즉 상품에서 카테고리로의 집합 연관만 존재하면 되고 상품에서 카테고리로의 단방향 M-N 연관만 구현하면 된다.
        ```
        public class Product {
            private Set<CategoryId> categoryIds;
        }
        ```
        * RDBMS에서 M-N을 구현할려면 조인 테이블을 사용
        * JPA를 이용해서 매핑을 하면 ID 참조를 이용한 M-N 단방향 연관을 구현이 가능한데 밸류 타입에 대한 컬렉션 매핑을 사용할 수 있다.
        ```
        ©Entity
        @Table(name = "product")
        public class Product {
            @EmbeddedId
            private Productld id;

            @ElementCollection
            @CollectionTable(name = "product_category", joinColumns = @JoinColumn(name = "product_id"))
            private Set<CategoryId> categorylds;
        }
        ```
        * JPQL의 member of 연산자를 이용해서 특정 카테고리에 속한 상품을 구할 수 있다.
        ```
        ©Repository
        public class JpaProductRepository implements ProductRepository {
            @PersistenceContext
            private EntityManager entityManager;
            
            @Override
            public List<Product> findByCategoryld(Categoryld catld, int page, int size) {
                TypedQuery<Product> query = entityManager.createQuery(
                    "select p from Product p "+
                    "where :catld member of p.categorylds order by p.id.id desc", Product.class);
                query.setParameter("catId", catld);
                query.setFirstResult((page - 1) * size);
                query.setMaxResults(size);
                return query.getResultList0；
            }
        }
        ```

## 애그리거트를 팩토리로 사용하기
* 온라인 쇼핑몰에서 고객이 여러 차례 신고를 해서 특정 상점이 더 이상 물건을 등록하지 못하도록 차단한 상태라고 가정 해보는 경우 코드가 나빠 보이지는 않지만 중요한 도메인 로직 처리가 응용 서비스에 노출된다.
```
public class RegisterProductService {
  public ProductId registerNewProduct(NewProductRequest req) {
    Store store = storeRepository.findById(req.getStoreId());
    checkNull(store);
    if (!store.isBlocked()) {
      throw new StoreBlockedException();
    }
    ProductId id = productRepository.nextId();
    Product product = new Product(id, store.getId(), ...);
    productRepository.save(product);
    return id;
  }
}
```
* Store 가 Product 를 생성할 수 있는지 여부를 판단하고 Product 를 생성하는 것은 논리적으로 하나의 도메인 기능인데 이 도메인 기능을 응용 서비스에서 구현되고 있다.
* 별도 도메인 서비스나 팩토리 클래스를 만들 수도 있지만 이 기능을 Store 애그리거트에 옮겨보는 경우 아래 코드와 같이 구현된다.
* Store 애그리거트의 createProduct() 는 Product 애그리거트를 생성하는 팩토리 역할을 하면서 도메인 로직을 구현하고 있다.
```
public class Store {
  public Product createProduct(ProductId newProductId, ...) {
    if (isBlocked()) {
      throw new StoreBlockedException();
    }
    return new Product(newProductId, getId(), ...);
  }
}
```
* 더 이상 응용서비스에서 Store 의 상태를 확인하지 않게되고 이제 Product 생성 가능 여부를 확인하는 도메인 로직을 변경해도 응용서비스가 영향을 받지 않는다.
* 도메인 응집도가 높아지는데 애그리거트를 팩토리로 사용할 때 얻을수 있는 장점이다.
* 애그리거트가 갖고 있는 데이터를 이용해서 다른 애그리거트를 생성해야 한다면 애그리거트에 팩토리 메서드를 구현하는 것을 고려해본다.
* Store 애그리거트라 Product 애그리거트를 생성할 때 많은 정보를 알아야 한다면 직접 생성하지 않고 다른 팩토리에 위임하는 방법도 존재한다.
```
public class Store {
  public Product createProduct(ProductId newProductld, Productinfo pi) {
    if (isBlocked()) {
      throw new StoreBlockedException();
    }
    return new Product(newProductId, getId(), ...);
  }
}
```
* 다른 팩토리에 위임하더라도 차단 상태의 상점은 상품을 만들 수 없다는 도메인 로직은 한곳에 계속 위치한다는 점이다.