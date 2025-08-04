-- 사용자 테이블
CREATE TABLE User (
                      User_id VARCHAR(100) PRIMARY KEY,
                      name VARCHAR(20),
                      nickname VARCHAR(20) UNIQUE,
                      phone VARCHAR(30) UNIQUE,
                      email VARCHAR(50) UNIQUE,
                      address VARCHAR(200),
                      agreed BOOLEAN
);

-- 게시글 테이블
CREATE TABLE Post (
                      Post_num INT PRIMARY KEY AUTO_INCREMENT,
                      User_id VARCHAR(100),
                      tag VARCHAR(20), -- 음식 카테고리 (육류, 채소, 해산물, 과일, 유제품, 곡류, 가공식품)
                      title VARCHAR(100),
                      content VARCHAR(1000),
                      expiration_date DATE,
                      status BOOLEAN, -- 나눔 상태 (true: 진행중, false: 완료)
                      created_at DATE,
                      refridger_food VARCHAR(100), -- 냉장고 안 재료일 시 매핑, 아니면 NULL
                      image_url VARCHAR(500), -- 이미지 URL 저장

                      FOREIGN KEY (User_id) REFERENCES User(User_id),
                      FOREIGN KEY (tag) REFERENCES Tag(tag)
);

-- 태그 테이블 (음식 카테고리)
CREATE TABLE Tag (
                     tag VARCHAR(20) PRIMARY KEY -- 육류, 채소, 해산물, 과일, 유제품, 곡류, 가공식품
);

-- 음식 카테고리 데이터 삽입
INSERT INTO Tag (tag) VALUES ('육류');
INSERT INTO Tag (tag) VALUES ('채소');
INSERT INTO Tag (tag) VALUES ('해산물');
INSERT INTO Tag (tag) VALUES ('과일');
INSERT INTO Tag (tag) VALUES ('유제품');
INSERT INTO Tag (tag) VALUES ('곡류');
INSERT INTO Tag (tag) VALUES ('가공식품');

-- 쪽지 테이블
CREATE TABLE Chat (
                      chat_num INT PRIMARY KEY AUTO_INCREMENT,
                      User_id VARCHAR(100),
                      content VARCHAR(1000),
                      send_time DATE,

                      FOREIGN KEY (User_id) REFERENCES User(User_id)
);

-- 냉장고 테이블
CREATE TABLE Refrigerator (
                              fridge_num INT PRIMARY KEY AUTO_INCREMENT,
                              User_id VARCHAR(100),
                              food_name VARCHAR(100),
                              category VARCHAR(20), -- 유제품, 채소, 육류 등
                              quantity INT,
                              is_opend BOOLEAN,
                              expiration_date_before_open DATE,
                              expiration_date_after_open DATE,
                              registered_at DATE,

                              FOREIGN KEY (User_id) REFERENCES User(User_id)
);

-- 신고 테이블
CREATE TABLE Report (
                        report_num INT PRIMARY KEY AUTO_INCREMENT,
                        User_id VARCHAR(100),
                        reason VARCHAR(1000),
                        created_time DATE,

                        FOREIGN KEY (User_id) REFERENCES User(User_id)
);
