import time

import pymysql
from elasticsearch import Elasticsearch
from elasticsearch import helpers

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

start_time = time.time()

es = Elasticsearch("localhost:9200")

cs1 = connection.cursor()
sql = 'select mid,pic,showname,score from movie'
count = cs1.execute(sql)
print("movie表查询到%d条数据" % count)
results = cs1.fetchall()

movies = []

for index, result in enumerate(results):
    print("当前为第{}个，共有{}".format(index, len(results)))
    try:
        # 数据准备
        mid = result['mid']
        pic = result['pic']
        showname = result['showname'][2:-2]
        score = result['score']
        sql = 'select name from actor where id in(select aid from actors_union where mid=%s)' % (mid)
        cs1.execute(sql)
        actors = cs1.fetchall()
        actor = "/".join([actor['name'] for actor in actors])
        # 插入elasticsearch
        movie = {
            "_index": "movies",
            "_type": "_doc",
            "_id": mid,
            "_source": {
                'name': showname,
                'actor': actor,
                'score': score,
                'pic': pic
            }
        }
        movies.append(movie)
        if len(movies)>=1000:
            helpers.bulk(es, movies)
            movies.clear()
    except Exception as e:
        print(e)

helpers.bulk(es, movies)

end_time = time.time()
t = end_time - start_time
print('用时{}s'.format(t))