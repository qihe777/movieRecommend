# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html
from .items import IdList, MovieMeta, connection, Comment

import logging


class DoubanscrapyPipeline(object):

    def process_item(self, item, spider):
        if isinstance(item, IdList):
            self.save_movie_ids(item)

        elif isinstance(item, MovieMeta):
            self.save_movie_info(item)
        elif isinstance(item, Comment):
            logging.info("正在添加评论")
            self.save_comment(item)

        return item

    def save_movie_ids(self, item):
        for id in item['id_list']:
            logging.info("正在添加" + str(id))
            try:
                sql = 'INSERT INTO movie_index VALUES (%s)' % (id)
                connection.cursor().execute(sql)
                connection.commit()
            except Exception as e:
                logging.error("出现重复id" + str(id) + str(e))

    def save_movie_info(self, item):
        # 保存电影表
        try:
            sql = 'INSERT INTO movie VALUE ("%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s",null,null)' \
                  % (item['douban_id'], item['cover'], item['name'], item['year'], item['regions'], item['mins'],
                     item['imdb_id'], item['douban_score'], item['douban_votes'], item['storyline'], item['showname'],
                     item['score_list'][0], item['score_list'][1], item['score_list'][2], item['score_list'][3],
                     item['score_list'][4])
            connection.cursor().execute(sql)
            connection.commit()
        except Exception as e:
            logging.error("添加电影错误{},{}".format(item['douban_id'], e))
        # 保存类型genre
        for genre in item['genres']:
            try:
                sql = 'insert into genres_union value (null,"%s","%s")' % (genre, item['douban_id'])
                connection.cursor().execute(sql)
                connection.commit()
            except Exception as e:
                logging.error("添加电影genre错误:{},{},{}".format(item['douban_id'], genre, e))
        # 保存tag
        for tag in item['tags']:
            try:
                sql = 'insert into tag_union value (null,"%s","%s")' % (tag, item['douban_id'])
                connection.cursor().execute(sql)
                connection.commit()
            except Exception as e:
                logging.error("添加电影tag错误:{},{},{}".format(item['douban_id'], tag, e))
        # 保存演员
        for actor in item['actors']:
            try:
                sql = 'insert into actor value ("%s","%s","%s")' % (actor.id, actor.name, actor.pic)
                connection.cursor().execute(sql)
                connection.commit()
            except Exception as e:
                logging.error("添加电影actor错误{},{}".format(item['douban_id'], e))
            try:
                sql = 'insert into actors_union value (%s,"%s","%s")' % (actor.id,actor.role, item['douban_id'])
                connection.cursor().execute(sql)
                connection.commit()
            except Exception as e:
                logging.error("添加电影actor_union错误:{}.{}".format(item['douban_id'], e))

    def save_comment(self, item):
        try:
            sql = 'INSERT INTO comment VALUES ("%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s","%s")' \
                  % (item['cid'], item['mid'], item['comment'],
                     item['rating'], item['vote_count'], item['create_time'], item['user_id'],
                     item['user_name'], item['user_gender'], item['user_avatar'], item['user_city'],
                     item['user_reg_time'])
            connection.cursor().execute(sql)
            connection.commit()
        except Exception as e:
            logging.error("添加评论失败{}".format(e))
