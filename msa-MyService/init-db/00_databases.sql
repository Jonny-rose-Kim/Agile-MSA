-- 서비스별 독립 데이터베이스 생성 및 권한 부여
-- (MariaDB 컨테이너 최초 기동 시 1회 실행. 볼륨이 이미 있으면 수동 실행 필요:
--  docker exec lecturedb mariadb -uroot -pSqlDba-1 < init-db/00_databases.sql )
--
-- 기본 lecture_db 외에 각 담당자가 자기 서비스용 스키마를 분리해서 쓴다.

CREATE DATABASE IF NOT EXISTS material_db CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON material_db.* TO 'manager'@'%';

CREATE DATABASE IF NOT EXISTS order_db CHARACTER SET utf8mb4;
GRANT ALL PRIVILEGES ON order_db.* TO 'manager'@'%';

FLUSH PRIVILEGES;
