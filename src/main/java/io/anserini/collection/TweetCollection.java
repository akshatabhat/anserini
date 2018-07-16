/**
 * Anserini: An information retrieval toolkit built on Lucene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.collection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import io.anserini.util.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Class representing an instance of a Twitter collection.
 */
public class TweetCollection extends DocumentCollection
    implements FileSegmentProvider<TweetCollection.Document> {

  @Override
  public List<Path> getFileSegmentPaths() {
    return super.discover();
  }

  @Override
  public FileSegment createFileSegment(Path p) throws IOException {
    return new FileSegment(p);
  }

  public class FileSegment extends AbstractFileSegment<Document> {
    protected FileSegment(Path path) throws IOException {
      dType = new TweetCollection.Document();

      this.path = path;
      this.bufferedReader = null;
      String fileName = path.toString();
      if (fileName.endsWith(".gz")) { //.gz
        InputStream stream = new GZIPInputStream(
            Files.newInputStream(path, StandardOpenOption.READ), BUFFER_SIZE);
        bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
      } else { // plain text file
        bufferedReader = new BufferedReader(new FileReader(fileName));
      }
    }
  }

  /**
   * A Twitter document (status).
   */
  public static class Document implements SourceDocument {
    // Required fields
    protected String screenName;
    protected int followersCount;
    protected int friendsCount;
    protected int statusesCount;
    protected String createdAt;
    protected String id;
    protected long idLong;
    protected String text;
    protected TweetObject jsonObject;
    protected String jsonString;

    // Optional fields
    protected Optional<String> name;
    protected Optional<String> profileImageUrl;
    protected OptionalLong timestampMs;
    protected OptionalLong epoch;
    protected Optional<String> lang;
    protected OptionalLong inReplyToStatusId;
    protected OptionalLong inReplyToUserId;
    protected OptionalDouble latitude;
    protected OptionalDouble longitude;
    protected OptionalLong retweetStatusId;
    protected OptionalLong retweetUserId;
    protected OptionalLong retweetCount;

    //private boolean keepRetweets;

    private static final Logger LOG = LogManager.getLogger(Document.class);
    private static final String DATE_FORMAT = "E MMM dd HH:mm:ss ZZZZZ yyyy"; // "Fri Mar 29 11:03:41 +0000 2013"

    public Document() {
      super();
    }

    @Override
    public Document readNextRecord(BufferedReader reader) throws IOException {
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          if (fromJson(line)) {
            return this;
          } // else: not desired JSON data, read the next line
        }
      } catch (IOException e) {
        LOG.error("Exception from BufferedReader:", e);
      }
      return null;
    }

    public boolean fromJson(String json) {
      ObjectMapper mapper = new ObjectMapper();
      TweetObject tweetObj = null;
      try {
        tweetObj = mapper
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES) // Ignore unrecognized properties
                .registerModule(new Jdk8Module()) // Deserialize Java 8 Optional: http://www.baeldung.com/jackson-optional
                .readValue(json, TweetObject.class);
      } catch (IOException e) {
        return false;
      }

      if (JsonParser.isFieldAvailable(tweetObj.getDelete())) {
        return false;
      }

      id = tweetObj.getIdStr();
      idLong = Long.parseLong(id);
      text = tweetObj.getText();
      createdAt = tweetObj.getCreatedAt();

      try {
        timestampMs = OptionalLong.of((new SimpleDateFormat(DATE_FORMAT, Locale.ENGLISH)).parse(createdAt).getTime());
        epoch = timestampMs.isPresent() ? OptionalLong.of(timestampMs.getAsLong() / 1000) : OptionalLong.empty();
      } catch (ParseException e) {
        timestampMs = OptionalLong.of(-1L);
        epoch = OptionalLong.of(-1L);
        return false;
      }

      if (JsonParser.isFieldAvailable(tweetObj.getInReplyToStatusId())) {
        inReplyToStatusId = tweetObj.getInReplyToStatusId();
      } else {
        inReplyToStatusId = OptionalLong.empty();
      }

      if (JsonParser.isFieldAvailable(tweetObj.getInReplyToUserId())) {
        inReplyToUserId = tweetObj.getInReplyToUserId();
      } else {
        inReplyToUserId = OptionalLong.empty();
      }

      if (JsonParser.isFieldAvailable(tweetObj.getRetweetedStatus())) {
        retweetStatusId = tweetObj.getRetweetedStatus().get().getId();
        if (JsonParser.isFieldAvailable(tweetObj.getRetweetedStatus().get().getUser())) {
          retweetUserId = tweetObj.getRetweetedStatus().get().getUser().get().getId();
        } else {
          retweetUserId = OptionalLong.empty();
        }
        retweetCount = tweetObj.getRetweetCount();
      } else {
        retweetStatusId = OptionalLong.empty();
        retweetUserId = OptionalLong.empty();
        retweetCount = OptionalLong.empty();
      }

      if (JsonParser.isFieldAvailable(tweetObj.getCoordinates()) &&
          JsonParser.isFieldAvailable(tweetObj.getCoordinates().get().getCoordinates()) &&
          tweetObj.getCoordinates().get().getCoordinates().get().size() >= 2) {
        longitude = tweetObj.getCoordinates().get().getCoordinates().get().get(0);
        latitude = tweetObj.getCoordinates().get().getCoordinates().get().get(1);
      } else {
        latitude = OptionalDouble.empty();
        longitude = OptionalDouble.empty();
      }

      if (JsonParser.isFieldAvailable(tweetObj.getLang())) {
        lang = tweetObj.getLang();
      } else {
        lang = Optional.empty();
      }

      followersCount = tweetObj.getUser().getFollowersCount();
      friendsCount = tweetObj.getUser().getFriendsCount();
      statusesCount = tweetObj.getUser().getStatusesCount();
      screenName = tweetObj.getUser().getScreenName();

      if (JsonParser.isFieldAvailable(tweetObj.getUser().getName())) {
        name = tweetObj.getUser().getName();
      } else {
        name = Optional.empty();
      }

      if (JsonParser.isFieldAvailable(tweetObj.getUser().getProfileImageUrl())) {
        profileImageUrl = tweetObj.getUser().getProfileImageUrl();
      } else {
        profileImageUrl = Optional.empty();
      }

      jsonString = json;
      jsonObject = tweetObj;

      return true;
    }

    public Document fromTSV(String tsv) {
      String[] columns = tsv.split("\t");

      if (columns.length < 4) {
        System.err.println("error parsing: " + tsv);
        return null;
      }

      id = columns[0];
      idLong = Long.parseLong(columns[0]);
      screenName = columns[1];
      createdAt = columns[2];

      StringBuilder b = new StringBuilder();
      for (int i = 3; i < columns.length; i++) {
        b.append(columns[i] + " ");
      }
      text = b.toString().trim();

      return this;
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public String content() {
      return text;
    }

    @Override
    public boolean indexable() {
      return true;
    }

    public long getIdLong() {
      return idLong;
    }

    public String getScreenName() {
      return screenName;
    }

    public String getCreatedAt() {
      return createdAt;
    }

    public String getText() {
      return text;
    }

    public TweetObject getJsonObject() {
      return jsonObject;
    }

    public String getJsonString() {
      return jsonString;
    }

    public int getFollowersCount() {
      return followersCount;
    }

    public int getFriendsCount() {
      return friendsCount;
    }

    public int getStatusesCount() {
      return statusesCount;
    }

    public Optional<String> getName() {
      return name;
    }

    public Optional<String> getProfileImageURL() {
      return profileImageUrl;
    }

    public OptionalLong getTimestampMs() {
      return timestampMs;
    }

    public OptionalLong getEpoch() {
      return epoch;
    }

    public Optional<String> getLang() {
      return lang;
    }

    public OptionalLong getInReplyToStatusId() {
      return inReplyToStatusId;
    }

    public OptionalLong getInReplyToUserId() {
      return inReplyToUserId;
    }

    public OptionalDouble getlatitude() {
      return latitude;
    }

    public OptionalDouble getLongitude() {
      return longitude;
    }

    public OptionalLong getRetweetedStatusId() {
      return retweetStatusId;
    }

    public OptionalLong getRetweetedUserId() {
      return retweetUserId;
    }

    public OptionalLong getRetweetCount() {
      return retweetCount;
    }

    /**
     * A Twitter document object class used in Jackson JSON parser
     */
    public static class TweetObject {

      // Required fields
      protected String createdAt;
      protected String idStr;
      protected String text;
      protected TweetObject.User user;

      // Optional fields
      protected OptionalLong retweetCount;
      protected OptionalLong inReplyToStatusId;
      protected OptionalLong inReplyToUserId;
      protected Optional<TweetObject.RetweetedStatus> retweetedStatus;
      protected Optional<String> lang;
      protected Optional<TweetObject.Delete> delete;
      protected Optional<TweetObject.Coordinates> coordinates;

      // Must make inner classes static for deserialization in Jackson
      // http://www.cowtowncoder.com/blog/archives/2010/08/entry_411.html
      public static class Delete {
        protected Optional<String> timestampMs;

        @JsonGetter("timestamp_ms")
        public Optional<String> getTimestampMs() {
          return timestampMs;
        }
      }

      public static class Coordinates {
        protected Optional<List<OptionalDouble>> coordinates;

        @JsonGetter("coordinates")
        public Optional<List<OptionalDouble>> getCoordinates() { return coordinates; }
      }

      public static class RetweetedStatus {
        protected OptionalLong id;
        protected Optional<TweetObject.User> user;

        @JsonGetter("id")
        public OptionalLong getId() {
          return id;
        }

        @JsonGetter("user")
        public Optional<TweetObject.User> getUser() {
          return user;
        }
      }

      public static class User {
        // Required fields
        protected String screenName;
        protected int followersCount;
        protected int friendsCount;
        protected int statusesCount;

        // Opional fields
        protected Optional<String> name;
        protected Optional<String> profileImageUrl;
        protected OptionalLong id;

        @JsonCreator
        public User(
                @JsonProperty(value = "followers_count", required = true) int followersCount,
                @JsonProperty(value = "friends_count", required = true) int friendsCount,
                @JsonProperty(value = "statuses_count", required = true) int statusesCount,
                @JsonProperty(value = "screen_name", required = true) String screenName) {
          this.followersCount = followersCount;
          this.friendsCount = friendsCount;
          this.statusesCount = statusesCount;
          this.screenName = screenName;
        }

        @JsonGetter("screen_name")
        public String getScreenName() {
          return screenName;
        }

        @JsonGetter("followers_count")
        public int getFollowersCount() {
          return followersCount;
        }

        @JsonGetter("friends_count")
        public int getFriendsCount() {
          return friendsCount;
        }

        @JsonGetter("statuses_count")
        public int getStatusesCount() {
          return statusesCount;
        }

        @JsonGetter("name")
        public Optional<String> getName() {
          return name;
        }

        @JsonGetter("profile_image_url")
        public Optional<String> getProfileImageUrl() {
          return profileImageUrl;
        }

        @JsonGetter("id")
        public OptionalLong getId() {
          return id;
        }
      }

      @JsonCreator
      public TweetObject(
              @JsonProperty(value = "created_at", required = true) String createdAt,
              @JsonProperty(value = "id_str", required = true) String idStr,
              @JsonProperty(value = "text", required = true) String text,
              @JsonProperty(value = "user", required = true) TweetObject.User user) {
        this.createdAt = createdAt;
        this.idStr = idStr;
        this.text = text;
        this.user = user;
      }

      @JsonGetter("id_str")
      public String getIdStr() {
        return idStr;
      }

      @JsonGetter("text")
      public String getText() {
        return text;
      }

      @JsonGetter("user")
      public TweetObject.User getUser() {
        return user;
      }

      @JsonGetter("created_at")
      public String getCreatedAt() {
        return createdAt;
      }

      @JsonGetter("retweet_count")
      public OptionalLong getRetweetCount() {
        return retweetCount;
      }

      @JsonSetter("retweet_count")
      public void setRetweetCountInternal(JsonNode retweetCountInternal) {
        if (retweetCountInternal != null) {
          if (retweetCountInternal.isTextual()) {
            // retweet_count might say "100+"
            // TODO: This is ugly, come back and fix later.
            retweetCount = OptionalLong.of(Long.parseLong(retweetCountInternal.asText().replace("+", "")));
          } else if (retweetCountInternal.isNumber()) {
            retweetCount = OptionalLong.of(retweetCountInternal.asLong());
          }
        }
      }

      @JsonGetter("in_reply_to_status_id")
      public OptionalLong getInReplyToStatusId() {
        return inReplyToStatusId;
      }

      @JsonGetter("in_reply_to_user_id")
      public OptionalLong getInReplyToUserId() {
        return inReplyToUserId;
      }

      @JsonGetter("retweeted_status")
      public Optional<TweetObject.RetweetedStatus> getRetweetedStatus() {
        return retweetedStatus;
      }

      @JsonGetter("lang")
      public Optional<String> getLang() {
        return lang;
      }

      @JsonGetter("delete")
      public Optional<TweetObject.Delete> getDelete() {
        return delete;
      }

      @JsonGetter("coordinates")
      public Optional<TweetObject.Coordinates> getCoordinates() { return coordinates; }
    }
  }
}
