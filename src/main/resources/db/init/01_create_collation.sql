-- 한글 정렬을 위한 ICU Collation 정의
--
-- - OS locale(ko_KR.UTF-8)에 의존하지 않음
-- - Docker / CI / 로컬 환경에서 동일 동작
--
-- deterministic = false:
-- - 대소문자 / 악센트 차이를 무시한 문자열 정렬 허용

CREATE COLLATION IF NOT EXISTS ko_icu (
    provider = icu,
    locale = 'ko-KR',
    deterministic = false
    );