package com.example.enrollment.common.response;

import java.util.List;

/**
 * 커서 기반 페이지네이션 응답.
 *
 * offset 방식(LIMIT ... OFFSET ...) 대신 커서 방식을 쓴 이유:
 * offset은 페이지가 뒤로 갈수록 건너뛸 행이 많아져 느려지고,
 * 조회 도중 데이터가 추가/삭제되면 항목이 밀리거나 중복될 수 있다.
 * 커서(마지막으로 본 id 기준)는 이 문제가 없고 성능이 일정하다.
 *
 * @param content    조회된 항목 목록
 * @param nextCursor 다음 페이지 요청 시 사용할 커서(마지막 항목의 id). 없으면 null
 * @param hasNext    다음 페이지 존재 여부
 */
public record CursorPage<T>(
	List<T> content,
	Long nextCursor,
	boolean hasNext
) {
	/**
	 * 조회 결과로부터 페이지를 생성한다.
	 *
	 * 핵심 트릭: 서비스에서 (size + 1)개를 조회해 넘긴다.
	 * 실제로 size보다 1개 더 받아왔다면 "다음 페이지가 있다"는 뜻이므로,
	 * 마지막 1개를 잘라내고 hasNext=true로 만든다.
	 *
	 * @param items          실제 조회된 항목들 (size+1개를 시도해서 받은 결과)
	 * @param size           클라이언트가 요청한 페이지 크기
	 * @param cursorExtractor 항목에서 커서값(id)을 꺼내는 함수
	 */
	public static <T> CursorPage<T> of(
		List<T> items,
		int size,
		java.util.function.Function<T,Long> cursorExtractor
	){
		boolean hasNext=items.size()>size;
		List<T> content = hasNext ? items.subList(0,size):items;

		Long nextCursor = content.isEmpty() ? null : cursorExtractor.apply(content.get(content.size()-1));

		return new CursorPage<>(content, nextCursor, hasNext);
	}
}
