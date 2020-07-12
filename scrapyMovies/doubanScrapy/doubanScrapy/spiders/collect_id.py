from scrapy import Spider, Request
import json
import logging
from ..items import IdList, r


class CollectId(Spider):
    name = "collect_id"
    allowed_domains = ["douban.com"]
    # url_list1 = [
    #     "https://movie.douban.com/j/new_search_subjects?sort=U&range=1,10&tags=%E7%94%B5%E5%BD%B1&start=" + str(x * 20)
    #     for x in range(1,250)]
    url_list2 = [
        "https://movie.douban.com/j/new_search_subjects?sort=T&range=1,10&tags=%E7%94%B5%E5%BD%B1&start=" + str(x * 20)
        for x in range(250)]
    start_urls = url_list2
    #start_urls = ["https://movie.douban.com/j/new_search_subjects?sort=U&range=1,10&tags=%E7%94%B5%E5%BD%B1&start=0"]

    def parse(self, response):
        body=bytes.decode(response.body)
        logging.info("正在解析" + body)
        oldurl = response.request.url
        oldip = response.meta['proxy']
        if response.status == 403:
            # 从redis中删除此ip
            r.srem('ippool', oldip)
            logging.warning("403,无法使用的ip" + oldip + "重试：" + oldurl)
            # 进行重试
            return Request(url=oldurl)
        else:
            if body.find("data")>=0:
                results = json.loads(body)
                movie_list = results['data']
                idList = IdList()
                idList['id_list'] = [movie['id'] for movie in movie_list]
                logging.info("爬取成功" + str(len(idList['id_list'])))
                return idList
            else:
                # 从redis中删除此ip
                r.srem('ippool', oldip)
                logging.warning("需要登录" + "重试：" + oldurl)
                # 进行重试
                return Request(url=oldurl)
