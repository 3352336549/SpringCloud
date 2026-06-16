package com.hmall.item.es;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmall.item.domin.po.Item;
import com.hmall.item.domin.po.ItemDoc;
import com.hmall.item.service.IItemService;
import org.apache.http.HttpHost;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

//@SpringBootTest(properties = "spring.profiles.active=local")
public class ElasticSearchTest {

    private RestHighLevelClient client;

    @Test
    void testMatchAll() throws IOException {
        //1.创建request对象
        SearchRequest request = new SearchRequest("items");
        //2.配置request参数
        request.source()
                .query(QueryBuilders.matchAllQuery());
        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        SearchHits searchHits = response.getHits();
        //4.1 获取总条数
        TotalHits total = searchHits.getTotalHits();
        //4.2命中的数据
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //4.2.1获取source结果
            String json = hit.getSourceAsString();
            //4.2.2转为ItemDoc
            ItemDoc doc = JSONUtil.toBean(json, ItemDoc.class);

            System.out.println( doc);
        }
    }


    @BeforeEach
    void setUp() {
        this.client = new RestHighLevelClient(RestClient.builder(
                HttpHost.create("http://192.168.18.99:9200")
        ));
    }

    @AfterEach
    void tearDown() throws IOException {
        this.client.close();
    }

    @Test
    void testHighlight() throws IOException {
        //1.创建request对象
        SearchRequest request = new SearchRequest("items");

        //2.组织DSL参数
        //2.1query条件
        request.source().query(QueryBuilders.matchQuery("name","脱脂牛奶"));
        //2.2高亮条件
        request.source().highlighter(SearchSourceBuilder.highlight().field("name"));

        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        handleResponse(response);
    }

    @Test
    void testAgg() throws IOException {
        //1.创建request对象
        SearchRequest request = new SearchRequest("items");

        //2.组织DSL参数
        //2.1分页
        request.source().size(0);
        //2.2聚合条件
        String brandAggName = "brandAgg";
        request.source().aggregation(
                AggregationBuilders
                        .terms(brandAggName)
                        .field("brand")
                        .size(10)
        );

        //3.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        //4.解析结果
        System.out.println("response = " + response);
    }


    private void handleResponse(SearchResponse response){
        SearchHits searchHits = response.getHits();
        //1.获取总条数
        long total = searchHits.getTotalHits().value;
        System.out.println("total = " + total);
        //2.遍历结果数组
        SearchHit[] hits = searchHits.getHits();
        for (SearchHit hit : hits) {
            //3.得到_source,也就是原始json文档
            String source = hit.getSourceAsString();
            //4.反序列号
            ItemDoc item = JSONUtil.toBean(source, ItemDoc.class);
            //5.获取高亮结果
            Map<String, HighlightField> hfs = hit.getHighlightFields();
            if(CollUtil.isNotEmpty(hfs)){
                //5.1有高亮结果，获取name的高亮结果
                HighlightField hf = hfs.get("name");
                if (hf != null) {
                    //5.2获取第一个高亮结果片段，就是商品名称的高亮值
                    String hfname = hf.getFragments()[0].string();
                    item.setName(hfname);
                }
            }
            System.out.println("item = " + item);
        }
    }
}






















