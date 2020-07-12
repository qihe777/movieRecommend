import pymysql

genres = {"M": "男", "F": "女"}
ages = {1: "Under18", 18: "18-24", 25: "25-34", 35: "35-44", 45: "45-49", 50: "50-55", 56: "56+"}
occupys = {0: "other", 1: "academic/educator", 2: "artist", 3: "clerical/admin", 4: "college/grad student",
          5: "customer service", 6: "doctor/health care", 7: "executive/managerial", 8: "farmer", 9: "homemaker",
          10: "K-12 student", 11: "lawyer", 12: "programmer", 13: "retired", 14: "sales/marketing", 15: "scientist",
          16: "self-employed", 17: "technician/engineer", 18: "tradesman/craftsman", 19: "unemployed", 20: "writer"}

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

cs1 = connection.cursor()
sql = 'select * from data_user'
count = cs1.execute(sql)
print("user表查询到%d条数据" % count)
results = cs1.fetchall()
for index, result in enumerate(results):
    print("当前为第{}个，共有{}".format(index, len(results)))
    try:
        uid = result['uid']
        gender = genres[result['gender']]
        age = ages[result['age']]
        occupation = occupys[result['occupation']]
        sql = 'INSERT INTO data_users(uid,gender,age,occupation) VALUE (%s,"%s","%s","%s")' % (uid,gender,age,occupation)
        connection.cursor().execute(sql)
        connection.commit()
    except Exception as e:
        print(e)
