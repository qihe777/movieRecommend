#!/usr/bin/env python
# -*- coding: utf-8 -*-

import json
import logging
import random
import string

from ..items import MovieMeta, Actor, connection, r, Comment

from scrapy import Request, Spider

cursor = connection.cursor()


class MovieCommentSpider(Spider):
    name = 'comment'
    allowed_domains = ['movie.douban.com']
    sql = 'SELECT mid FROM movie WHERE votes>20000 and mid NOT IN \
           (SELECT mid FROM comment GROUP BY mid) ORDER BY mid DESC'
    cursor.execute(sql)
    movies = cursor.fetchall()
    start_urls = {
        str(i['mid']): ('https://m.douban.com/rexxar/api/v2/movie/%s/interests?count=50&order_by=hot&start=0&ck=dNhr&for_mobile=1' % i['mid']) for i
        in movies
    }

    # start_urls = {
    #     "1291543": 'https://m.douban.com/rexxar/api/v2/movie/1291543/interests?count=50&order_by=hot&start=0&ck=dNhr&for_mobile=1'
    # }

    def start_requests(self):
        for (key, url) in self.start_urls.items():
            headers = {
                'Referer': 'https://m.douban.com/movie/subject/%s/comments' % key
            }
            bid = ''.join(random.choice(string.ascii_letters + string.digits) for x in range(11))
            cookies = {
                'bid': bid,
                'dont_redirect': True,
                'handle_httpstatus_list': [302],
            }
            yield Request(url, headers=headers, cookies=cookies)

    def parse(self, response):
        body=bytes.decode(response.body)
        if 302 == response.status:
            logging.error("出现302{}".format(body))
        else:
            douban_id = response.url.split('/')[-2]
            logging.info("正在解析{}:{}".format(douban_id,body))
            items = json.loads(response.body)['interests']
            for item in items:
                comment = Comment()
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
                comment['user_name'] = change(item['user']['name'])
                comment['user_avatar'] = item['user']['avatar']
                comment['user_id'] = item['user']['id']
                comment['user_gender'] = item['user']['gender']
                loc = item['user']['loc']
                if loc:
                    comment['user_city'] = loc['name']
                else:
                    comment['user_city'] = ""
                comment['user_reg_time'] = item['user']['reg_time']

                yield comment

def change(str):
    str = str.replace("'", "\\'")
    str = str.replace('"', '\\"')
    return str
