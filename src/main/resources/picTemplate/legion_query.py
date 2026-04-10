import math

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


def insert_data(data_to_insert: list):
    legion_sql = text("""
            update character_record
            set legion            = :legion,
                legion_raid_power = :legion_raid_power,
                legion_rank       = :legion_rank
                update_time       = now()  
            where character_name = :character_name and world_id = :world_id
        """)

    for item in data_to_insert:
        with engine.begin() as conn:
            conn.execute(legion_sql, item)



@sleep_and_retry
@limits(calls=1, period=1)
def do_query(url, page_index):
    res = requests.get(url, timeout=30)
    res_json = json.loads(res.text)

    raw_data = res_json["ranks"]
    total_count = res_json["totalCount"]
    quota = int(total_count * 0.25)
    quota = quota if quota > 10_000 else 10_000
    stop_flag = page_index > quota

    data_to_insert = []
    for item in raw_data:
        data_to_insert.append({
            "character_name": item["characterName"],
            "legion": item["legionLevel"],
            "legion_raid_power": item["raidPower"],
            "legion_rank": item["rank"],
            "world_id": item["worldID"]
        })
    try:
        insert_data(data_to_insert)
        logger.info("保存[%s]联盟数据", ",".join([item["character_name"] for item in data_to_insert]))
    except Exception as ex:
        logger.exception("写入数据库异常,%s", ex)
    return stop_flag


def execute_query(base_url):
    page_index = 1

    while page_index < 100_000:
        try:
            stop_flag = do_query(base_url.format(page_index), page_index)
            if stop_flag:
                break
            page_index += 10
        except RateLimitException as re:
            raise
        except Exception as ex:
            logger.error("查询出错", ex)

    return 0


if __name__ == "__main__":
    worlds = [('na', 45),
              ('na', 70),
              ('na', 1),
              ('na', 19),
              ('eu', 30),
              ('eu', 46)]

    for region, world_id in worlds:
        base_url = "https://www.nexon.com/api/maplestory/no-auth/ranking/v2/" \
                   + region + \
                   "?type=legion&id=" \
                   + str(world_id) \
                   + "&reboot_index=0&page_index={}"
        execute_query(base_url)
