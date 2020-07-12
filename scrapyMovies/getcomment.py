import random
import string

import requests
import json
import time

from requests.cookies import RequestsCookieJar

first_url = 'https://m.douban.com/rexxar/api/v2/movie/1291543/interests?count=20&order_by=hot&start=0&ck=dNhr&for_mobile=1'
url = 'https://m.douban.com/rexxar/api/v2/movie/1291543/interests'
# 移动端头部信息
useragents = [
    "Mozilla/5.0 (iPhone; CPU iPhone OS 9_2 like Mac OS X) AppleWebKit/601.1 (KHTML, like Gecko) CriOS/47.0.2526.70 Mobile/13C71 Safari/601.1.46",
    "Mozilla/5.0 (Linux; U; Android 4.4.4; Nexus 5 Build/KTU84P) AppleWebkit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30",
    "Mozilla/5.0 (compatible; MSIE 9.0; Windows Phone OS 7.5; Trident/5.0; IEMobile/9.0)"
]


def change(str):
    str = str.replace("'", "\\'")
    str = str.replace('"', '\\"')
    return str


def visit_URL(i):
    print("开始访问", i)
    # 请求头部
    headers = {
        'Host': 'm.douban.com',
        'Upgrade-Insecure-Requests': '1',
        'User-Agent': random.choice(useragents),
        'Referer': 'https://m.douban.com/movie/subject/26322642/comments'
    }

    bid = ''.join(random.choice(string.ascii_letters + string.digits) for x in range(11))
    cookie_jar = RequestsCookieJar()
    cookie_jar.set("bid", bid, domain="douban.com")
    cookie_jar.set("dont_redirect", "True", domain="douban.com")
    cookie_jar.set("handle_httpstatus_list", "[302]", domain="douban.com")
    params = {
        'count': '50',
        'order_by': 'hot',
        'start': str(i),
        'for_mobile': '1',
        'ck': 'dNhr'
    }

    res = requests.get(url=url, headers=headers, params=params, cookies=cookie_jar)
    print(res.text)
    items = json.loads(res.text)['interests']
    print("评论条数{}".format(len(items)))
    comment = {}
    douban_id = res.url.split('/')[-2]
    for item in items:
        comment['mid'] = douban_id
        comment['cid'] = item['id']
        comment['comment'] = change(item['comment'])
        comment['create_time'] = item['create_time']
        comment['vote_count'] = item['vote_count']
        rating = item['rating']
        if rating:
            comment['rating'] = item['rating']['value']
        else:
            comment['rating'] = 0
        comment['user_name'] = item['user']['name']
        comment['user_avatar'] = item['user']['avatar']
        comment['user_id'] = item['user']['id']
        comment['user_gender'] = item['user']['gender']
        loc = item['user']['loc']
        if loc:
            comment['user_city'] = loc['name']
        else:
            comment['user_city'] = ""
        comment['user_reg_time'] = item['user']['reg_time']
        print(comment)

if __name__ == '__main__':
    visit_URL(0)
    visit_URL(50)
