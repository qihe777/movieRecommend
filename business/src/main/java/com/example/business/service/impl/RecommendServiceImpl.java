package com.example.business.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.business.dao.mapper.RecommendMapper;
import com.example.business.pojo.BriefMovie;
import com.example.business.pojo.BriefMovieWithActor;
import com.example.business.service.RecommendService;
import com.example.business.utils.Constant;
import com.github.pagehelper.PageInfo;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.Fuzziness;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendServiceImpl implements RecommendService {

    @Autowired
    RecommendMapper recommendMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public List<BriefMovie> getAlsoSawMovies(int mid, int size) {
        return recommendMapper.getAlsoSawMovies(mid, size);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#mid")
    public List<BriefMovie> getContentSimMovie(int mid, int size) {
        return recommendMapper.getContentSimMovie(mid, size);
    }

    @Override
    public List<BriefMovie> getGussLikeMovies(int uid, int size) {
        return recommendMapper.getGussLikeMovies(uid, size);
    }

    @Override
    public List<BriefMovie> getStreamMovies(int uid, int size) {
        return recommendMapper.getStreamMovies(uid, size);
    }

    @Override
    @Cacheable(value = "movie", key = "#root.methodName+'-'+#text+'-'+#type")
    public List<BriefMovieWithActor> getFuzzyMovies(String text, int type) throws Exception {
        //创建检索请求
        SearchRequest searchRequest = new SearchRequest(Constant.ES_INDEX);
        //创建搜索构建者
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置构建搜索属性
        sourceBuilder.from(0); // 需要注意这里是从多少条开始
        sourceBuilder.size(10); //共返回多少条数据
        //sourceBuilder.sort(new FieldSortBuilder("score").order(SortOrder.DESC)); //根据自己的需求排序

        if (!StringUtils.isEmpty(text)) {
            //自定义组合查询
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            if(type==1){
                MatchQueryBuilder actorQuery = QueryBuilders.matchQuery("actor", text);
                //.fuzziness(Fuzziness.AUTO); //模糊匹配
                boolQueryBuilder.must(actorQuery);
            }
            else {
                MatchQueryBuilder nameQuery = QueryBuilders.matchQuery("name", text);
                //.fuzziness(Fuzziness.AUTO); //模糊匹配
                boolQueryBuilder.must(nameQuery);
            }
            sourceBuilder.query(boolQueryBuilder);
        }
        //传入构建进行搜索
        searchRequest.source(sourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //处理结果
        RestStatus restStatus = searchResponse.status();
        if (restStatus != RestStatus.OK) {
            throw new Exception("搜索错误");
        }
        List<BriefMovieWithActor> list = new ArrayList<>();
        SearchHits hits = searchResponse.getHits();
        for (SearchHit hit : hits) {
            JSONObject json = JSONObject.parseObject(hit.getSourceAsString());
            BriefMovieWithActor tmp=new BriefMovieWithActor();
            tmp.setMid(Integer.valueOf(hit.getId()));
            tmp.setActor(json.getString("actor"));
            tmp.setName(json.getString("name"));
            tmp.setPic(json.getString("pic"));
            tmp.setScore(json.getDouble("score"));
            list.add(tmp);
        }

        return list;
    }

    @Override
    public List<BriefMovie> streamRecommend(int uid, int size) throws Exception {
        List<BriefMovie> list = recommendMapper.streamRecommend(uid,size);
        if(list.isEmpty())
            throw new Exception("不存在");
        //修改为看过
        recommendMapper.updateStatus(uid);
        return list;
    }
}



