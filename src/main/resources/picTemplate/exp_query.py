import sys

import requests
import json

from ratelimit import sleep_and_retry, limits, RateLimitException
from sqlalchemy import create_engine, text
import logging
from datetime import datetime

import yaml

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

with open("py_config.yml", "r", encoding="utf-8") as f:
    config = yaml.safe_load(f)

mysql_config = config["mysql"]
log_config = config["logs"]

if not logger.handlers:
    formatter = logging.Formatter(
        "%(asctime)s [%(levelname)s] [%(threadName)s] %(message)s"
    )

    today = datetime.now().strftime("%Y-%m-%d")
    log_file = f"{log_config['folder']}/{log_config['base_name']}_{today}.log"
    fileHandler = logging.FileHandler(log_file, encoding="utf-8")
    fileHandler.setFormatter(formatter)
    logger.addHandler(fileHandler)

    streamHandler = logging.StreamHandler()
    streamHandler.setFormatter(formatter)
    logger.addHandler(streamHandler)


DATABASE_URL = (
    f"mysql+pymysql://{mysql_config['username']}:{mysql_config['password']}@{mysql_config['ip']}:{mysql_config['port']}/{mysql_config['database']}"
    "?charset=utf8mb4"
)

engine = create_engine(
    DATABASE_URL,
    pool_size=10,  # 连接池大小（常用 5~20）
    max_overflow=20,  # 超出 pool_size 允许的临时连接
    pool_timeout=30,  # 获取连接超时
    pool_recycle=1800,  # 防止 MySQL wait_timeout 断开
    pool_pre_ping=True  # 自动检测断线并重连（非常重要）
)


def get_lv_exp():
    sql = text("select lv,exp_to_next_level from lv_exp")
    lv_map = {}
    with engine.begin() as conn:
        result = conn.execute(sql)
        for item in result:
            lv_map[item[0]] = item[1]
    return lv_map


def insert_data(data_to_insert: list):
    record_sql = text("""
            INSERT INTO character_record (
                character_name, region, world_id, level, level_percent, character_img_url,
                job_name, update_time
            )
            VALUES (:character_name, :region, :world_id, :level, :level_percent,
                :character_img_url, :job_name, NOW()
            )
            ON DUPLICATE KEY UPDATE
                id = LAST_INSERT_ID(id),
                world_id = values(world_id),
                level = VALUES(level),
                level_percent = VALUES(level_percent),
                character_img_url = values(character_img_url),
                job_name = values(job_name),
                update_time = NOW()
        """)
    exp_sql = text("""
            insert into character_exp_daily
                (character_id, record_date, current_exp, level, level_percent)
                value
                (:character_id, curdate(), :current_exp, :level, :level_percent)
            on duplicate key update current_exp = values(current_exp),
                                    level = values(level),
                                    level_percent = values(level_percent),
                                    create_time = now()
        """)

    for item in data_to_insert:
        with engine.begin() as conn:
            result = conn.execute(record_sql, item)
            character_id = result.lastrowid
            item["character_id"] = character_id
            conn.execute(exp_sql, item)
            logger.info("保存[%s]数据", item["character_name"])

@sleep_and_retry
@limits(calls=1, period=1)
def do_query(url, lv_map, region):
    res = requests.get(url, timeout=30)
    res_json = json.loads(res.text)

    raw_data = res_json["ranks"]

    data_to_insert = []
    stop_flag = False
    for item in raw_data:
        lv = item["level"]
        stop_flag = lv < 270
        if stop_flag:
            break
        if lv < 300:
            exp_need = int(lv_map[lv])
            current_exp = int(item["exp"])
            level_percent = (current_exp / exp_need) * 100
        else:
            level_percent = 0
        data_to_insert.append({
            "character_name": item["characterName"],
            "current_exp": item["exp"],
            "region": region,
            "level": lv,
            "level_percent": level_percent,
            "world_id": item["worldID"],
            "job_name": item["jobName"],
            "character_img_url": item["characterImgURL"]
        })
    try:
        insert_data(data_to_insert)
    except Exception as ex:
        logger.exception("写入数据库异常,%s", ex)
    return stop_flag


def execute_query(base_url, offset, page_size, region):
    lv_map = get_lv_exp()
    page_index = 1
    while page_index <= page_size:
        try:
            stop_flag = do_query(base_url.format(offset + page_index), lv_map, region)
            if stop_flag:
                break
            page_index += 10
        except RateLimitException as re:
            raise
        except Exception as ex:
            logger.error("查询出错", ex)
    return 0


if __name__ == "__main__":
    args = sys.argv
    region = args[1]
    if region not in ['na', 'eu']:
        print("region [{}] is not supported".format(region))

    offset = int(args[2])
    page_size = int(args[3])
    base_url = "https://www.nexon.com/api/maplestory/no-auth/ranking/v2/" + region \
               + "?type=overall&id=legendary&reboot_index=0&page_index={}"

    execute_query(base_url, offset, page_size, region)

