import json
import time
import pymysql
import random
import requests

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36'}

urls = ["https://douban.uieee.com/v2/movie/subject/{}"
    , "https://douban-api.uieee.com/v2/movie/subject/{}"]


def change(str):
    str = str.replace("'", "\\'")
    str = str.replace('"', '\\"')
    return str


csr = connection.cursor()
# 查询全部的电影
sql = 'select mid from movie where mid not in(select mid from actors_union group by mid)'
count = csr.execute(sql)
ids = csr.fetchall()
ids = [id['mid'] for id in ids]
# ids = [1291545]

for i, id in enumerate(ids):
    print("正在处理第{}个,id为{}，共{}".format(i, id, len(ids)))
    try:
        # 从actor_union处获取
        sql = 'select role from actor_union where mid={} order by id'.format(id)
        count = csr.execute(sql)
        roles = csr.fetchall()
        roles = [change(role['role']) for role in roles]
        # 从网站爬取数据：
        url = random.choice(urls).format(id)
        res = requests.get(url=url, headers=headers)
        page = json.loads(res.text)
        # 如果roles长度为0,直接插入
        if len(roles) <= 0:
            for director in page['directors']:
                if director['id']:
                    sql = "insert into actors_union value (null,{},'{}',{})".format(director['id'], "导演", id)
                    csr.execute(sql)
                    connection.commit()
            for cast in page['casts']:
                if cast['id']:
                    sql = "insert into actors_union value (null,{},'{}',{})".format(cast['id'], "演员", id)
                    csr.execute(sql)
                    connection.commit()
        # 否则进行遍历插入
        else:
            index = 0
            for director in page['directors']:
                if roles[index] != "导演":
                    break
                if director['id'] and index < len(roles):
                    sql = "insert into actors_union value (null,{},'{}',{})".format(director['id'], roles[index],                                                                                    id)
                    index += 1
                    csr.execute(sql)
                    connection.commit()
            for cast in page['casts']:
                if cast['id'] and index < len(roles):
                    sql = "insert into actors_union value (null,{},'{}',{})".format(cast['id'], roles[index], id)
                    index += 1
                    csr.execute(sql)
                    connection.commit()
    except Exception as e:
        print("error{}".format(e))
