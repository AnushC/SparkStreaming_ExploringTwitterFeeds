package com.nus.sparkstreaming

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._
import Utilities._

object PopularWords {
  /** Our main function where the action happens */
  def main(args: Array[String]) {
    //Makes use of Utilities.scala file
    // Configure Twitter credentials using twitter.txt
    setupTwitter()
    
    // Set up a Spark streaming context named "PopularHashtags" that runs locally using
    // all CPU cores and one-second batches of data
    val ssc = new StreamingContext("local[*]", "PopularWords", Seconds(1))
    
    // Get rid of log spam (should be called after the context is set up)
    setupLogging()

    // Create a DStream from Twitter using our streaming context
    val tweets = TwitterUtils.createStream(ssc, None)
    
    // Now extract the text of each status update into DStreams using map()
    val statuses = tweets.map(status => status.getText())
    
    // Blow out each word into a new DStream
    // We use a flat map for this(Number of outputs differ from number of inputs)
    val tweetwords = statuses.flatMap(tweetText => tweetText.split(" "))   
        
    // Map each hashtag to a key/value pair of (hashtag, 1) so we can count them up by adding up the values
    // Count them by using a reduce operation 
    val wordsKeyValues = tweetwords.map(hashtag => (hashtag, 1))
    
    // Now we count them all up by using the "key"
    // Now count them up over a 5 minute window sliding every one second
    val wordsCounts = wordsKeyValues.reduceByKeyAndWindow( (x,y) => x + y, (x,y) => x - y, Seconds(300), Seconds(1))
    //  You will often see this written in the following shorthand:
    //val hashtagCounts = hashtagKeyValues.reduceByKeyAndWindow( _ + _, _ -_, Seconds(300), Seconds(1))
    
    // Sort the results by the count values
    val sortedResults = wordsCounts.transform(rdd => rdd.sortBy(x => x._2, false))
    
    // Print the top 10
    sortedResults.print
    
    // Set a checkpoint directory, and kick it all off
    // I could watch this all day!
    ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }  
}