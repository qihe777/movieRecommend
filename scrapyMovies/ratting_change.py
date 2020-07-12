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
sql = 'select * from data_rating'
count = cs1.execute(sql)
print("user表查询到%d条数据" % count)
results = cs1.fetchall()
for index, result in enumerate(results):
    print("当前为第{}个，共有{}".format(index, len(results)))
    try:
        uid = result['uid']
        mid = result['mid']
        ratting = result['ratting']
        time = result['timestamp']
        sql = 'INSERT INTO data_ratings(uid,mid,ratting,time) VALUE (%s,%s,%s,from_unixtime(%s))' % (
        uid, mid, ratting, time)
        connection.cursor().execute(sql)
        connection.commit()
    except Exception as e:
        print(e)
