package com.finlay.scaffold.repository;

import com.finlay.scaffold.model.Book;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author: Finlay
 * @description: CRUD, 泛型说明： 1、操作的实体对象类型  2、实体对象中ID字段的类型
 * @date: 2020-07-31 5:34 下午
 */
public interface BookRepository extends ElasticsearchRepository<Book,String> {
    //No property age found for type Book! Did you mean 'name'?
    List<Book> findByNameAndPrice(String name, BigDecimal price);
}
