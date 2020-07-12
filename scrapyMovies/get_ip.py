import requests
import redis
import time

pool = redis.ConnectionPool(host='localhost', port=6379)

r = redis.Redis(connection_pool=pool)

url = "http://120.79.85.144/index.php/api/entry?method=proxyServer.tiqu_api_url&packid=0&fa=0&dt=0&groupid=0&fetch_key=&qty=5&time=1&port=1&format=txt&ss=6&css=%2C&dt=0&pro=&city=&usertype=6"

count = 0

while True:
    # 查看当前redis中的用户数量
    num = r.scard('ippool')
    print("redis中剩余的ip数量：{},{}*20s没有变化过".format(num, count))
    # 如果20分钟都没有增加过ip，说明爬取完成
    if count == 60:
        break
    if num < 10:
        count = 0
        result = requests.get(url).text.strip()
        print("result" + result)
        # 如果不能获取ip了，停止
        ipList = result.split(',')
        print(str(ipList))
        for ip in ipList:
            myip = ip.split(':')[0]
            thisProxy = "http://" + ip
            try:
                res = requests.get(url="http://icanhazip.com/", timeout=8, proxies={"http": thisProxy})
                proxyIP = res.text.strip()
                if (proxyIP == myip):
                    print("代理IP:'" + proxyIP + "'有效！" + myip)
                    r.sadd('ippool', thisProxy)
                else:
                    print("代理IP无效！" + proxyIP + "," + myip)
            except:
                print("代理IP无效！")
        print("增加后redis中的ip数量：{}".format(r.scard('ippool')))
        # time.sleep(10)
    else:
        # 20s一刷新
        time.sleep(20)
        count = count + 1
