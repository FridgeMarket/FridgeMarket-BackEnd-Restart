CREATE DATABASE IF NOT EXISTS FridgeMarket CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE FridgeMarket;

-- 회원가입 테이블
CREATE TABLE Users (
	ID VARCHAR(40) NOT NULL,
    PW VARCHAR(100) NOT NULL,
    UserName VARCHAR(20) NOT NULL,
    PhoneNum VARCHAR(30) NOT NULL,
    UserNum INT(500) NOT NULL PRIMARY KEY
);

-- 게시판 테이블
CREATE TABLE Board (
	Title VARCHAR(500) NOT NULL,
    Detail VARCHAR(2000) NOT NULL,
    BoardNum INT(500) NOT NULL PRIMARY KEY,
    UserNum INT(500) NOT NULL,
    FOREIGN KEY (UserNum) REFERENCES Users(UserNum)
);

-- 댓글 테이블
CREATE TABLE Comments (
	CommentNum INT(500) NOT NULL PRIMARY KEY,
    CommentContent VARCHAR(500) NOT NULL,
    UserNum INT(500) NOT NULL,
    BoardNum INT(500) NOT NULL,
    FOREIGN KEY (UserNum) REFERENCES Users(UserNum),
    FOREIGN KEY (BoardNum) REFERENCES Board(BoardNum)
);

-- 냉장고 테이블
CREATE TABLE Refrigerator (
	FoodNum INT(500) NOT NULL PRIMARY KEY,
    DateBeforeOpening DATE NOT NULL,
    DateAfterOpening DATE NOT NULL,
    FoodName VARCHAR(200) NOT NULL,
    UserNum INT(500) NOT NULL,
    FOREIGN KEY (UserNum) REFERENCES Users(UserNum)
);
