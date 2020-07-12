import pymysql
import jieba
from jieba import analyse

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

csr = connection.cursor()

# 读取停用词
path = "/root/data/hit_stopwords.txt"
analyse.set_stop_words(path)

# 从数据u找到全部的id
ids = []
try:
    sql = 'select distinct(mid) from comment where mid not in (select mid from comment_parse)'
    count = csr.execute(sql)
    ids = csr.fetchall()
    ids = [id['mid'] for id in ids]
    print("共有%d条电影,确实有%d" % (count, len(ids)))
except Exception as e:
    print(e)


# 便利id，获取每id的全部评论

def change(str):
    str = str.replace("'", "\\'")
    str = str.replace('"', '\\"')
    return str


comments = []

for index, id in enumerate(ids):
    print("正在处理第{}个，共{}".format(index, len(ids)))
    try:
        sql = 'select comment from comment where mid={}'.format(id)
        count = csr.execute(sql)
        comments = csr.fetchall()
        print("电影%d共有%d条评论" % (id, count))
        # 拼成一个字符串
        comments = ';'.join([comment['comment'] for comment in comments])
        # 对分好的词进行分析,topK返回的关键词个数，withWeight带着权重
        tags_ret = analyse.extract_tags(comments, topK=40, withWeight=True)
        dict = {}
        for v, n in tags_ret:
            # 权重是小数，为了凑整，乘了一万
            dict[v] = int(n * 10000)
        parse = change(str(dict))

        # 插入到数据库
        sql = 'insert into comment_parse value (%s,"%s")' % (id, parse)
        connection.cursor().execute(sql)
        connection.commit()
        print("电影%d处理成功" % id)
    except Exception as e:
        print(e)
