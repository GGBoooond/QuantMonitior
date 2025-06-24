
CREATE TABLE future (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    symbol VARCHAR(50) NOT NULL COMMENT '期货代码',
    name VARCHAR(100) COMMENT '期货名称',
    current_price DOUBLE COMMENT '当前价格',
    high7d DOUBLE COMMENT '7天最高价',
    low7d DOUBLE COMMENT '7天最低价',
    last_updated TIMESTAMP COMMENT '最后更新时间'
);

INSERT INTO future (symbol, name) VALUES
('CL', '原油期货'),
('GC', '黄金期货'),
('SI', '白银期货'),
('HG', '铜期货'),
('NG', '天然气期货'); 