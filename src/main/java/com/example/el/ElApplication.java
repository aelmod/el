package com.example.el;

import com.fasterxml.jackson.annotation.JsonProperty;
import javafx.util.Pair;
import lombok.Data;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class ElApplication {

    static List<String> collect;

    @Bean
    CommandLineRunner commandLineRunner() {
        return (args) -> {
            new Mult(collect).start();
            new Mult(collect).start();
        };
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        collect = Files.lines(Paths.get(ElApplication.class.getClassLoader().getResource("doyle-return-388.txt").toURI()))
                .flatMap(s -> Stream.of(s.split(" ")))
                .collect(Collectors.toList());
        SpringApplication.run(ElApplication.class, args);
    }
}

class Mult extends Thread {
    Logger logger = LoggerFactory.getLogger(ElApplication.class);

    List<String> collect;

    public Mult(List<String> collect) {
        this.collect = collect;
    }

    @Override
    public void run() {
        Random random = new Random();
        while (true) {
            String s = "";
            for (int i = 0; i < random.nextInt(30) + 10; i++) {
                String str = collect.get(random.nextInt(collect.size()));
                s = s.concat(str).concat(" ");
            }
            switch (random.nextInt(5)) {
                case 0: logger.info(s); break;
                case 1: logger.debug(s); break;
                case 2: logger.warn(s); break;
                case 3: logger.info(s); break;
                case 4: logger.error(s); break;
            }
        }
    }
}

@RepositoryRestResource(path = "/users")
interface UserRepository extends ElasticsearchRepository<User, String> {

    Page<User> findAllByNameIsContaining(@Param("name") String name, Pageable pageable);
}

interface LogRepository extends ElasticsearchRepository<LogEntry, String> {

}

@Data
@Document(indexName = "el", type = "user")
class User {

    @Id
    private String id;

    private String name;

    private int age;
}

@Data
@Document(indexName = "logstash", type = "logs")
class LogEntry {

    @Id
    private String id;

    private String message;

    private String level;

    @Field(type = FieldType.Date, format = DateFormat.date_optional_time)
    @JsonProperty(value = "@timestamp")
//    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern ="yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private Date timestamp;
}

@RestController
class LogController {

    private final LogRepository logRepository;

    @Autowired
    LogController(LogRepository logRepository) {this.logRepository = logRepository;}

    @GetMapping("/logs")
    public Pair<Long, List<LogEntry>> get(@RequestParam String search, Pageable pageable) {
        QueryBuilder query = QueryBuilders.regexpQuery("message", search);

        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder()
                .withSort(new FieldSortBuilder("@timestamp").order(SortOrder.DESC))
                .withQuery(query)
                .withPageable(pageable);
        Page<LogEntry> entries = logRepository.search(nativeSearchQueryBuilder.build());
        return new Pair<>(entries.getTotalElements(), entries.getContent());
    }
}
