import json
import sys

import yaml
from sqlalchemy import create_engine, text

if __name__ == '__main__':
    # with open("/home/minnan/robot-manage/config/py_config.yml", "r", encoding="utf-8") as f:
    #     config = yaml.safe_load(f)
    with open(r"F:\Minnan\robot-manage\src\main\resources\picTemplate\py_config.yml", "r", encoding="utf-8") as f:
        config = yaml.safe_load(f)

    mysql_config = config["mysql"]

    args = sys.argv
    character_name = str(args[1]).lower()
    if len(args) > 2:
        region = str(args[2])
    else:
        region = 'na'

    DATABASE_URL = (
        f"mysql+pymysql://{mysql_config['username']}:{mysql_config['password']}@{mysql_config['ip']}:{mysql_config['port']}/{mysql_config['database']}"
        "?charset=utf8mb4"
    )

    engine = create_engine(
        DATABASE_URL,
        pool_size=1,  # 连接池大小（常用 5~20）
        max_overflow=20,  # 超出 pool_size 允许的临时连接
        pool_timeout=30,  # 获取连接超时
        pool_recycle=1800,  # 防止 MySQL wait_timeout 断开
        pool_pre_ping=True  # 自动检测断线并重连（非常重要）
    )

    world_map = {
        45: "Kronos",
        70: "Hyperion",
        1: "Bera",
        19: "Scania",
        30: "Luna",
        46: "Solis"
    }

    character_sql = text("""
        select id, character_name, character_img_url, job_name, world_id, level, level_percent, update_time, query_time
        from character_record where lower(character_name) = :character_name and region = :region limit 1
    """)
    exp_sql = text("""
        select character_id, record_date,current_exp, level, level_percent
        from character_exp_daily
        where character_id = :character_id
        order by record_date desc limit 20
    """)
    rank_sql = text("""
        select r_g.rank_global, r_w.rank_world, r_j.rank_job, r_w_j.rank_world_job  from
        (SELECT COUNT(*) + 1 AS rank_global
        FROM character_record
        WHERE
            region = :region
            AND level > :L
            OR (level = :L AND level_percent > :P)
            OR (level = :L AND level_percent = :P AND id < :cid)) as r_g,
        (SELECT COUNT(*) + 1 AS rank_world
        FROM character_record
        WHERE world_id = :W
          AND (
                level > :L
             OR (level = :L AND level_percent > :P)
             OR (level = :L AND level_percent = :P AND id < :cid)
          )
        ) as r_w,
        (SELECT COUNT(*) + 1 AS rank_job
        FROM character_record
        WHERE :region = :region
          AND job_name = :J
          AND (
                level > :L
             OR (level = :L AND level_percent > :P)
             OR (level = :L AND level_percent = :P AND id < :cid)
          )
        ) as r_j,
        (SELECT COUNT(*) + 1 AS rank_world_job
        FROM character_record
        WHERE world_id = :W
          AND job_name = :J
          AND (
                level > :L
             OR (level = :L AND level_percent > :P)
             OR (level = :L AND level_percent = :P AND id < :cid)
          )) as r_w_j
    """)
    near_rank_sql = text("""
        SELECT id, character_name, level, round(level_percent, 2), rn
        FROM (SELECT id, character_name, level, level_percent,
                ROW_NUMBER() OVER (
                    PARTITION BY world_id, job_name
                    ORDER BY level DESC, level_percent DESC, id
                ) AS rn
            FROM character_record
            WHERE world_id = :world_id AND job_name = :job_name
        ) t
        WHERE rn BETWEEN :rn - 2 AND :rn + 2
        ORDER BY rn;    
    """)

    with engine.begin() as conn:
        record_res = conn.execute(character_sql, {"character_name": character_name, "region": region})
        record_result = record_res.fetchone()
        if record_result is None:
            print("")
            sys.stdout.flush()
        else:
            character_id = record_result[0]
            exp_res = conn.execute(exp_sql, {"character_id": character_id})
            exp_result = exp_res.fetchall()

            world_id = int(record_result[4])
            character = {
                "name": str(record_result[1]),
                "characterImgUrl": str(record_result[2]),
                "job": str(record_result[3]),
                "server": world_map[world_id],
                "worldId": world_id,
                "level": int(record_result[5]),
                "expPercent": float(record_result[6]),
                "updateTime": str(record_result[7]),
                "queryTime": str(record_result[8]),
                "source": "minnan.site"
            }

            exp_list = [{
                "recordDate": str(exp[1]),
                "currentExp": int(exp[2]),
                "level": int(exp[3]),
                "levelPercent": float(exp[4])
            } for exp in exp_result]

            rank_res = conn.execute(rank_sql, {
                "L": character["level"],
                "P": character["expPercent"],
                "cid": int(record_result[0]),
                "W": world_id,
                "J": character["job"],
                "region": region
            })
            rank_data = rank_res.fetchone()
            character["globalLevelRank"] = rank_data[0]
            character["serverLevelRank"] = rank_data[1]
            character["globalClassRank"] = rank_data[2]
            character["serverClassRank"] = rank_data[3]

            near_rank_res = conn.execute(near_rank_sql, {
                "world_id": world_id,
                "job_name": character["job"],
                "rn": character["serverClassRank"]
            })
            near_rank_data = near_rank_res.fetchall()
            near_rank_list = [{
                "name": str(r[1]),
                "level": int(r[2]),
                "expPercent": float(r[3]),
                "serverClassRank": int(r[4])
            } for r in near_rank_data]
            character["nearRank"] = near_rank_list

            result = {
                "character": character,
                "exp": exp_list
            }

            query_sql = text("""
                update character_record set query_time = now() where id = :character_id
            """)
            conn.execute(query_sql, {"character_id": character_id})

            print(json.dumps(result), end="")
            sys.stdout.flush()