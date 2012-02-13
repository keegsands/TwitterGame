package com.keegsands.twittergame;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import twitter4j.Paging;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Tweet;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;

public class TwitterGame {

	public static final String AT_SIGN = "@";
	public static final String ADAM_LAMBERT = "adamlambert";
	public static final int CELEBRITY_FOLLOWER_THRESHOLD = 1000;

	public static void main(final String[] args) {
		if (args.length == 0) {
			throw new IllegalArgumentException(
					"Requires at least one argument. Correct usage java -jar TwitterGame.jar <username> -v for verbose");
		}
		// The factory instance is re-useable and thread safe.
		final Twitter twitter = new TwitterFactory().getInstance();
		final String username = args[0];
		boolean verbose = false;
		if (args.length >= 2) {
			verbose = args[1].equals("-v");
		}

		final Date twentyFourAgo = new Date(System.currentTimeMillis() - 24
				* 60 * 60 * 1000);
		try {

			processReplies(twitter, username, twentyFourAgo, verbose);
			processReturnReplies(twitter, username, twentyFourAgo, verbose);

		} catch (TwitterException t) {
			System.out.println("Trouble connecting to Twitter.");
		}

	}

	private static void processReplies(final Twitter twitter,
			final String username, final Date twentyFourAgo,
			final boolean verbose) throws TwitterException {
		final Paging paging = new Paging(1, 50);
		final List<Status> statuses = twitter.getUserTimeline(username, paging);
		final Map<String, User> users = new HashMap<String, User>();

		int totalReplyCount = 0;
		int adamReplyCount = 0;

		final List<Status> prevDays = getPreviousDaysReplyTweets(statuses,
				twentyFourAgo);
		System.out.println("Processing " + prevDays.size()
				+ " status updates from the last 24 hours.");
		for (final Status status : prevDays) {

			final String replyUsername = parseAtUsername(status);
			User currentUser = users.get(replyUsername);
			if (currentUser == null) {
				if (verbose) {
					System.out.println("Retrieving information for "
							+ replyUsername);
				}
				currentUser = twitter.showUser(replyUsername);
				users.put(replyUsername, currentUser);
			}
			if (currentUser.getFollowersCount() > CELEBRITY_FOLLOWER_THRESHOLD) {
				if (verbose) {
					System.out.println(status.getCreatedAt() + " "
							+ status.getText());
				}
				totalReplyCount++;
			}

			if (currentUser.getScreenName().equals(ADAM_LAMBERT)) {
				adamReplyCount++;
			}

		}
		System.out.println("Total Tweets @Celebrities: " + totalReplyCount);
		System.out.println("Total Tweets @adamlambert: " + adamReplyCount);

	}

	private static void processReturnReplies(final Twitter twitter,
			final String username, final Date twentyFourAgo,
			final boolean verbose) throws TwitterException {
		int miracles = 0;
		final QueryResult result = twitter
				.search(new Query(AT_SIGN + username));
		for (final Tweet tweet : result.getTweets()) {
			if (tweet.getCreatedAt().after(twentyFourAgo)) {

				final User user = twitter.showUser(tweet.getFromUser());

				if (user.getFollowersCount() > CELEBRITY_FOLLOWER_THRESHOLD) {
					miracles++;
				}
			}
		}

		System.out.println("Total Tweets @" + username + " from celebrities: "
				+ miracles);
	}

	private static String parseAtUsername(final Status status) {
		final int spaceLocation = status.getText().indexOf(' ');
		return status.getText().substring(1, spaceLocation);
	}

	private static List<Status> getPreviousDaysReplyTweets(
			List<Status> statuses, final Date twentyFourAgo) {
		final List<Status> prevDaysUpdates = new ArrayList<Status>();
		for (final Status status : statuses) {
			if (status.getCreatedAt().after(twentyFourAgo)
					&& status.getText().startsWith(AT_SIGN)) {
				prevDaysUpdates.add(status);

			}

		}
		return prevDaysUpdates;
	}
}
