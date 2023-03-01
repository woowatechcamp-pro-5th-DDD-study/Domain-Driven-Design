# Chapter 5 - 스프링 데이터 JPA 를 이용한 조회 기능

## 5.1 시작에 앞서

* CQRS : 명령<sup>Command</sup> 모델과 조회<sup>Query</sup> 모델을 분리하는 패턴
  * 명령 모델 : 상태를 변경하는 기능을 구현할 때 사용  
  * 조회 모델 : 데이터를 조회하는 기능을 구현할 때 사용  

도메인 모델(엔티티, 애그리거트, 리포지터리)은 명령 모델로 주로 사용된다.  
정렬, 검색 조건 지정과 같은 기능은 조회 기능에 사용된다.


## 5.2 검색을 위한 스펙

## 5.3 스프링 데이터 JPA 를 이용한 스펙 구현

## 5.4 리포지터리/DAO 에서 스펙 사용하기

## 5.5 스펙 조합

## 5.6 정렬 지정하기

## 5.7 페이징 처리하기

## 5.8 스펙 조합을 위한 스펙 빌더 클래스

## 5.9 동적 인스턴스 생성

## 5.10 하이버네이트 @Subselect 사용