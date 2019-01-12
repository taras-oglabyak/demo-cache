package com.example.democache;

import com.google.common.cache.CacheBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
@SpringBootApplication
public class DemoCacheApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(DemoCacheApplication.class, args);
    }

    @Bean
    public CacheManager cacheManager() {
        // return new ConcurrentMapCacheManager("users");

//        SimpleCacheManager cacheManager = new SimpleCacheManager();
//        cacheManager.setCaches(Collections.singletonList(new ConcurrentMapCache("users")));
//        cacheManager.initializeCaches();
//        return cacheManager;

        GuavaCacheManager cacheManager = new GuavaCacheManager();
        cacheManager.setCacheBuilder(CacheBuilder.newBuilder().maximumSize(100));
        return cacheManager;
    }

    @Autowired
    private Service service;

    @Override
    public void run(String... args) throws InterruptedException {
        for (int i = 0; i < 1; i++) {
            doJob(service);
        }
        System.exit(0);
    }

    private void doJob(Service service) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        Random random = new Random();
        List<Callable<User>> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(() -> service.getUser(random.nextInt(10)));
        }

        long start = System.currentTimeMillis();
        List<User> users = executorService.invokeAll(tasks).stream().map(userFuture -> {
                    try {
                        return userFuture.get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    User user = new User();
                    user.id = -1;
                    return user;
                }
        ).sorted(Comparator.comparing(User::getId).thenComparing(User::getLoaded)).collect(Collectors.toList());
        System.out.println(System.currentTimeMillis() - start);
        // users.forEach(System.out::println);
    }


    @Component
    public static class Service {
        @Autowired
        private Repository repository;
        @Cacheable(cacheNames = "users", key = "#id", sync = true)
        public User getUser(int id) {
            return repository.getUser(id);
        }
    }

    @Component
    public static class Repository {
        public User getUser(int id) {
            System.out.println(id + " loaded");
            User user = new User();
            user.id = id;
            user.name = UUID.randomUUID().toString();
            return user;
        }
    }

    private static class User {
        private int id;
        private String name;
        private long loaded = System.nanoTime();

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", loaded=" + loaded +
                    '}';
        }

        public int getId() {
            return id;
        }

        public long getLoaded() {
            return loaded;
        }
    }
}
