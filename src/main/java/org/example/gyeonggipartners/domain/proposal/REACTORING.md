## domaion
### evidence ,  Proposal, ProposalConsent
- 문자열 하드코딩 (근데 반복되는 문자열은 아님)
- 비니지스 규칙 위반에대해 시스템 에러 메시지(IllegalArgumentException) 던진다

### 그외 
- domain/repository , Enum 파일 등
- 별도의 문제되는 부분 없음

## exception
- 문제되는 부분 없음

## application

### ProposalService

**도메인 경계**
- 타 도메인 리포지토리(UserRepository, MemberRepository) 직접 참조. 조회 전용이라 허용하되, 정리 시 QueryService로 분리 고려.

**개선 대상**
- `closeExpiredProposals`: 예외 발생 시 로그만 남기고 재처리 로직 없음
- `consentProposal`: `findByProposalId().size()` 대신 count 쿼리 사용 권장
- `validateEditableStatus`: default 케이스가 UNAUTHORIZED_ACCESS. 새 상태 추가 시 감지 불가, 별도 에러코드 고려
- 클래스 레벨 `@Transactional` + 메서드 레벨 `readOnly = true` 혼용. 동작하나 메서드별 명시가 더 명확