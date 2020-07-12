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

sql = 'select showname from movie limit 5'
cs1.execute(sql)
names = cs1.fetchall()
for name in names:
    print(name['showname'][2:-2])

