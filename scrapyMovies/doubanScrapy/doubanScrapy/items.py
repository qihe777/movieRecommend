# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

from scrapy import Item, Field

import pymysql
import redis

MYSQL_DB = 'movie_recommend'
MYSQL_USER = 'root'
MYSQL_PASS = 'ZHANGyinqi123...'
MYSQL_HOST = 'localhost'

connection = pymysql.connect(host=MYSQL_HOST, user=MYSQL_USER,
                             password=MYSQL_PASS, db=MYSQL_DB,
                             charset='utf8mb4',
                             cursorclass=pymysql.cursors.DictCursor)

pool = redis.ConnectionPool(host='localhost', port=6379)

r = redis.Redis(connection_pool=pool)


class IdList(Item):
    id_list = Field()


class MovieMeta(Item):
    douban_id = Field()
    # 封面src
    cover = Field()
    # 电影名称
    name = Field()
    # 上映年份
    year = Field()
    # 类型
    genres = Field()
    # 制片国家/地区
    regions = Field()
    # 电影时长
    mins = Field()
    # imdb的id
    imdb_id = Field()
    douban_score = Field()
    # 评分人数
    douban_votes = Field()
    # 标签
    tags = Field()
    # 故事简介
    storyline = Field()
    # 新增爬取

    # 展示的电影名,有电影原名，比较好看
    showname = Field()
    # 评分占比
    score_list = Field()
    # 演员封面以及链接
    actors = Field()


class Actor():
    def __init__(self, id, pic, role, name):
        self.id = id
        self.pic = pic
        self.role = role
        self.name = name


class Comment(Item):
    cid = Field()
    mid = Field()
    comment = Field()
    rating = Field()
    vote_count = Field()
    create_time = Field()
    user_city = Field()
    user_name = Field()
    user_gender = Field()
    user_reg_time = Field()
    user_avatar = Field()
    user_id = Field()


class DoubanscrapyItem(Item):
    # define the fields for your item here like:
    # name = scrapy.Field()
    pass
