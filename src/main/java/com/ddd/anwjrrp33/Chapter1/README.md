## Chapter1 도메인 모델 시작하기

### 1.1 도메인이란?
`구현할 소프트웨어의 대상`으로 하나의 도메인은 다시 하위 도메인으로 나눌 수 있다. 온라인 서점이라는 도메인은 아래의 그림 1.1과 같이 하위 도메인으로 구성되어 있다. 소프트웨어가 도메인의 모든 기능을 제공하지는 않기 때문에 도메인의 구성 여부는 상황에 따라서 달라진다.

<img src="./그림 1.1.png" width="75%" height="75%">

### 1.2 도메인 전문가와 개발자 간 지식 공유
도메인의 영역마다 전문가들이 존재하고 전문가들은 지식과 경험을 바탕으로 기능 개발을 요구하는데 개발자는 요구사항을 분석하고 설계해서 소트프웨어를 개발한다. 개발자가 요구사항을 올바르게 이해하는 것이 중요한데 그렇다면 요구사항 분석을 잘할려면 어떻게 해야할까? 바로 `도메인 전문가와 개발자가 직접 대화`를 하는 것이다. 개발자는 도메인 전문가만큼은 아니지만 도메인에 대한 지식을 갖춰야 한다.

### 1.3 도메인 모델
도메인 모델에는 다양한 정의가 존재하고 기본적으로 특정 도메인을 개념적으로 표현한 것이다. 주문 도메인으로 가정했을 때 아래의 그림 1.3과 같이 객체 모델로 구성할 수 있다. 도메인 모델을 만드는데 있어서 클래스 다이어그램, 상태 다이어그램, 수학 공식을 통해서도 만들 수 있다. 도메인 모델을 사용하면 여러 관계자(개발자, 도메인 전문가, 기획자 등)들이 동일한 모습으로 도메인을 이해하고 도메인 지식의 공유에 도움이 된다.

<img src="./그림 1.3.png" width="75%" height="75%">

### 1.4 도메인 모델 패턴
일반적으로 애플리케이션 아키텍처 구조는 아래의 그림 1.5와 같이 네 개의 영역으로 구성된다.

<img src="./그림 1.5.png" width="75%" height="75%">

`도메인 계층은 도메인의 핵심 규칙을 구현한다.` 도메인의 중요 업무 규칙이나 프로세스는 해당 도메인 계층으로 작성해야한다. 실제로 코드로 구현된다면 도메인 객체 책임과 역할을 부여하는 것과 같은데 핵심 규칙을 구현한 코드는 도메인 모델에만 위치하기 때문에 규칙이 바뀌거나 규칙을 확장해야 할 때 즉 코드가 변경되야 할 때 다른 코드에 영향을 덜 주고 변경할 수 있다.

### 1.5 도메인 모델 도출
개발을 하기에 앞서서 기획서, 유스케이스, 사용자 스토리와 같은 요구사항을 분석하여 도메인을 이해하고 이를 바탕으로 도메인 모델 초안을 만들고 개발을 시작해야 한다. 도메인을 모델링할 때 기본이 되는 작업은 모델을 구성하는 핵심 구성요소, 규칙, 기능을 찾고 모델을 만들어야 하며 이렇게 만든 모델은 도메인 전문가나 다른 개발자와 논의하는 과정에서 공유되기도 하고 화이트 보드나 위키같은 도구를 사용해서 누구나 쉽게 접근할 수 있도록 해야한다.

### 1.6 엔티티와 밸류
도메인 모델은 크게 엔티티와 밸류로 구분할 시 있고 엔티티와 밸류를 제대로 구분해야 올바른 도메인 모델을 설계하고 구현할 수 있다.

* 엔티티
    * 엔티티의 가장 큰 특징은 id와 같은 고유의 식별자를 가진다는 것이며 엔티티를 생성하고 속성을 바꾸고 삭제할 때까지 식별자는 유지한다. 애플리케이션은 보통 DB의 AutoIncrement를 사용하는데 실제로 JPA를 활용해서 한다면 엔티티란 @Entity가 걸려있는 객체를 의미하고 @Id는 고유의 식별자를 의미한다. 즉 `엔티티란 테이블을 의미`한다고 생각할 수 있다.
    * 엔티티의 식별자 생성은 사용하는 기술에 따라서 달라지며 특정 규칙에 따라 생성, UUID와 Nano ID와 같은 고유 식별자 생성기 사용, 값을 직접 입력, 일련번호 사용(시퀀스나 DB의 AutoIncrement와 같은 컬럼)
* 밸류
    * `밸류란 값 객체`를 의미한다. 실제로 엔티티를 객체로 구현하게 된다면 여러개의 데이터를 가지고 있게 된다. 데이터 부분을 따로 객체로 생성하고 사용할 수 도 있다. 아래 코드는 JPA로 작성되어 있는데 Member는 엔티티지만 실제로 UserId, Password, Name, RegNo은 값 객체인 밸류로 생각할 수 있다. 즉 `밸류란 테이블의 컬럼이 원시타입, 레퍼런스 타입과 같은 형태가 아닌 객체 형태로 생성`한 것이다.
    * 밸류 타입은 불변으로 구현해야 하는데 그 이유는 안전한 코드를 작성할 수 있다. 즉 set 메소드를 통해서 값이 변경된다면 잘못된 값이 반영될 수 있고 해당 규칙을 지키게 된다면 참조 투명성과 스레드에 안전한 특징을 가질 수 있다. 엔티티가 가지고 있는 불변 객체인 밸류를 변경하고 싶다면 set 메소드를 통해서 데이터를 변경하는 것이 아닌 객체를 새로 생성해서 엔티티의 밸류를 변경해야한다.
```
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    private UserId userId;

    @Embedded
    private Password password;

    @Embedded
    private Name name;

    @Embedded
    private RegNo regNo;

    protected Member() {
    }

    private Member(UserId userId, Password password, Name name, RegNo regNo) {
        validate(name, regNo);
        this.userId = userId;
        this.password = password;
        this.name = name;
        this.regNo = regNo;
    }

    public static Member of(String userId, String password, String name, String regNo) {
        return new Member(UserId.from(userId), Password.from(password), Name.from(name), RegNo.from(regNo));
    }
}
```

* 엔티티 식별자와 밸류 타입
    * 엔티티의 식별자는 실제 데이터가 String으로 구성된 경우가 많다 신용카드 번호, 고객의 이메일 주소 등으로 생각할 수 있다. 즉 식별자를 밸류 타입으로 사용해서 의미를 잘 드러나도록 구성할 수 있고 코드를 분리하면서 실제 엔티티에 사용된 메소드를 분리해서 가독성을 높일 수 있다.

* 도메인 모델에 Set 메서드 넣지 않기
    * 도메인 모델에는 get/set 메서드를 무심코 사용할 수 있는데 set의 경우 도메인의 핵심 개념이나 의도를 코드에서 사라지게 한다. 즉 set 메소드로 인해서 도메인에 부여된 역할이 서비스 레이어에서 set 메소드를 통해서 코드로 구현될 수 있기 때문에 좋지 않는 방식이다. get 메서드 또한 사용하지 않는다면 코드를 추가하지 않는 것이 좋다.

### 1.7 도메인 용어와 유비쿼터스 언어
도메인에서 사용하는 용어는 매우 중요한데 도메인에서 사용하는 용어를 코드에 반영하지 않으면 그 코드는 개발자에게 코드의 의미를 해석해야하는 부담을 주게 된다. 

예를 들어 주문상태가 존재하는데 주문상태는 결제 대기 중, 상품 준비 중, 출고 완료됨, 배송 중, 배송 완료됨, 주문 취소됨이 존재하는데 실제 코드가 아래와 같이 STEP1, STEP2, STEP3, STEP4, STEP5, STEP6와 같이 구현된다면 STEP 과정을 이해하기 위한 불필요한 해석이 필요하다.
```
public OrderState {
    STEP1, STEP2, STEP3, STEP4, STEP5, STEP6
}
```

도메인 용어를 사용할 때는 전문가, 관계자, 개발자가 도메인과 관련된 공통의 언어를 사용하고 이를 대화, 문서, 도메인 모델, 코드, 테스트등 모든 곳에서 같은 용어를 사용해서 소통 과정에서 발생하는 용어의 모호함을 줄일 수 있고 개발자는 도메인과 코드 사이에 불필요한 해석 과정을 줄일 수 있다. 올바른 도메인 용어를 정하고 부여하는 건 쉽지 않고 시간이 오래 걸리지만 좋은 코드를 만드는데 매우 중요한 요소이기에 도메인 용어를 찾는 시간을 아까워하지 말아야 한다.