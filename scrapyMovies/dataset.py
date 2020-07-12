import pymysql

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

# user表格式:
# UserID::Gender::Age::Occupation::Zip-code(美国邮编)
# 1::F::1::10::48067
datanum = 0
successnum = 0
# with open("/root/data/ml-1m/users.dat", 'r', encoding="ISO-8859-1") as f:
#     for line in f.readlines():
#         datanum += 1
#         # 直接添加到数据库
#         user = line.split("::")
#         try:
#             cs1 = connection.cursor()
#             sql = 'insert into data_user value(%s,"%s",%s,%s,%s)' % (user[0], user[1], user[2], user[3], user[4])
#             connection.cursor().execute(sql)
#             connection.commit()
#             successnum += 1
#         except Exception as e:
#             print(e)
# print("user表添加完成，共有{}条数据,添加到数据库{}条".format(datanum, successnum))

# 从数据库中加载id和豆瓣id的对应关系
linkdict = {}
ids = []
try:
    cs1 = connection.cursor()
    sql = 'select id,mid from movie_union'
    count = cs1.execute(sql)
    print("查询到%d条数据" % count)
    results = cs1.fetchall()
    ids = [str(result['id']) for result in results]
    mids = [result['mid'] for result in results]
    linkdict = dict(zip(ids, mids))
except Exception as e:
    print(e)
print("读取数据库完成，{}个id，{}个linkdict".format(len(ids), len(linkdict)))
# 读取评分文件，删掉不存在的id的评分，并将存在的id转化为豆瓣id，插入到数据库

# rating表格式：
# UserID::MovieID::Rating::Timestamp
# 1::1193::5::978300760
uncontain = 0
datanum = 0
successnum = 0
with open("/root/data/ml-1m/ratings.dat", 'r', encoding="ISO-8859-1") as f:
    for line in f.readlines():
        datanum += 1
        rate = line.split("::")
        id = rate[1]
        if id in ids:
            print("正在处理{}".format(id))
            try:
                mid = linkdict[id]
                cs1 = connection.cursor()
                sql = 'insert into data_rating value(null,%s,%s,%s,%s)' % (rate[0], mid, rate[2], rate[3])
                connection.cursor().execute(sql)
                connection.commit()
                successnum += 1
            except Exception as e:
                print(e)
        else:
            uncontain += 1

print("添加完成，共有{}条数据,添加到数据库{}条,不包含{}条".format(datanum, successnum, uncontain))
