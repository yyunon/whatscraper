package com.recommendmecheap.scraper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.text.ParseException;
import java.util.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jsoup.*;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.errors.RecordTooLargeException;
import org.apache.kafka.common.requests.ProduceRequest;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import static com.recommendmecheap.scraper.WebScraperProxy.*;

public class Scraper 
{
    private static final Logger logger = LogManager.getLogger(Scraper.class);

    public static void main( String[] args ) throws Exception
    {
        Context ctx = Context.Build()
                             .InitDocumentCache("tmp")
                             .InitProxyCache();
        CLI commandLineParser = CLI.Build(args);
        commandLineParser.Parse();
        logger.info("Command line args are " + commandLineParser.cliArgs.toString());

        WebScraperProxy proxy = WebScraperProxy.GetProxyBuilder()
                                        .setProxySite(commandLineParser.cliArgs.proxySite)
                                        .setContext(ctx)
                                        .build();
        proxy.Init();
        proxy.Scrape(commandLineParser.cliArgs.siteToScrape + commandLineParser.cliArgs.proxyQuery, true);

        //Producer<Long, String> producer = ScraperProducer.createProducer();
        //ProducerRecord<Long, String> record = new ProducerRecord<Long, String>(ScraperProducer.TOPIC, proxyResponse);
        //RecordMetadata metadata = producer.send(record).get(); 

    }

}
