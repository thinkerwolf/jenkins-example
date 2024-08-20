UPDATE account_info
SET create_time='2024-08-10 13:00:00'
WHERE id >= 10
  AND id < 20;

CREATE TABLE `test`
(
    `id`       int NOT NULL AUTO_INCREMENT,
    `list_id`  int DEFAULT NULL,
    `video_id` int DEFAULT NULL,
    `order`    int DEFAULT NULL,
    `time`     int DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(1, 11, 1, 1001);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(1, 12, 2, 1007);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(2, 21, 1, 1003);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(2, 22, 2, 1008);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(3, 31, 1, 1005);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(3, 32, 1, 1006);
INSERT INTO test (list_id, video_id, `order`, `time`) VALUES(4, 41, 1, 999);
