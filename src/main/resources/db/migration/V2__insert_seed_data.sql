-- Seed 데이터: 샘플 10개 (실제 100~200개는 별도 작업)

INSERT INTO book_tb (b_isbn, b_title, b_author, b_coverurl, b_affiliateurl) VALUES
('9788937460883', '데미안', '헤르만 헤세', 'https://placeholder.com/demian.jpg', 'https://example.com/affiliate/demian'),
('9788937461033', '어린 왕자', '앙투안 드 생텍쥐페리', 'https://placeholder.com/petit-prince.jpg', 'https://example.com/affiliate/petit-prince'),
('9788932917245', '노인과 바다', '어니스트 헤밍웨이', 'https://placeholder.com/old-man-sea.jpg', 'https://example.com/affiliate/old-man-sea'),
('9788954699952', '아몬드', '손원평', 'https://placeholder.com/almond.jpg', 'https://example.com/affiliate/almond'),
('9788936434120', '채식주의자', '한강', 'https://placeholder.com/vegetarian.jpg', 'https://example.com/affiliate/vegetarian');

INSERT INTO snippet_tb (s_book, s_text, s_tag) VALUES
(1, '새는 알에서 나오려고 투쟁한다. 알은 세계이다. 태어나려는 자는 하나의 세계를 깨뜨려야 한다.', '성장'),
(1, '나는 내 안에서 솟아나오려는 것, 그것을 살아보려 했을 뿐이다. 왜 그것이 그토록 어려웠을까.', '성장'),
(2, '사막이 아름다운 것은 어딘가에 우물을 숨기고 있기 때문이야.', '감성'),
(2, '네 장미를 그토록 소중하게 만든 건 네가 장미에게 쏟은 시간이야.', '관계'),
(3, '인간은 패배하도록 만들어지지 않았어. 인간은 파괴될 수는 있어도 패배할 수는 없어.', '의지'),
(3, '지금은 생각할 때가 아니야. 지금은 낚시할 때야.', '집중'),
(4, '감정을 느끼지 못하는 나에게 세상은 늘 설명이 필요한 곳이었다.', '감성'),
(4, '아몬드 모양의 편도체가 남들보다 작다는 것. 그게 나와 세상 사이의 거리였다.', '성장'),
(5, '아내의 채식이 시작된 건 꿈 때문이었다.', '문학'),
(5, '나는 믿어요. 인간은 모두 근본적으로 식물이라는 것을.', '철학');
