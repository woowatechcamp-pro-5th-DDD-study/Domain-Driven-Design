# Chapter 6. 응용 서비스와 표현 영역

---

<!-- TOC -->
* [Chapter 6. 응용 서비스와 표현 영역](#chapter-6-응용-서비스와-표현-영역)
  * [6.1 표현 영역과 응용 영역](#61-표현-영역과-응용-영역)
  * [6.2 응용 서비스의 역할](#62-응용-서비스의-역할)
    * [6.2.1 도메인 로직 넣지 않기](#621-도메인-로직-넣지-않기)
  * [6.3 응용 서비스의 구현](#63-응용-서비스의-구현)
    * [6.3.1 응용 서비스의 크기](#631-응용-서비스의-크기)
    * [6.3.2 응용 서비스의 인터페이스와 클래스](#632-응용-서비스의-인터페이스와-클래스)
    * [6.3.3 메서드 파라미터와 값 리턴](#633-메서드-파라미터와-값-리턴)
    * [6.3.4 표현 영역에 의존하지 않기](#634-표현-영역에-의존하지-않기)
    * [6.3.5 트랜잭션 처리](#635-트랜잭션-처리)
  * [6.4 표현 영역](#64-표현-영역)
  * [6.5 값 검증](#65-값-검증)
  * [6.6 권한 검사](#66-권한-검사)
  * [6.7 조회 전용 기능과 응용 서비스](#67-조회-전용-기능과-응용-서비스)
  * [생각해보자](#생각해보자)
<!-- TOC -->

---

## 6.1 표현 영역과 응용 영역

도메인 영역은 사용자의 요구를 충족하는 기능을 구현하는 것이라면, 표현 영역과 응용 영역은 사용자와 도메인을 연결해 주는 매개체 역할을 수행한다.

- 표현 영역
  - 사용자의 요청을 해석(Http Request 해석)
  - 응용 서비스가 요구하는 형식으로 사용자 요청을 변환
    ex) 폼에 입력한 요청 파라미터 값을 사용해서 응용 서비스가 요구하는 객체를 생성하여 응용 서비스의 메서드 호출
  - 응용 서비스를 실행한 뒤 사용자에게 알맞은 형식으로 적절한 응답 (JSON, HTML …)
- 응용 영역
  - 사용자가 원하는 기능을 제공
  - 응용영역은 표현영혁이 REST API를 호출하는 지, TCP 소켓을 사용하는 지 알 필요가 없다.
    단지 기능 실행에 필요한 입력 값을 받고 실행 결과만 리턴하면 된다.

---

## 6.2 응용 서비스의 역할

- 응용 서비스는 사용자(Client)가 요청한 기능을 실행한다.
  - 응용 서비스는 사용자의 요청을 처리하기 위해 리포지터리에서 도메인 객체를 가져와 사용한다.
  - 표현 영역에서 봤을땐 **응용 서비스는 도메인과 표현 영역을 연결해 주는 창구 역할** 수행
  - 응용 서비스는 주로 도메인 객체 간의 **흐름을 제어**하기 때문에 다음과 같이 단순한 형태를 갖는다.

```java
public Result doSumeFunc(SomeReq req) {
	// 1. 리포지터리에서 애그리거트를 구한다.
	SomeAgg agg = someAggRepository.findById(req.getId());
	checkNull(agg);

	// 2. 애그리거트의 도메인 기능을 실행한다.
	agg.doFunc(req.getValue());
	
	// 3. 결과를 리턴한다.
	return createSuccessResult(agg);
}
```

- 새로운 애그리거트를 생성하는 응용 서비스의 경우

```java
public Result doSomeCreation(CreateSomeReq req) {
	// 1. 데이터 중복 등 유효성 검사
	validate(req);

	// 2. 애그리거트를 생성
	SomeAgg newAgg = createSome(req);

	// 3. 리포지터리에 애그리거트를 저장한다.
	someAggRepository.save(newAgg);

	// 4. 결과를 리턴한다.
	return createSuccessResult(newAgg);
}
```

- 응용 서비스가 복잡하다면?
  —> **도메인 로직의 일부를 응용 서비스에서 구현하고 있을 가능성이 높음**
  —> 도메인 로직을 응용 서비스에서 구현하면 **코드 중복,** **로직 분산** 등 코드 품질에 안 좋은 영향을 줄 수 있다.
- 응용 서비스는 트랜잭션 처리도 담당한다.
  - 응용 서비스는 도메인의 상태 변경을 트랜잭션으로 처리해야 한다.
  - 응용 서비스의 메소드는 트랜잭션 범위에서 실행되어야 한다.
- **접근 제어**
- **이벤트 처리**

### 6.2.1 도메인 로직 넣지 않기

Ex 1) 암호 변경 기능 : 도메인에 변경하는 암호 변경하는 로직을 구현하고, 응용 서비스는 단순히 이를 호출하는 역할을 수행한다.

```java
public class ChangePasswordService {
	public void changePassword(String memberId, String oldPw, String newPw) {
		Member member = memberRepository.findById(memberId);
		checkMemberExists(member);
		member.changePassword(oldPw, newPw);
	}
	// ...
}
```

Q) 만약 응용 서비스에서 도메인 로직을 구현하면 어떤 문제가 발생하는가?

- 코드의 응집성이 떨어진다. —> 코드 품질에 문제가 발생한다.
- 여러 응용 서비스에서 동일한 도메인 로직을 구현할 가능성이 높아진다.

---

## 6.3 응용 서비스의 구현

- 응용 서비스는 표현 영역과 도메인 영역을 연결하는 매개체 역할을 수행
- 디자인 패턴 중 파사드(facade) 와 같은 역할을 수행.
- 응용 서비스 자체는 복잡한 로직을 수행하지 않지만, 응용 서비스를 구현할 때 고려해야할 몇 가지 사항과 트랜잭션과 같은 구현 기술의 연동에 대해 살펴보자!

### 6.3.1 응용 서비스의 크기

1. 한 응용 서비스 클래스에 회원 도메인의 모든 기능 구현하기(ex : MemberService)
2. 구분되는 기능별로 응용 서비스 클래스를 따로 구현하기(ex : ChangePasswordService)

—> 코드 중복 제거와 코드 품질 저하 사이에서 적절한 trade off 를 해야함

- 코드 중복 제거 : private 메소드로 같은 도메인에 대한 응용 서비스에서 공유하므로, 중복 코드 제거 효과
- 코드 품질 저하 : 분리하는게 좋을 것 같은 코드임에도 억지로 한 클래스에 낑겨넣는 습관..
  - Notifier 같은 멤버가 필요한 필요한 메서드는 극히 일부인데, 억지로 관계가 부족한 멤버를 모든 메소드가 공유하게 되는 경우…

필자의 Best Practice —> 잘게 쪼개기

- 구분되는 기능별로 응용 서비스 클래스를 따로 구현
- 각 응용 서비스에서 공통되는 로직을 별도 클래스로 구현

### 6.3.2 응용 서비스의 인터페이스와 클래스

- 인터페이스가 필요한 경우는 구현 클래스가 여러 개인 경우이다.
- Mockito 같은 테스트 도구를 사용하면 인터페이스가 필요 없다.
- 따라서 필요한 시점이 생기기 전에 인터페이스를 미리 작성하는 것은 좋은 선택이라고 볼 수는 없다!

### 6.3.3 메서드 파라미터와 값 리턴

- 파라미터 vs 파라미터용 객체
- 만약 요청 파라미터가 두 개 이상이면, 데이터 전달을 위한 별도 클래스를 사용하는 것이 편리
- 응용 서비스의 결과를 표현 영역에서 사용해야 한다면, 응용 서비스 메서드의 결과로 필요한 데이터를 리턴함
  - 표현 영역에서 주문 후 주문 상세 내역 페이지로 반환하는 경우, 주문번호를 응용 서비스에서 표현영역으로 반환해 줌

### 6.3.4 표현 영역에 의존하지 않기

- 응용 서비스의 파라미터로 표현 영역과 관련된 타입을 사용하면 안된다.(HttpServletRequest, HttpSession 등등 …)
  - 이런 파라미터를 사용하면, **응용 서비스만 단독으로 테스트하기가 어려워 진다.**
  - 표현 영역의 구현이 변경되면, 응용 서비스의 구현도 함께 변경해야 하는 문제도 발생한다.
  - 응용 서비스가 표현 영역의 역할까지 대신하는 상황이 벌어질 수 있다.
    (HttpServletRequest를 전달받아, HttpSession을 생성하고 request에 인증과 관련된 정보를 담는 경우 …)

### 6.3.5 트랜잭션 처리

- Spring 의 `@Transactional` 적극적 사용 —> 트랜잭션 처리 코드를 간결하게 유지할 수 있다.
  - RuntimeException이 발생하면 롤백, 아니면 commit
  - Annotation를 사용하는 이유 조금 더 과장하면 AOP를 사용하는 이유와도 직결됨(핵심 비즈니스 로직만 남기고, 별도 관심사는 Annotation을 통해 분리할 수 있음)

---

## 6.4 표현 영역

표현영역의 책임

- 사용자가 시스템을 사용할 수 있는 흐름(화면)을 제공하고 제어한다.
- 사용자의 요청을 알맞은 응용 서비스에 전달하고 결과를 사용자에게 제공한다.
- 사용자의 세션을 관리한다. (권한 검사와 연관)

—> MVC 프레임워크는 HTTP 요청 파라미터로부터 자바 객체를 생성하는 기능을 지원하므로, 응용 서비스에 전달할 자바 객체를 보다 손쉽게 생성할 수 있다.(Request 매핑(Jackson 라이브러리 기능)

---

## 6.5 값 검증

값 검증은 표현 영역과 응용 서비스 두 곳에서 모두 수행할 수 있다.

다만, **원칙적으로는 모든 값에 대한 검증은 응용 서비스에서 처리**한다.

그런데 표현 영역에서는 잘못된 값이 들어오면, 이를 사용자에게 알려주고 값을 다시 입력받아야 한다.

Spring MVC는 폼에 입력한 값이 잘못된 경우 에러 메시지를 보여주기 위한 용도로 Errors 나 BindingResult 를 사용하는데, 컨트롤러에서 위오 같은 응용 서비스를 사용하면, 폼에 에러 메시지를 보여주기 위해 다음과 같이 다소 번잡한 코드를 작성해야 한다.

```java
@Controller
public class Controller {
	@PostMapping("/member/join")
	public String join(JoinRequest joinRequest, Errors errors) {
		try {
			joinService.join(joinRequest);
			return successView;
		} catch(EmptyPropertyException ex) {
			errors.rejectValue(ex.getPropertyName(), "empty");
			return formView;
		} catch(InvalidPropertyException ex) {
			errors.rejectValue(ex.getPropertyName(), "invalid");
			return formView;
		} catch(DuplicateIdException ex) {
			errors.rejectValue(ex.getPropertyName(), "duplicate");
			return formView;
		}
}
```

- 응용 서비스에서 각 값이 유효한지 확인할 목적으로 익셉션을 사용할 때의 문제점은 사용자에게 좋지 않은 경험을 제공하게 된다.
- 사용자는 입력한 값 중 어떤 값이 유효한건지 한번에 알고싶어 한다. —> 한번에 제대로 입력할 수 있길 기대한다.(~~Front의 역할이 중요한 것 아닐까?..~~)
- 이런 불편을 해소하기 위해 에러 코드를 모아 하나의 Exception으로 발생시키는 방법도 있다.
  - ValidationError 를 생성해서, errors 목록에 추가
  - 값 검증이 끝난 뒤에 errors에 값이 존재하면, errors 목록을 갖는 ValidationErrorException을 발생시킨다.
- **표현 영역에서 필수 값을 검증할 수도 있다.**
- **스프링에선 값 검증을 위한 Validator 인터페이스를 별도로 제공**하므로 검증기를 따로 구현하면 간결하게 코드를 작성할 수 있다.

- 표현 영역 : 필수 값, 값의 형식, 범위 등을 검증
- 응용 서비스 : 데이터의 존재 유무와 같은 논리적 오류를 검증

으로 구현 하는 것이 일반적이지만, 필자는 응용 서비스에서 필요한 값 검증 + 논리적인 검증을 모두 하는편
—> 프레임워크에서 제공하는 검증 기능을 사용할 때 보다 작성할 코드는 늘어남
—> But, 응용 서비스의 완성도가 높아지는 이점이 있다.

---

## 6.6 권한 검사

‘사용자 U 가 기능 F를 실행할 수 있는지?‘ == 권한 검사

- 시스템마다 권한의 복잡도가 다르다
  - 인증 여부 검사
  - 권한에 따라 사용할 수 있는 기능이 달라지는 경우
- Spring Security 같은 프레임워크는 유연하고 확장 가능한 구조를 갖는다
  - 유연한 만큼 목잡하다
  - **따라서, 시스템 규모에 따라 직접 권한 검사 기능을 직접 구현하는 것이 유지보수에 유리할 수도 있다.**
- 보안 프레임워크의 복잡도를 떠나 보통 다음 세 곳에서 권한 검사를 수행할 수 있다
  - 표현 영역
  - 응용 서비스
  - 도메인

표현영역에서의 권한 검사

- 인증된 사용자인지 아닌지 검사
  - URL을 처리하기 전에 (Filter / Interceptor) 인증 여부를 검사해서 **인증된 사용자만 컨트롤러에 전달**
  - 인증된 사용자가 아닐 경우 로그인 화면으로 리다이렉트 시킨다.
  - Servlet Filter 가 적절(Spring Security 도 필터를 활용)

응용 서비스 영역에서의 권한 검사

- URL 만으로 접근 제어를 할 수 없는 경우, 메서드 단위로 권한 검사 수행
  - 모든 서비스 영역 메서드에서 할 필요는 없다
  - Spring Security 는 AOP를 이용해서 `@PreAuthorize("hasRole('ADMIN')")` 어노테이션으로 권한 검사 할 수 있다.

도메인 영역에서의 권한 검사

- 예를 들면, 게시글 삭제는 본인 또는 관리자 역할을 가진 사용자만 가능하다면?
  - 본인의 게시글 애그리거트를 먼저 로딩
  - 이후 도메인에서 권한 검사 구현(**도메인 서비스(permissionService)**를 구현해서 활용)

## 6.7 조회 전용 기능과 응용 서비스

## 생각해보자