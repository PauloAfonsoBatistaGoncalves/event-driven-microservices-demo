package com.microservices.twittertokafkaservice.runner.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.microservices.twittertokafkaservice.config.TwitterToKafkaServiceConfigData;
import com.microservices.twittertokafkaservice.exception.TwitterToKafkaServiceException;
import com.microservices.twittertokafkaservice.listener.TwitterKafkaStatusListener;
import com.microservices.twittertokafkaservice.runner.StreamRunner;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner{

	private final static Logger LOG = LoggerFactory.getLogger(MockKafkaStreamRunner.class);

	private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;

	private final TwitterKafkaStatusListener twitterKafkaStatusListener;
	
	private static final Random RANDOM = new Random();
	
	private static final String[] WORDS = new String[]{
            "Lorem",
            "ipsum",
            "dolor",
            "sit",
            "amet",
            "consectetuer",
            "adipiscing",
            "elit",
            "Maecenas",
            "porttitor",
            "congue",
            "massa",
            "Fusce",
            "posuere",
            "magna",
            "sed",
            "pulvinar",
            "ultricies",
            "purus",
            "lectus",
            "malesuada",
            "libero"
    };

    private static final String tweetAsRawJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\":{\"id\":\"{3}\"}" +
            "}";
    
    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

	public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData,
			TwitterKafkaStatusListener twitterKafkaStatusListener) {
		super();
		this.twitterToKafkaServiceConfigData = twitterToKafkaServiceConfigData;
		this.twitterKafkaStatusListener = twitterKafkaStatusListener;
	}

	@Override
	public void start() throws TwitterException {
		String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
		int minTweetLength = twitterToKafkaServiceConfigData.getMockMinTweetLength();
		int maxTweetLength = twitterToKafkaServiceConfigData.getMockMaxTweetLength();
		long sleepTimeMs = twitterToKafkaServiceConfigData.getMockSleepMs();
		
		LOG.info("Starting Mock filtering Twitter for keywords ", Arrays.toString(keywords));
		
		simulateTwitterStream(keywords, minTweetLength, maxTweetLength, sleepTimeMs);

	}

	private void simulateTwitterStream(String[] keywords, int minTweetLength, int maxTweetLength, long sleepTimeMs) {
		
		Executors.newSingleThreadExecutor().submit(() ->{
			try {
				while(true) {
					String formattedTwitterRawJson = getFormattedTweet(keywords, minTweetLength, maxTweetLength);
					Status status = TwitterObjectFactory.createStatus(formattedTwitterRawJson);
					twitterKafkaStatusListener.onStatus(status);
					sleep(sleepTimeMs);
				}
			}catch(TwitterException e) {
				LOG.error("Error creating twitter stream!", e);
			}
		});
		
	}

	private void sleep(long sleepTimeMs) {
		try {
			Thread.sleep(sleepTimeMs);
		} catch (InterruptedException e) {
			throw new TwitterToKafkaServiceException("Error while sleeping for waiting new status to create!!");
		}
		
	}

	private String getFormattedTweet(String[] keywords, int minTweetLength, int maxTweetLength) {
		String[] params = new String[] {
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
				String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
				getRandomTweetContent(keywords, minTweetLength, maxTweetLength),
				String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
		};
		return formatTweetAsJsonWithParams(params);
	}

	private String formatTweetAsJsonWithParams(String[] params) {
		String tweet = tweetAsRawJson;
		
		for (int i = 0; i < params.length; i++) {
			tweet = tweet.replace("{"+i+"}", params[i]);
		}
		return tweet;
	}

	private String getRandomTweetContent(String[] keywords, int minTweetLength, int maxTweetLength) {
		StringBuilder tweet = new StringBuilder();
		int tweetLenght = RANDOM.nextInt(maxTweetLength - minTweetLength + 1) + minTweetLength;
		return constructRandomTweet(keywords, tweet, tweetLenght);
	}

	private String constructRandomTweet(String[] keywords, StringBuilder tweet, int tweetLenght) {
		for (int i = 0; i < tweetLenght; i++) {
			if(i ==  tweetLenght/2) {
				tweet.append(keywords[RANDOM.nextInt(keywords.length)]).append(" ");
			}
		}
		return tweet.toString().trim();
	}

}
