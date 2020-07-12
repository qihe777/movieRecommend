import pymysql

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

cs1 = connection.cursor()
sql = 'select mid from movie_union'
count = cs1.execute(sql)
print("union表查询到%d条数据" % count)
results = cs1.fetchall()
print("union示例数据：{}".format(results[0]))
unionids = [result['mid'] for result in results]
print("movie_union中共有{}条数据".format(len(results)))

cs2 = connection.cursor()
sql = 'select id from movie_index'
count = cs2.execute(sql)
print("index查询到%d条数据" % count)
results = cs2.fetchall()
print("index示例数据：{}".format(results[0]))
indexids = [result['id'] for result in results]
print("movie_index中共有{}条数据".format(len(indexids)))

ids = [id for id in unionids if not id in indexids]
print("补充和数据共有".format(len(ids)))
print("插入的示例数据：{}".format(ids[0]))

for index, id in enumerate(ids):
    print("当前为第{}个，共有{}".format(index, len(ids)))
    try:
        sql = 'INSERT INTO movie_index VALUE (%s)' % (id)
        connection.cursor().execute(sql)
        connection.commit()
    except Exception as e:
        print(e)
