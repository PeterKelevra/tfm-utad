package org.utad.tfm.ppena.core

/**
  */
object Constants {

  val MASTER_HOST = "localhost" //"sandbox"

  val ES_CLUSTER_NAME = "production"

  val ES_HTTP_PORT = "9200"

  val ES_TRANSPORT_PORT = "9300"

  val ES_RSVPS_INDEX = "meetup-rsvps/rsvps"

  val ES_GROUPS_INDEX = "meetup-groups/groups"

  val ES_EVENTS_INDEX = "meetup-events/events"

  val ES_CLUSTER_GROUPS_INDEX = "meetup-cluster/groups"

  val DEFAULT_REDIS_PORT = "6379"

  val KAFKA_PORT = "9092"

  val KAFKA_ENRICH_TOPIC = "enrich_topic"

  val KAFKA_ALERT_TOPIC = "alerts_topic"

  val KAFKA_TRENDING_TOPS_TOPIC = "trending_tops_topic"

  val MESSAGES_PER_MINUTE = "MESSAGES_PER_MINUTE"

  val DEFAULT_MESSAGES_PER_MINUTE_LIMIT = 5

  val DATE_FORMAT = "yyyy-MM-dd"

  val TOP_TRENDING_EVENTS = 5

}
