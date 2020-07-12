import csv
import pymysql
import requests
import json
import codecs

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36Edge/13.10586'}

# 读取文件

# 1::Toy Story (1995)::Animation|Children's|Comedy
# 2::Jumanji (1995)::Adventure|Children's|Fantasy
idList = []
with open("/root/movies.dat", 'r', encoding="ISO-8859-1") as f:
    for line in f.readlines():
        idList.append(line.split("::")[0])

# movieId,imdbId,tmdbId
# 1,0114709,862
# 2,0113497,8844
linkdict = {}
with open('/root/links.csv', 'r', encoding="ISO-8859-1") as f:
    reader = csv.reader(f)
    for row in list(reader)[1:-1]:
        linkdict[row[0]] = row[1]

# 查看数据库中的id
results = []
try:
    cs1 = connection.cursor()
    sql = 'select id from movie_union'
    count = cs1.execute(sql)
    print("查询到%d条数据" % count)
    results = cs1.fetchall()
    print(type(results))
    print("示例数据：{}".format(results[0]))
    results=[str(result['id']) for result in results]
    print(results[0])
    print(len(results))
except Exception as e:
    print(e)

baseurl = 'https://douban.uieee.com/v2/movie/search?q=tt{}&apikey=02646d3fb69a52ff072d47bf23cef8fd'

ids = [id for id in idList if not id in results]
print("一共{},还需要爬取{}".format(len(idList),len(ids)))


def search(idmbid):
    url = baseurl.format(idmbid)
    res = requests.get(url=url, headers=headers)
    html=res.text.strip()
    myjson = json.loads(html)
    print(html)
    return myjson['subjects'][0]['id']


lacknum = 0

for index, id in enumerate(ids):
    print("当前为第{}个，共有{}".format(index, len(ids)))
    try:
        idmbid = linkdict[id]
        print("找到对应的idmbid：{}".format(idmbid))
        douban_id = search(idmbid)
        print("数据爬取成功：{}".format(douban_id))
        sql = "insert into movie_union value (%s,'%s',%s)" % (id, idmbid, douban_id)
        connection.cursor().execute(sql)
        connection.commit()
    except Exception as e:
        print(e)
        lacknum = lacknum + 1

print("不存在的对应id个数{}".format(lacknum))
