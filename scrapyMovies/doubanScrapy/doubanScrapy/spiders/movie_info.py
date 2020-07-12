from scrapy import Request, Spider
import random
import string
import logging
import re

from ..items import MovieMeta, Actor, connection, r

cursor = connection.cursor()


class MovieInfoSpider(Spider):
    name = 'movie_info'
    user_agent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 \
                      (KHTML, like Gecko) Chrome/67.0.3396.62 Safari/537.36'
    allowed_domains = ["movie.douban.com"]
    sql = 'select id from movie_index where id not in (select mid from movie)'
    cursor.execute(sql)
    movie_ids = cursor.fetchall()
    movie_ids = [movie['id'] for movie in movie_ids]
    start_urls = [
        'https://movie.douban.com/subject/%s/' % id for id in movie_ids
    ]

    # start_urls = [
    #     'https://movie.douban.com/subject/1294099/'
    # ]

    def start_requests(self):
        for url in self.start_urls:
            bid = ''.join(random.choice(string.ascii_letters + string.digits) for x in range(11))
            cookies = {
                'bid': bid,
                'dont_redirect': True,
                'handle_httpstatus_list': [302],
            }
            yield Request(url, cookies=cookies)

    def parse(self, response):
        body = bytes.decode(response.body)
        if 35000 > len(response.body):
            oldip = response.meta['proxy']
            # 从redis中删除此ip
            r.srem('ippool', oldip)
            oldurl = response.request.url
            logging.warning("返回结果长度太小{},oldip:{},重试：{}".format(body, oldip, oldurl))
            # 进行重试
            return Request(url=oldurl)
        elif 404 == response.status:
            logging.warning("404:{}".format(response.url))
        else:
            meta = MovieMeta()
            self.get_douban_id(meta, response)
            self.get_cover(meta, response)
            self.get_name(meta, response)
            self.get_year(meta, response)
            self.get_genres(meta, response)
            self.get_regions(meta, response)
            self.get_runtime(meta, response)
            self.get_imdb_id(meta, response)
            self.get_score(meta, response)
            self.get_votes(meta, response)
            self.get_tags(meta, response)
            self.get_storyline(meta, response)
            self.get_showname(meta, response)
            self.get_score_list(meta, response)
            self.get_actors(meta, response)
            return meta

    def get_douban_id(self, meta, response):
        meta['douban_id'] = response.url[33:-1]
        return meta

    def get_cover(self, meta, response):
        regx = '//img[@rel="v:image"]/@src'
        data = response.xpath(regx).extract()
        if data:
            if (data[0].find('default') == -1):
                meta['cover'] = data[0].replace('spst', '\
lpst').replace('mpic', 'lpic')
            else:
                meta['cover'] = ''
        return meta

    def get_name(self, meta, response):
        regx = '//title/text()'
        data = response.xpath(regx).extract()
        if data:
            meta['name'] = data[0][:-5].strip()
        else:
            meta['name'] = ""
        return meta

    def get_year(self, meta, response):
        regx = '//span[@class="year"]/text()'
        data = response.xpath(regx).extract()
        if data:
            meta['year'] = match_year(data[0])
        else:
            meta['year'] = 0
        return meta

    def get_genres(self, meta, response):
        regx = '//span[@property="v:genre"]/text()'
        genres = response.xpath(regx).extract()
        if genres:
            meta['genres'] = genres
        else:
            meta['genres'] = []
        return meta

    def get_regions(self, meta, response):
        regx = '//text()[preceding-sibling::span[text()="制片国家/地区:"]][fo\
llowing-sibling::br]'
        data = response.xpath(regx).extract()
        if data:
            meta['regions'] = data[0]
        else:
            meta['regions'] = ""
        return meta

    def get_runtime(self, meta, response):
        regx = '//span[@property="v:runtime"]/@content'
        data = response.xpath(regx).extract()
        if data:
            meta['mins'] = data[0]
        else:
            meta['mins'] = 0
        return meta

    def get_imdb_id(self, meta, response):
        regx = '//a[preceding-sibling::span[text()="IMDb链接:"]][following-si\
bling::br]/@href'
        data = response.xpath(regx).extract()
        if data:
            meta['imdb_id'] = data[0].strip().split('?')[0][26:]
        else:
            meta['imdb_id'] = ""
        return meta

    def get_score(self, meta, response):
        regx = '//strong[@property="v:average"]/text()'
        data = response.xpath(regx).extract()
        if data:
            meta['douban_score'] = data[0]
        else:
            meta['douban_score'] = 0
        return meta

    def get_votes(self, meta, response):
        regx = '//span[@property="v:votes"]/text()'
        data = response.xpath(regx).extract()
        if data:
            meta['douban_votes'] = data[0]
        else:
            meta['douban_votes'] = 0
        return meta

    def get_tags(self, meta, response):
        regx = '//div[@class="tags-body"]/a/text()'
        tags = response.xpath(regx).extract()
        if tags:
            meta['tags'] = tags
        else:
            meta['tags'] = []
        return meta

    def get_storyline(self, meta, response):
        regx = '//span[@class="all hidden"]/text()'
        data = response.xpath(regx).extract()
        if data:
            meta['storyline'] = change(str(data[0]))
        else:
            regx = '//span[@property="v:summary"]/text()'
            data = response.xpath(regx).extract()
            if data:
                meta['storyline'] = change(str(data[0]))
            else:
                meta['storyline'] = ""
        return meta

    def get_showname(self, meta, response):
        regx = '//span[@property="v:itemreviewed"]/text()'
        showname = response.xpath(regx).extract()
        if showname:
            meta['showname'] = change(str(showname))
        else:
            logging.warning("获取展示名称失败{}".format(meta['douban_id']))
            meta['showname'] = ""
        return meta

    def get_score_list(self, meta, response):
        regx = '//div[@class="ratings-on-weight"]'
        scores = response.xpath(regx)
        scoreList = scores[0].xpath('//span[@class="rating_per"]/text()').extract()
        if len(scoreList) == 5:
            result = []
            for score in scoreList:
                result.append(score[0:-1])
            meta['score_list'] = result
        else:
            meta['score_list'] = [0, 0, 0, 0, 0]
            logging.warning("获取分数站比失败".format(meta['douban_id']))
        return meta

    def get_actors(self, meta, response):
        actors = []
        try:
            regx = '//li[@class="celebrity"]'
            lis = response.xpath(regx)
            pics = lis[0].xpath('//div[@class="avatar"]/@style').extract()
            for i in range(0, len(pics)):
                reObject = re.search(r'url\((.*)\)', pics[i])
                if reObject:
                    pics[i] = reObject.group(1)

            idall = lis[0].xpath('//a[@class="name"]/@href').extract()
            ids = []
            for i in range(0, len(idall)):
                reObject = re.search(r'celebrity/(.*)/', idall[i])
                if reObject:
                    ids.append(reObject.group(1))

            names = lis[0].xpath('//a[@class="name"]/text()').extract()
            names = names[0:len(ids)]
            roles = lis[0].xpath('//span[@class="role"]/text()').extract()
            for pic, id, name, role in zip(pics, ids, names, roles):
                actor = Actor(id, pic, role, name)
                actors.append(actor)
        except Exception as e:
            logging.error(e)
        meta['actors'] = actors
        return meta


def match_year(s):
    matches = re.findall('[\d]{4}', s)
    if matches:
        return matches[0]
    else:
        return '0'


def change(str):
    str = str.replace("'", "\\'")
    str = str.replace('"', '\\"')
    return str
