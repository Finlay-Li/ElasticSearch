package test;


import com.finlay.scaffold.model.Book;
import com.finlay.scaffold.repository.BookRepository;
import com.google.common.collect.Lists;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class Test1 extends ApplicationTest {

    @Autowired
    private BookRepository bookRepository;
    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    /***
     * @description: id存在则更新，不存在则新增：因此先查询再更新，字段值不能为空,否则es 更新为null
     * @return: void
     **/
    @Test
    public void saveOrUpdate() {
        Book book = new Book();
        book.setAuthor("罗永浩")
                .setContent("这是大杯~")
                .setId("2")
                .setName("这是大杯中杯论")
                .setPrice(new BigDecimal("18"))
                .setPubDate(new Date());
        Book save = bookRepository.save(book);
    }

    @Test
    public void delete() {
        bookRepository.deleteById("AXOyjx7bCVm4kyqzZpwh");
    }

    @Test
    public void selectAll() {
        Iterable<Book> all = bookRepository.findAll();
        all.forEach(a -> {
            System.out.println(a);
        });
    }

    /***
     * @description: https://docs.spring.io/spring-data/elasticsearch/docs/3.0.7.RELEASE/reference/html/#reference
     * @return: void
     **/
    @Test
    public void findNameAndAge() {
        List<Book> byNameAndPrice = bookRepository.findByNameAndPrice("ElasticSearch", BigDecimal.TEN);
        byNameAndPrice.forEach(a -> {
            System.out.println(a);
        });
    }

    @Test
    public void page() {
        //第1页
        int page = 1;
        //定义查询方式
        MatchAllQueryBuilder matchAllQueryBuilder = QueryBuilders.matchAllQuery();
        //创建查询条件
        SearchQuery query = new NativeSearchQuery(matchAllQueryBuilder).setPageable(PageRequest.of(page - 1, 1));
        Page<Book> search = bookRepository.search(query);
        //处理结果
        System.out.println(search.getNumber());
        System.out.println(search.getTotalElements());
        System.out.println(search.getTotalPages());
        //直接遍历出page内的元素
        search.forEach(a -> {
            System.out.println(a);
        });
    }

    /***
     * @description: 条件查询，排序，分页
     **/
    @Test
    public void pageAndSort() {

        //排序字段
        FieldSortBuilder price = new FieldSortBuilder("price").order(SortOrder.DESC);
        //构建查询条件
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withIndices("alibaba")
                .withTypes("book")
                .withQuery(QueryBuilders.matchAllQuery())
                .withSort(price)
                .withPageable(PageRequest.of(0, 2))
                .build();
        AggregatedPage<Book> books = elasticsearchTemplate.queryForPage(searchQuery, Book.class);
        //直接遍历出page内的元素
        books.forEach(a -> System.out.println(a));
    }

    /***
     * @description: 多字段分词检索，条件过滤，分页，排序，高亮
     * @return: void
     **/
    @Test
    public void high() {

        //高亮的字段
        HighlightBuilder.Field nameField = new HighlightBuilder.Field("name").requireFieldMatch(false);//开启多字段同时高亮
        nameField.preTags("<span style='color:red'>");
        nameField.postTags("</span>");

        //不建议使用 *
        HighlightBuilder.Field contentField = new HighlightBuilder.Field("content").requireFieldMatch(false);//开启多字段同时高亮
        contentField.preTags("<span style='color:red'>");
        contentField.postTags("<span/>");

        //构建查询条件
        NativeSearchQuery build = new NativeSearchQueryBuilder()
                .withIndices("alibaba")//book 对象已有映射说明，可以不写
                .withTypes("book")
                .withQuery(QueryBuilders.queryStringQuery("这是").field("name").field("content"))//分词检索
                .withFilter(QueryBuilders.rangeQuery("price").gt(8))//设置过滤条件
                .withSort(new FieldSortBuilder("price").order(SortOrder.DESC))
                .withPageable(PageRequest.of(0, 2))
                .withHighlightFields(nameField, contentField)
                .build();


        //对查询结果进行高亮处理
        AggregatedPage<Book> books = elasticsearchTemplate.queryForPage(build, Book.class, new SearchResultMapper() {
            @Override
            public <T> AggregatedPage<T> mapResults(SearchResponse searchResponse, Class<T> aClass, Pageable pageable) {
                SearchHit[] hits = searchResponse.getHits().getHits();
                List<Book> result = Lists.newArrayList();

                for (SearchHit s :
                        hits) {
                    //获取原始数据
//                    System.out.println(s.getSourceAsString());
                    Map<String, Object> sourceAsMap = s.getSourceAsMap();

                    //获取高亮map
                    Map<String, HighlightField> highlightFields = s.getHighlightFields();

                    Book book = new Book();
                    book.setId(sourceAsMap.get("id").toString());
                    book.setName(sourceAsMap.get("name").toString());
                    //替换高亮字段
                    if (highlightFields.containsKey("name")) {
                        book.setName(highlightFields.get("name").fragments()[0].toString());
                    }
                    book.setAuthor(sourceAsMap.get("author").toString());
                    book.setContent(sourceAsMap.get("content").toString());
                    //替换高亮字段
                    if (highlightFields.containsKey("content")) {
                        book.setContent(highlightFields.get("content").fragments()[0].toString());
                    }
                    book.setPrice(new BigDecimal(sourceAsMap.get("id").toString()));
                    book.setPubDate(new Date(Long.valueOf(sourceAsMap.get("pubDate").toString())));

                    result.add(book);
                }

                return new AggregatedPageImpl<T>((List<T>) result);
            }
        });

        //直接遍历出聚合查询的元素
        books.forEach(a -> {
            System.out.println(a);
        });
    }
}
