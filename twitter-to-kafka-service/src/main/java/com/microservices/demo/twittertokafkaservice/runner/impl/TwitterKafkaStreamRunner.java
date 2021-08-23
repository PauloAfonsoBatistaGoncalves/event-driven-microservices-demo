package com.microservices.demo.twittertokafkaservice.runner.impl;

import java.util.Arrays;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.microservices.demo.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twittertokafkaservice.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twittertokafkaservice.runner.StreamRunner;

import twitter4j.FilterQuery;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "false", matchIfMissing = true)
public class TwitterKafkaStreamRunner implements StreamRunner {
	
	private final static Logger LOG = LoggerFactory.getLogger(TwitterKafkaStreamRunner.class);

	private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
	
	private final TwitterKafkaStatusListener twitterKafkaStatusListener;
	
	private TwitterStream twitterStream;
	
	public TwitterKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData,
			TwitterKafkaStatusListener twitterKafkaStatusListener) {
		super();
		this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
		this.twitterKafkaStatusListener = twitterKafkaStatusListener;
	}



	@Override
	public void start() throws TwitterException {
		twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.addListener(twitterKafkaStatusListener);
		addFilter();
	}

	@PreDestroy
	public void shutdown() {
		if(twitterStream != null) {
			LOG.info("Closing twitter stream!");
			twitterStream.shutdown();
		}
	}


	private void addFilter() {
		String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
		FilterQuery filterQuery = new FilterQuery(keywords);
		twitterStream.filter(filterQuery);
		
		LOG.info("Started filtering Twwitter for keywords ", Arrays.toString(keywords));
	}

}
